package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import com.aoo.bcg.gamespi.RuntimeMetadataProjectionSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads the release/build-provided manifest resource. This class deliberately
 * has no filesystem path or environment-variable fallback: a production
 * checkout is not a runtime input.
 */
public final class CanonicalRuntimeMetadataManifestReader {
    public static final String DEFAULT_RESOURCE = "/runtime-metadata-canonical-manifest.json";
    private static final String MANIFEST_KIND = "runtime-metadata-canonical";
    private static final String SIGNED_STATUS = "SIGNED_CANONICAL_INPUT";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int SCHEMA_VERSION = 1;

    private CanonicalRuntimeMetadataManifestReader() { }

    public static RuntimeMetadataProjectionSource bundled() {
        return () -> readResource(CanonicalRuntimeMetadataManifestReader.class, DEFAULT_RESOURCE);
    }

    public static RuntimeMetadataProjection readResource(Class<?> anchor, String resourceName) {
        Objects.requireNonNull(anchor, "anchor");
        if (resourceName == null || resourceName.isBlank() || !resourceName.startsWith("/")) {
            throw new IllegalArgumentException("RUNTIME_METADATA_RESOURCE_NAME_INVALID");
        }
        try (InputStream input = anchor.getResourceAsStream(resourceName)) {
            if (input == null) throw new IllegalStateException("RUNTIME_METADATA_RESOURCE_MISSING " + resourceName);
            return read(input.readAllBytes());
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
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length == 0) throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_EMPTY");
        try {
            JsonNode root = JSON.readTree(bytes);
            JsonNode schema = root.path("schemaVersion");
            if (!schema.canConvertToInt() || schema.asInt() != SCHEMA_VERSION) {
                throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_SCHEMA_INVALID");
            }
            requireText(root, "manifestKind", MANIFEST_KIND);
            requireText(root, "manifestStatus", SIGNED_STATUS);
            if (!root.path("consumable").asBoolean(false)) {
                throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_NOT_CONSUMABLE");
            }
            JsonNode editionNode = root.path("releaseEdition");
            if (!editionNode.isTextual() || editionNode.textValue().isBlank()) {
                throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_EDITION_REQUIRED");
            }
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
            return new RuntimeMetadataProjection(sha, editionNode.textValue(), entries);
        } catch (IOException | RuntimeException error) {
            if (error instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_INVALID", error);
        }
    }

    private static RuntimeMetadataProjection.Entry parseRow(JsonNode row) {
        if (row == null || !row.isObject()) throw new IllegalStateException("RUNTIME_METADATA_ROW_INVALID");
        return new RuntimeMetadataProjection.Row(
                requiredInt(row, "gameId"), requiredText(row, "gameCode"), requiredText(row, "family"),
                requiredText(row, "playVersion"), requiredText(row, "regionCode"),
                requiredText(row, "snapshotSchemaVersion"), requiredText(row, "cardCodecVersion"),
                requiredText(row, "eventInterpreterVersion"), requiredText(row, "protocolVersion"),
                requiredText(row, "contractVersion"),
                requiredText(row, "sourceHash"), requiredLong(row, "releaseId"),
                requiredLong(row, "indexGeneration"));
    }

    private static void requireText(JsonNode node, String name, String expected) {
        if (!node.path(name).isTextual() || !expected.equals(node.path(name).textValue())) {
            throw new IllegalStateException("RUNTIME_METADATA_MANIFEST_" + name.toUpperCase() + "_INVALID");
        }
    }

    private static String requiredText(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalStateException("RUNTIME_METADATA_FIELD_REQUIRED field=" + name);
        }
        return value.textValue();
    }

    private static int requiredInt(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.canConvertToInt() || value.asInt() <= 0) {
            throw new IllegalStateException("RUNTIME_METADATA_FIELD_INVALID field=" + name);
        }
        return value.asInt();
    }

    private static long requiredLong(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.canConvertToLong() || value.asLong() <= 0) {
            throw new IllegalStateException("RUNTIME_METADATA_FIELD_INVALID field=" + name);
        }
        return value.asLong();
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
