package com.aoo.bcg.billing;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLedgerRepository implements LedgerRepository {
    private final ConcurrentHashMap<String, LedgerEntry> entries = new ConcurrentHashMap<>();
    @Override public Optional<LedgerEntry> findByBusinessId(String businessId) { return Optional.ofNullable(entries.get(businessId)); }
    @Override public LedgerEntry append(LedgerEntry entry) {
        LedgerEntry existing = entries.putIfAbsent(entry.businessId(), entry);
        if (existing != null) throw new IllegalStateException("duplicate businessId: " + entry.businessId());
        return entry;
    }
    public java.util.List<LedgerEntry> entries(){return entries.values().stream().sorted(java.util.Comparator.comparing(LedgerEntry::createdAt).thenComparing(LedgerEntry::businessId)).toList();}
}
