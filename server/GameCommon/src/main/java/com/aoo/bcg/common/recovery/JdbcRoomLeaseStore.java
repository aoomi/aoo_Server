package com.aoo.bcg.common.recovery;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;

public final class JdbcRoomLeaseStore implements RoomLeaseStore {
    private final DataSource dataSource;
    private final Clock clock;
    public JdbcRoomLeaseStore(DataSource dataSource, Clock clock) { this.dataSource=java.util.Objects.requireNonNull(dataSource,"dataSource"); this.clock=java.util.Objects.requireNonNull(clock,"clock"); }
    @Override public RoomLease acquire(long roomId, String nodeId, Duration ttl) {
        if (roomId <= 0 || nodeId == null || nodeId.isBlank() || ttl == null || ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("invalid room lease");
        var now=clock.instant(); var expires=now.plus(ttl);
        try (var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var select=connection.prepareStatement("SELECT owner_node,fencing_token,expires_at FROM aoo_room_lease WHERE room_id=? FOR UPDATE")) {
                select.setLong(1,roomId); try(var result=select.executeQuery()) {
                    long token=1;
                    if(result.next()) { if(result.getTimestamp(3).toInstant().isAfter(now) && !result.getString(1).equals(nodeId)) throw new IllegalStateException("room lease held by another node"); token=Math.incrementExact(result.getLong(2)); }
                    try(var write=connection.prepareStatement("INSERT INTO aoo_room_lease(room_id,owner_node,fencing_token,expires_at) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE owner_node=VALUES(owner_node),fencing_token=VALUES(fencing_token),expires_at=VALUES(expires_at)")) { write.setLong(1,roomId); write.setString(2,nodeId); write.setLong(3,token); write.setTimestamp(4,Timestamp.from(expires)); write.executeUpdate(); }
                    connection.commit(); return new RoomLease(roomId,nodeId,token,expires);
                }
            } catch (RuntimeException exception) { connection.rollback(); throw exception; }
        } catch (java.sql.SQLException exception) { throw new IllegalStateException("cannot acquire room lease",exception); }
    }
    @Override public boolean isCurrent(RoomLease lease) {
        if (lease == null) return false;
        try(var connection=dataSource.getConnection(); var statement=connection.prepareStatement("SELECT owner_node,fencing_token,expires_at FROM aoo_room_lease WHERE room_id=?")) { statement.setLong(1,lease.roomId()); try(var result=statement.executeQuery()) { return result.next() && result.getString(1).equals(lease.ownerNode()) && result.getLong(2)==lease.fencingToken() && result.getTimestamp(3).toInstant().isAfter(clock.instant()); } }
        catch(java.sql.SQLException exception) { throw new IllegalStateException("cannot verify room lease",exception); }
    }
    @Override public void release(RoomLease lease) {
        if (lease == null) return;
        String sql="DELETE FROM aoo_room_lease WHERE room_id=? AND owner_node=? AND fencing_token=?";
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement(sql)) {
            statement.setLong(1,lease.roomId());statement.setString(2,lease.ownerNode());statement.setLong(3,lease.fencingToken());statement.executeUpdate();
        } catch(java.sql.SQLException exception) { throw new IllegalStateException("cannot release room lease",exception); }
    }
}
