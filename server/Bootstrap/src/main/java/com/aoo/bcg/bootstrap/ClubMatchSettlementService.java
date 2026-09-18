package com.aoo.bcg.bootstrap;

import com.aoo.bcg.club.JdbcClubService;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Idempotently projects a completed club match into club score, fee-share and record ledgers. */
final class ClubMatchSettlementService {
    private static final System.Logger LOG = System.getLogger(ClubMatchSettlementService.class.getName());
    private final DataSource source;
    private final ObjectMapper json;
    private final JdbcClubService clubs;
    private final Clock clock;

    ClubMatchSettlementService(DataSource source, ObjectMapper json, Clock clock) {
        this.source = Objects.requireNonNull(source);
        this.json = Objects.requireNonNull(json);
        this.clock = Objects.requireNonNull(clock);
        this.clubs = new JdbcClubService(source, json, clock);
    }

    int recoverCompletedMatches(int limit) {
        List<Long> rooms = new ArrayList<>();
        String sql = "SELECT s.room_id FROM aoo_room_snapshot s JOIN aoo_hall_room h ON h.room_id=s.room_id "
                + "LEFT JOIN aoo_club_write_idempotency i ON i.scope_key=CONCAT('club-match:',h.club_id,':',s.room_id) "
                + "WHERE h.scope_type='CLUB' AND h.club_id IS NOT NULL AND i.scope_key IS NULL "
                + "AND JSON_EXTRACT(s.state_payload,'$.state.finished')=true "
                + "AND CAST(JSON_UNQUOTE(JSON_EXTRACT(s.state_payload,'$.roundNo')) AS UNSIGNED)>="
                + "CAST(JSON_UNQUOTE(JSON_EXTRACT(s.state_payload,'$.roundLimit')) AS UNSIGNED) "
                + "ORDER BY s.captured_at LIMIT ?";
        try (Connection connection = source.getConnection(); PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, Math.max(1, Math.min(limit, 256)));
            try (ResultSet rows = query.executeQuery()) { while (rows.next()) rooms.add(rows.getLong(1)); }
        } catch (Exception failure) { throw new IllegalStateException("club match recovery scan failed", failure); }
        int completed = 0;
        for (long roomId : rooms) if (settle(roomId)) completed++;
        return completed;
    }

    boolean settle(long roomId) {
        Metadata metadata = metadata(roomId);
        if (metadata == null) return false;
        Map<Long, BigDecimal> scores = scores(roomId, metadata.roundNo());
        if (scores.isEmpty()) throw new IllegalStateException("completed club room has no settlement scores: " + roomId);
        String businessKey = "club-match:" + metadata.clubId() + ':' + roomId;
        clubs.update(businessKey, metadata.clubId(), current -> apply(current, metadata, scores, businessKey));
        LOG.log(System.Logger.Level.INFO,
                "[ClubMatchSettlement] roomId={0} clubId={1} rounds={2} feeType={3} scores={4}",
                roomId, metadata.clubId(), metadata.roundNo(), metadata.feeType(), scores);
        return true;
    }

    private JdbcClubService.State apply(JdbcClubService.State state, Metadata metadata,
                                         Map<Long, BigDecimal> scores, String businessKey) {
        if (!state.members().keySet().containsAll(scores.keySet()))
            throw new IllegalStateException("club match contains a non-member");
        Map<Long, JdbcClubService.MemberExtraState> extras = new LinkedHashMap<>(state.memberExtras());
        List<JdbcClubService.LedgerState> ledger = new ArrayList<>(state.ledger());
        XqpClubFeeCalculator.Result feeResult = XqpClubFeeCalculator.calculate(
                metadata.feeType(), metadata.conditions(), scores);
        LOG.log(System.Logger.Level.INFO,
                "[ClubMatchSettlement] roomId={0} feeType={1} playerCharges={2} distributableFee={3} guarantee={4}",
                metadata.roomId(), metadata.feeType(), feeResult.playerCharges(),
                feeResult.roomFee(), feeResult.guarantee());
        Map<Long, BigDecimal> fees = new LinkedHashMap<>(feeResult.playerCharges());
        for (var entry : scores.entrySet()) {
            long player = entry.getKey(); BigDecimal score = entry.getValue();
            adjust(extras, player, score);
            append(ledger, businessKey + ":score:" + player, player, score, "MATCH_SCORE");
            BigDecimal fee = fees.getOrDefault(player, BigDecimal.ZERO);
            if (fee.signum() > 0) {
                adjust(extras, player, fee.negate());
                append(ledger, businessKey + ":fee:" + player, player, fee.negate(), "ENTRY_FEE");
            }
        }
        distributePool(state, extras, ledger, businessKey, scores.keySet().stream().toList(), feeResult.roomFee());
        creditOwner(state, extras, ledger, businessKey, feeResult.guarantee(), "ENTRY_FEE_GUARANTEE");
        List<JdbcClubService.MatchState> records = new ArrayList<>(state.records());
        Map<Long,Integer> integerScores = new LinkedHashMap<>();
        scores.forEach((player, score) -> integerScores.put(player, score.intValueExact()));
        records.add(new JdbcClubService.MatchState(String.valueOf(metadata.roomId()), Map.copyOf(integerScores), clock.instant()));
        Map<String,Object> settings = new LinkedHashMap<>(state.settings());
        settings.put("matchFee." + metadata.roomId(), Map.copyOf(fees));
        return JdbcClubService.copy(state, state.name(), state.status(), state.members(), state.templates(),
                state.tables(), state.invites(), List.copyOf(records), List.copyOf(ledger), Map.copyOf(settings),
                state.applications(), Map.copyOf(extras), state.groupings(), state.roomBans(), state.viewedRooms());
    }

    private void distributePool(JdbcClubService.State state,
                                Map<Long, JdbcClubService.MemberExtraState> extras,
                                List<JdbcClubService.LedgerState> ledger, String businessKey,
                                List<Long> players, BigDecimal pool) {
        if (pool.signum() <= 0 || players.isEmpty()) return;
        BigDecimal average = pool.divide(BigDecimal.valueOf(players.size()), 2, RoundingMode.DOWN);
        BigDecimal distributed = BigDecimal.ZERO;
        for (int i = 0; i < players.size(); i++) {
            BigDecimal contribution = i == players.size() - 1 ? pool.subtract(distributed) : average;
            distribute(state, extras, ledger, businessKey, players.get(i), contribution);
            distributed = distributed.add(contribution);
        }
    }

    private void creditOwner(JdbcClubService.State state,
                             Map<Long, JdbcClubService.MemberExtraState> extras,
                             List<JdbcClubService.LedgerState> ledger, String businessKey,
                             BigDecimal amount, String reason) {
        if (amount.signum() <= 0) return;
        long owner = state.members().entrySet().stream().filter(e -> "OWNER".equals(e.getValue()))
                .map(Map.Entry::getKey).findFirst().orElseThrow();
        adjust(extras, owner, amount);
        append(ledger, businessKey + ":guarantee:" + owner, owner, amount, reason);
    }

    private void distribute(JdbcClubService.State state,
                            Map<Long, JdbcClubService.MemberExtraState> extras,
                            List<JdbcClubService.LedgerState> ledger, String businessKey,
                            long player, BigDecimal fee) {
        long owner = state.members().entrySet().stream().filter(e -> "OWNER".equals(e.getValue()))
                .map(Map.Entry::getKey).findFirst().orElseThrow();
        BigDecimal allocated = BigDecimal.ZERO;
        long current = player;
        int depth = 0;
        while (depth++ < state.members().size()) {
            JdbcClubService.MemberExtraState extra = extras.get(current);
            long parent = extra == null ? 0 : extra.upPlayerId();
            if (parent <= 0 || parent == owner) break;
            BigDecimal target = shareAmount(state, parent, fee);
            BigDecimal increment = target.subtract(allocated).max(BigDecimal.ZERO).min(fee.subtract(allocated));
            if (increment.signum() > 0) {
                adjust(extras, parent, increment);
                append(ledger, businessKey + ":share:" + player + ':' + parent, parent, increment, "PROMOTION_SHARE");
                allocated = allocated.add(increment);
            }
            current = parent;
        }
        BigDecimal remainder = fee.subtract(allocated);
        if (remainder.signum() > 0) {
            adjust(extras, owner, remainder);
            append(ledger, businessKey + ":share:" + player + ':' + owner, owner, remainder, "ENTRY_FEE_OWNER_REMAINDER");
        }
    }

    private static BigDecimal shareAmount(JdbcClubService.State state, long captain, BigDecimal fee) {
        int type = number(state.settings().get("promotionShareType." + captain));
        BigDecimal value = decimal(state.settings().get("promotionShareValue." + captain));
        if (type == 1) return value.max(BigDecimal.ZERO).min(fee);
        return fee.multiply(value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
    }

    private static void adjust(Map<Long, JdbcClubService.MemberExtraState> extras, long player, BigDecimal delta) {
        JdbcClubService.MemberExtraState old = extras.getOrDefault(player,
                new JdbcClubService.MemberExtraState("", false, 0, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, 0));
        extras.put(player, new JdbcClubService.MemberExtraState(old.remarkName(), old.promotionManager(),
                old.upPlayerId(), old.clubCent().add(delta), old.caseClubCent(), old.warningPoint(), old.eliminatePoint()));
    }

    private void append(List<JdbcClubService.LedgerState> ledger, String key, long player,
                        BigDecimal amount, String reason) {
        if (amount.signum() != 0) ledger.add(new JdbcClubService.LedgerState(key, player, amount, reason, clock.instant()));
    }

    private Metadata metadata(long roomId) {
        String sql = "SELECT h.club_id,h.rules_json,JSON_EXTRACT(s.state_payload,'$.roundNo') "
                + "FROM aoo_hall_room h JOIN aoo_room_snapshot s ON s.room_id=h.room_id WHERE h.room_id=? "
                + "AND h.scope_type='CLUB' AND JSON_EXTRACT(s.state_payload,'$.state.finished')=true "
                + "AND CAST(JSON_UNQUOTE(JSON_EXTRACT(s.state_payload,'$.roundNo')) AS UNSIGNED)>="
                + "CAST(JSON_UNQUOTE(JSON_EXTRACT(s.state_payload,'$.roundLimit')) AS UNSIGNED)";
        try (Connection connection = source.getConnection(); PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, roomId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return null;
                Map<?,?> rules = json.readValue(row.getString(2), Map.class);
                Map<?,?> rule = rules.get("rule") instanceof Map<?,?> value ? value : Map.of();
                int feeType = number(rule.get("type"));
                if (feeType == 0) feeType = switch (number(rules.get("roomSportsType"))) {
                    case 0 -> XqpClubFeeCalculator.STAGE;
                    case 1 -> XqpClubFeeCalculator.AA;
                    case 2 -> XqpClubFeeCalculator.RATIO;
                    default -> 0;
                };
                List<XqpClubFeeCalculator.Condition> conditions = conditions(rule, rules, feeType);
                return new Metadata(roomId, row.getLong(1), row.getInt(3), feeType, conditions);
            }
        } catch (Exception failure) { throw new IllegalStateException("club match metadata lookup failed", failure); }
    }

    private static List<XqpClubFeeCalculator.Condition> conditions(Map<?,?> rule, Map<?,?> rules, int feeType) {
        List<XqpClubFeeCalculator.Condition> result = new ArrayList<>();
        if (rule.get("conditions") instanceof List<?> rows) for (Object value : rows) {
            if (value instanceof Map<?,?> row) result.add(new XqpClubFeeCalculator.Condition(
                    decimal(row.get("minWin")), decimal(row.get("cost")), decimal(row.get("bd"))));
        }
        if (!result.isEmpty()) return List.copyOf(result);
        if (feeType == XqpClubFeeCalculator.AA) result.add(new XqpClubFeeCalculator.Condition(BigDecimal.valueOf(-9_999_999),
                decimal(rules.get("roomSportsEveryoneConsume")), decimal(rules.get("prizePool"))));
        else if (feeType == XqpClubFeeCalculator.RATIO) result.add(new XqpClubFeeCalculator.Condition(BigDecimal.valueOf(-9_999_999),
                decimal(rules.get("percentage")), decimal(rules.get("prizePool"))));
        else if (rules.get("bigWinnerConsumeList") instanceof List<?> rows) for (Object value : rows) {
            if (value instanceof Map<?,?> row) result.add(new XqpClubFeeCalculator.Condition(
                    decimal(row.get("winScore")), decimal(row.get("clubCent")),
                    decimal(row.get("clubCentCost"))));
        }
        return List.copyOf(result);
    }

    private Map<Long, BigDecimal> scores(long roomId, int roundNo) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        String sql = "SELECT ss.player_id,SUM(ss.score_delta),COUNT(DISTINCT s.round_no) "
                + "FROM aoo_settlement s JOIN aoo_settlement_score ss ON ss.settlement_id=s.settlement_id "
                + "WHERE s.room_id=? GROUP BY ss.player_id";
        try (Connection connection = source.getConnection(); PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, roomId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    if (rows.getInt(3) != roundNo) throw new IllegalStateException("club match settlement rounds incomplete");
                    result.put(rows.getLong(1), rows.getBigDecimal(2));
                }
            }
            return Map.copyOf(result);
        } catch (Exception failure) { throw new IllegalStateException("club match scores lookup failed", failure); }
    }

    private static int number(Object value) { return value instanceof Number number ? number.intValue() : 0; }
    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        if (value == null) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(value)); } catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }
    private record Metadata(long roomId, long clubId, int roundNo, int feeType,
                            List<XqpClubFeeCalculator.Condition> conditions) {}
}
