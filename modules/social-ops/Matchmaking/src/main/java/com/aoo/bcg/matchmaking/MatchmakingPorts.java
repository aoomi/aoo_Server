package com.aoo.bcg.matchmaking;

import java.util.Map;

public final class MatchmakingPorts {
    private MatchmakingPorts() {}
    @FunctionalInterface public interface HallRoomPort { Map<String,Object> create(long accountId,String idempotencyKey,Map<String,Object> request); }
    @FunctionalInterface public interface BillingRewardPort { void credit(String businessId,long accountId,String currency,long amount,String reasonCode); }
}
