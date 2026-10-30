package com.aoo.bcg.common.settlement;

public final class SettlementBusinessId {
    private SettlementBusinessId() {}
    public static String round(long roomId, int roundNo, String version) {
        if (roomId <= 0 || roundNo <= 0 || version == null || version.isBlank()) throw new IllegalArgumentException();
        return "round:" + roomId + ":" + roundNo + ":" + version;
    }
    public static String finalRoom(long roomId, String version) {
        if (roomId <= 0 || version == null || version.isBlank()) throw new IllegalArgumentException();
        return "final:" + roomId + ":" + version;
    }
}
