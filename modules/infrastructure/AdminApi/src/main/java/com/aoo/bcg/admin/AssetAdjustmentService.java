package com.aoo.bcg.admin;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Four-eyes, limited, append-only and automatically reconciled asset adjustment. */
public final class AssetAdjustmentService {
    public enum State { PENDING_APPROVAL, APPROVED, APPLIED, RECONCILIATION_FAILED }
    public record Policy(BigDecimal perAdjustmentLimit, BigDecimal operatorDailyLimit) {
        public Policy {
            if (perAdjustmentLimit == null || operatorDailyLimit == null
                    || perAdjustmentLimit.signum() <= 0 || operatorDailyLimit.signum() <= 0
                    || perAdjustmentLimit.compareTo(operatorDailyLimit) > 0)
                throw new IllegalArgumentException("valid adjustment limits required");
        }
    }
    public record Adjustment(String adjustmentId, long playerId, String currency, BigDecimal amount,
            long requesterId, Long approverId, String reason, State state, Instant updatedAt) {
        public Adjustment {
            if (blank(adjustmentId) || playerId <= 0 || blank(currency) || amount == null
                    || amount.signum() == 0 || requesterId <= 0 || blank(reason) || state == null || updatedAt == null)
                throw new IllegalArgumentException("invalid asset adjustment");
        }
    }
    public record BalanceMutation(BigDecimal before, BigDecimal after, String businessId) {
        public BalanceMutation {
            if (before == null || after == null || blank(businessId))
                throw new IllegalArgumentException("complete balance mutation required");
        }
    }
    public record LedgerEntry(String adjustmentId, long playerId, String currency, BigDecimal amount,
            BigDecimal before, BigDecimal after, long requesterId, long approverId, String reason,
            Instant occurredAt, String previousHash, String hash, boolean reconciled) { }
    public interface BalancePort {
        BalanceMutation adjust(String businessId, long playerId, String currency, BigDecimal amount);
        BigDecimal current(long playerId, String currency);
    }

    private final Map<String, Adjustment> adjustments = new ConcurrentHashMap<>();
    private final Map<String, Adjustment> requestResults = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> dailyUsage = new ConcurrentHashMap<>();
    private final List<LedgerEntry> ledger = new ArrayList<>();
    private final Policy policy;
    private final BalancePort balances;
    private final Clock clock;

