package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameIdentityPolicy;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.common.persistence.PersistenceCapability;
import com.aoo.bcg.config.GameConfigurationRepository;
import com.aoo.bcg.config.PublishedGameConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BootstrapAPPTest {
    @Test void discoversPackagedProvidersWithStableDescriptors() {
        GameRegistry registry = BootstrapAPP.loadRegistry();
        Map<Integer, GameDescriptor> games = registry.descriptors().stream()
                .collect(Collectors.toMap(GameDescriptor::gameId, Function.identity()));

        List<GameDescriptor> catalog = catalogDescriptors();
        List<GameDescriptor> currentCatalog = catalog.stream()
                .filter(game -> !GameIdentityPolicy.isCompatibilityOnly(game))
                .toList();
        Map<Integer, GameDescriptor> catalogById = currentCatalog.stream()
                .collect(Collectors.toMap(GameDescriptor::gameId, Function.identity()));
        assertEquals(currentCatalog.size(), catalogById.size(), "Current Catalog game IDs must be unique");
        assertEquals((long) currentCatalog.size(), catalogById.keySet().stream().filter(games::containsKey).count(),
                "Every current Catalog projection row must be registered");
        currentCatalog.forEach(expected -> assertEquals(expected, games.get(expected.gameId()),
                "registered descriptor must match current Catalog row " + expected.gameId()));

        assertCanonicalIdentity(games, 1001, "chengdu1001", GameCategory.MAHJONG, "mahjong:xue-zhan");
        assertCanonicalIdentity(games, 2001, "chengdu2001", GameCategory.POKER, "poker:pao-de-kuai");
        assertCanonicalIdentity(games, 2002, "neijiang2002", GameCategory.POKER, "poker:pao-de-kuai");
        assertCanonicalIdentity(games, 2003, "liangshan2003", GameCategory.POKER, "poker:pao-de-kuai");
        assertCanonicalIdentity(games, 2999, "chengdu2999", GameCategory.POKER, "poker:cx");
        assertFalse(games.containsKey(630), "historical gameId=630 must not enter the current production registry");
        assertCanonicalIdentity(games, 5, "quanguo2998", GameCategory.POKER, "poker:betting");
        assertCanonicalIdentity(games, 9, "quanguo2997", GameCategory.POKER, "poker:compare-hand");
        games.values().stream()
                .filter(game -> GameIdentityPolicy.isCurrentPublished(game.gameId(), game.version()))
                .forEach(game -> {
            var provider = registry.require(game.gameId(), game.version());
            assertTrue(provider.commandHandler().isPresent(), provider.descriptor().code() + " command SPI");
            assertTrue(provider.reconnectViewProvider().isPresent(), provider.descriptor().code() + " reconnect SPI");
            assertTrue(provider.settlementProvider().isPresent(), provider.descriptor().code() + " settlement SPI");
            assertFalse(provider.ruleComponents().isEmpty(), provider.descriptor().code() + " rule chain");
            assertFalse(provider.defaultConfiguration().isEmpty(), provider.descriptor().code() + " defaults");
        });
        assertTrue(games.values().stream().filter(game -> game.category() == GameCategory.POKER)
                .allMatch(game -> game.family().startsWith("poker:")),
                "published Poker families must use canonical codes");
    }

    @Test void productionCatalogExcludesCompatibilityRowsWhileCompatibilityRetainsThem() {
        List<GameDescriptor> catalog = catalogDescriptors();
        List<GameDescriptor> compatibilityRows = catalog.stream()
                .filter(GameIdentityPolicy::isCompatibilityOnly)
                .toList();
        assertEquals(10, compatibilityRows.size(),
                "the ten historical WORD_CARD rows must stay outside the production projection");
        assertTrue(compatibilityRows.stream().allMatch(game -> game.category() == GameCategory.WORD_CARD),
                "only the explicit WordCard compatibility rows are excluded by this Catalog boundary");

        GameRegistry compatibility = GameRegistryFactory.compatibility();
        assertEquals(catalog.size(), GameCatalogLoader.registerMissing(compatibility),
                "Compatibility must retain every historical Catalog row");
        assertEquals(10, compatibility.descriptors().stream()
                .filter(game -> game.category() == GameCategory.WORD_CARD).count());

        GameRegistry production = BootstrapAPP.loadRegistry();
        assertTrue(production.descriptors().stream()
                .noneMatch(game -> game.category() == GameCategory.WORD_CARD),
                "production must not expose Compatibility-only WordCard rows");
    }

    private static void assertCanonicalIdentity(Map<Integer, GameDescriptor> games, int gameId, String code,
                                                GameCategory category, String family) {
        GameDescriptor descriptor = games.get(gameId);
        assertNotNull(descriptor, "canonical identity must be registered: " + gameId + "/" + code);
        assertEquals(code, descriptor.code());
        assertEquals(category, descriptor.category());
        assertEquals(family, descriptor.family());
        assertFalse(descriptor.version().isBlank());
    }

    private static List<GameDescriptor> catalogDescriptors() {
        List<GameDescriptor> descriptors = new ArrayList<>();
        try (var input = BootstrapAPPTest.class.getResourceAsStream("/game-catalog-528.tsv")) {
            assertNotNull(input, "current Catalog resource must be packaged");
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                assertEquals("gameId\tcode\tdisplayName\tcategory\tfamily\tregionScope\tprovinceCode\tcityCode\tversion\tenabled\tsourceModule", header);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] values = line.split("\\t", -1);
                    assertEquals(11, values.length, "Catalog row must keep its schema");
                    descriptors.add(new GameDescriptor(Integer.parseInt(values[0]), values[1], values[2],
                            GameCategory.valueOf(values[3]), values[4], RegionScope.valueOf(values[5]),
                            values[6], values[7], values[8]));
                }
            }
        } catch (java.io.IOException failure) {
            throw new AssertionError("cannot read current Catalog", failure);
        }
        return List.copyOf(descriptors);
    }

    @Test void initializesPackagedGameEntrypoints() {
        assertDoesNotThrow(() -> Class.forName("core.server.scjymj.SCJYMJAPP", true,
                Thread.currentThread().getContextClassLoader()));
        assertDoesNotThrow(() -> Class.forName("core.server.xcpdk.XCPDKAPP", true,
                Thread.currentThread().getContextClassLoader()));
    }

    @Test void assemblesEveryRequiredProductionPersistenceCapability() {
        GameConfigurationRepository configurations = new GameConfigurationRepository() {
            @Override public <C> Optional<PublishedGameConfiguration<C>> find(int gameId, String playVersion) {
                return Optional.empty();
            }
        };
        ProductionPersistenceAssembly assembly = ProductionPersistenceAssembly.create(
                new NonConnectingDataSource(), new ObjectMapper(), configurations, () -> 1L, Clock.systemUTC());

        assertEquals(Set.of(PersistenceCapability.values()), assembly.bindings().stream()
                .map(binding -> binding.capability()).collect(Collectors.toSet()));
        assertTrue(assembly.bindings().stream().allMatch(binding -> binding.durable() && binding.distributed()));
        assertSame(configurations, assembly.gameConfigurations());
        assertNotNull(assembly.billing());
        assertNotNull(assembly.settlements());
        assertNotNull(assembly.clubMembers());
    }

    private static final class NonConnectingDataSource implements DataSource {
        @Override public Connection getConnection() throws SQLException { throw new SQLException("test must not connect"); }
        @Override public Connection getConnection(String username, String password) throws SQLException { return getConnection(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return Logger.getGlobal(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("not a wrapper"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
