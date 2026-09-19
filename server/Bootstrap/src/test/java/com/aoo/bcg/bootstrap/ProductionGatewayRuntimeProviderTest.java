package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductionGatewayRuntimeProviderTest {
    @Test void resolvesLegacyPlayersSnapshot() {
        assertEquals(1,ProductionGatewayRuntimeProvider.authoritativeSeat(
                Map.of("players",Map.of(0,100L,1,200L)),200L));
    }

    @Test void resolvesStructuredSeatsSnapshotUsedByCn297() {
        assertEquals(2,ProductionGatewayRuntimeProvider.authoritativeSeat(
                Map.of("seats",Map.of("0",Map.of("playerId",100L),"2",Map.of("playerId",619L))),619L));
    }

    @Test void returnsNullForNonMemberOrMalformedSnapshot() {
        assertNull(ProductionGatewayRuntimeProvider.authoritativeSeat(
                Map.of("seats",Map.of(0,Map.of("nickname","missing-id"))),619L));
        assertNull(ProductionGatewayRuntimeProvider.authoritativeSeat(Map.of(),619L));
    }
}
