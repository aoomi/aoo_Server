package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class GatewayAuthorityLeaveContractTest {
    @Test
    void anAlreadyAbsentAuthoritySeatIsAnIdempotentLeaveSuccess() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        assertTrue(source.contains("\"status\", \"ALREADY_LEFT\""));
        assertTrue(source.contains("seatNo < 0"));
        assertTrue(source.contains("session.stateVersion()"));
    }
}
