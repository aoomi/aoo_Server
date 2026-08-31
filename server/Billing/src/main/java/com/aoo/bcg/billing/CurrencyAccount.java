package com.aoo.bcg.billing;

/** Canonical player asset address. Scope 0 is global; scoped currencies require a positive owner id. */
public record CurrencyAccount(long playerId, String currency, long scopeId) {
    public CurrencyAccount {
        if (playerId <= 0) throw new IllegalArgumentException("invalid playerId");
        if (currency == null || !currency.matches("ROOM_CARD|CITY_ROOM_CARD|GOLD|CRYSTAL|SPORTS_POINT"))
            throw new IllegalArgumentException("unsupported currency");
        boolean scoped = currency.equals("CITY_ROOM_CARD") || currency.equals("SPORTS_POINT");
        if (scoped != (scopeId > 0)) throw new IllegalArgumentException("invalid currency scope");
    }
    public CurrencyAccount(long playerId,String currency){this(playerId,currency,0);}

    public boolean scoped() { return scopeId > 0; }

    /** Stable, delimiter-safe identity used by workflow snapshots and reconciliation. */
    public String canonicalId() { return playerId + "/" + currency + "/" + scopeId; }
}
