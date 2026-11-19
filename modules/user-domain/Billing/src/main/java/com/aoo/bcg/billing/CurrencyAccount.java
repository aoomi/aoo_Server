package com.aoo.bcg.billing;

/** Canonical player asset address. Only ROOM_CARD and GOLD are active currencies; both use global scope 0. */
public record CurrencyAccount(long playerId, String currency, long scopeId) {
    public CurrencyAccount {
        if (playerId <= 0) throw new IllegalArgumentException("invalid playerId");
        if (currency == null || !currency.matches("ROOM_CARD|GOLD"))
            throw new IllegalArgumentException("unsupported currency");
        if (scopeId != 0) throw new IllegalArgumentException("invalid currency scope");
    }
    public CurrencyAccount(long playerId,String currency){this(playerId,currency,0);}

    public boolean scoped() { return scopeId > 0; }

    /** Stable, delimiter-safe identity used by workflow snapshots and reconciliation. */
    public String canonicalId() { return playerId + "/" + currency + "/" + scopeId; }
}