    public AssetAdjustmentService(Policy policy, BalancePort balances, Clock clock) {
        this.policy = Objects.requireNonNull(policy);
        this.balances = Objects.requireNonNull(balances);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized Adjustment request(String requestId, String adjustmentId, long playerId,
            String currency, BigDecimal amount, long requesterId, String reason) {
        Adjustment repeated = repeat(requestId, adjustmentId);
        if (repeated != null) return repeated;
        if (amount == null || amount.abs().compareTo(policy.perAdjustmentLimit()) > 0)
            throw new IllegalArgumentException("per-adjustment limit exceeded");
        String usageKey = requesterId + ":" + LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        BigDecimal used = dailyUsage.getOrDefault(usageKey, BigDecimal.ZERO);
        if (used.add(amount.abs()).compareTo(policy.operatorDailyLimit()) > 0)
            throw new IllegalArgumentException("operator daily limit exceeded");
        if (adjustments.containsKey(adjustmentId)) throw new IllegalArgumentException("adjustment already exists");
        Adjustment value = new Adjustment(adjustmentId, playerId, normalizeCurrency(currency), amount,
                requesterId, null, requireReason(reason), State.PENDING_APPROVAL, clock.instant());
        adjustments.put(adjustmentId, value);
        requestResults.put(requestId, value);
        dailyUsage.put(usageKey, used.add(amount.abs()));
        return value;
    }

    public synchronized Adjustment approve(String requestId, String adjustmentId, long approverId, String reason) {
        Adjustment repeated = repeat(requestId, adjustmentId);
        if (repeated != null) return repeated;
        Adjustment current = required(adjustmentId, State.PENDING_APPROVAL);
        if (current.requesterId() == approverId) throw new IllegalArgumentException("requester cannot approve own adjustment");
        requireReason(reason);
        Adjustment approved = copy(current, approverId, State.APPROVED);
        adjustments.put(adjustmentId, approved);
        requestResults.put(requestId, approved);
        return approved;
    }

    public synchronized Adjustment apply(String requestId, String adjustmentId, long operatorId, String reason) {
        Adjustment repeated = repeat(requestId, adjustmentId);
        if (repeated != null) return repeated;
        Adjustment current = required(adjustmentId, State.APPROVED);
        if (operatorId != current.requesterId() && operatorId != current.approverId())
            throw new IllegalArgumentException("only workflow participants may apply adjustment");
        requireReason(reason);
        BalanceMutation mutation = balances.adjust("admin-adjustment:" + adjustmentId,
                current.playerId(), current.currency(), current.amount());
        boolean reconciled = mutation.after().subtract(mutation.before()).compareTo(current.amount()) == 0
                && balances.current(current.playerId(), current.currency()).compareTo(mutation.after()) == 0;
        LedgerEntry entry = ledger(current, mutation, reconciled);
        ledger.add(entry);
        Adjustment applied = copy(current, current.approverId(),
                reconciled ? State.APPLIED : State.RECONCILIATION_FAILED);
        adjustments.put(adjustmentId, applied);
        requestResults.put(requestId, applied);
        return applied;
    }

    public Optional<Adjustment> find(String adjustmentId) { return Optional.ofNullable(adjustments.get(adjustmentId)); }
    public synchronized List<LedgerEntry> immutableLedger() { return List.copyOf(ledger); }
    public synchronized boolean verifyLedger() {
        String previous = "GENESIS";
        for (LedgerEntry entry : ledger) {
            String expected = hash(previous, entry.adjustmentId(), Long.toString(entry.playerId()),
                    entry.currency(), entry.amount().toPlainString(), entry.before().toPlainString(),
                    entry.after().toPlainString(), Long.toString(entry.requesterId()),
                    Long.toString(entry.approverId()), entry.reason(), entry.occurredAt().toString(),
                    Boolean.toString(entry.reconciled()));
            if (!previous.equals(entry.previousHash()) || !expected.equals(entry.hash())) return false;
            previous = entry.hash();
        }
        return true;
    }

    private LedgerEntry ledger(Adjustment adjustment, BalanceMutation mutation, boolean reconciled) {
        String previous = ledger.isEmpty() ? "GENESIS" : ledger.getLast().hash();
        Instant now = clock.instant();
        String hash = hash(previous, adjustment.adjustmentId(), Long.toString(adjustment.playerId()),
                adjustment.currency(), adjustment.amount().toPlainString(), mutation.before().toPlainString(),
                mutation.after().toPlainString(), Long.toString(adjustment.requesterId()),
                Long.toString(adjustment.approverId()), adjustment.reason(), now.toString(),
                Boolean.toString(reconciled));
        return new LedgerEntry(adjustment.adjustmentId(), adjustment.playerId(), adjustment.currency(),
                adjustment.amount(), mutation.before(), mutation.after(), adjustment.requesterId(),
                adjustment.approverId(), adjustment.reason(), now, previous, hash, reconciled);
    }

    private Adjustment repeat(String requestId, String adjustmentId) {
        if (blank(requestId) || requestId.length() > 128) throw new IllegalArgumentException("requestId required");
        Adjustment repeated = requestResults.get(requestId);
        if (repeated != null && !repeated.adjustmentId().equals(adjustmentId))
            throw new IllegalArgumentException("requestId already used by another adjustment");
        return repeated;
    }
    private Adjustment required(String adjustmentId, State state) {
        Adjustment current = Optional.ofNullable(adjustments.get(adjustmentId))
                .orElseThrow(() -> new IllegalArgumentException("adjustment not found"));
        if (current.state() != state) throw new IllegalStateException("adjustment state does not permit action");
        return current;
    }
    private Adjustment copy(Adjustment value, Long approver, State state) {
        return new Adjustment(value.adjustmentId(), value.playerId(), value.currency(), value.amount(),
                value.requesterId(), approver, value.reason(), state, clock.instant());
    }
    private String normalizeCurrency(String currency) {
        if (blank(currency) || !currency.matches("[A-Z][A-Z0-9_]{1,31}"))
            throw new IllegalArgumentException("invalid currency");
        return currency;
    }
    private static String requireReason(String reason) {
        if (blank(reason) || reason.length() > 500) throw new IllegalArgumentException("reason required");
        return reason;
    }
    private String hash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) { digest.update(value.getBytes(StandardCharsets.UTF_8)); digest.update((byte) 0); }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    /** Deterministic idempotent balance port for local verification. */
    public static final class InMemoryBalancePort implements BalancePort {
        private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
        private final Map<String, BalanceMutation> mutations = new ConcurrentHashMap<>();
        @Override public synchronized BalanceMutation adjust(String businessId, long playerId,
                String currency, BigDecimal amount) {
            BalanceMutation repeated = mutations.get(businessId);
            if (repeated != null) return repeated;
            String key = playerId + ":" + currency;
            BigDecimal before = balances.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal after = before.add(amount);
            BalanceMutation mutation = new BalanceMutation(before, after, businessId);
            balances.put(key, after);
            mutations.put(businessId, mutation);
            return mutation;
        }
        @Override public BigDecimal current(long playerId, String currency) {
            return balances.getOrDefault(playerId + ":" + currency, BigDecimal.ZERO);
        }
    }
}
