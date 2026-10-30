package com.aoo.bcg.gateway;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source contract for the real internal destroy ingress. */
final class RoomDestroyRouteTest {
    @Test
    void internalRemoveUsesTypedDestroyAndNotLegacyVoidRemoval() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/gateway/GatewayApplication.java"));
        assertTrue(source.contains("else if(path.endsWith(\"/remove\")){data=authority.destroy(body);"));
        assertTrue(source.contains("status=destroyHttpStatus(data)"));
        assertTrue(source.contains("destroyHttpStatus"));
        assertFalse(source.contains("authority.remove(((Number)body.get(\"roomId\"))"));
    }

    @Test
    void typedStatusMappingIsMachineReadable() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/gateway/GatewayApplication.java"));
        assertTrue(source.contains("case \"DESTROYED\",\"ALREADY_DESTROYED\"->200"));
        assertTrue(source.contains("case \"CAS_CONFLICT\"->409"));
        assertTrue(source.contains("case \"NOT_FOUND\"->404"));
        assertTrue(source.contains("errorCode(data)"));
    }
}
