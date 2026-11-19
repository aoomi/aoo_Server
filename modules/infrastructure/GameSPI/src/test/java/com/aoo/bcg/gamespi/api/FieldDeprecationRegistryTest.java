package com.aoo.bcg.gamespi.api;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class FieldDeprecationRegistryTest {
    @Test void stagesStopProductionConsumptionAndMajorRemoval() {
        var v1=SemanticVersion.parse("1.0.0");var fields=Map.of("oldField",new ApiLifecycleCatalog.FieldMetadata("oldField",v1));
        var lifecycle=new ApiLifecycleCatalog(List.of(new ApiLifecycleCatalog.Contract(ApiOwnershipCatalog.Transport.WSS,"common.room.join_req",v1,fields)));
        var registry=new FieldDeprecationRegistry(lifecycle);var produced=new AtomicLong(1);var consumed=new AtomicLong(1);
        registry.register(new FieldDeprecationRegistry.FieldDeprecation(ApiOwnershipCatalog.Transport.WSS,"common.room.join_req","oldField",v1,
            SemanticVersion.parse("1.2.0"),SemanticVersion.parse("1.4.0"),2,"newField"),produced::get,consumed::get);
        assertThrows(IllegalStateException.class,()->registry.requireCanProduce(ApiOwnershipCatalog.Transport.WSS,"common.room.join_req","oldField",SemanticVersion.parse("1.2.0")));
        assertThrows(IllegalStateException.class,()->registry.requireCanConsume(ApiOwnershipCatalog.Transport.WSS,"common.room.join_req","oldField",SemanticVersion.parse("1.4.0")));
        assertFalse(registry.removable(ApiOwnershipCatalog.Transport.WSS,"common.room.join_req","oldField",SemanticVersion.parse("2.0.0")));
        produced.set(0);consumed.set(0);assertTrue(registry.removable(ApiOwnershipCatalog.Transport.WSS,"common.room.join_req","oldField",SemanticVersion.parse("2.0.0")));
    }
}
