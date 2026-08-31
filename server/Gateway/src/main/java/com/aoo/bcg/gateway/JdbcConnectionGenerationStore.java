package com.aoo.bcg.gateway;

import javax.sql.DataSource;

/** Transactional durable generation allocation for multi-node reconnect races. */
public final class JdbcConnectionGenerationStore implements ConnectionGenerationStore {
    private final DataSource dataSource;
    public JdbcConnectionGenerationStore(DataSource dataSource) { this.dataSource = java.util.Objects.requireNonNull(dataSource); }
    @Override public long next(String userId, String roomId, int seatId) {
        if (userId == null || userId.isBlank() || roomId == null || roomId.isBlank() || seatId < 0) throw new IllegalArgumentException("invalid connection scope");
        long numericRoomId;
        try { numericRoomId=Long.parseLong(roomId); } catch(NumberFormatException error) { throw new IllegalArgumentException("roomId must be numeric",error); }
        if(numericRoomId<=0)throw new IllegalArgumentException("roomId must be positive");
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long generation;
                try (var select = connection.prepareStatement("SELECT generation FROM aoo_connection_generation WHERE user_id=? AND room_id=? AND seat_id=? FOR UPDATE")) {
                    select.setString(1, userId); select.setLong(2, numericRoomId); select.setInt(3, seatId);
                    try (var rows = select.executeQuery()) { generation = rows.next() ? Math.addExact(rows.getLong(1), 1L) : 1L; }
                }
                try (var upsert = connection.prepareStatement("INSERT INTO aoo_connection_generation(user_id,room_id,seat_id,generation,updated_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE generation=VALUES(generation),updated_at=VALUES(updated_at)")) {
                    upsert.setString(1, userId); upsert.setLong(2, numericRoomId); upsert.setInt(3, seatId); upsert.setLong(4, generation); upsert.executeUpdate();
                }
                connection.commit(); return generation;
            } catch (Exception failure) { connection.rollback(); throw failure; }
            finally { connection.setAutoCommit(true); }
        } catch (Exception failure) { throw new IllegalStateException("cannot allocate connection generation", failure); }
    }
}
