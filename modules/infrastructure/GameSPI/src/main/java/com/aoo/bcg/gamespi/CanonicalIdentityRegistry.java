package com.aoo.bcg.gamespi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable identity-contract projection shared by descriptor, provider,
 * lobby, gateway and client adapters.  It carries no filesystem or database
 * lookup: a release/build adapter must supply the signed registry entries.
 * Missing contract evidence is retained as a fail-closed entry rather than
 * being replaced by a default, a gameId formula, or a compatibility snapshot.
 */
public final class CanonicalIdentityRegistry {
    public static final int REQUIRED_ROW_COUNT = 32;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private final String registrySha256;
    private final String releaseEdition;
    private final List<Entry> rows;
    private final Map<Integer, Entry> byId;
    private final Map<String, Entry> byCode;

    public CanonicalIdentityRegistry(String registrySha256, String releaseEdition,
                                     Collection<? extends Entry> entries) {
        this.registrySha256 = requireSha(registrySha256, "IDENTITY_REGISTRY_SHA_REQUIRED");
        this.releaseEdition = requireText(releaseEdition, "IDENTITY_REGISTRY_EDITION_REQUIRED");
        Objects.requireNonNull(entries, "entries");
        if (entries.size() != REQUIRED_ROW_COUNT) {
            throw new IllegalArgumentException("IDENTITY_REGISTRY_ROW_COUNT_INVALID rows="
                    + entries.size() + " expected=" + REQUIRED_ROW_COUNT);
        }
        Map<Integer, Entry> ids = new LinkedHashMap<>();
        Map<String, Entry> codes = new LinkedHashMap<>();
        for (Entry entry : entries) {
            Objects.requireNonNull(entry, "identity registry entry");
            if (entry.gameId() == 516) throw new IllegalArgumentException(
                    "IDENTITY_REGISTRY_COMPATIBILITY_IDENTITY gameId=516");
            if (ids.put(entry.gameId(), entry) != null) throw new IllegalArgumentException(
                    "IDENTITY_REGISTRY_DUPLICATE_GAME_ID gameId=" + entry.gameId());
            if (codes.put(entry.gameCode(), entry) != null) throw new IllegalArgumentException(
                    "IDENTITY_REGISTRY_DUPLICATE_GAME_CODE gameCode=" + entry.gameCode());
        }
        this.rows = List.copyOf(entries);
        this.byId = Map.copyOf(ids);
        this.byCode = Map.copyOf(codes);
    }

    public String registrySha256() { return registrySha256; }
    public String releaseEdition() { return releaseEdition; }
    public List<Entry> rows() { return rows; }
    public Entry require(int gameId) {
        Entry entry = byId.get(gameId);
        if (entry == null) throw new IllegalStateException("IDENTITY_REGISTRY_ROW_MISSING gameId=" + gameId);
        return entry;
    }

    public Entry requireIdentity(int gameId, String gameCode, String family,
                                 String playVersion, String regionCode) {
        if (gameId == 516) throw new IllegalStateException("IDENTITY_CONTRACT_COMPATIBILITY_IDENTITY");
        Entry entry = require(gameId);
        if (!entry.gameCode().equals(requireText(gameCode, "gameCode"))
                || !entry.family().equals(requireText(family, "family"))
                || !entry.playVersion().equals(requireText(playVersion, "playVersion"))
                || !entry.regionCode().equals(requireText(regionCode, "regionCode"))) {
            throw new IllegalStateException("IDENTITY_CONTRACT_PROJECTION_MISMATCH gameId=" + gameId);
        }
        if (!entry.isConsumable()) {
            throw new IllegalStateException("IDENTITY_CONTRACT_PROJECTION_FAIL_CLOSED gameId="
                    + gameId + " reasons=" + String.join(",", entry.failureCodes()));
        }
        return entry;
    }

    /** Ensures a release cannot accidentally admit a partial registry. */
    public void requireConsumable() {
        List<Entry> blocked = rows.stream().filter(entry -> !entry.isConsumable()).toList();
        if (!blocked.isEmpty()) {
            throw new IllegalStateException("IDENTITY_REGISTRY_NOT_CONSUMABLE gameIds="
                    + blocked.stream().map(entry -> Integer.toString(entry.gameId())).toList());
        }
    }

    /** One immutable value is intentionally shared by every downstream projection. */
    public Projection project(int gameId, String gameCode, String family,
                              String playVersion, String regionCode) {
        Entry entry = requireIdentity(gameId, gameCode, family, playVersion, regionCode);
        return new Projection(entry, entry, entry, entry, entry);
    }

    public record Projection(Entry serverDescriptor, Entry provider, Entry lobby,
                             Entry gateway, Entry client) {
        public Projection {
            Objects.requireNonNull(serverDescriptor, "serverDescriptor");
            Objects.requireNonNull(provider, "provider");
            Objects.requireNonNull(lobby, "lobby");
            Objects.requireNonNull(gateway, "gateway");
            Objects.requireNonNull(client, "client");
            if (!serverDescriptor.equals(provider) || !serverDescriptor.equals(lobby)
                    || !serverDescriptor.equals(gateway) || !serverDescriptor.equals(client)) {
                throw new IllegalArgumentException("IDENTITY_CONTRACT_PROJECTION_SPLIT_SOURCE");
            }
        }
    }

    public enum Status { CONSUMABLE, FAIL_CLOSED }

    public record Entry(int gameId, String gameCode, String family, String playVersion,
                        String regionCode, String providerKey, String clientRuntimeEntry,
                        String contractVersion, Status status, List<String> failureCodes) {
        public Entry {
            if (gameId <= 0) throw new IllegalArgumentException("IDENTITY_REGISTRY_GAME_ID_INVALID");
            gameCode = requireText(gameCode, "gameCode");
            family = requireText(family, "family");
            playVersion = requireText(playVersion, "playVersion");
            regionCode = requireText(regionCode, "regionCode");
            clientRuntimeEntry = requireText(clientRuntimeEntry, "clientRuntimeEntry");
            status = Objects.requireNonNull(status, "status");
            failureCodes = List.copyOf(Objects.requireNonNull(failureCodes, "failureCodes"));
            if (providerKey != null && providerKey.isBlank()) throw new IllegalArgumentException("IDENTITY_REGISTRY_PROVIDER_KEY_INVALID");
            if (contractVersion != null && contractVersion.isBlank()) throw new IllegalArgumentException("IDENTITY_REGISTRY_CONTRACT_VERSION_INVALID");
            if ("compatibility-v1".equals(contractVersion)) throw new IllegalArgumentException("IDENTITY_REGISTRY_COMPATIBILITY_CONTRACT");
            if (status == Status.CONSUMABLE && (providerKey == null || contractVersion == null || !failureCodes.isEmpty())) {
                throw new IllegalArgumentException("IDENTITY_REGISTRY_CONSUMABLE_FIELDS_INCOMPLETE");
            }
            if (status == Status.FAIL_CLOSED && failureCodes.isEmpty()) {
                throw new IllegalArgumentException("IDENTITY_REGISTRY_FAIL_CLOSED_REASON_REQUIRED");
            }
        }

        public boolean isConsumable() { return status == Status.CONSUMABLE; }
    }

    private static String requireSha(String value, String error) {
        if (value == null || !SHA256.matcher(value).matches()) throw new IllegalArgumentException(error);
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
