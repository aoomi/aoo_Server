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
        assertTrue(source.contains("reason=authority-runtime-missing"));
        assertTrue(source.contains("catch (IllegalArgumentException missingRoom)"));
        assertTrue(source.contains("reason=authority-route-"));
        assertTrue(source.contains("lifecycle.isTerminal()"));
        assertTrue(source.contains("reason=authority-session-terminal"));
        assertTrue(source.contains("isTerminalAuthoritySession(session, authorityState)"));
        assertTrue(source.contains("authorityState.get(\"roomTerminal\")"));
        assertTrue(source.contains("authorityState.get(\"dissolved\")"));
        assertTrue(source.contains("authorityState.get(\"phase\")"));
        assertTrue(source.contains("\"FINISHED\".equalsIgnoreCase"));
    }
}
