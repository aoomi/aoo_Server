package com.aoo.bcg.gateway;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerAuthorityInputGuardTest {
    @Test void requiresAndMatchesServerStateVersionForCanonicalActions() {
        assertThrows(SecurityException.class,
                () -> ServerAuthorityInputGuard.validate("game.action", Map.of("intent", Map.of()), 4));
        assertThrows(SecurityException.class,
                () -> ServerAuthorityInputGuard.validate("game.action", Map.of("expectedStateVersion", 3), 4));
        assertDoesNotThrow(() -> ServerAuthorityInputGuard.validate("game.action",
                Map.of("expectedStateVersion", 4, "intent", Map.of("cards", java.util.List.of(7))), 4));
    }

    @Test void rejectsNestedClientClaimsForServerOwnedState() {
        assertThrows(SecurityException.class, () -> ServerAuthorityInputGuard.validate("poker.dispatch",
                Map.of("payload", Map.of("balance", 999_999)), 0));
        assertThrows(SecurityException.class, () -> ServerAuthorityInputGuard.validate("mahjong.dispatch",
                Map.of("payload", Map.of("hands", Map.of(0, java.util.List.of(1, 2, 3)))), 0));
        assertThrows(SecurityException.class, () -> ServerAuthorityInputGuard.validate("game.action",
                Map.of("expectedStateVersion", 0, "stateVersion", 9), 0));
    }
}
