package core.replay;

import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

/** JDBC authority for stable short-code mappings and possession-based access grants. */
public final class JdbcReplayCodeRepository implements ReplayCodeRepository {
    private final DataSource source;
    public JdbcReplayCodeRepository(DataSource source) { this.source=java.util.Objects.requireNonNull(source); }
    @Override public Optional<Mapping> findShortCode(String code) {
        String sql="SELECT short_code,room_id,set_id,status,expires_at FROM replay_short_code WHERE short_code=?";
        try(var c=source.getConnection();var q=c.prepareStatement(sql)){q.setString(1,code);try(var r=q.executeQuery()){if(!r.next())return Optional.empty();var expiry=r.getTimestamp(5);return Optional.of(new Mapping(r.getString(1),r.getLong(2),r.getInt(3),r.getString(4),expiry==null?null:expiry.toInstant()));}}catch(Exception e){throw failure("find short replay code",e);}
    }
    @Override public Optional<Mapping> findByTarget(long roomId,int setId) {
        String sql="SELECT short_code,room_id,set_id,status,expires_at FROM replay_short_code WHERE room_id=? AND set_id=?";
        try(var c=source.getConnection();var q=c.prepareStatement(sql)){q.setLong(1,roomId);q.setInt(2,setId);try(var r=q.executeQuery()){if(!r.next())return Optional.empty();var expiry=r.getTimestamp(5);return Optional.of(new Mapping(r.getString(1),r.getLong(2),r.getInt(3),r.getString(4),expiry==null?null:expiry.toInstant()));}}catch(Exception e){throw failure("find replay target",e);}
    }
    @Override public Optional<LegacyTarget> findLegacyCode(String code) {
        try(var c=source.getConnection();var q=c.prepareStatement("SELECT roomID,setID,gameType FROM PlayerPlayBack WHERE playBackCode=? ORDER BY id DESC LIMIT 1")){q.setLong(1,Long.parseLong(code));try(var r=q.executeQuery()){return r.next()?Optional.of(new LegacyTarget(code,r.getLong(1),r.getInt(2),r.getInt(3))):Optional.empty();}}catch(Exception e){throw failure("find legacy replay code",e);}
    }
    @Override public boolean replayReady(long roomId,int setId) {
        try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM replay_set_manifest WHERE room_id=? AND set_id=? LIMIT 1")){q.setLong(1,roomId);q.setInt(2,setId);try(var r=q.executeQuery()){return r.next();}}catch(Exception e){throw failure("check replay readiness",e);}
    }
    @Override public boolean mayView(long playerId,long roomId,int setId) {
        String sql="SELECT 1 FROM (SELECT room_id,set_id,player_id FROM replay_participant UNION ALL SELECT room_id,set_id,player_id FROM replay_participant_archive UNION ALL SELECT c.room_id,c.set_id,a.player_id FROM replay_short_code_access a JOIN replay_short_code c ON c.short_code=a.short_code) p WHERE player_id=? AND room_id=? AND set_id=? LIMIT 1";
        try(var c=source.getConnection();var q=c.prepareStatement(sql)){q.setLong(1,playerId);q.setLong(2,roomId);q.setInt(3,setId);try(var r=q.executeQuery()){return r.next();}}catch(Exception e){throw failure("authorize replay",e);}
    }
    @Override public void grantCodeAccess(String code,long playerId) {
        try(var c=source.getConnection();var q=c.prepareStatement("INSERT INTO replay_short_code_access(short_code,player_id,granted_at) VALUES(?,?,CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE granted_at=granted_at")){q.setString(1,code);q.setLong(2,playerId);q.executeUpdate();}catch(Exception e){throw failure("grant replay code access",e);}
    }
    @Override public long allocatedCount(int length) {
        try(var c=source.getConnection();var q=c.prepareStatement("SELECT COUNT(*) FROM replay_short_code WHERE code_length=?")){q.setInt(1,length);try(var r=q.executeQuery()){r.next();return r.getLong(1);}}catch(Exception e){throw failure("count replay codes",e);}
    }
    @Override public InsertResult insert(String code,long roomId,int setId) {
        try(var c=source.getConnection();var q=c.prepareStatement("INSERT INTO replay_short_code(short_code,code_length,room_id,set_id,status,created_at) VALUES(?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP(3))")){q.setString(1,code);q.setInt(2,code.length());q.setLong(3,roomId);q.setInt(4,setId);q.executeUpdate();return InsertResult.INSERTED;}catch(SQLException e){if(!"23000".equals(e.getSQLState()))throw failure("insert replay code",e);return findByTarget(roomId,setId).isPresent()?InsertResult.TARGET_EXISTS:InsertResult.CODE_COLLISION;}catch(Exception e){throw failure("insert replay code",e);}
    }
    private static IllegalStateException failure(String action,Exception e){return new IllegalStateException("cannot "+action,e);}
}
