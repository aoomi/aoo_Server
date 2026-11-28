package com.aoo.bcg.bootstrap;

import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import com.aoo.bcg.hall.room.JdbcHallRepository;
import com.aoo.bcg.hall.room.RoomMembershipAuthorityPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LocalMirrorJdbcHallCatalogIntegrationTest {
    @AfterEach
    void clearMode() {
        System.clearProperty(CanonicalRuntimeMetadataManifestReader.MODE_PROPERTY);
    }

    @Test
    void isolatedImportedDumpPublishesAllLocalMirrorCatalogAndConfigurations() {
        String url = System.getenv("AOO_LOCAL_MIRROR_INTEGRATION_URL");
        String user = System.getenv("AOO_LOCAL_MIRROR_INTEGRATION_USER");
        String password = System.getenv("AOO_LOCAL_MIRROR_INTEGRATION_PASSWORD");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "isolated database URL not supplied");
        Assumptions.assumeTrue(user != null && !user.isBlank(), "isolated database user not supplied");

        System.setProperty(CanonicalRuntimeMetadataManifestReader.MODE_PROPERTY, "local-mirror");
        var projection = CanonicalRuntimeMetadataManifestReader.bundled().projection();
        var source = new DriverManagerDataSource(url, user, password == null ? "" : password);
        RoomMembershipAuthorityPort authority = (RoomMembershipAuthorityPort) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{RoomMembershipAuthorityPort.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError("catalog/configuration must not call room membership authority");
                });
        var repository = new JdbcHallRepository(
                source, new ObjectMapper().findAndRegisterModules(), Clock.systemUTC(), authority, projection);

        List<Map<String, Object>> catalog = repository.catalog("ALL", "3.8.8");
        assertEquals(32, catalog.size());
        for (Map<String, Object> game : catalog) {
            long gameId = ((Number) game.get("gameId")).longValue();
            String playVersion = String.valueOf(game.get("playVersion"));
            String regionCode = String.valueOf(game.get("classificationRegionCode"));
            Map<String, Object> configuration = repository.configuration(
                    gameId, regionCode, playVersion, "3.8.8");
            assertEquals(gameId, ((Number) configuration.get("gameId")).longValue());
            assertEquals(playVersion, configuration.get("playVersion"));
            assertEquals(projection.manifestSha256(), configuration.get("manifestSha256"));
        }
    }
}
