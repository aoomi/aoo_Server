package com.aoo.bcg.billing;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Objects;
import java.util.function.LongSupplier;
import com.aoo.bcg.common.concurrency.LockOrderGuard;

/** Transactional authoritative balance and immutable-ledger implementation. */
public final class JdbcBillingService implements BillingService {
    private final DataSource dataSource;
    private final LongSupplier idGenerator;
    private final Clock clock;

    public JdbcBillingService(DataSource dataSource, LongSupplier idGenerator, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public LedgerEntry debit(String businessId, long playerId, String currency, long amount, String reasonCode) {
        return change(businessId, playerId, currency, Math.negateExact(positive(amount)), reasonCode);
    }

    @Override
    public LedgerEntry credit(String businessId, long playerId, String currency, long amount, String reasonCode) {
        return change(businessId, playerId, currency, positive(amount), reasonCode);
    }

    @Override public LedgerEntry debit(String businessId,CurrencyAccount account,long amount,String reasonCode){return change(businessId,account,Math.negateExact(positive(amount)),reasonCode);}
    @Override public LedgerEntry credit(String businessId,CurrencyAccount account,long amount,String reasonCode){return change(businessId,account,positive(amount),reasonCode);}

    private LedgerEntry change(String businessId, long playerId, String currency, long delta, String reasonCode) {
        return change(businessId,new CurrencyAccount(playerId,currency),delta,reasonCode);
    }

    private LedgerEntry change(String businessId, CurrencyAccount account, long delta, String reasonCode) {
        long playerId=account.playerId();String currency=account.currency();long scopeId=account.scopeId();
        validate(businessId, playerId, currency, reasonCode);
        try (var asset = LockOrderGuard.enter(LockOrderGuard.Level.ASSET, playerId + ":" + currency); var database = LockOrderGuard.enter(LockOrderGuard.Level.DATABASE, "primary"); Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                requireActiveCurrency(connection,account);
                ensureBalanceRow(connection, playerId, currency,scopeId);
                long currentBalance = lockBalance(connection, playerId, currency,scopeId);
                LedgerEntry existing = findByBusinessId(connection, businessId,scopeId);
                if (existing != null) {
                    requireSameCommand(existing, playerId, currency, delta, reasonCode);
                    connection.rollback();
                    return existing;
                }
                long nextBalance = Math.addExact(currentBalance, delta);
                if (nextBalance < 0) throw new IllegalStateException("insufficient balance");
                updateBalance(connection, playerId, currency,scopeId, nextBalance);
                LedgerEntry entry = new LedgerEntry(businessId, playerId, currency, delta, nextBalance,
                        reasonCode, clock.instant());
                insertLedger(connection, entry,scopeId);
                connection.commit();
                return entry;
            } catch (Exception error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception error) {
            if (error instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("cannot update authoritative balance", error);
        }
    }

    private void requireActiveCurrency(Connection connection,CurrencyAccount account)throws SQLException{
        try(var statement=connection.prepareStatement("SELECT scope_type FROM aoo_currency_catalog WHERE currency_code=? AND status='ACTIVE'")){statement.setString(1,account.currency());try(var row=statement.executeQuery()){if(!row.next())throw new IllegalArgumentException("inactive or unknown currency");String scope=row.getString(1);if(("GLOBAL".equals(scope))!=(account.scopeId()==0))throw new IllegalArgumentException("currency scope mismatch");}}
    }

    private void ensureBalanceRow(Connection connection, long playerId, String currency,long scopeId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "INSERT INTO aoo_currency_balance(player_id,currency,currency_scope_id,balance,version,updated_at) "
                        + "VALUES(?,?,?,0,0,CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE player_id=player_id")) {
            statement.setLong(1, playerId);
            statement.setString(2, currency);
            statement.setLong(3,scopeId);
            statement.executeUpdate();
        }
    }

    private long lockBalance(Connection connection, long playerId, String currency,long scopeId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT balance FROM aoo_currency_balance WHERE player_id=? AND currency=? AND currency_scope_id=? FOR UPDATE")) {
            statement.setLong(1, playerId);
            statement.setString(2, currency);
            statement.setLong(3,scopeId);
            try (var row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalStateException("balance row missing");
                return row.getLong(1);
            }
        }
    }

    private LedgerEntry findByBusinessId(Connection connection, String businessId,long expectedScopeId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT player_id,currency,currency_scope_id,delta,balance_after,reason_code,entry_status,channel_code,created_at FROM aoo_ledger WHERE business_id=? FOR UPDATE")) {
            statement.setString(1, businessId);
            try (var row = statement.executeQuery()) {
                if (!row.next()) return null;
                if(row.getLong(3)!=expectedScopeId)throw new IllegalArgumentException("businessId reused with different billing scope");
                return new LedgerEntry(businessId, row.getLong(1), row.getString(2), row.getLong(4),
                        row.getLong(5), row.getString(6),row.getString(7),row.getString(8),row.getTimestamp(9).toInstant());
            }
        }
    }

    private void updateBalance(Connection connection, long playerId, String currency,long scopeId, long balance) throws SQLException {
        try (var statement = connection.prepareStatement(
                "UPDATE aoo_currency_balance SET balance=?,version=version+1,updated_at=CURRENT_TIMESTAMP(3) "
                        + "WHERE player_id=? AND currency=? AND currency_scope_id=?")) {
            statement.setLong(1, balance);
            statement.setLong(2, playerId);
            statement.setString(3, currency);
            statement.setLong(4,scopeId);
            if (statement.executeUpdate() != 1) throw new IllegalStateException("balance update lost");
        }
    }

    private void insertLedger(Connection connection, LedgerEntry entry,long scopeId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "INSERT INTO aoo_ledger(ledger_id,business_id,player_id,currency,currency_scope_id,delta,balance_after,reason_code,entry_status,channel_code,created_at) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
            statement.setLong(1, com.aoo.bcg.common.math.ExactDomainMath.requirePositiveId(
                    idGenerator.getAsLong(), "ledger id"));
            statement.setString(2, entry.businessId());
            statement.setLong(3, entry.playerId());
            statement.setString(4, entry.currency());
            statement.setLong(5, scopeId);
            statement.setLong(6, entry.delta());
            statement.setLong(7, entry.balanceAfter());
            statement.setString(8, entry.reasonCode());
            statement.setString(9,entry.entryStatus());statement.setString(10,entry.channelCode());statement.setTimestamp(11, Timestamp.from(entry.createdAt()));
            statement.executeUpdate();
        }
    }

    private void requireSameCommand(LedgerEntry existing, long playerId, String currency,
            long delta, String reasonCode) {
        if (existing.playerId() != playerId || !existing.currency().equals(currency)
                || existing.delta() != delta || !existing.reasonCode().equals(reasonCode))
            throw new IllegalArgumentException("businessId reused with different billing command");
    }

    private long positive(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        return amount;
    }

    private void validate(String businessId, long playerId, String currency, String reasonCode) {
        if (businessId == null || businessId.isBlank() || businessId.length() > 128)
            throw new IllegalArgumentException("invalid businessId");
        if (playerId <= 0) throw new IllegalArgumentException("invalid playerId");
        if (currency == null || !currency.matches("[A-Z0-9_]{1,32}"))
            throw new IllegalArgumentException("invalid currency");
        if (reasonCode == null || !reasonCode.matches("[A-Za-z0-9_.:-]{1,64}"))
            throw new IllegalArgumentException("invalid reasonCode");
    }
}
