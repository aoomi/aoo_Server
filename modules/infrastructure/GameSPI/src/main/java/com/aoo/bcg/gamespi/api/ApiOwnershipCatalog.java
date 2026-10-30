package com.aoo.bcg.gamespi.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Authoritative ownership directory for every public HTTP route and WSS namespace. */
public final class ApiOwnershipCatalog {
    /** Canonical owner for the shared Lobby entry and Club public capability. */
    public static final String LOBBY_CLUB_OWNER = "Lobby/Club";

    public enum Transport { HTTP, WSS }
    public record Ownership(Transport transport, String pattern, String ownerModule, String maintainerTeam) {
        public Ownership {
            if (transport == null || pattern == null || pattern.isBlank() || ownerModule == null || ownerModule.isBlank()
                    || maintainerTeam == null || maintainerTeam.isBlank()) throw new IllegalArgumentException("incomplete API ownership");
            if (pattern.indexOf('*') >= 0 && !(pattern.endsWith(".*") || pattern.endsWith("/*")))
                throw new IllegalArgumentException("API ownership wildcard must be terminal: " + pattern);
        }
        boolean matches(Transport requestedTransport, String endpoint) {
            if (transport != requestedTransport || endpoint == null || endpoint.isBlank()) return false;
            String prefix = wildcardPrefix(pattern);
            return prefix == null ? pattern.equals(endpoint) : endpoint.startsWith(prefix);
        }
    }

    private final List<Ownership> entries;
    public ApiOwnershipCatalog(List<Ownership> entries) {
        if (entries == null || entries.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("API ownership entries are required");
        this.entries = List.copyOf(entries);
        for (int left=0; left<this.entries.size(); left++) for (int right=left+1; right<this.entries.size(); right++) {
            Ownership first = this.entries.get(left);
            Ownership second = this.entries.get(right);
            if (overlaps(first, second))
                throw new IllegalArgumentException("overlapping API ownership patterns: "
                        + first.transport() + " " + first.pattern() + " / " + second.pattern());
        }
    }
    public Ownership requireOwner(Transport transport, String endpoint) {
        return entries.stream().filter(value -> value.matches(transport, endpoint)).findFirst()
            .orElseThrow(() -> new IllegalStateException("public API has no owner: " + transport + " " + endpoint));
    }
    public List<Ownership> entries() { return entries; }

    private static boolean overlaps(Ownership first, Ownership second) {
        if (first.transport() != second.transport()) return false;
        String firstPrefix = wildcardPrefix(first.pattern());
        String secondPrefix = wildcardPrefix(second.pattern());
        if (firstPrefix == null && secondPrefix == null) return first.pattern().equals(second.pattern());
        if (firstPrefix == null) return first.pattern().startsWith(secondPrefix);
        if (secondPrefix == null) return second.pattern().startsWith(firstPrefix);
        return firstPrefix.startsWith(secondPrefix) || secondPrefix.startsWith(firstPrefix);
    }

    private static String wildcardPrefix(String pattern) {
        if (pattern.endsWith(".*") || pattern.endsWith("/*")) return pattern.substring(0, pattern.length() - 1);
        return null;
    }

    public static ApiOwnershipCatalog standard() {
        List<Ownership> values = new ArrayList<>();
        for (String route : List.of("/api/v2/admin/game-profiles", "/api/v2/admin/operation-switches",
                "/api/v2/admin/appeals", "/api/v2/admin/reconciliation", "/api/v2/admin/data-lifecycle"))
            values.add(new Ownership(Transport.HTTP, route, "AdminApi", "platform-control-plane"));
        values.add(new Ownership(Transport.HTTP, "/health", "accountServer", "platform-runtime"));
        values.add(new Ownership(Transport.HTTP, "/api/v2/account/*", "Account", "platform-identity"));
        values.add(new Ownership(Transport.HTTP, "/api/v2/club/*", LOBBY_CLUB_OWNER, "platform-hall"));
        values.add(new Ownership(Transport.HTTP, "/api/v2/hall/*", "Lobby", "platform-hall"));
        values.add(new Ownership(Transport.HTTP, "/api/v2/replay/*", "Replay", "platform-replay"));
        values.add(new Ownership(Transport.HTTP, "/api/v2/room/*", "Room", "realtime-platform"));
        values.add(new Ownership(Transport.WSS, "account.*", "Account", "platform-identity"));
        values.add(new Ownership(Transport.WSS, "club.*", LOBBY_CLUB_OWNER, "platform-hall"));
        values.add(new Ownership(Transport.WSS, "common.*", "Gateway", "realtime-platform"));
        values.add(new Ownership(Transport.WSS, "game.*", "Gateway", "realtime-platform"));
        values.add(new Ownership(Transport.WSS, "hall.*", "Lobby", "platform-hall"));
        values.add(new Ownership(Transport.WSS, "mahjong.*", "Mahjong", "game-mahjong"));
        values.add(new Ownership(Transport.WSS, "poker.*", "Poker", "game-poker"));
        values.add(new Ownership(Transport.WSS, "replay.*", "Replay", "platform-replay"));
        values.add(new Ownership(Transport.WSS, "room.*", "Room", "realtime-platform"));
        values.add(new Ownership(Transport.WSS, "system.*", "Gateway", "realtime-platform"));
        values.add(new Ownership(Transport.WSS, "long-card.*", "LongCard", "game-long-card"));
        values.add(new Ownership(Transport.WSS, "word-card.*", "WordCard", "game-word-card"));
        values.add(new Ownership(Transport.WSS, "family.*", "Families", "game-families"));
        return new ApiOwnershipCatalog(values);
    }
}
