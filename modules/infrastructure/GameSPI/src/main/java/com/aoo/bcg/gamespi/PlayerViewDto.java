package com.aoo.bcg.gamespi;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shared public player projection for lobby, club, room, reconnect and settlement UI. */
public record PlayerViewDto(long playerId, int seatId, String nickname, String avatarUrl,
                            boolean owner, boolean dealer, boolean online, boolean ready,
                            boolean trusteeship) {
    public PlayerViewDto {
        if (playerId <= 0 || seatId < 0 || nickname == null || nickname.isBlank() || avatarUrl == null)
            throw new IllegalArgumentException("invalid player view");
    }
    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("playerId", playerId); value.put("seatId", seatId); value.put("nickname", nickname);
        value.put("avatarUrl", avatarUrl); value.put("owner", owner); value.put("dealer", dealer);
        value.put("online", online); value.put("ready", ready); value.put("trusteeship", trusteeship);
        return Map.copyOf(value);
    }
}
