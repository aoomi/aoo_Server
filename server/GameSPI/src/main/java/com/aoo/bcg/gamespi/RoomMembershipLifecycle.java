package com.aoo.bcg.gamespi;

/** Canonical committed-result markers for room membership lifecycle changes. */
public interface RoomMembershipLifecycle {
    String MEMBER_LEFT_FIELD = "roomMemberLeft";
    String MEMBER_LEFT_ACCOUNT_ID_FIELD = "roomMemberLeftAccountId";

    /** Ready state does not affect exit. A seated member may leave until cards are presented. */
    static boolean canLeave(java.util.Map<String, Object> roomView) {
        Object explicit = roomView.get("cardsDealt");
        if (explicit instanceof Boolean dealt) return !dealt;
        String phase = String.valueOf(roomView.getOrDefault("phase", "")).trim().toUpperCase(java.util.Locale.ROOT);
        return !phase.equals("PLAYING") && !phase.equals("RESPONDING");
    }
}
