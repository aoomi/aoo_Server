package com.aoo.bcg.gateway;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecureWsTicketServiceTest {
    @Test
    void consumesTicketExactlyOnceWithBoundIdentity() {
        SecureWsTicketService service = new SecureWsTicketService(new SecureRandom(), Clock.systemUTC());
        String ticket = service.issue(7, "device-a", "https://game.example", Duration.ofSeconds(30));

        assertEquals(7, service.consumeOnce(ticket, "https://game.example").userId());
        assertThrows(SecurityException.class, () -> service.consumeOnce(ticket, "https://game.example"));
    }

    @Test
    void rejectsWrongOriginAndExcessiveLifetime() {
        SecureWsTicketService service = new SecureWsTicketService(new SecureRandom(), Clock.systemUTC());
        String ticket = service.issue(7, "device-a", "https://game.example", Duration.ofSeconds(30));

        assertThrows(SecurityException.class, () -> service.consumeOnce(ticket, "https://evil.example"));
        assertThrows(IllegalArgumentException.class,
                () -> service.issue(7, "device-a", "https://game.example", Duration.ofSeconds(31)));
    }
}
