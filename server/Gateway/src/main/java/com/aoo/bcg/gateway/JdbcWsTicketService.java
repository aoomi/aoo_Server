package com.aoo.bcg.gateway;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Durable ticket authority. Issuance and one-time consumption are database-atomic across Gateway nodes. */
public final class JdbcWsTicketService {
    public static final Duration TTL = Duration.ofSeconds(30);
    private final DataSource source; private final Clock clock; private final SecureRandom random; private final RiskAdmissionAuthority risk;
    public JdbcWsTicketService(DataSource source, Clock clock) { this(source, clock, new SecureRandom(),new JdbcRiskAdmissionAuthority(source,clock)); }
    JdbcWsTicketService(DataSource source, Clock clock, SecureRandom random) { this(source,clock,random,new JdbcRiskAdmissionAuthority(source,clock)); }
    JdbcWsTicketService(DataSource source,Clock clock,SecureRandom random,RiskAdmissionAuthority risk) { this.source=Objects.requireNonNull(source);this.clock=Objects.requireNonNull(clock);this.random=Objects.requireNonNull(random);this.risk=Objects.requireNonNull(risk); }

    public Issued issue(String accessToken,String device,String origin,String pageInstanceId,String idempotencyKey) {
        require(accessToken,"access token");require(device,"device");require(origin,"origin");require(pageInstanceId,"page instance");require(idempotencyKey,"idempotency key");
        if(idempotencyKey.length()>128)throw new IllegalArgumentException("idempotency key too long");
        return tx(c->{Principal p=authorize(c,accessToken,device);if(!risk.decide(RiskAdmissionAuthority.Action.LOGIN,p.accountId,device,null,idempotencyKey).allowed())throw new RiskAdmissionException();Instant now=clock.instant(), expiry=now.plus(TTL);String raw=token();
            try(PreparedStatement q=c.prepareStatement("INSERT INTO gateway_ws_ticket(ticket_hash,account_id,session_id,device_id,allowed_origin,idempotency_key,expires_at,issued_at) VALUES(?,?,?,?,?,?,?,?)")){
                q.setString(1,hash(raw));q.setLong(2,p.accountId);q.setString(3,p.sessionId);q.setString(4,device);q.setString(5,origin);q.setString(6,idempotencyKey);time(q,7,expiry);time(q,8,now);q.executeUpdate();
            }catch(SQLIntegrityConstraintViolationException duplicate){throw new GatewayTicketException(GatewayErrorCode.IDEMPOTENCY_CONFLICT);}
            return new Issued(raw,expiry,p.accountId,device,origin,pageInstanceId);
        });
    }

