package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CanonicalRuntimeMetadataManifestPackagingTest {
    private static final String RESOURCE = "runtime-metadata-canonical-manifest.json";
    private static final String EXPECTED_SHA256 =
            System.getProperty("aoo.runtime.metadata.expectedSha256",
                    "4e1f611b56d7c8039bceb407b687fb0d27ea9d2e227df1c5ab5f1546d0e985e2");
    private static final String EXPECTED_EDITION =
            System.getProperty("aoo.runtime.metadata.expectedEdition", "r50-current-20261011");

    @Test
    void classpathContainsExactlyOneSignedCanonicalProjection() throws Exception {
        ClassLoader loader = CanonicalRuntimeMetadataManifestReader.class.getClassLoader();
        Enumeration<URL> found = loader.getResources(RESOURCE);
        List<URL> resources = new ArrayList<>();
        found.asIterator().forEachRemaining(resources::add);
        assertEquals(1, resources.size(), "canonical runtime metadata resource must be singleton");
        byte[] bytes;
        try (InputStream input = resources.getFirst().openStream()) {
            bytes = input.readAllBytes();
        }
        RuntimeMetadataProjection projection = CanonicalRuntimeMetadataManifestReader.read(bytes);
        assertEquals(EXPECTED_SHA256,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        assertEquals(EXPECTED_SHA256, projection.manifestSha256());
        assertEquals(EXPECTED_EDITION, projection.edition());
        assertEquals(RuntimeMetadataProjection.REQUIRED_ROW_COUNT, projection.rowCount());
        assertEquals(RuntimeMetadataProjection.REQUIRED_FIELDS,
                projection.manifestEnvelope().get("requiredFields"));
        assertEquals("common-room-v1", projection.rows().getFirst().contractVersion());
        assertNotNull(loader.getResource(RESOURCE));
    }
}
