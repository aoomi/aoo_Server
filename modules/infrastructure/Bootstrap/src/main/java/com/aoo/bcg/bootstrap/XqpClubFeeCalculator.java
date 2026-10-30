package com.aoo.bcg.bootstrap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** XQP room-fee selection semantics adapted to Aoo's two-decimal club-point precision. */
final class XqpClubFeeCalculator {
    static final int RATIO = 1;
    static final int STAGE = 2;
    static final int AA = 5;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    record Condition(BigDecimal minWin, BigDecimal cost, BigDecimal guarantee) {}
    record Result(Map<Long, BigDecimal> playerCharges, BigDecimal roomFee, BigDecimal guarantee) {}

    static Result calculate(int type, List<Condition> configured, Map<Long, BigDecimal> scores) {
        List<Map.Entry<Long, BigDecimal>> roles = new ArrayList<>(scores.entrySet());
        roles.sort(Map.Entry.<Long, BigDecimal>comparingByValue(Comparator.reverseOrder()));
        List<Condition> conditions = new ArrayList<>(configured);
        conditions.sort(Comparator.comparing(Condition::minWin).reversed());
        return switch (type) {
            case RATIO -> ratio(roles, conditions);
            case STAGE -> fixed(roles, conditions, winnerCount(roles));
            case AA -> fixed(roles, conditions, roles.size());
            default -> empty();
        };
    }

    private static Result fixed(List<Map.Entry<Long, BigDecimal>> roles, List<Condition> conditions, int payerCount) {
        if (payerCount <= 0) return empty();
        Map<Long, BigDecimal> charges = new LinkedHashMap<>();
        BigDecimal room = BigDecimal.ZERO, guarantee = BigDecimal.ZERO;
        for (int i = 0; i < Math.min(payerCount, roles.size()); i++) {
            var role = roles.get(i);
            Condition condition = match(conditions, role.getValue());
            if (condition == null) continue;
            BigDecimal divisor = BigDecimal.valueOf(payerCount);
            BigDecimal cost = points(condition.cost().divide(divisor, 2, RoundingMode.DOWN));
            BigDecimal bd = points(condition.guarantee().divide(divisor, 2, RoundingMode.DOWN));
            if (cost.signum() > 0) charges.put(role.getKey(), cost);
            room = room.add(cost.subtract(bd).max(BigDecimal.ZERO));
            guarantee = guarantee.add(bd.max(BigDecimal.ZERO));
        }
        return new Result(Map.copyOf(charges), points(room), points(guarantee));
    }

    private static Result ratio(List<Map.Entry<Long, BigDecimal>> roles, List<Condition> conditions) {
        if (conditions.isEmpty()) return empty();
        Condition condition = conditions.get(0);
        BigDecimal totalWin = roles.stream().map(Map.Entry::getValue).filter(value -> value.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWin.signum() <= 0) return empty();
        BigDecimal totalGuarantee = totalWin.multiply(condition.guarantee()).divide(HUNDRED, 8, RoundingMode.DOWN);
        BigDecimal totalCost = totalWin.multiply(condition.cost()).divide(HUNDRED, 8, RoundingMode.DOWN)
                .subtract(totalGuarantee);
        Map<Long, BigDecimal> charges = new LinkedHashMap<>();
        BigDecimal room = BigDecimal.ZERO, guarantee = BigDecimal.ZERO;
        for (var role : roles) {
            BigDecimal score = role.getValue();
            if (score.signum() <= 0) break;
            BigDecimal bd = points(score.multiply(condition.guarantee())
                    .divide(HUNDRED.multiply(BigDecimal.valueOf(2)), 2, RoundingMode.DOWN));
            BigDecimal cost = points(score.multiply(totalCost)
                    .divide(totalWin.multiply(BigDecimal.valueOf(2)), 2, RoundingMode.DOWN)).max(BigDecimal.ZERO);
            if (cost.signum() > 0) charges.put(role.getKey(), cost);
            room = room.add(cost);
            guarantee = guarantee.add(bd.max(BigDecimal.ZERO));
        }
        return new Result(Map.copyOf(charges), points(room), points(guarantee));
    }

    private static int winnerCount(List<Map.Entry<Long, BigDecimal>> roles) {
        if (roles.isEmpty() || roles.get(0).getValue().signum() <= 0) return 0;
        BigDecimal maximum = roles.get(0).getValue();
        int count = 0;
        for (var role : roles) {
            if (role.getValue().signum() <= 0 || role.getValue().compareTo(maximum) != 0) break;
            count++;
        }
        return count;
    }

    private static Condition match(List<Condition> conditions, BigDecimal score) {
        return conditions.stream().filter(condition -> score.compareTo(condition.minWin()) >= 0)
                .findFirst().orElse(null);
    }

    private static Result empty() { return new Result(Map.of(), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2)); }
    private static BigDecimal points(BigDecimal value) { return value.setScale(2, RoundingMode.DOWN); }
}
