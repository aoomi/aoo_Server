package com.aoo.bcg.gamespi;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerViewDtoTest {
    @Test void exportsEverySharedUiIdentityAndStatusField() {
        PlayerViewDto value = new PlayerViewDto(7, 2, "player-7", "https://avatar/7", true, false, false, true, true);
        assertEquals(java.util.Set.of("playerId", "seatId", "nickname", "avatarUrl", "owner", "dealer", "online", "ready", "trusteeship"), value.toMap().keySet());
        assertEquals(7L, value.toMap().get("playerId")); assertEquals(false, value.toMap().get("online"));
        assertThrows(IllegalArgumentException.class, () -> new PlayerViewDto(0, 0, "x", "", false, false, true, false, false));
    }
}
