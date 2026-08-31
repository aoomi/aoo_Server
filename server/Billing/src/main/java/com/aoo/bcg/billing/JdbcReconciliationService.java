package com.aoo.bcg.billing;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Detects ledger continuity breaks and refunds without their matching consumption. */
public final class JdbcReconciliationService implements ReconciliationService {
    private final ConnectionFactory connections;

    public JdbcReconciliationService(ConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    @Override
    public Report reconcile(LocalDate businessDate) {
        return reconcile(businessDate,"POSTED",null);
    }

    public Report reconcile(LocalDate businessDate,String entryStatus,String channelCode) {
        Objects.requireNonNull(businessDate, "businessDate");
        if(entryStatus==null||!entryStatus.matches("POSTED|REVERSED|PENDING")||(channelCode!=null&&!channelCode.matches("[A-Z0-9_]{1,32}")))throw new IllegalArgumentException("invalid reconciliation dimensions");
        List<Difference> differences = new ArrayList<>();
        try (Connection connection = connections.open()) {
            findBalanceBreaks(connection, businessDate,entryStatus,channelCode,differences);
            findOrphanRefunds(connection, businessDate,entryStatus,channelCode,differences);
            return new Report(businessDate, differences);
        } catch (SQLException error) {
            throw new IllegalStateException("cannot reconcile ledger", error);
        }
    }

    private void findBalanceBreaks(Connection connection, LocalDate date,String status,String channel,
            List<Difference> differences) throws SQLException {
        String sql = "SELECT cur.business_id,"
                + "((SELECT prev.balance_after FROM aoo_ledger prev "
                + "WHERE prev.player_id=cur.player_id AND prev.currency=cur.currency "
                + "AND (prev.created_at<cur.created_at OR (prev.created_at=cur.created_at AND prev.ledger_id<cur.ledger_id)) "
                + "ORDER BY prev.created_at DESC,prev.ledger_id DESC LIMIT 1)+cur.delta) expected_balance,"
                + "cur.balance_after FROM aoo_ledger cur WHERE cur.entry_status=? AND (? IS NULL OR cur.channel_code=?) AND cur.created_at>=? AND cur.created_at<? "
                + "HAVING expected_balance IS NOT NULL AND expected_balance<>cur.balance_after";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1,status);statement.setString(2,channel);statement.setString(3,channel);statement.setDate(4, Date.valueOf(date));statement.setDate(5, Date.valueOf(date.plusDays(1)));
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) differences.add(new Difference("BALANCE_CHAIN", rows.getString(1),
                        Long.toString(rows.getLong(2)), Long.toString(rows.getLong(3))));
            }
        }
    }

    private void findOrphanRefunds(Connection connection, LocalDate date,String status,String channel,
            List<Difference> differences) throws SQLException {
        String sql = "SELECT refund.business_id FROM aoo_ledger refund LEFT JOIN aoo_ledger consume "
                + "ON consume.business_id=CONCAT(SUBSTRING(refund.business_id,1,LENGTH(refund.business_id)-6),'consume') "
                + "WHERE refund.entry_status=? AND (? IS NULL OR refund.channel_code=?) AND refund.created_at>=? AND refund.created_at<? AND refund.business_id LIKE 'room:%:player:%:refund' "
                + "AND consume.business_id IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1,status);statement.setString(2,channel);statement.setString(3,channel);statement.setDate(4, Date.valueOf(date));statement.setDate(5, Date.valueOf(date.plusDays(1)));
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) differences.add(new Difference("REFUND_WITHOUT_CONSUME",
                        rows.getString(1), "matching consume", "missing"));
            }
        }
    }

    @FunctionalInterface
    public interface ConnectionFactory { Connection open() throws SQLException; }
}
