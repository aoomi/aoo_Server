package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.common.persistence.PersistenceCapability;
import com.aoo.bcg.config.GameConfigurationRepository;
import com.aoo.bcg.config.PublishedGameConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
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

        assertEquals(533, games.size());
        assertEquals(365, games.values().stream().filter(game -> game.category() == GameCategory.MAHJONG).count());
        assertEquals(154, games.values().stream().filter(game -> game.category() == GameCategory.POKER).count());
        assertEquals(10, games.values().stream().filter(game -> game.category() == GameCategory.WORD_CARD).count());
        assertEquals(4, games.values().stream().filter(game -> game.category() == GameCategory.LONG_CARD).count());
        assertEquals(GameCategory.MAHJONG, games.get(516).category());
        assertEquals("mahjong:xue-zhan", games.get(516).family());
        assertEquals("cdxzmj", games.get(516).code());
        assertEquals(GameCategory.POKER, games.get(629).category());
        assertEquals("poker:pao-de-kuai", games.get(629).family());
        assertEquals("njpdk", games.get(629).code());
        assertEquals(GameCategory.MAHJONG, games.get(628).category());
        assertEquals("scjymj", games.get(628).code());
        assertEquals(GameCategory.POKER, games.get(618).category());
        assertEquals("poker:pao-de-kuai", games.get(618).family());
        assertEquals("xcpdk", games.get(618).code());
        assertEquals(GameCategory.POKER, games.get(9).category());
        assertEquals("poker:compare-hand", games.get(9).family());
        assertEquals("zjh", games.get(9).code());
        assertEquals(GameCategory.POKER, games.get(62).category());
        assertEquals("poker:generic-card-round", games.get(62).family());
        assertEquals("zypk", games.get(62).code());
        assertNotEquals(games.get(516).version(), "");
        assertNotEquals(games.get(629).version(), "");
        assertNotEquals(games.get(628).version(), "");
        assertNotEquals(games.get(618).version(), "");
        assertNotEquals(games.get(9).version(), "");
        assertNotEquals(games.get(62).version(), "");
        games.keySet().forEach(gameId -> {
            var provider = registry.require(gameId);
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

    @Test void initializesEveryPackagedGameEntrypoint() {
        assertDoesNotThrow(() -> Class.forName("core.server.njpdk.NJPDKAPP", true,
                Thread.currentThread().getContextClassLoader()));
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
