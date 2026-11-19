package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gateway.GatewayRuntimeProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionGatewayReadinessContractTest {
    @Test
    void productionAdapterDelegatesReadinessToTheJdbcAuthority() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/bootstrap/ProductionGatewayRuntimeProvider.java"));
        assertTrue(source.contains("public GatewayRuntimeProvider.Readiness readiness()"));
        assertTrue(source.contains("return authority.readiness();"));
        assertTrue(GatewayRuntimeProvider.Readiness.notReady().code().matches("[A-Z0-9_]+"));
    }
}
