package com.aoo.bcg.gamespi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandSchemaTest {
    private final CommandSchema schema = CommandSchema.of(
            CommandFieldSpec.integer("card", true, 1, 54),
            CommandFieldSpec.text("action", true, Set.of("PLAY", "PASS")),
            CommandFieldSpec.list("targets", false, 4),
            CommandFieldSpec.serverDerived("score", CommandFieldTrust.SERVER_DERIVED),
            CommandFieldSpec.serverDerived("playerId", CommandFieldTrust.SERVER_IDENTITY));

    @Test void validatesTypeRangeEnumSizeAndClosedWorld() {
        schema.validate(CommandPayload.copyOf(Map.of("card", 3, "action", "PLAY", "targets", java.util.List.of(1, 2))));
        assertThrows(IllegalArgumentException.class, () -> schema.validate(CommandPayload.copyOf(Map.of("card", 55, "action", "PLAY"))));
        assertThrows(IllegalArgumentException.class, () -> schema.validate(CommandPayload.copyOf(Map.of("card", new BigDecimal("3.1"), "action", "PLAY"))));
        assertThrows(IllegalArgumentException.class, () -> schema.validate(CommandPayload.copyOf(Map.of("card", BigInteger.ONE.shiftLeft(80), "action", "PLAY"))));
        assertThrows(IllegalArgumentException.class, () -> schema.validate(CommandPayload.copyOf(Map.of("card", 3, "action", "CHEAT"))));
        assertThrows(IllegalArgumentException.class, () -> schema.validate(CommandPayload.copyOf(Map.of("card", 3, "action", "PLAY", "extra", true))));
        assertThrows(SecurityException.class, () -> schema.validate(CommandPayload.copyOf(Map.of("card", 3, "action", "PLAY", "score", 99))));
        assertThrows(SecurityException.class, () -> schema.validate(CommandPayload.copyOf(Map.of("card", 3, "action", "PLAY", "playerId", 9))));
    }

    @Test void bindsAllAuthoritativeFieldsOnlyFromServerContext() {
        var context = new AuthoritativeCommandContext(88, 3, "rules-v7", "user-9", 2,
                "connection-new", 4, 11);
        GameCommandRequest request = context.bind(new ClientCommandIntent(
                "game.play_req", "request-1", 5, Map.of("card", 8, "action", "PLAY")), schema);
        assertEquals(88, request.roomId());
        assertEquals(3, request.roundNo());
        assertEquals("rules-v7", request.playVersion());
        assertEquals("user-9", request.authenticatedUserId());
        assertEquals(2, request.seatId());
    }
}
