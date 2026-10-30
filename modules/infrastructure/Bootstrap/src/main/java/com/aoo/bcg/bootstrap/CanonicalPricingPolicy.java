package com.aoo.bcg.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Signed, release-bound billing input. It is configuration, never a price fallback. */
final class CanonicalPricingPolicy {
    static final String RESOURCE = "/canonical-pricing-policy.json";
    private final List<GamePolicy> policies;
    private final ClubCreateAction clubCreate;

    private CanonicalPricingPolicy(List<GamePolicy> policies, ClubCreateAction clubCreate) {
        this.policies = List.copyOf(policies);
        this.clubCreate = clubCreate;
    }

    ClubCreateAction requireClubCreate() {
        if (clubCreate == null) throw new IllegalStateException("club create policy is missing");
        return clubCreate;
    }

    static CanonicalPricingPolicy load() {
        try (InputStream stream = CanonicalPricingPolicy.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("canonical pricing source is missing");
            return parse(new ObjectMapper().findAndRegisterModules().readTree(stream));
        } catch (IOException e) {
            throw new IllegalStateException("canonical pricing source is unreadable", e);
        }
    }

    Optional<GamePolicy> forIdentity(long gameId, String playVersion, String regionCode) {
        return policies.stream().filter(p -> p.key().matches(gameId, playVersion, regionCode)).findFirst();
    }

