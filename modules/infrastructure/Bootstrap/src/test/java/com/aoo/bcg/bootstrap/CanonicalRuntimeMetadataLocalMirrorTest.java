package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import com.aoo.bcg.hall.http.HallError;
import com.aoo.bcg.lobby.JdbcLobbyRepository;
import com.aoo.bcg.lobby.LobbyMembershipAuthorityPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanonicalRuntimeMetadataLocalMirrorTest {
    @AfterEach
    void clearMode() {
        System.clearProperty(CanonicalRuntimeMetadataManifestReader.MODE_PROPERTY);
    }

    @Test
    void localMirrorModeLoadsOnlyExplicitlyMarkedMirrorResource() {
        System.setProperty(CanonicalRuntimeMetadataManifestReader.MODE_PROPERTY, "local-mirror");
        var projection = CanonicalRuntimeMetadataManifestReader.bundled().projection();
        assertEquals(RuntimeMetadataProjection.Trust.VERIFIED_LOCAL_MIRROR, projection.trust());
        assertEquals(32, projection.rows().size());
        assertEquals("nn-v1.0.0", projection.require(5).playVersion());
        assertEquals("zjh-v1.0.0", projection.require(9).playVersion());
        assertEquals("cx-v1.0.0", projection.require(2999).playVersion());
    }

    @Test
    void localMirrorProjectionAssemblesAllProviders() {
        System.setProperty(CanonicalRuntimeMetadataManifestReader.MODE_PROPERTY, "local-mirror");
        var registry = ProviderCatalogAssembler.production();
        assertEquals(32, registry.descriptors().size());
        assertEquals("nn-v1.0.0", registry.require(5, "nn-v1.0.0").descriptor().version());
        assertEquals("zjh-v1.0.0", registry.require(9, "zjh-v1.0.0").descriptor().version());
        assertEquals("cx-v1.0.0", registry.require(2999, "cx-v1.0.0").descriptor().version());
    }

    @Test
    void localMirrorParserRejectsProductionOrUnmarkedInput() {
        String production = minimal("SIGNED_CANONICAL_INPUT", true);
        String unmarked = minimal("LOCAL_MIRROR_INPUT", false);
        assertThrows(IllegalStateException.class, () -> CanonicalRuntimeMetadataManifestReader.readLocalMirror(
                new ByteArrayInputStream(production.getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalStateException.class, () -> CanonicalRuntimeMetadataManifestReader.readLocalMirror(
                new ByteArrayInputStream(unmarked.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void productionParserRejectsLocalMirrorInput() {
        byte[] mirror = localMirrorBytes();
        assertThrows(IllegalStateException.class, () -> CanonicalRuntimeMetadataManifestReader.read(
                new ByteArrayInputStream(mirror)));
    }

    @Test
    void localMirrorFingerprintDriftFailsClosed() {
        byte[] bytes = localMirrorBytes();
        bytes[bytes.length - 2] = bytes[bytes.length - 2] == ' ' ? (byte) '\n' : (byte) ' ';
        assertThrows(IllegalStateException.class,
                () -> CanonicalRuntimeMetadataManifestReader.readLocalMirror(bytes));
    }

    @Test
    void hallAcceptsVerifiedLocalMirrorOnlyInExplicitMode() {
        System.setProperty(CanonicalRuntimeMetadataManifestReader.MODE_PROPERTY, "local-mirror");
        RuntimeMetadataProjection projection = CanonicalRuntimeMetadataManifestReader.bundled().projection();
        JdbcLobbyRepository repository = repository(projection);
        assertThrows(UnsupportedOperationException.class, () -> repository.catalog("ALL", "3.8.8"));

        System.clearProperty(CanonicalRuntimeMetadataManifestReader.MODE_PROPERTY);
        HallError rejected = assertThrows(HallError.class,
                () -> repository.catalog("ALL", "3.8.8"));
        assertEquals("HALL_RUNTIME_METADATA_PROJECTION_REQUIRED", rejected.code());
    }

    @Test
    void unverifiedProjectionCannotClaimProductionTrust() {
        byte[] bytes = new String(localMirrorBytes(), StandardCharsets.UTF_8)
                .replace("LOCAL_MIRROR_INPUT", "SIGNED_CANONICAL_INPUT")
                .getBytes(StandardCharsets.UTF_8);
        RuntimeMetadataProjection projection = CanonicalRuntimeMetadataManifestReader.read(bytes);
        assertEquals(RuntimeMetadataProjection.Trust.UNVERIFIED, projection.trust());
        HallError rejected = assertThrows(HallError.class,
                () -> repository(projection).catalog("ALL", "3.8.8"));
        assertEquals("HALL_RUNTIME_METADATA_PROJECTION_REQUIRED", rejected.code());
    }

    @Test
    void unknownModeFailsClosed() {
        System.setProperty(CanonicalRuntimeMetadataManifestReader.MODE_PROPERTY, "anything-else");
        assertThrows(IllegalStateException.class, CanonicalRuntimeMetadataManifestReader::bundled);
    }

    private static String minimal(String status, boolean marked) {
        return "{\"schemaVersion\":1,\"manifestKind\":\"runtime-metadata-canonical\","
                + "\"manifestStatus\":\"" + status + "\",\"consumable\":true,"
                + "\"localMirrorOnly\":" + marked + ",\"releaseEdition\":\"test\","
                + "\"requiredFields\":[],\"rows\":[]}";
    }

    private static byte[] localMirrorBytes() {
        try (var input = CanonicalRuntimeMetadataLocalMirrorTest.class.getResourceAsStream(
                CanonicalRuntimeMetadataManifestReader.LOCAL_MIRROR_RESOURCE)) {
            if (input == null) throw new AssertionError("local mirror test resource missing");
            return input.readAllBytes();
        } catch (java.io.IOException error) {
            throw new AssertionError(error);
        }
    }

    private static JdbcLobbyRepository repository(RuntimeMetadataProjection projection) {
        DataSource source = (DataSource) Proxy.newProxyInstance(
                CanonicalRuntimeMetadataLocalMirrorTest.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, arguments) -> {
                    throw new UnsupportedOperationException("trusted projection reached database boundary");
                });
        LobbyMembershipAuthorityPort authority = new LobbyMembershipAuthorityPort() {
            @Override public Map<String, Object> join(long roomId, long accountId, int seatNo,
                    String playVersion, String requestId, String traceId, Map<String, Object> admission) {
                throw new UnsupportedOperationException();
            }
            @Override public Map<String, Object> leave(long roomId, long accountId, String playVersion,
                    String requestId, String traceId, Map<String, Object> completeIdentity) {
                throw new UnsupportedOperationException();
            }
        };
        return new JdbcLobbyRepository(source, new ObjectMapper(), Clock.systemUTC(), authority, projection);
    }
}
