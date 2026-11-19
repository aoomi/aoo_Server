package com.aoo.bcg.account.wechat;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WeChatOAuthAndBindingTest {
    private static final Instant NOW=Instant.parse("2026-08-24T00:00:00Z");

    @Test void exchangesRealProtocolFieldsAndUsesUnionIdAsStableSubject(){
        var oauth=client((uri,timeout)->{
            assertEquals("api.weixin.qq.com",uri.getHost());assertTrue(uri.getQuery().contains("grant_type=authorization_code"));
            assertTrue(uri.getQuery().contains("appid=wx-app"));assertTrue(uri.getQuery().contains("code=single-use"));
            return new WeChatOAuthClient.Response(200,"{\"access_token\":\"TOKEN\",\"expires_in\":7200,\"refresh_token\":\"REFRESH\",\"openid\":\"OPEN\",\"scope\":\"snsapi_userinfo\",\"unionid\":\"UNION\"}");
        });
        var ticket=oauth.exchange("single-use");assertEquals("unionid:UNION",ticket.subject());assertEquals(7200,ticket.expiresInSeconds());
    }

    @Test void mapsProviderErrorsWithoutLeakingConfiguredSecret(){
        var oauth=client((uri,timeout)->new WeChatOAuthClient.Response(200,"{\"errcode\":40029,\"errmsg\":\"invalid code\"}"));
        var failure=assertThrows(WeChatProtocolException.class,()->oauth.exchange("bad"));
        assertEquals(WeChatProtocolException.Code.INVALID_CODE,failure.code());assertFalse(failure.getMessage().contains("very-secret"));
    }

    @Test void retriesTransientGetsAndOpensCircuitAfterBoundedFailures(){
        var calls=new AtomicInteger();var fixed=Clock.fixed(NOW,ZoneOffset.UTC);
        var oauth=new WeChatOAuthClient("wx-app","very-secret-value".toCharArray(),WeChatOAuthClient.DEFAULT_ENDPOINT,Duration.ofSeconds(2),(uri,timeout)->{calls.incrementAndGet();return new WeChatOAuthClient.Response(503,"busy");},new WeChatOAuthClient.RetryPolicy(2,Duration.ZERO,1,Duration.ofSeconds(30)),fixed,millis->{});
        assertThrows(WeChatProtocolException.class,()->oauth.exchange("code-one"));assertEquals(2,calls.get());
        var open=assertThrows(WeChatProtocolException.class,()->oauth.exchange("code-two"));assertEquals(WeChatProtocolException.Code.PROVIDER_UNAVAILABLE,open.code());assertEquals(2,calls.get());
    }

    @Test void bindingIsAuthorizedUniqueVersionedAndIdempotent(){
        Repository repo=new Repository();
        var oauth=client((uri,timeout)->new WeChatOAuthClient.Response(200,"{\"access_token\":\"TOKEN\",\"expires_in\":7200,\"openid\":\"OPEN\",\"unionid\":\"UNION\"}"));
        var service=new WeChatBindingService(oauth,repo,(account,token)->token.equals("access-"+account),Clock.fixed(NOW,ZoneOffset.UTC));
        assertEquals(WeChatBindingService.ErrorCode.UNAUTHORIZED,assertThrows(WeChatBindingService.BindingException.class,()->service.bind(7,"wrong","code","request-1")).code());
        var first=service.bind(7,"access-7","code","request-1");var repeated=service.bind(7,"access-7","code","request-1");assertEquals(first,repeated);
        assertEquals(WeChatBindingService.ErrorCode.IDENTITY_ALREADY_BOUND,assertThrows(WeChatBindingService.BindingException.class,()->service.bind(8,"access-8","code","request-2")).code());
        assertEquals(WeChatBindingService.ErrorCode.VERSION_CONFLICT,assertThrows(WeChatBindingService.BindingException.class,()->service.unbind(7,"access-7",2,"request-3")).code());
        assertTrue(service.unbind(7,"access-7",1,"request-3"));assertFalse(service.unbind(7,"access-7",1,"request-3"));
    }

    private static WeChatOAuthClient client(WeChatOAuthClient.Transport transport){return new WeChatOAuthClient("wx-app","very-secret-value".toCharArray(),WeChatOAuthClient.DEFAULT_ENDPOINT,Duration.ofSeconds(2),transport);}
    private static final class Repository implements WeChatBindingService.Repository{
        final Map<Long,WeChatBindingService.Binding> byAccount=new HashMap<>();final Map<String,Long> owners=new HashMap<>();
        public synchronized Optional<WeChatBindingService.Binding> findByAccountId(long id){return Optional.ofNullable(byAccount.get(id));}
        public synchronized WeChatBindingService.Claim claim(WeChatBindingService.Binding c){var existing=byAccount.get(c.accountId());if(existing!=null)return existing.subject().equals(c.subject())?new WeChatBindingService.Claim(WeChatBindingService.ClaimStatus.IDEMPOTENT,existing):new WeChatBindingService.Claim(WeChatBindingService.ClaimStatus.ACCOUNT_BOUND_TO_ANOTHER_IDENTITY,existing);Long owner=owners.get(c.subject());if(owner!=null&&owner!=c.accountId())return new WeChatBindingService.Claim(WeChatBindingService.ClaimStatus.IDENTITY_OWNED_BY_ANOTHER_ACCOUNT,null);byAccount.put(c.accountId(),c);owners.put(c.subject(),c.accountId());return new WeChatBindingService.Claim(WeChatBindingService.ClaimStatus.CREATED,c);}
        public synchronized boolean delete(long id,String subject,long version){var value=byAccount.get(id);if(value==null||!value.subject().equals(subject)||value.version()!=version)return false;byAccount.remove(id);owners.remove(subject,id);return true;}
    }
}
