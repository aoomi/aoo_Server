package com.aoo.bcg.luckdraw;

public final class LuckDrawPorts {
    private LuckDrawPorts() {}
    @FunctionalInterface public interface PlayerAuthenticator { long authenticate(String authorization); }
    public interface RewardPort {
        void creditCurrency(String requestId,long playerId,String currencyCode,long amount);
        void grantItem(String requestId,long playerId,String itemCode,long quantity);
    }
}
