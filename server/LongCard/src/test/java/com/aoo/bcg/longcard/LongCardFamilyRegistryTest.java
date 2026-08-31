package com.aoo.bcg.longcard;

import com.aoo.bcg.gamespi.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class LongCardFamilyRegistryTest {
    @Test void allFourRowsUseOneFamilyProviderAndKeepRegionalSessions() {
        for (var row : List.of(new Object[]{80,"aydss"},new Object[]{138,"aycp"},new Object[]{210,"zgcp"},new Object[]{211,"zgdss"})) {
            var descriptor = new GameDescriptor((int)row[0],(String)row[1],(String)row[1],GameCategory.LONG_CARD,"long-card-regional",RegionScope.PROVINCE,"sichuan","","1.0.0");
            var provider = LongCardCatalogRuntimeRegistry.providerFor(descriptor).orElseThrow();
            assertEquals(LongCardFamilyProvider.class, provider.getClass());
            assertEquals(descriptor, provider.descriptor());
            assertTrue(provider.eventReplayProvider().isPresent());
            assertNotNull(provider.roomFactory());
        }
    }
    @Test void rejectsOtherCategoriesAndUnknownCodes() {
        assertTrue(LongCardCatalogRuntimeRegistry.providerFor(new GameDescriptor(1,"x","x",GameCategory.POKER,"x",RegionScope.NATIONAL,"","","1")).isEmpty());
        assertTrue(LongCardCatalogRuntimeRegistry.providerFor(new GameDescriptor(1,"x","x",GameCategory.LONG_CARD,"x",RegionScope.NATIONAL,"","","1")).isEmpty());
    }
}
