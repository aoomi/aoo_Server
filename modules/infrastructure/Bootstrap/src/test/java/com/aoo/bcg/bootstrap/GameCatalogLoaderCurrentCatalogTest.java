package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class GameCatalogLoaderCurrentCatalogTest {
    @Test
    void productionUsesCurrentPublishedCatalogAndCompatibilityUsesHistoricalCatalog() {
        assertEquals("/game-catalog-current-32.tsv", GameCatalogLoader.catalogResourceFor(true));
        assertEquals("/game-catalog-528.tsv", GameCatalogLoader.catalogResourceFor(false));
    }

    @Test
    void currentPublishedCatalogContainsExactlyThirtyTwoRows() throws Exception {
        try (var input = GameCatalogLoader.class.getResourceAsStream("/game-catalog-current-32.tsv")) {
            assertNotNull(input);
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                assertEquals(33L, reader.lines().count());
            }
        }
    }
}