    public ConnectionIdentity consume(String raw,String device,String origin,String pageInstanceId) {
        require(raw,"ticket");require(device,"device");require(origin,"origin");require(pageInstanceId,"page instance");
        return tx(c->{Ticket t;
            try(PreparedStatement q=c.prepareStatement("SELECT t.account_id,t.device_id,t.allowed_origin,t.expires_at,t.session_id,s.auth_generation FROM gateway_ws_ticket t JOIN aoo_account_session s ON s.session_id=t.session_id JOIN aoo_account a ON a.account_id=t.account_id WHERE t.ticket_hash=? AND s.revoked_at IS NULL AND s.auth_generation=a.auth_generation FOR UPDATE")){
                q.setString(1,hash(raw));try(ResultSet r=q.executeQuery()){if(!r.next())throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_REJECTED);t=new Ticket(r.getLong(1),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),r.getString(5),r.getLong(6));}
            }
            if(!clock.instant().isBefore(t.expiry)) { delete(c,raw); throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_EXPIRED); }
            if(!MessageDigest.isEqual(t.device.getBytes(StandardCharsets.UTF_8),device.getBytes(StandardCharsets.UTF_8)) || !t.origin.equals(origin)) throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_REJECTED);
            delete(c,raw);return new ConnectionIdentity(t.accountId,t.device,t.origin,t.sessionId,t.authGeneration,pageInstanceId);
        });
    }
    /** Browser WebSocket upgrades cannot set a device header. The one-time ticket is already device-bound at issuance. */
    public ConnectionIdentity consume(String raw,String origin,String pageInstanceId) {
        require(raw,"ticket");require(origin,"origin");require(pageInstanceId,"page instance");
        return tx(c->{Ticket t=null;
            try(PreparedStatement q=c.prepareStatement("SELECT t.account_id,t.device_id,t.allowed_origin,t.expires_at,t.session_id,s.auth_generation FROM gateway_ws_ticket t JOIN aoo_account_session s ON s.session_id=t.session_id JOIN aoo_account a ON a.account_id=t.account_id JOIN aoo_account_device d ON d.account_id=t.account_id AND d.device_id=t.device_id WHERE t.ticket_hash=? AND s.revoked_at IS NULL AND d.revoked_at IS NULL AND s.auth_generation=a.auth_generation AND s.access_expires_at>? AND (a.banned_until IS NULL OR a.banned_until<=?) FOR UPDATE")){
                q.setString(1,hash(raw));time(q,2,clock.instant());time(q,3,clock.instant());try(ResultSet r=q.executeQuery()){if(r.next())t=new Ticket(r.getLong(1),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),r.getString(5),r.getLong(6));}
            }
            if(t==null)return consumeHallTicket(c,raw,origin,pageInstanceId);
            if(!clock.instant().isBefore(t.expiry)){delete(c,raw);throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_EXPIRED);}
            if(!t.origin.equals(origin))throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_REJECTED);
            delete(c,raw);return new ConnectionIdentity(t.accountId,t.device,t.origin,t.sessionId,t.authGeneration,pageInstanceId);
        });
    }
    private ConnectionIdentity consumeHallTicket(Connection c,String raw,String origin,String pageInstanceId)throws SQLException{
        try(PreparedStatement q=c.prepareStatement("SELECT t.room_id,t.account_id,t.device_fingerprint,t.expires_at,t.consumed_at,s.session_id,s.auth_generation FROM aoo_hall_game_ticket t JOIN aoo_hall_room_member m ON m.room_id=t.room_id AND m.account_id=t.account_id JOIN aoo_room_authority_route a ON a.room_id=t.room_id JOIN aoo_account ac ON ac.account_id=t.account_id JOIN aoo_account_device d ON d.account_id=t.account_id AND d.device_id=t.device_fingerprint JOIN aoo_account_session s ON s.account_id=t.account_id AND s.device_id=t.device_fingerprint AND s.auth_generation=ac.auth_generation AND s.revoked_at IS NULL AND s.access_expires_at>CURRENT_TIMESTAMP(3) WHERE t.ticket_hash=SHA2(?,256) AND m.status='JOINED' AND a.lifecycle_state='ACTIVE' AND a.lease_expires_at>CURRENT_TIMESTAMP(3) AND d.revoked_at IS NULL AND (ac.banned_until IS NULL OR ac.banned_until<=CURRENT_TIMESTAMP(3)) FOR UPDATE")){
            q.setString(1,raw);try(ResultSet r=q.executeQuery()){if(!r.next()||r.getTimestamp(5)!=null)throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_REJECTED);if(!clock.instant().isBefore(r.getTimestamp(4).toInstant()))throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_EXPIRED);try(PreparedStatement u=c.prepareStatement("UPDATE aoo_hall_game_ticket SET consumed_at=CURRENT_TIMESTAMP(3) WHERE ticket_hash=SHA2(?,256) AND consumed_at IS NULL")){u.setString(1,raw);if(u.executeUpdate()!=1)throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_REJECTED);}return new ConnectionIdentity(r.getLong(2),r.getString(3),origin,r.getString(6),r.getLong(7),pageInstanceId);}
        }catch(SQLException absent){if("42S02".equals(absent.getSQLState())||"42102".equals(absent.getSQLState()))throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_REJECTED);throw absent;}
    }
    private Principal authorize(Connection c,String raw,String device)throws SQLException{
        try(PreparedStatement q=c.prepareStatement("SELECT s.account_id,s.session_id,s.device_id,s.access_expires_at,s.auth_generation,a.auth_generation,a.banned_until,s.revoked_at,d.revoked_at FROM aoo_account_session s JOIN aoo_account a ON a.account_id=s.account_id JOIN aoo_account_device d ON d.account_id=s.account_id AND d.device_id=s.device_id WHERE s.access_hash=?")){
            q.setString(1,hash(raw));try(ResultSet r=q.executeQuery()){
                if(!r.next())throw rejected("access-token-not-found");
                if(r.getTimestamp(8)!=null)throw rejected("session-revoked");
                if(r.getTimestamp(9)!=null)throw rejected("device-revoked");
                if(!device.equals(r.getString(3)))throw rejected("device-mismatch");
                if(r.getLong(5)!=r.getLong(6))throw rejected("auth-generation-mismatch");
                if(!clock.instant().isBefore(r.getTimestamp(4).toInstant()))throw rejected("access-token-expired");
                if(r.getTimestamp(7)!=null&&clock.instant().isBefore(r.getTimestamp(7).toInstant()))throw rejected("account-banned");
                return new Principal(r.getLong(1),r.getString(2));
            }
        }
    }
    private static GatewayTicketException rejected(String reason){return new TicketAuthorizationException(reason);}
    private void delete(Connection c,String raw)throws SQLException{try(PreparedStatement d=c.prepareStatement("DELETE FROM gateway_ws_ticket WHERE ticket_hash=?")){d.setString(1,hash(raw));if(d.executeUpdate()!=1)throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_REJECTED);}}
    private <T>T tx(Sql<T> work){try(Connection c=source.getConnection()){boolean old=c.getAutoCommit();c.setAutoCommit(false);try{T out=work.run(c);c.commit();return out;}catch(Exception e){c.rollback();if(e instanceof RuntimeException r)throw r;throw new IllegalStateException("gateway ticket persistence failed",e);}finally{c.setAutoCommit(old);}}catch(SQLException e){throw new IllegalStateException("gateway ticket persistence unavailable",e);}}
    private String token(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static void require(String v,String n){if(v==null||v.isBlank())throw new IllegalArgumentException("invalid "+n);}
    private static void time(PreparedStatement p,int i,Instant v)throws SQLException{p.setTimestamp(i,Timestamp.from(v));}
    @FunctionalInterface private interface Sql<T>{T run(Connection c)throws Exception;} private record Principal(long accountId,String sessionId){} private record Ticket(long accountId,String device,String origin,Instant expiry,String sessionId,long authGeneration){}
    static final class TicketAuthorizationException extends GatewayTicketException { private final String reason; TicketAuthorizationException(String reason){super(GatewayErrorCode.UNAUTHORIZED);this.reason=reason;} String reason(){return reason;} }
    public record Issued(String ticket,Instant expiresAt,long accountId,String deviceId,String origin,String pageInstanceId){}
}
