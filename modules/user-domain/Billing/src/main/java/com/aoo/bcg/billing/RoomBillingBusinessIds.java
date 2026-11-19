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
    public static String reserve(long roomId,long playerId,String attempt){return key("reserve",roomId,playerId,attempt);}
    public static String consume(long roomId,long playerId,String attempt){return key("consume",roomId,playerId,attempt);}
    public static String release(long roomId,long playerId,String attempt){return key("release",roomId,playerId,attempt);}
    public static String refund(long roomId,long playerId,String attempt){return key("refund",roomId,playerId,attempt);}
    public static String partialRefund(long roomId,long playerId,int roundsPlayed,String attempt){
        if(roundsPlayed<=0)throw new IllegalArgumentException("roundsPlayed must be positive");
        return key("refund-r"+roundsPlayed,roomId,playerId,attempt);
    }
    static void requireAttempt(String attempt){
        if(attempt==null||!attempt.matches("[A-Za-z0-9_.:-]{1,48}"))throw new IllegalArgumentException("invalid billing attempt");
    }
    private static String key(String action,long roomId,long playerId,String attempt){
        if(roomId<=0||playerId<=0)throw new IllegalArgumentException("invalid room billing identity");
        requireAttempt(attempt);
        String id="r:"+roomId+":p:"+playerId+":a:"+attempt+":"+action;
        if(id.length()>128)throw new IllegalArgumentException("billing business identity is too long");
        return id;
    }
    private static String key(String action, long roomId, long playerId) {
        if (roomId <= 0 || playerId <= 0) throw new IllegalArgumentException("invalid room billing identity");
        return "room:" + roomId + ":player:" + playerId + ":" + action;
    }
}
