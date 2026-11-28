package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import com.aoo.bcg.gamespi.RuntimeMetadataManifestAttestor;
import com.aoo.bcg.gamespi.RuntimeMetadataProjectionSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Reads the release/build-provided manifest resource. This class deliberately
 * has no filesystem path or environment-variable fallback: a production
 * checkout is not a runtime input.
 */
public final class CanonicalRuntimeMetadataManifestReader {
    public static final String DEFAULT_RESOURCE = "/runtime-metadata-canonical-manifest.json";
    public static final String LOCAL_MIRROR_RESOURCE = "/runtime-metadata-local-mirror-manifest.json";
    public static final String MODE_PROPERTY = "aoo.runtime.metadata.mode";
    public static final String LOCAL_MIRROR_SHA256 = RuntimeMetadataManifestAttestor.LOCAL_MIRROR_SHA256;
    private static final String SIGNED_STATUS = "SIGNED_CANONICAL_INPUT";

    private CanonicalRuntimeMetadataManifestReader() { }

    public static RuntimeMetadataProjectionSource bundled() {
        String mode = System.getProperty(MODE_PROPERTY, "production").trim();
        return switch (mode) {
            case "", "production" -> () -> readResource(
                    CanonicalRuntimeMetadataManifestReader.class, DEFAULT_RESOURCE);
            case "local-mirror" -> () -> readLocalMirrorResource(
                    CanonicalRuntimeMetadataManifestReader.class, LOCAL_MIRROR_RESOURCE);
            default -> throw new IllegalStateException("RUNTIME_METADATA_MODE_INVALID mode=" + mode);
        };
    }

    public static RuntimeMetadataProjection readResource(Class<?> anchor, String resourceName) {
        Objects.requireNonNull(anchor, "anchor");
        if (resourceName == null || resourceName.isBlank() || !resourceName.startsWith("/")) {
            throw new IllegalArgumentException("RUNTIME_METADATA_RESOURCE_NAME_INVALID");
        }
        try (InputStream input = anchor.getResourceAsStream(resourceName)) {
            if (input == null) throw new IllegalStateException("RUNTIME_METADATA_RESOURCE_MISSING " + resourceName);
            return RuntimeMetadataManifestAttestor.attestSignedClasspath(
                    input.readAllBytes(), anchor, resourceName);
        } catch (IOException error) {
            throw new IllegalStateException("RUNTIME_METADATA_RESOURCE_READ_FAILED", error);
        }
    }

    public static RuntimeMetadataProjection read(InputStream input) {
        Objects.requireNonNull(input, "input");
        try {
            return read(input.readAllBytes());
        } catch (IOException error) {
            throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_READ_FAILED", error);
        }
    }

    public static RuntimeMetadataProjection read(byte[] bytes) {
        return RuntimeMetadataManifestAttestor.parseUnverified(bytes, SIGNED_STATUS);
    }

    public static RuntimeMetadataProjection readLocalMirror(InputStream input) {
        Objects.requireNonNull(input, "input");
        try {
            return readLocalMirror(input.readAllBytes());
        } catch (IOException error) {
            throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_READ_FAILED", error);
        }
    }

    public static RuntimeMetadataProjection readLocalMirror(byte[] bytes) {
        return RuntimeMetadataManifestAttestor.attestLocalMirror(bytes);
    }

    private static RuntimeMetadataProjection readLocalMirrorResource(Class<?> anchor, String resourceName) {
        Objects.requireNonNull(anchor, "anchor");
        try (InputStream input = anchor.getResourceAsStream(resourceName)) {
            if (input == null) throw new IllegalStateException("RUNTIME_METADATA_RESOURCE_MISSING " + resourceName);
            return readLocalMirror(input.readAllBytes());
        } catch (IOException error) {
            throw new IllegalStateException("RUNTIME_METADATA_RESOURCE_READ_FAILED", error);
        }
    }

}
