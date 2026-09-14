package com.aoo.bcg.gateway;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Durable risk decision authority combining bans, device, location and frequency evidence. */
public final class JdbcRiskAdmissionAuthority implements RiskAdmissionAuthority {
    private final DataSource source; private final Clock clock;
    public JdbcRiskAdmissionAuthority(DataSource source, Clock clock) { this.source=Objects.requireNonNull(source);this.clock=Objects.requireNonNull(clock); }

    @Override public Decision decide(Action action,long playerId,String deviceId,Long roomId,String requestId) {
        if(playerId<=0||action==null||requestId==null||requestId.isBlank())throw new IllegalArgumentException("invalid admission context");
        if(action!=Action.LOGIN&&(roomId==null||roomId<=0))throw new IllegalArgumentException("roomId required");
        Instant now=clock.instant();String deviceHash=hash(deviceId);try(Connection c=source.getConnection()){
            c.setAutoCommit(false);try{
                List<String> reasons=new ArrayList<>();int score=0;
                long accountId=resolveAccountId(c,playerId);
                if(accountBanned(c,accountId,now)){score=100;reasons.add("ACCOUNT_BAN");}
                if(deviceRevoked(c,accountId,deviceId)){score=100;reasons.add("DEVICE_BAN");}
                if(activeRisk(c,playerId,roomId,now)){score=Math.max(score,80);reasons.add("ACTIVE_RISK");}
                long recent=recentAttempts(c,action,playerId,now);
                int limit=switch(action){case LOGIN->20;case CREATE_ROOM->10;case JOIN_ROOM->30;};
                if(recent>=limit){score=Math.max(score,75);reasons.add("FREQUENCY");}
                if(roomId!=null&&deviceHash!=null&&sharedDevice(c,playerId,roomId,deviceHash,now)){score=Math.max(score,90);reasons.add("SHARED_DEVICE");}
                if(roomId!=null&&sharedLocation(c,playerId,roomId,now)){score=Math.max(score,70);reasons.add("SHARED_LOCATION");}
                boolean allowed=score<70;
                record(c,action,playerId,deviceHash,roomId,requestId,allowed,score,reasons,now);
                c.commit();return allowed?Decision.allow():Decision.reject();
            }catch(Exception failure){c.rollback();throw failure;}finally{c.setAutoCommit(true);}
        }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("risk admission authority unavailable",e);}
    }

    private static boolean accountBanned(Connection c,long id,Instant now)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT banned_until FROM aoo_account WHERE account_id=?")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())return true;Timestamp t=r.getTimestamp(1);return t!=null&&now.isBefore(t.toInstant());}}}
    private static long resolveAccountId(Connection c,long playerId)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT account_id FROM db_player WHERE id=?")){p.setLong(1,playerId);try(ResultSet r=p.executeQuery()){if(r.next()&&r.getLong(1)>0)return r.getLong(1);}}catch(SQLException missingLegacyMapping){if(!"42S02".equals(missingLegacyMapping.getSQLState())&&!"42102".equals(missingLegacyMapping.getSQLState()))throw missingLegacyMapping;}return playerId;}
    private static boolean deviceRevoked(Connection c,long id,String device)throws SQLException{if(device==null||device.isBlank())return false;try(PreparedStatement p=c.prepareStatement("SELECT revoked_at FROM aoo_account_device WHERE account_id=? AND device_id=?")){p.setLong(1,id);p.setString(2,device);try(ResultSet r=p.executeQuery()){return !r.next()||r.getTimestamp(1)!=null;}}}
    private static boolean activeRisk(Connection c,long id,Long room,Instant now)throws SQLException{String sql="SELECT 1 FROM risk_event WHERE player_id=? AND status IN ('PENDING_REVIEW','CONFIRMED') AND risk_score>=70 AND created_at>=? AND (room_id IS NULL OR ? IS NULL OR room_id=?) LIMIT 1";try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,id);p.setTimestamp(2,Timestamp.from(now.minus(Duration.ofDays(7))));if(room==null){p.setNull(3,Types.BIGINT);p.setNull(4,Types.BIGINT);}else{p.setLong(3,room);p.setLong(4,room);}try(ResultSet r=p.executeQuery()){return r.next();}}}
    private static long recentAttempts(Connection c,Action action,long id,Instant now)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM risk_admission_decision WHERE action_name=? AND player_id=? AND decided_at>=?")){p.setString(1,action.name());p.setLong(2,id);p.setTimestamp(3,Timestamp.from(now.minusSeconds(60)));try(ResultSet r=p.executeQuery()){r.next();return r.getLong(1);}}}
    private static boolean sharedDevice(Connection c,long id,long room,String hash,Instant now)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT COUNT(DISTINCT player_id) FROM telemetry_signal WHERE room_id=? AND device_hash=? AND player_id<>? AND received_at>=?")){p.setLong(1,room);p.setString(2,hash);p.setLong(3,id);p.setTimestamp(4,Timestamp.from(now.minus(Duration.ofHours(24))));try(ResultSet r=p.executeQuery()){r.next();return r.getLong(1)>0;}}}
    private static boolean sharedLocation(Connection c,long id,long room,Instant now)throws SQLException{String sql="SELECT 1 FROM telemetry_signal mine JOIN telemetry_signal other ON other.room_id=mine.room_id AND other.geo_cell=mine.geo_cell AND other.player_id<>mine.player_id WHERE mine.room_id=? AND mine.player_id=? AND mine.geo_cell IS NOT NULL AND mine.received_at>=? AND other.received_at>=? LIMIT 1";try(PreparedStatement p=c.prepareStatement(sql)){Timestamp since=Timestamp.from(now.minus(Duration.ofHours(24)));p.setLong(1,room);p.setLong(2,id);p.setTimestamp(3,since);p.setTimestamp(4,since);try(ResultSet r=p.executeQuery()){return r.next();}}}
    private static void record(Connection c,Action action,long id,String hash,Long room,String request,boolean allowed,int score,List<String> reasons,Instant now)throws SQLException{try(PreparedStatement p=c.prepareStatement("INSERT INTO risk_admission_decision(request_id,action_name,player_id,device_hash,room_id,decision_code,risk_score,reason_codes,decided_at) VALUES(?,?,?,?,?,?,?,?,?)")){p.setString(1,request);p.setString(2,action.name());p.setLong(3,id);p.setString(4,hash);if(room==null)p.setNull(5,Types.BIGINT);else p.setLong(5,room);p.setString(6,allowed?"ALLOW":"REJECT");p.setInt(7,score);p.setString(8,String.join(",",reasons));p.setTimestamp(9,Timestamp.from(now));p.executeUpdate();}catch(SQLIntegrityConstraintViolationException duplicate){throw new GatewayTicketException(GatewayErrorCode.IDEMPOTENCY_CONFLICT);}}
    private static String hash(String value){if(value==null||value.isBlank())return null;try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
