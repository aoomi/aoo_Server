package com.aoo.bcg.gamespi.api;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ApiLifecycleCatalogTest {
    @Test void everyInterfaceMessageAndFieldHasIntroducedVersion() {
        var catalog = ApiLifecycleCatalog.standard();
        assertFalse(catalog.contracts().isEmpty());
        for (var contract : catalog.contracts()) {
            assertNotNull(contract.introducedVersion());
            assertFalse(contract.fields().isEmpty());
            contract.fields().values().forEach(field -> assertNotNull(field.introducedVersion()));
        }
        assertEquals("2.0.0", catalog.require(ApiOwnershipCatalog.Transport.WSS,
            "poker.paodekuai.play_cards_req").introducedVersion().toString());
    }
}
