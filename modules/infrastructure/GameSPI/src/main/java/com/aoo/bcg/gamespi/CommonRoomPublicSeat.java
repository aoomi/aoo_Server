package com.aoo.bcg.gamespi;

import java.util.LinkedHashMap;
import java.util.Map;

/** Public, game-neutral identity for one physical room seat. */
public record CommonRoomPublicSeat(int seatId, boolean occupied, Long playerId,
        String displayName, String avatarUrl, Long score) {
    public CommonRoomPublicSeat {
        if (seatId < 0 || seatId > 9) throw new IllegalArgumentException("seatId must be 0..9");
        if (!occupied && (playerId != null || displayName != null || avatarUrl != null || score != null)) {
            throw new IllegalArgumentException("vacant seat cannot expose identity or score");
        }
        if (occupied && (playerId == null || playerId <= 0)) {
            throw new IllegalArgumentException("occupied seat requires positive playerId");
        }
        displayName = optionalText(displayName, "displayName");
        avatarUrl = optionalText(avatarUrl, "avatarUrl");
    }

    public Map<String,Object> asMap() {
        var value = new LinkedHashMap<String,Object>();
        value.put("seatId", seatId);
        value.put("occupied", occupied);
        if (playerId != null) value.put("playerId", playerId);
        if (displayName != null) value.put("displayName", displayName);
        if (avatarUrl != null) value.put("avatarUrl", avatarUrl);
        if (score != null) value.put("score", score);
        return Map.copyOf(value);
    }

    private static String optionalText(String value, String field) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " cannot be blank");
        return normalized;
    }
}
