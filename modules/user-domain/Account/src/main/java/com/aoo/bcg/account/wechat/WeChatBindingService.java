package com.aoo.bcg.account.wechat;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Authenticated, versioned WeChat binding orchestration; persistence is supplied by the application. */
public final class WeChatBindingService {
    public static final String API_VERSION = "v1";
    private final WeChatOAuthClient oauth;
    private final Repository repository;
    private final AccountAuthorizer authorizer;
    private final Clock clock;

    public WeChatBindingService(WeChatOAuthClient oauth, Repository repository, AccountAuthorizer authorizer, Clock clock) {
        this.oauth=Objects.requireNonNull(oauth,"oauth");this.repository=Objects.requireNonNull(repository,"repository");
        this.authorizer=Objects.requireNonNull(authorizer,"authorizer");this.clock=Objects.requireNonNull(clock,"clock");
    }

    public Binding bind(long accountId, String accessToken, String code, String requestId) {
        authorize(accountId, accessToken); requestId=text(requestId,"requestId");
        WeChatOAuthClient.Ticket ticket=oauth.exchange(code);
        Binding candidate=new Binding(accountId,oauth.appId(),ticket.openId(),ticket.unionId(),1,clock.instant());
        Claim result=repository.claim(candidate);
        if(result.status()==ClaimStatus.IDENTITY_OWNED_BY_ANOTHER_ACCOUNT)throw new BindingException(ErrorCode.IDENTITY_ALREADY_BOUND,"WeChat identity is already bound");
        if(result.status()==ClaimStatus.ACCOUNT_BOUND_TO_ANOTHER_IDENTITY)throw new BindingException(ErrorCode.ACCOUNT_ALREADY_BOUND,"account already has a different WeChat identity");
        return Objects.requireNonNull(result.binding(),"repository binding");
    }

    /** The identity is loaded server-side; callers cannot select another openid to unbind. */
    public boolean unbind(long accountId, String accessToken, long expectedVersion, String requestId) {
        authorize(accountId,accessToken);text(requestId,"requestId");
        if(expectedVersion<1)throw new IllegalArgumentException("expectedVersion must be positive");
        Optional<Binding> current=repository.findByAccountId(accountId);
        if(current.isEmpty())return false;
        if(current.get().version()!=expectedVersion)throw new BindingException(ErrorCode.VERSION_CONFLICT,"stale binding version");
        if(!repository.delete(accountId,current.get().subject(),expectedVersion)) {
            Optional<Binding> after=repository.findByAccountId(accountId);
            if(after.isEmpty())return false;
            throw new BindingException(ErrorCode.VERSION_CONFLICT,"binding changed concurrently");
        }
        return true;
    }

    public Optional<Binding> binding(long accountId, String accessToken) { authorize(accountId,accessToken);return repository.findByAccountId(accountId); }
    private void authorize(long accountId,String token){if(accountId<=0)throw new IllegalArgumentException("accountId must be positive");if(!authorizer.authorize(accountId,text(token,"accessToken")))throw new BindingException(ErrorCode.UNAUTHORIZED,"account authorization failed");}
    private static String text(String v,String n){if(v==null||v.isBlank())throw new IllegalArgumentException(n+" is blank");return v.strip();}

    public record Binding(long accountId,String appId,String subject,String unionId,long version,Instant boundAt){public Binding{if(accountId<=0||version<=0)throw new IllegalArgumentException("invalid binding");text(appId,"appId");text(subject,"subject");Objects.requireNonNull(boundAt,"boundAt");}}
    public record Claim(ClaimStatus status,Binding binding){public Claim{Objects.requireNonNull(status,"status");}}
    public enum ClaimStatus { CREATED, IDEMPOTENT, IDENTITY_OWNED_BY_ANOTHER_ACCOUNT, ACCOUNT_BOUND_TO_ANOTHER_IDENTITY }
    public enum ErrorCode { UNAUTHORIZED, IDENTITY_ALREADY_BOUND, ACCOUNT_ALREADY_BOUND, VERSION_CONFLICT }
    public static final class BindingException extends SecurityException {private final ErrorCode code;public BindingException(ErrorCode code,String message){super(message);this.code=Objects.requireNonNull(code);}public ErrorCode code(){return code;}}
    @FunctionalInterface public interface AccountAuthorizer { boolean authorize(long accountId,String accessToken); }
    public interface Repository {
        Optional<Binding> findByAccountId(long accountId);
        /** Atomically enforces unique accountId and unique subject. Same request/identity must be idempotent. */
        Claim claim(Binding candidate);
        /** Compare-and-delete using all identity/version fields. */
        boolean delete(long accountId,String subject,long expectedVersion);
    }
}
