package com.aoo.bcg.config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Cryptographically binds approval to the exact normalized content that will be published. */
public record SignedReleaseApproval(String draftId, String contentHash, long approverId,
                                    Instant approvedAt, String signature) {
    public SignedReleaseApproval {
        if (draftId == null || draftId.isBlank() || contentHash == null || !contentHash.matches("[0-9a-f]{64}")
                || approverId <= 0 || approvedAt == null || signature == null || signature.isBlank())
            throw new IllegalArgumentException("invalid signed release approval");
    }

    public static SignedReleaseApproval approve(GameConfigurationDraftCompiler.ValidatedDraft draft,
                                                 long approverId, Clock clock, byte[] key) {
        if (approverId <= 0 || clock == null) throw new IllegalArgumentException("approver and clock are required");
        String hash = contentHash(draft);
        Instant now = clock.instant();
        return new SignedReleaseApproval(draft.source().draftId(), hash, approverId, now,
                sign(draft.source().draftId(), hash, approverId, now, key));
    }

    public static SignedReleaseApproval approvePublication(GameConfigurationDraftCompiler.ValidatedDraft draft,
                                                            PlayAvailabilityPolicy availability,
                                                            Instant activateAt,
                                                            long approverId, Clock clock, byte[] key) {
        if (availability == null || activateAt == null || approverId <= 0 || clock == null)
            throw new IllegalArgumentException("publication approval fields are required");
        String hash = publicationHash(draft, availability, activateAt);
        Instant now = clock.instant();
        return new SignedReleaseApproval(draft.source().draftId(), hash, approverId, now,
                sign(draft.source().draftId(), hash, approverId, now, key));
    }

    public void verify(GameConfigurationDraftCompiler.ValidatedDraft publishContent, byte[] key) {
        if (!draftId.equals(publishContent.source().draftId()) || !contentHash.equals(contentHash(publishContent)))
            throw new IllegalStateException("approved content differs from publication content");
        String expected = sign(draftId, contentHash, approverId, approvedAt, key);
        try {
            if (!MessageDigest.isEqual(Base64.getDecoder().decode(expected), Base64.getDecoder().decode(signature)))
                throw new IllegalStateException("release approval signature mismatch");
        } catch (IllegalArgumentException malformed) {
            throw new IllegalStateException("release approval signature is malformed", malformed);
        }
    }

    public void verifyPublication(GameConfigurationDraftCompiler.ValidatedDraft publishContent,
                                  PlayAvailabilityPolicy availability, Instant activateAt, byte[] key) {
        if (!draftId.equals(publishContent.source().draftId())
                || !contentHash.equals(publicationHash(publishContent, availability, activateAt)))
            throw new IllegalStateException("approved publication policy/time/content differs from release request");
        verifySignature(key);
    }

    public static String contentHash(GameConfigurationDraftCompiler.ValidatedDraft draft) {
        if (draft == null) throw new IllegalArgumentException("validated draft is required");
        GameConfigurationDraft source = draft.source();
        StringBuilder canonical = new StringBuilder();
        append(canonical, source.draftId()); append(canonical, source.gameId()); append(canonical, source.playType());
        append(canonical, source.playVersion()); append(canonical, source.schemaVersion());
        append(canonical, draft.regionalRules().rulesByRegion());
        append(canonical, source.uiSelectableCombinations().stream().map(draft.schema()::normalize).collect(java.util.stream.Collectors.toSet()));
        append(canonical, source.serverRunnableCombinations().stream().map(draft.schema()::normalize).collect(java.util.stream.Collectors.toSet()));
        append(canonical, draft.components().ordered().stream().map(component -> Map.of(
                "id", component.componentId(), "version", component.version(), "kind", component.kind().name())).toList());
        append(canonical, source.manifest()); append(canonical, source.switches()); append(canonical, source.feeExplanation());
        append(canonical, source.requestedActivationAt());
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) { throw new IllegalStateException("cannot hash release content", error); }
    }

    private static String publicationHash(GameConfigurationDraftCompiler.ValidatedDraft draft,
                                          PlayAvailabilityPolicy availability, Instant activateAt) {
        StringBuilder canonical = new StringBuilder(contentHash(draft));
        append(canonical, Map.of("listed", availability.listed(), "maintenance", availability.maintenance(),
                "percent", availability.rolloutPercent(), "channels", availability.channels(),
                "regions", availability.regionCodes(), "rolloutKey", availability.rolloutKey()));
        append(canonical, activateAt);
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) { throw new IllegalStateException("cannot hash publication policy", error); }
    }

    private void verifySignature(byte[] key) {
        String expected = sign(draftId, contentHash, approverId, approvedAt, key);
        try {
            if (!MessageDigest.isEqual(Base64.getDecoder().decode(expected), Base64.getDecoder().decode(signature)))
                throw new IllegalStateException("release approval signature mismatch");
        } catch (IllegalArgumentException malformed) {
            throw new IllegalStateException("release approval signature is malformed", malformed);
        }
    }

    private static String sign(String draftId, String hash, long approverId, Instant approvedAt, byte[] key) {
        if (key == null || key.length < 32) throw new IllegalArgumentException("release signing key must contain at least 32 bytes");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            String value = draftId + '\n' + hash + '\n' + approverId + '\n' + approvedAt + '\n';
            return Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) { throw new IllegalStateException("cannot sign release approval", error); }
    }

    private static void append(StringBuilder target, Object value) {
        if (value == null) { target.append("null;"); return; }
        if (value instanceof Map<?, ?> map) {
            target.append('{');
            map.entrySet().stream().sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .forEach(entry -> { append(target, String.valueOf(entry.getKey())); append(target, entry.getValue()); });
            target.append('}'); return;
        }
        if (value instanceof Set<?> set) {
            List<String> canonicalItems = new ArrayList<>();
            set.forEach(item -> { StringBuilder nested = new StringBuilder(); append(nested, item); canonicalItems.add(nested.toString()); });
            canonicalItems.sort(String::compareTo);
            target.append('['); canonicalItems.forEach(target::append); target.append(']'); return;
        }
        if (value instanceof Collection<?> collection) {
            target.append('['); collection.forEach(item -> append(target, item)); target.append(']'); return;
        }
        if (value instanceof ReleaseManifest manifest) {
            append(target, List.of(manifest.gameId(), manifest.playVersion(), manifest.protocolVersion(),
                    manifest.componentVersion(), manifest.clientBundleVersion(), manifest.publishedAt(), manifest.checksums())); return;
        }
        if (value instanceof com.aoo.bcg.common.operations.OperationSwitches switches) {
            append(target, List.of(switches.allowNewRooms(), switches.allowExistingRoomsToFinish(), switches.maintenance(),
                    switches.minimumClientVersion(), String.valueOf(switches.rolloutKey()), switches.serverRouteVersion())); return;
        }
        target.append(value.getClass().getSimpleName()).append(':').append(value).append(';');
    }
}
