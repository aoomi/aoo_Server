package com.aoo.bcg.account.wechat;

import com.aoo.bcg.account.wechat.WeChatHttpRoutes.AuthenticatedAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;

/** Durable WeChat login/session authority backed by the canonical account/session tables. */
public final class JdbcWeChatSessionService implements WeChatHttpRoutes.LoginHandler,
        WeChatHttpRoutes.RouteAuthenticator, WeChatBindingService.AccountAuthorizer {
    private static final Duration ACCESS_TTL=Duration.ofMinutes(15),REFRESH_TTL=Duration.ofDays(30);
    private final DataSource source; private final WeChatOAuthClient oauth; private final Clock clock; private final SecureRandom random=new SecureRandom();
    public JdbcWeChatSessionService(DataSource source,WeChatOAuthClient oauth,Clock clock){this.source=Objects.requireNonNull(source);this.oauth=Objects.requireNonNull(oauth);this.clock=Objects.requireNonNull(clock);}

    @Override public Object login(String code,String device,String channel,String version,String ip,String mode){
        var ticket=oauth.exchange(code);boolean single=switch(mode){case "SINGLE_DEVICE"->true;case "MULTI_DEVICE"->false;default->throw new IllegalArgumentException("invalid loginMode");};
        return tx(c->{long account=account(c,ticket);long generation=allowedGeneration(c,account,true);Instant now=clock.instant();
            if(single){try(var p=c.prepareStatement("UPDATE aoo_account SET auth_generation=auth_generation+1,updated_at=? WHERE account_id=?")){time(p,1,now);p.setLong(2,account);p.executeUpdate();}generation++;try(var p=c.prepareStatement("UPDATE aoo_account_session SET revoked_at=?,revoke_reason='WECHAT_SINGLE_DEVICE' WHERE account_id=? AND revoked_at IS NULL")){time(p,1,now);p.setLong(2,account);p.executeUpdate();}}
            try(var p=c.prepareStatement("INSERT INTO aoo_account_device(account_id,device_id,channel_name,client_version,first_seen_at,last_seen_at) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE channel_name=VALUES(channel_name),client_version=VALUES(client_version),last_seen_at=VALUES(last_seen_at),revoked_at=NULL")){p.setLong(1,account);p.setString(2,required(device));p.setString(3,required(channel));p.setString(4,required(version));time(p,5,now);time(p,6,now);p.executeUpdate();}
            String access=token(),refresh=token(),session=UUID.randomUUID().toString(),family=UUID.randomUUID().toString();Instant ae=now.plus(ACCESS_TTL),re=now.plus(REFRESH_TTL);
            try(var p=c.prepareStatement("INSERT INTO aoo_account_session(session_id,account_id,device_id,token_family,access_hash,refresh_hash,access_expires_at,refresh_expires_at,auth_generation,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)")){p.setString(1,session);p.setLong(2,account);p.setString(3,device);p.setString(4,family);p.setString(5,hash(access));p.setString(6,hash(refresh));time(p,7,ae);time(p,8,re);p.setLong(9,generation);time(p,10,now);p.executeUpdate();}
            try(var p=c.prepareStatement("INSERT INTO aoo_account_audit(account_id,action_name,detail_value,source_ip,occurred_at) VALUES(?,'WECHAT_SESSION_ISSUED',?,?,?)")){p.setLong(1,account);p.setString(2,device);p.setString(3,required(ip));time(p,4,now);p.executeUpdate();}
            return new TokenPair(access,refresh,ae,re);
        });
    }
    @Override public AuthenticatedAccount authenticate(String bearer){return new AuthenticatedAccount(authorize(bearer));}
    @Override public boolean authorize(long accountId,String bearer){try{return authorize(bearer)==accountId;}catch(SecurityException e){return false;}}
    private long authorize(String bearer){return tx(c->{String sql="SELECT s.account_id,s.access_expires_at,s.auth_generation,a.auth_generation,a.banned_until FROM aoo_account_session s JOIN aoo_account a ON a.account_id=s.account_id WHERE s.access_hash=? AND s.revoked_at IS NULL";try(var p=c.prepareStatement(sql)){p.setString(1,hash(required(bearer)));try(var r=p.executeQuery()){if(!r.next()||!clock.instant().isBefore(r.getTimestamp(2).toInstant())||r.getLong(3)!=r.getLong(4))throw new SecurityException("invalid or expired bearer credential");Timestamp banned=r.getTimestamp(5);if(banned!=null&&clock.instant().isBefore(banned.toInstant()))throw new SecurityException("account unavailable");return r.getLong(1);}}});}
    private long account(Connection c,WeChatOAuthClient.Ticket t)throws SQLException{String sql="SELECT DISTINCT account_id FROM aoo_account_identity WHERE status='ACTIVE' AND verified=TRUE AND ((identity_type='WECHAT_OPEN' AND value_hash=?) OR (identity_type='WECHAT_UNION' AND value_hash=?)) FOR UPDATE";try(var p=c.prepareStatement(sql)){p.setString(1,hash(oauth.appId()+":"+t.openId()));p.setString(2,hash(t.unionId()==null?"":t.unionId()));try(var r=p.executeQuery()){if(!r.next())throw new SecurityException("WeChat identity is not bound");long id=r.getLong(1);if(r.next())throw new IllegalStateException("ambiguous WeChat identity conflict");return id;}}}
    private long allowedGeneration(Connection c,long id,boolean lock)throws SQLException{try(var p=c.prepareStatement("SELECT auth_generation,banned_until FROM aoo_account WHERE account_id=?"+(lock?" FOR UPDATE":""))){p.setLong(1,id);try(var r=p.executeQuery()){if(!r.next())throw new SecurityException("account unavailable");Timestamp b=r.getTimestamp(2);if(b!=null&&clock.instant().isBefore(b.toInstant()))throw new SecurityException("account unavailable");return r.getLong(1);}}}
    private <T>T tx(Work<T>w){try(Connection c=source.getConnection()){c.setAutoCommit(false);try{T v=w.run(c);c.commit();return v;}catch(Exception e){c.rollback();if(e instanceof RuntimeException x)throw x;throw new IllegalStateException("WeChat account persistence failed",e);}}catch(SQLException e){throw new IllegalStateException("WeChat account persistence unavailable",e);}}
    private String token(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}private static String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}private static String required(String v){if(v==null||v.isBlank())throw new IllegalArgumentException("required value missing");return v;}private static void time(PreparedStatement p,int i,Instant v)throws SQLException{p.setTimestamp(i,Timestamp.from(v));}
    @FunctionalInterface private interface Work<T>{T run(Connection c)throws Exception;}
    public record TokenPair(String accessToken,String refreshToken,Instant accessExpiresAt,Instant refreshExpiresAt){}
}
