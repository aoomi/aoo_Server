package com.aoo.bcg.club;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClubHttpRoutesErrorContractTest {
    @Test void mapsDistinctFailureRootsToCanonicalCodes() {
        assertEquals("UPSTREAM_UNAVAILABLE", ClubHttpRoutes.canonicalCode(new IllegalStateException("room authority unavailable")));
        assertEquals("PERMISSION_DENIED", ClubHttpRoutes.canonicalCode(new SecurityException("manager required")));
        assertEquals("CONFIG_NOT_FOUND", ClubHttpRoutes.canonicalCode(new IllegalStateException("template missing")));
        assertEquals("ROOM_STATE_CONFLICT", ClubHttpRoutes.canonicalCode(new IllegalStateException("active game")));
        assertEquals("REQUEST_REPLAYED", ClubHttpRoutes.canonicalCode(new IllegalStateException("club exists")));
        assertEquals("INVALID_ENVELOPE", ClubHttpRoutes.canonicalCode(new IllegalArgumentException("unknown action")));
    }

    @Test void preservesTheTenSharedCategoriesForClubProviderAndConfigurationFailures() {
        assertEquals("IDENTITY_NOT_FOUND", ClubHttpRoutes.canonicalCode(new IllegalStateException("HALL_IDENTITY_NOT_FOUND")));
        assertEquals("CONFIG_NOT_FOUND", ClubHttpRoutes.canonicalCode(new IllegalStateException("HALL_CONFIGURATION_NOT_FOUND")));
        assertEquals("PROVIDER_MISSING", ClubHttpRoutes.canonicalCode(new IllegalStateException("HALL_GAME_PROVIDER_MISSING")));
        assertEquals("PRICING_POLICY_MISSING", ClubHttpRoutes.canonicalCode(new IllegalStateException("HALL_COST_POLICY_MISSING")));
        assertEquals("SNAPSHOT_MISSING", ClubHttpRoutes.canonicalCode(new IllegalStateException("HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE")));
        assertEquals("IDENTITY_MISMATCH", ClubHttpRoutes.canonicalCode(new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_REQUIRED")));
        assertEquals("VERSION_UNSUPPORTED", ClubHttpRoutes.canonicalCode(new IllegalStateException("HALL_VERSION_UNSUPPORTED")));
        assertEquals("UPSTREAM_UNAVAILABLE", ClubHttpRoutes.canonicalCode(new IllegalStateException("INTERNAL_AUTHORITY_FAILED")));
        assertEquals("INVALID_ENVELOPE", ClubHttpRoutes.canonicalCode(new IllegalStateException("HALL_INVALID_ENVELOPE")));
        assertEquals("PERMISSION_DENIED", ClubHttpRoutes.canonicalCode(new IllegalStateException("HALL_CLUB_MANAGER_REQUIRED")));
    }
}
