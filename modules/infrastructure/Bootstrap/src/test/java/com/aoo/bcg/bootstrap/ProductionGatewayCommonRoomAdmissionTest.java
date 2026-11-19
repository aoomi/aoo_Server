package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ProductionGatewayCommonRoomAdmissionTest {
    @Test void requiresProviderForEveryClientRegisteredFamily() {
        assertTrue(ProductionGatewayRuntimeProvider.requiresCommonRoomPublicView("chengdu2004","poker:pao-de-kuai"));
        assertTrue(ProductionGatewayRuntimeProvider.requiresCommonRoomPublicView("liangshan2003","poker:pao-de-kuai"));
        assertTrue(ProductionGatewayRuntimeProvider.requiresCommonRoomPublicView("chengdu1001","mahjong:xue-zhan"));
        assertTrue(ProductionGatewayRuntimeProvider.requiresCommonRoomPublicView("chengdu2999","poker:cx"));
        assertTrue(ProductionGatewayRuntimeProvider.requiresCommonRoomPublicView("quanguo2998","poker:betting"));
        assertTrue(ProductionGatewayRuntimeProvider.requiresCommonRoomPublicView("quanguo2997","poker:compare-hand"));
    }

    @Test void unrelatedAuthoritativeGamesRemainOptIn() {
        assertFalse(ProductionGatewayRuntimeProvider.requiresCommonRoomPublicView("unrelated-game","poker:other"));
        assertFalse(ProductionGatewayRuntimeProvider.requiresCommonRoomPublicView("LONGCARD","longcard"));
    }
}
