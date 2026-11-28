package com.aoo.bcg.gamespi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Raw-byte authority boundary for trusted runtime metadata projections. */
public final class RuntimeMetadataManifestAttestor {
    public static final String PRODUCTION_RESOURCE = "/runtime-metadata-canonical-manifest.json";
    public static final String LOCAL_MIRROR_SHA256 = "31d04c18a734620a7c80320bb517022ca65d243a512c3423b93247e7d56e6199";
    private static final String READER_CLASS = "com.aoo.bcg.bootstrap.CanonicalRuntimeMetadataManifestReader";
    private static final ObjectMapper JSON = new ObjectMapper();

    private RuntimeMetadataManifestAttestor() { }

    public static RuntimeMetadataProjection attestSignedClasspath(
            byte[] bytes, Class<?> anchor, String resourceName) {
        Objects.requireNonNull(anchor, "anchor");
        Class<?> authorizedAnchor;
        try {
            authorizedAnchor = Class.forName(READER_CLASS, false,
                    RuntimeMetadataManifestAttestor.class.getClassLoader());
        } catch (ClassNotFoundException error) {
            throw new IllegalStateException("RUNTIME_METADATA_SIGNED_RESOURCE_AUTHORITY_MISSING", error);
        }
        if (anchor != authorizedAnchor || !PRODUCTION_RESOURCE.equals(resourceName)) {
            throw new IllegalStateException("RUNTIME_METADATA_SIGNED_RESOURCE_AUTHORITY_INVALID");
        }
        try (InputStream embedded = anchor.getResourceAsStream(resourceName)) {
            if (embedded == null || !Arrays.equals(bytes, embedded.readAllBytes())) {
                throw new IllegalStateException("RUNTIME_METADATA_SIGNED_RESOURCE_FINGERPRINT_INVALID");
            }
        } catch (java.io.IOException error) {
            throw new IllegalStateException("RUNTIME_METADATA_SIGNED_RESOURCE_READ_FAILED", error);
        }
        return parse(bytes, "SIGNED_CANONICAL_INPUT", false,
                RuntimeMetadataProjection.Trust.SIGNED_CANONICAL);
    }

    public static RuntimeMetadataProjection attestLocalMirror(byte[] bytes) {
        if (!"local-mirror".equals(System.getProperty("aoo.runtime.metadata.mode"))) {
            throw new IllegalStateException("RUNTIME_METADATA_LOCAL_MIRROR_MODE_REQUIRED");
        }
        if (!LOCAL_MIRROR_SHA256.equals(sha256(bytes))) {
            throw new IllegalStateException("RUNTIME_METADATA_LOCAL_MIRROR_FINGERPRINT_INVALID");
        }
        return parse(bytes, "LOCAL_MIRROR_INPUT", true,
                RuntimeMetadataProjection.Trust.VERIFIED_LOCAL_MIRROR);
    }

    public static RuntimeMetadataProjection parseUnverified(byte[] bytes, String expectedStatus) {
        return parse(bytes, expectedStatus, false, RuntimeMetadataProjection.Trust.UNVERIFIED);
    }

    private static RuntimeMetadataProjection parse(byte[] bytes, String expectedStatus,
            boolean localMirror, RuntimeMetadataProjection.Trust trust) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length == 0) throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_EMPTY");
        try {
            JsonNode root = JSON.readTree(bytes);
            if (!root.path("schemaVersion").canConvertToInt() || root.path("schemaVersion").asInt() != 1) {
                throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_SCHEMA_INVALID");
            }
            requireText(root, "manifestKind", "runtime-metadata-canonical");
            requireText(root, "manifestStatus", expectedStatus);
            if (localMirror && !root.path("localMirrorOnly").asBoolean(false)) {
                throw new IllegalStateException("RUNTIME_METADATA_LOCAL_MIRROR_MARKER_REQUIRED");
            }
            if (!root.path("consumable").asBoolean(false)) {
                throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_NOT_CONSUMABLE");
            }
            String edition = requiredText(root, "releaseEdition");
            JsonNode required = root.path("requiredFields");
            if (!required.isArray() || required.size() != RuntimeMetadataProjection.REQUIRED_FIELDS.size()) {
                throw new IllegalStateException("RUNTIME_METADATA_REQUIRED_FIELDS_INVALID");
            }
            for (int i = 0; i < RuntimeMetadataProjection.REQUIRED_FIELDS.size(); i++) {
                if (!RuntimeMetadataProjection.REQUIRED_FIELDS.get(i).equals(required.get(i).asText())) {
                    throw new IllegalStateException("RUNTIME_METADATA_REQUIRED_FIELDS_INVALID");
                }
            }
            JsonNode rows = root.path("rows");
            if (!rows.isArray()) throw new IllegalStateException("RUNTIME_METADATA_ROWS_REQUIRED");
            List<RuntimeMetadataProjection.Entry> entries = new ArrayList<>();
            for (JsonNode row : rows) entries.add(parseRow(row));
            String sha = sha256(bytes);
            return trust == RuntimeMetadataProjection.Trust.UNVERIFIED
                    ? new RuntimeMetadataProjection(sha, edition, entries)
                    : RuntimeMetadataProjection.trusted(sha, edition, entries, trust);
        } catch (java.io.IOException | RuntimeException error) {
            if (error instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_INVALID", error);
        }
    }

    private static RuntimeMetadataProjection.Entry parseRow(JsonNode row) {
        if (row == null || !row.isObject()) throw new IllegalStateException("RUNTIME_METADATA_ROW_INVALID");
        return new RuntimeMetadataProjection.Row(requiredInt(row, "gameId"), requiredText(row, "gameCode"),
                requiredText(row, "family"), requiredText(row, "playVersion"), requiredText(row, "regionCode"),
                requiredText(row, "snapshotSchemaVersion"), requiredText(row, "cardCodecVersion"),
                requiredText(row, "eventInterpreterVersion"), requiredText(row, "protocolVersion"),
                requiredText(row, "contractVersion"), requiredText(row, "sourceHash"),
                requiredLong(row, "releaseId"), requiredLong(row, "indexGeneration"));
    }

    private static void requireText(JsonNode node, String name, String expected) {
        if (!node.path(name).isTextual() || !expected.equals(node.path(name).textValue())) {
            throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_" + name.toUpperCase() + "_INVALID");
        }
    }
    private static String requiredText(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.isTextual() || value.textValue().isBlank())
            throw new IllegalStateException("RUNTIME_METADATA_FIELD_REQUIRED field=" + name);
        return value.textValue();
    }
    private static int requiredInt(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.canConvertToInt() || value.asInt() <= 0)
            throw new IllegalStateException("RUNTIME_METADATA_FIELD_INVALID field=" + name);
        return value.asInt();
    }
    private static long requiredLong(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.canConvertToLong() || value.asLong() <= 0)
            throw new IllegalStateException("RUNTIME_METADATA_FIELD_INVALID field=" + name);
        return value.asLong();
    }
    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
