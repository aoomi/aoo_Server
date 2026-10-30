package com.aoo.bcg.gamespi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalIdentityRegistryContractTest {
    private static final String SHA = "a".repeat(64);
    private static final List<Integer> CURRENT_IDS = List.of(
            5, 9, 630, 1001, 2001, 2002, 2003, 2004, 2005, 2006,
            2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015,
            2016, 2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024,
            2025, 2026, 2027, 2028);

    @Test
    void oneImmutableEntryFeedsAllFiveProjectionBoundaries() {
        CanonicalIdentityRegistry registry = new CanonicalIdentityRegistry(SHA, "test-edition", entries(false));
        CanonicalIdentityRegistry.Projection projection = registry.project(
                5, "game-5", "family-5", "play-5", "region-5");
        assertSame(projection.serverDescriptor(), projection.provider());
        assertSame(projection.serverDescriptor(), projection.lobby());
        assertSame(projection.serverDescriptor(), projection.gateway());
        assertSame(projection.serverDescriptor(), projection.client());
    }

    @Test
    void missingContractEvidenceFailsClosedAtIdentityAdmission() {
        CanonicalIdentityRegistry registry = new CanonicalIdentityRegistry(SHA, "test-edition", entries(true));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> registry.requireIdentity(5, "game-5", "family-5", "play-5", "region-5"));
        assertEquals(true, error.getMessage().contains("IDENTITY_CONTRACT_PROJECTION_FAIL_CLOSED"));
    }

    @Test
    void rowCountAndHistoricalIdentityAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalIdentityRegistry(SHA, "test-edition", entries(false).subList(0, 31)));
        ArrayList<CanonicalIdentityRegistry.Entry> historical = new ArrayList<>(entries(false));
        historical.set(0, new CanonicalIdentityRegistry.Entry(516, "game-516", "family-516",
                "play-516", "region-516", "provider-516", "client-516", "common-room-v1",
                CanonicalIdentityRegistry.Status.CONSUMABLE, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalIdentityRegistry(SHA, "test-edition", historical));
    }

    private static List<CanonicalIdentityRegistry.Entry> entries(boolean failFirst) {
        return CURRENT_IDS.stream().map(id -> new CanonicalIdentityRegistry.Entry(
                id, "game-" + id, "family-" + id, "play-" + id, "region-" + id,
                failFirst && id == 5 ? null : "provider-" + id,
                "client-" + id,
                failFirst && id == 5 ? null : "common-room-v1",
                failFirst && id == 5 ? CanonicalIdentityRegistry.Status.FAIL_CLOSED : CanonicalIdentityRegistry.Status.CONSUMABLE,
                failFirst && id == 5 ? List.of("CONTRACT_VERSION_MISSING_FROM_FORMAL_RELEASE_SOURCE") : List.of()))
                .toList();
    }
}