    private static CanonicalPricingPolicy parse(JsonNode root) {
        if (root == null || !root.isObject() || integer(root, "schemaVersion") != 1
                || !"sha256-canonical-v1".equals(text(root, "hashAlgorithm"))) {
            throw new IllegalStateException("canonical pricing source contract is invalid");
        }
        JsonNode rows = root.get("policies");
        if (rows == null || !rows.isArray() || rows.isEmpty()) {
            throw new IllegalStateException("canonical pricing policies are missing");
        }
        List<GamePolicy> policies = new ArrayList<>();
        Set<Key> keys = new HashSet<>();
        for (JsonNode row : rows) {
            GamePolicy policy = GamePolicy.parse(root, row);
            if (!keys.add(policy.key())) throw new IllegalStateException("duplicate canonical pricing identity");
            policies.add(policy);
        }
        return new CanonicalPricingPolicy(policies, root.has("clubCreate") ? ClubCreateAction.parse(root.get("clubCreate")) : null);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalStateException("canonical pricing field is invalid: " + field);
        }
        return value.textValue();
    }

    private static int integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber()) {
            throw new IllegalStateException("canonical pricing integer is invalid: " + field);
        }
        long result = value.longValue();
        if (result <= 0 || result > Integer.MAX_VALUE) {
            throw new IllegalStateException("canonical pricing integer is invalid: " + field);
        }
        return (int) result;
    }

    record ClubCreateAction(String currency, boolean eligibilityEnabled, long minimumRoomCardBalance,
                            boolean chargeEnabled, long chargeRoomCardAmount, int policyVersion, String contentHash) {
        static ClubCreateAction parse(JsonNode row) {
            if (row == null || !row.isObject() || !"ROOM_CARD".equals(text(row, "currency"))) {
                throw new IllegalStateException("club create currency is invalid");
            }
            ClubCreateAction value = new ClubCreateAction("ROOM_CARD", bool(row, "eligibilityEnabled"),
                    nonnegative(row, "minimumRoomCardBalance"), bool(row, "chargeEnabled"),
                    nonnegative(row, "chargeRoomCardAmount"), version(row), text(row, "contentHash"));
            if (!value.contentHash.matches("[0-9a-f]{64}") || !sha256(value.canonicalPayload()).equals(value.contentHash)) {
                throw new IllegalStateException("club create content hash mismatch");
            }
            return value;
        }

        String canonicalPayload() {
            return String.join("\n", "schemaVersion=1", "action=CLUB_CREATE", "currency=" + currency,
                    "eligibilityEnabled=" + eligibilityEnabled, "minimumRoomCardBalance=" + minimumRoomCardBalance,
                    "chargeEnabled=" + chargeEnabled, "chargeRoomCardAmount=" + chargeRoomCardAmount,
                    "policyVersion=" + policyVersion);
        }

        private static int version(JsonNode row) {
            JsonNode value = row.get("policyVersion");
            if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() <= 0) {
                throw new IllegalStateException("club create version is invalid");
            }
            return value.intValue();
        }

        private static boolean bool(JsonNode row, String key) {
            JsonNode value = row.get(key);
            if (value == null || !value.isBoolean()) throw new IllegalStateException("club create boolean is invalid: " + key);
            return value.booleanValue();
        }

        private static long nonnegative(JsonNode row, String key) {
            JsonNode value = row.get(key);
            if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
                throw new IllegalStateException("club create amount is invalid: " + key);
            }
            return value.longValue();
        }
    }

    record Key(long gameId, String playVersion, String regionCode) {
        boolean matches(long id, String version, String region) {
            return gameId == id && playVersion.equals(version) && regionCode.equals(region);
        }
    }

    record ReleaseBinding(long releaseId, long indexGeneration, String runtimeSourceHash) {}

    record Price(String measureKind, int measureValue, int playerCount, String payerMode,
                 String currencyCode, long costMinor, int policyVersion, String pricingSourceHash) {}

    record Selection(Key key, ReleaseBinding binding, Price price) {}

    record GamePolicy(Key key, String gameCode, ReleaseBinding binding, int policyVersion,
                      String pricingSourceHash, int playerCount, String payerMode,
                      String currencyCode, List<Price> prices) {
        static GamePolicy parse(JsonNode root, JsonNode row) {
            if (row == null || !row.isObject()) throw new IllegalStateException("canonical pricing row is invalid");
            long gameId = longInteger(row, "gameId");
            String gameCode = text(row, "gameCode");
            String playVersion = text(row, "playVersion");
            String regionCode = text(row, "regionCode");
            long releaseId = longInteger(row, "releaseId");
            long indexGeneration = longInteger(row, "indexGeneration");
            String runtimeSourceHash = hash(text(row, "runtimeSourceHash"), "runtimeSourceHash");
            int policyVersion = integer(row, "policyVersion");
            String pricingSourceHash = hash(text(row, "pricingSourceHash"), "pricingSourceHash");
            int playerCount = integer(row, "playerCount");
            String payerMode = text(row, "payerMode").toUpperCase(java.util.Locale.ROOT);
            String currencyCode = text(row, "currencyCode").toUpperCase(java.util.Locale.ROOT);
            JsonNode measures = row.get("measures");
            if (measures == null || !measures.isArray() || measures.isEmpty()) {
                throw new IllegalStateException("canonical pricing measures are missing");
            }
            List<Price> prices = new ArrayList<>();
            Set<String> unique = new HashSet<>();
            for (JsonNode measure : measures) {
                String kind = text(measure, "measureKind");
                int value = integer(measure, "measureValue");
                long cost = longInteger(measure, "costMinor");
                if (!("ROUND_COUNT".equals(kind) || "DURATION_MINUTES".equals(kind)) || cost < 0) {
                    throw new IllegalStateException("canonical pricing measure is invalid");
                }
                String tuple = kind + "|" + value;
                if (!unique.add(tuple)) throw new IllegalStateException("duplicate canonical pricing measure");
                prices.add(new Price(kind, value, playerCount, payerMode, currencyCode, cost,
                        policyVersion, pricingSourceHash));
            }
            prices.sort(Comparator.comparing(Price::measureKind).thenComparingInt(Price::measureValue));
            GamePolicy policy = new GamePolicy(new Key(gameId, playVersion, regionCode), gameCode,
                    new ReleaseBinding(releaseId, indexGeneration, runtimeSourceHash), policyVersion,
                    pricingSourceHash, playerCount, payerMode, currencyCode, List.copyOf(prices));
            String expected = sha256(canonicalPayload(root, policy));
            if (!expected.equals(pricingSourceHash)) {
                throw new IllegalStateException("canonical pricing source hash mismatch");
            }
            return policy;
        }

        Selection select(Map<String, Object> rules, Map<String, Object> defaults) {
            boolean hasRounds = rules.containsKey("roundCount");
            boolean hasDuration = rules.containsKey("roomDurationMinutes");
            if (hasRounds == hasDuration) throw new IllegalArgumentException("exactly one room end dimension is required");
            String kind = hasRounds ? "ROUND_COUNT" : "DURATION_MINUTES";
            String key = hasRounds ? "roundCount" : "roomDurationMinutes";
            int value = exactPositive(rules.get(key), key);
            int players = exactPositive(first(rules, defaults, "playerCount", "maxPlayers", "seatLimit", "playerNum"), "playerCount");
            if (players != playerCount) throw new IllegalArgumentException("playerCount does not match canonical pricing policy");
            Object payerValue = first(rules, defaults, "payerMode");
            if (!(payerValue instanceof String payer) || payer.isBlank() || !payerMode.equalsIgnoreCase(payer)) {
                throw new IllegalArgumentException("payerMode does not match canonical pricing policy");
            }
            Price price = prices.stream().filter(p -> p.measureKind().equals(kind) && p.measureValue() == value).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("measure is not published in canonical pricing policy"));
            return new Selection(key(), binding, price);
        }

        private static Object first(Map<String, Object> rules, Map<String, Object> defaults, String... keys) {
            for (String key : keys) {
                Object value = rules.containsKey(key) ? rules.get(key) : defaults.get(key);
                if (value != null) return value;
            }
            return null;
        }

        private static int exactPositive(Object value, String key) {
            if (!(value instanceof Number number) || number.longValue() <= 0 || number.longValue() != number.doubleValue()) {
                throw new IllegalArgumentException(key + " must be a positive integer");
            }
            return Math.toIntExact(number.longValue());
        }

        private static String canonicalPayload(JsonNode root, GamePolicy policy) {
            List<String> lines = new ArrayList<>();
            lines.add("schemaVersion=" + integer(root, "schemaVersion"));
            lines.add("gameId=" + policy.key.gameId());
            lines.add("gameCode=" + policy.gameCode);
            lines.add("playVersion=" + policy.key.playVersion());
            lines.add("regionCode=" + policy.key.regionCode());
            lines.add("releaseId=" + policy.binding.releaseId());
            lines.add("indexGeneration=" + policy.binding.indexGeneration());
            lines.add("runtimeSourceHash=" + policy.binding.runtimeSourceHash());
            lines.add("policyVersion=" + policy.policyVersion);
            lines.add("playerCount=" + policy.playerCount);
            lines.add("payerMode=" + policy.payerMode);
            lines.add("currencyCode=" + policy.currencyCode);
            policy.prices.stream().sorted(Comparator.comparing(Price::measureKind).thenComparingInt(Price::measureValue))
                    .forEach(price -> lines.add("measure=" + price.measureKind() + ":" + price.measureValue() + ":" + price.costMinor()));
            return String.join("\n", lines);
        }

        private static String hash(String value, String field) {
            if (!value.matches("[0-9a-f]{64}")) throw new IllegalStateException("canonical pricing hash is invalid: " + field);
            return value;
        }

        private static long longInteger(JsonNode node, String field) {
            JsonNode value = node.get(field);
            if (value == null || !value.isIntegralNumber() || value.longValue() <= 0) {
                throw new IllegalStateException("canonical pricing integer is invalid: " + field);
            }
            return value.longValue();
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("canonical pricing hash unavailable", e);
        }
    }
}
