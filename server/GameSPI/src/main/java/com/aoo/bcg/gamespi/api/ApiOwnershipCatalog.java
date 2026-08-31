package com.aoo.bcg.gamespi.api;

import java.util.ArrayList;
import java.util.List;

/** Authoritative ownership directory for every public HTTP route and WSS namespace. */
public final class ApiOwnershipCatalog {
    public enum Transport { HTTP, WSS }
    public record Ownership(Transport transport, String pattern, String ownerModule, String maintainerTeam) {
        public Ownership {
            if (transport == null || pattern == null || pattern.isBlank() || ownerModule == null || ownerModule.isBlank()
                    || maintainerTeam == null || maintainerTeam.isBlank()) throw new IllegalArgumentException("incomplete API ownership");
        }
        boolean matches(Transport requestedTransport, String endpoint) {
            if (transport != requestedTransport) return false;
            return pattern.endsWith(".*") ? endpoint.startsWith(pattern.substring(0, pattern.length() - 1)) : pattern.equals(endpoint);
        }
    }

    private final List<Ownership> entries;
    public ApiOwnershipCatalog(List<Ownership> entries) {
        this.entries = List.copyOf(entries);
        for (int left=0; left<this.entries.size(); left++) for (int right=left+1; right<this.entries.size(); right++)
            if (this.entries.get(left).transport()==this.entries.get(right).transport()
                    && this.entries.get(left).pattern().equals(this.entries.get(right).pattern()))
                throw new IllegalArgumentException("duplicate API ownership pattern: " + this.entries.get(left).pattern());
    }
    public Ownership requireOwner(Transport transport, String endpoint) {
        return entries.stream().filter(value -> value.matches(transport, endpoint)).findFirst()
            .orElseThrow(() -> new IllegalStateException("public API has no owner: " + transport + " " + endpoint));
    }
    public List<Ownership> entries() { return entries; }

    public static ApiOwnershipCatalog standard() {
        List<Ownership> values = new ArrayList<>();
        for (String route : List.of("/api/v2/admin/game-profiles", "/api/v2/admin/operation-switches",
                "/api/v2/admin/appeals", "/api/v2/admin/reconciliation", "/api/v2/admin/data-lifecycle"))
            values.add(new Ownership(Transport.HTTP, route, "AdminApi", "platform-control-plane"));
        values.add(new Ownership(Transport.HTTP, "/health", "accountServer", "platform-runtime"));
        values.add(new Ownership(Transport.HTTP, "/api/v2/account/*", "Account", "platform-identity"));
        values.add(new Ownership(Transport.HTTP, "/api/v2/club/*", "Families", "game-families"));
        values.add(new Ownership(Transport.HTTP, "/api/v2/hall/*", "Hall", "platform-hall"));
        values.add(new Ownership(Transport.HTTP, "/api/v2/replay/*", "Replay", "platform-replay"));
        values.add(new Ownership(Transport.HTTP, "/api/v2/room/*", "Room", "realtime-platform"));
        values.add(new Ownership(Transport.WSS, "account.*", "Account", "platform-identity"));
        values.add(new Ownership(Transport.WSS, "club.*", "Families", "game-families"));
        values.add(new Ownership(Transport.WSS, "common.*", "Gateway", "realtime-platform"));
        values.add(new Ownership(Transport.WSS, "game.*", "Gateway", "realtime-platform"));
        values.add(new Ownership(Transport.WSS, "hall.*", "Hall", "platform-hall"));
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
