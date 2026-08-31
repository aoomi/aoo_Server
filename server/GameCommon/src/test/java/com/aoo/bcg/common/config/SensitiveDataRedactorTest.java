package com.aoo.bcg.common.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SensitiveDataRedactorTest {
    @Test void masksCredentialsTokensCertificatesAndConnectionStrings() {
        String raw = "Authorization: Bearer abc.def password=hunter2 client_secret:top "
            + "jdbc:mysql://db/app?user=root&password=p4ss "
            + "-----BEGIN PRIVATE KEY-----\nmaterial\n-----END PRIVATE KEY-----";
        String safe = SensitiveDataRedactor.redact(raw);
        for (String secret : new String[]{"abc.def", "hunter2", "top", "p4ss", "material"}) assertFalse(safe.contains(secret));
        assertTrue(safe.contains(SensitiveDataRedactor.MASK));
    }

    @Test void recursivelyMasksStructuredLogsAndThrowableMessages() {
        Map<String,Object> safe = SensitiveDataRedactor.redact(Map.of(
            "databasePassword", "secret-one", "nested", Map.of("access_token", "secret-two"),
            "privateCards", java.util.List.of(1, 2, 3), "identityNumber", "11010519491231002X", "roomId", 7));
        assertEquals(SensitiveDataRedactor.MASK, safe.get("databasePassword"));
        assertFalse(safe.toString().contains("secret-two"));
        assertFalse(safe.toString().contains("11010519491231002X"));
        assertFalse(safe.toString().contains("[1, 2, 3]"));
        assertFalse(SensitiveDataRedactor.safeFailure(new RuntimeException("token=secret-three")).contains("secret-three"));
        assertEquals("identity=" + SensitiveDataRedactor.MASK,
                SensitiveDataRedactor.redact("identity=11010519491231002X"));
    }
}
