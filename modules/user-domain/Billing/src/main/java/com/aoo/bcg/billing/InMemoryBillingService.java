package com.aoo.bcg.billing;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryBillingService implements BillingService {
    private final LedgerRepository ledger;
    private final Clock clock;
    private final ConcurrentHashMap<Account, Long> balances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Account, Object> locks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,CurrencyAccount> businessAccounts=new ConcurrentHashMap<>();
    public InMemoryBillingService(LedgerRepository ledger, Clock clock, Map<Long, Long> initialBalances, String currency) {
        this.ledger = ledger; this.clock = clock;
        initialBalances.forEach((playerId, balance) -> balances.put(new Account(new CurrencyAccount(playerId, currency)), balance));
    }
    @Override public LedgerEntry debit(String businessId, long playerId, String currency, long amount, String reasonCode) { return change(businessId, playerId, currency, -positive(amount), reasonCode); }
    @Override public LedgerEntry credit(String businessId, long playerId, String currency, long amount, String reasonCode) { return change(businessId, playerId, currency, positive(amount), reasonCode); }
    @Override public LedgerEntry debit(String businessId,CurrencyAccount account,long amount,String reasonCode){return change(businessId,account,Math.negateExact(positive(amount)),reasonCode);}
    @Override public LedgerEntry credit(String businessId,CurrencyAccount account,long amount,String reasonCode){return change(businessId,account,positive(amount),reasonCode);}
    public long balance(long playerId, String currency) { return balance(new CurrencyAccount(playerId,currency)); }
    public long balance(CurrencyAccount account) { return balances.getOrDefault(new Account(account), 0L); }
    public void seed(CurrencyAccount account,long balance){if(balance<0||balances.putIfAbsent(new Account(account),balance)!=null)throw new IllegalArgumentException("account already seeded or invalid balance");}
    private LedgerEntry change(String businessId, long playerId, String currency, long delta, String reasonCode) {
        return change(businessId,new CurrencyAccount(playerId,currency),delta,reasonCode);
    }
    private LedgerEntry change(String businessId,CurrencyAccount validated,long delta,String reasonCode) {
        validateCommand(businessId,reasonCode);
        LedgerEntry previous = ledger.findByBusinessId(businessId).orElse(null);
        if (previous != null) {requireSame(previous,validated.playerId(),validated.currency(),delta,reasonCode);requireSameScope(businessId,validated);return previous;}
        Account account = new Account(validated);
        synchronized (locks.computeIfAbsent(account, ignored -> new Object())) {
            previous = ledger.findByBusinessId(businessId).orElse(null);
            if (previous != null) {requireSame(previous,validated.playerId(),validated.currency(),delta,reasonCode);requireSameScope(businessId,validated);return previous;}
            long next = Math.addExact(balance(validated), delta);
            if (next < 0) throw new IllegalStateException("insufficient balance");
            LedgerEntry entry = new LedgerEntry(businessId, validated.playerId(), validated.currency(), delta, next, reasonCode, clock.instant());
            ledger.append(entry);
            CurrencyAccount priorAccount=businessAccounts.putIfAbsent(businessId,validated);
            if(priorAccount!=null&&!priorAccount.equals(validated))throw new IllegalArgumentException("businessId reused across asset scopes");
            balances.put(account, next);
            return entry;
        }
    }
    private static long positive(long amount) { if (amount <= 0) throw new IllegalArgumentException("amount must be positive"); return amount; }
    private static void requireSame(LedgerEntry entry,long playerId,String currency,long delta,String reason){if(entry.playerId()!=playerId||!entry.currency().equals(currency)||entry.delta()!=delta||!entry.reasonCode().equals(reason))throw new IllegalArgumentException("businessId reused with different billing command");}
    private void requireSameScope(String businessId,CurrencyAccount account){CurrencyAccount existing=businessAccounts.get(businessId);if(existing!=null&&!existing.equals(account))throw new IllegalArgumentException("businessId reused across asset scopes");if(existing==null&&account.scopeId()!=0)throw new IllegalStateException("scoped ledger entry lacks account identity");}
    private static void validateCommand(String businessId,String reason){if(businessId==null||businessId.isBlank()||businessId.length()>128||reason==null||!reason.matches("[A-Za-z0-9_.:-]{1,64}"))throw new IllegalArgumentException("invalid billing command");}
    private record Account(long playerId,String currency,long scopeId){private Account(CurrencyAccount account){this(account.playerId(),account.currency(),account.scopeId());}}
}
