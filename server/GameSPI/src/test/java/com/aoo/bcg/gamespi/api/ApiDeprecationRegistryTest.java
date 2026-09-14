package com.aoo.bcg.gamespi.api;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ApiDeprecationRegistryTest {
    @Test void removalRequiresReasonReplacementObservedZeroUsageAndSunset() {
        var v1=SemanticVersion.parse("1.0.0");
        var field=Map.of("requestId",new ApiLifecycleCatalog.FieldMetadata("requestId",v1));
        var lifecycle=new ApiLifecycleCatalog(List.of(
            new ApiLifecycleCatalog.Contract(ApiOwnershipCatalog.Transport.HTTP,"/api/v1/room/create",v1,field),
            new ApiLifecycleCatalog.Contract(ApiOwnershipCatalog.Transport.HTTP,"/api/v2/room/create",SemanticVersion.parse("2.0.0"),field)));
        var usage=new AtomicLong(3);
        var registry=new ApiDeprecationRegistry(lifecycle,Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"),ZoneOffset.UTC));
        registry.register(new ApiDeprecationRegistry.Deprecation(ApiOwnershipCatalog.Transport.HTTP,"/api/v1/room/create",
            SemanticVersion.parse("2.0.0"),"replaced by authoritative contract","/api/v2/room/create",
            "api.v1.room.create.calls",Instant.parse("2026-08-31T00:00:00Z")),usage::get);
        assertFalse(registry.status(ApiOwnershipCatalog.Transport.HTTP,"/api/v1/room/create").orElseThrow().removable());
        usage.set(0);
        assertTrue(registry.status(ApiOwnershipCatalog.Transport.HTTP,"/api/v1/room/create").orElseThrow().removable());
    }
}
