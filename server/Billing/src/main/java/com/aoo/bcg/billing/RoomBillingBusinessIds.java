package com.aoo.bcg.billing;

public final class RoomBillingBusinessIds {
    private RoomBillingBusinessIds() {}
    public static String consume(long roomId, long playerId) { return key("consume", roomId, playerId); }
    public static String refund(long roomId, long playerId) { return key("refund", roomId, playerId); }
    public static String reserve(long roomId, long playerId) { return key("reserve", roomId, playerId); }
    public static String release(long roomId, long playerId) { return key("release", roomId, playerId); }
    public static String partialRefund(long roomId,long playerId,int roundsPlayed){
        if(roundsPlayed<=0)throw new IllegalArgumentException("roundsPlayed must be positive");
        return key("refund-r"+roundsPlayed,roomId,playerId);
    }
    private static String key(String action, long roomId, long playerId) {
        if (roomId <= 0 || playerId <= 0) throw new IllegalArgumentException("invalid room billing identity");
        return "room:" + roomId + ":player:" + playerId + ":" + action;
    }
}
