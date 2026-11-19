package com.aoo.bcg.common.error;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ErrorCodeCatalogTest {
    @Test void publishedMeaningIsImmutableAndRetiredNumberRemainsReserved(){
        var catalog=ErrorCodeCatalog.standard();
        var original=catalog.require(1009);
        assertThrows(IllegalStateException.class,()->catalog.publish(new ErrorCodeCatalog.Definition(1009,"OTHER","changed","2.1.0",null)));
        catalog.retire(1009,"3.0.0");
        assertEquals("3.0.0",catalog.require(1009).retiredVersion());
        assertThrows(IllegalStateException.class,()->catalog.publish(original));
        assertThrows(IllegalStateException.class,()->catalog.publish(new ErrorCodeCatalog.Definition(1099,"DUPLICATE_SUBMISSION","new use","3.0.0",null)));
    }
    @Test void crossServiceCodesExposeCanonicalMeaningAndPreserveLegacyAliases(){
        var catalog=ErrorCodeCatalog.standard();
        assertEquals("PRICING_POLICY_MISSING",catalog.canonicalSymbolicCode("HALL_COST_POLICY_MISSING"));
        assertEquals(409,catalog.requireSymbolic("HALL_COST_POLICY_MISSING").httpStatus());
        assertEquals("IDENTITY_NOT_FOUND",catalog.canonicalSymbolicCode("HALL_NOT_FOUND"));
        assertEquals("CLUB_LIST_EMPTY",catalog.canonicalSymbolicCode("CLUB_LEGACY_3008_EMPTY_LIST"));
        assertEquals("ROOM_STATE_CONFLICT",catalog.canonicalSymbolicCode("GATEWAY_ROOM_STATE_CONFLICT"));
    }

    @Test void legacyNumericCodesAreScopedAndCannotBeReusedWithinOneTransport(){
        var catalog=ErrorCodeCatalog.standard();
        assertThrows(IllegalStateException.class,()->catalog.publish(new ErrorCodeCatalog.SymbolicDefinition(
                "OTHER_ROOM_STATE","ROOM",409,"other","gateway",3008,java.util.Set.of())));
        assertDoesNotThrow(()->catalog.publish(new ErrorCodeCatalog.SymbolicDefinition(
                "OTHER_CLUB_LEGACY","CLUB",404,"other","club",3009,java.util.Set.of())));
    }

    @Test void requiredTaxonomyIsPublished(){
        var catalog=ErrorCodeCatalog.standard();
        for(String code:java.util.List.of("IDENTITY_NOT_FOUND","CONFIG_NOT_FOUND","PROVIDER_MISSING",
                "PRICING_POLICY_MISSING","SNAPSHOT_MISSING","IDENTITY_MISMATCH","VERSION_UNSUPPORTED",
                "UPSTREAM_UNAVAILABLE","INVALID_ENVELOPE","PERMISSION_DENIED"))
            assertEquals(code,catalog.requireSymbolic(code).code());
    }

    @Test void classificationKeepsIdentityAndCostRootsDistinctFromConfigAndUnknownLegacyCodes(){
        assertEquals("IDENTITY_NOT_FOUND",ErrorCodeCatalog.classify("HALL_IDENTITY_NOT_FOUND",null));
        assertEquals("IDENTITY_MISMATCH",ErrorCodeCatalog.classify("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_REQUIRED",null));
        assertEquals("PRICING_POLICY_MISSING",ErrorCodeCatalog.classify("HALL_COST_POLICY_MISSING",null));
        assertEquals("CONFIG_NOT_FOUND",ErrorCodeCatalog.classify("HALL_CONFIGURATION_NOT_FOUND",null));
        assertEquals("PROVIDER_MISSING",ErrorCodeCatalog.classify("HALL_GAME_PROVIDER_MISSING",null));
        assertEquals("SNAPSHOT_MISSING",ErrorCodeCatalog.classify("HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE",null));
        assertEquals("VERSION_UNSUPPORTED",ErrorCodeCatalog.classify("HALL_VERSION_UNSUPPORTED",null));
        assertEquals("UPSTREAM_UNAVAILABLE",ErrorCodeCatalog.classify("INTERNAL_AUTHORITY_FAILED",null));
        assertEquals("INVALID_ENVELOPE",ErrorCodeCatalog.classify("HALL_INVALID_ENVELOPE",null));
        assertEquals("PERMISSION_DENIED",ErrorCodeCatalog.classify("HALL_CLUB_MANAGER_REQUIRED",null));
        assertEquals("FAIL_CLOSED",ErrorCodeCatalog.classify("516","FAIL_CLOSED"));
    }
}
