package com.aoo.bcg.lobby;

import java.util.List;

/** Canonical capability owner for the modern lobby boundary. */
public final class LobbyCapabilityCatalog {
    public static final String CLUB_OWNER = "Lobby/Club";
    private static final List<String> CAPABILITIES = List.of(
            "account-session", "game-catalog", "room-management", "club-management");

    private LobbyCapabilityCatalog() {}

    public static List<String> capabilities() {
        return CAPABILITIES;
    }

    public static String ownerFor(String capability) {
        if ("club-management".equals(capability)) return CLUB_OWNER;
        if (CAPABILITIES.contains(capability)) return "Lobby";
        throw new IllegalStateException("lobby capability has no canonical owner: " + capability);
    }

    public static void requireComplete() {
        if (CAPABILITIES.size() != 4) {
            throw new IllegalStateException("lobby capability ownership is incomplete");
        }
    }
}
