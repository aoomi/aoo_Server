package com.aoo.bcg.billing;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.function.LongSupplier;

public final class JdbcLedgerRepository implements LedgerRepository {
    private final DataSource dataSource;
    private final LongSupplier idGenerator;
    public JdbcLedgerRepository(DataSource dataSource, LongSupplier idGenerator) { this.dataSource=dataSource; this.idGenerator=idGenerator; }
    @Override public Optional<LedgerEntry> findByBusinessId(String businessId) {
        String sql="SELECT player_id,currency,delta,balance_after,reason_code,entry_status,channel_code,created_at FROM aoo_ledger WHERE business_id=?";
        try(var connection=dataSource.getConnection(); var statement=connection.prepareStatement(sql)) { statement.setString(1,businessId); try(var result=statement.executeQuery()) { if(!result.next()) return Optional.empty(); return Optional.of(new LedgerEntry(businessId,result.getLong(1),result.getString(2),result.getLong(3),result.getLong(4),result.getString(5),result.getString(6),result.getString(7),result.getTimestamp(8).toInstant())); } }
        catch(java.sql.SQLException exception) { throw new IllegalStateException("cannot find ledger entry",exception); }
    }
    @Override public LedgerEntry append(LedgerEntry entry) {
        String sql="INSERT INTO aoo_ledger(ledger_id,business_id,player_id,currency,delta,balance_after,reason_code,entry_status,channel_code,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try(var connection=dataSource.getConnection(); var statement=connection.prepareStatement(sql)) { statement.setLong(1,com.aoo.bcg.common.math.ExactDomainMath.requirePositiveId(idGenerator.getAsLong(),"ledger id")); statement.setString(2,entry.businessId()); statement.setLong(3,entry.playerId()); statement.setString(4,entry.currency()); statement.setLong(5,entry.delta()); statement.setLong(6,entry.balanceAfter()); statement.setString(7,entry.reasonCode());statement.setString(8,entry.entryStatus());statement.setString(9,entry.channelCode());statement.setTimestamp(10,Timestamp.from(entry.createdAt())); statement.executeUpdate(); return entry; }
        catch(java.sql.SQLException exception) { throw new IllegalStateException("cannot append ledger entry: "+entry.businessId(),exception); }
    }
}
