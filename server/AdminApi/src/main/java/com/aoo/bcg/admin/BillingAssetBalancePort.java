package com.aoo.bcg.admin;

import com.aoo.bcg.billing.BillingService;
import com.aoo.bcg.billing.LedgerEntry;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;

/** Adapter that forces approved adjustments through Billing's authoritative immutable ledger. */
public final class BillingAssetBalancePort implements AssetAdjustmentService.BalancePort {
    @FunctionalInterface public interface BalanceReader { long current(long playerId, String currency); }
    private final BillingService billing;
    private final BalanceReader balances;

    public BillingAssetBalancePort(BillingService billing, BalanceReader balances) {
        this.billing = Objects.requireNonNull(billing);
        this.balances = Objects.requireNonNull(balances);
    }

    @Override public AssetAdjustmentService.BalanceMutation adjust(String businessId, long playerId,
            String currency, BigDecimal amount) {
        long delta;
        try { delta = amount.longValueExact(); }
        catch (ArithmeticException error) { throw new IllegalArgumentException("asset amount must be an integer", error); }
        LedgerEntry entry = delta > 0
                ? billing.credit(businessId, playerId, currency, delta, "ADMIN_ADJUSTMENT")
                : billing.debit(businessId, playerId, currency, Math.negateExact(delta), "ADMIN_ADJUSTMENT");
        return new AssetAdjustmentService.BalanceMutation(
                BigDecimal.valueOf(Math.subtractExact(entry.balanceAfter(), entry.delta())),
                BigDecimal.valueOf(entry.balanceAfter()), entry.businessId());
    }

    @Override public BigDecimal current(long playerId, String currency) {
        return BigDecimal.valueOf(balances.current(playerId, currency));
    }

    public static BalanceReader jdbc(JdbcAdminResourceRepository.ConnectionFactory connections) {
        Objects.requireNonNull(connections);
        return (playerId, currency) -> {
            String sql = "SELECT balance FROM aoo_currency_balance WHERE player_id=? AND currency=? AND currency_scope_id=0";
            try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, playerId); statement.setString(2, currency);
                try (var row = statement.executeQuery()) { return row.next() ? row.getLong(1) : 0L; }
            } catch (Exception error) { throw new IllegalStateException("cannot read authoritative balance", error); }
        };
    }
}
