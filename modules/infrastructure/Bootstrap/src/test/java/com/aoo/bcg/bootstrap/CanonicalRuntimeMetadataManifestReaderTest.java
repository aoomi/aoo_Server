package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aoo.bcg.gamespi.RuntimeMetadataProjection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

final class CanonicalRuntimeMetadataManifestReaderTest {
    @Test
    void readsOnlyTheSignedReleaseResourceShapeAndBindsItsByteHash() throws Exception {
        byte[] bytes = manifest(32);

        RuntimeMetadataProjection projection = CanonicalRuntimeMetadataManifestReader.read(bytes);

        assertEquals(32, projection.rowCount());
        assertEquals("r9-v4", projection.edition());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                projection.manifestSha256());
        assertEquals("protocol-v2", projection.require(32).protocolVersion());
    }

    @Test
    void missingRowsAndMissingReleaseEditionAreRejected() {
        assertThrows(IllegalStateException.class,
                () -> CanonicalRuntimeMetadataManifestReader.read(manifest(31)));
        String missingEdition = new String(manifest(32), StandardCharsets.UTF_8)
                .replace(",\"releaseEdition\":\"r9-v4\"", "");
        assertThrows(IllegalStateException.class,
                () -> CanonicalRuntimeMetadataManifestReader.read(
                        missingEdition.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] manifest(int rows) {
        StringBuilder json = new StringBuilder()
                .append("{\"manifestKind\":\"runtime-metadata-canonical\",")
                .append("\"manifestVersion\":\"r9-v4\",\"schemaVersion\":1,")
                .append("\"manifestStatus\":\"SIGNED_CANONICAL_INPUT\",\"consumable\":true,")
                .append("\"requiredFields\":[\"snapshotSchemaVersion\",\"cardCodecVersion\",")
                .append("\"eventInterpreterVersion\",\"protocolVersion\",\"sourceHash\",")
                .append("\"releaseId\",\"indexGeneration\"],\"rows\":[");
        for (int gameId = 1; gameId <= rows; gameId++) {
            if (gameId > 1) json.append(',');
            json.append("{\"gameId\":").append(gameId)
                    .append(",\"gameCode\":\"game").append(gameId)
                    .append("\",\"family\":\"family").append(gameId)
                    .append("\",\"playVersion\":\"play-v").append(gameId)
                    .append("\",\"regionCode\":\"region-").append(gameId)
                    .append("\",\"snapshotSchemaVersion\":\"snapshot-v1\"")
                    .append(",\"cardCodecVersion\":\"card-v3\"")
                    .append(",\"eventInterpreterVersion\":\"events-v4\"")
                    .append(",\"protocolVersion\":\"protocol-v2\"")
                    .append(",\"sourceHash\":\"")
                    .append(String.format("%064x", gameId))
                    .append("\",\"releaseId\":").append(1000 + gameId)
                    .append(",\"indexGeneration\":").append(2000 + gameId).append('}');
        }
        return json.append("],\"releaseEdition\":\"r9-v4\"}")
                .toString().getBytes(StandardCharsets.UTF_8);
    }
}
