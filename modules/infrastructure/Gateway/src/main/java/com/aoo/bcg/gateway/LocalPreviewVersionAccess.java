package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

/** Explicit local preview permission. It never supplies player or production authorization. */
final class LocalPreviewVersionAccess {
    private static final String ORIGIN = "http://localhost:7456";
    private final Path descriptor, serviceBearer;
    private final Set<String> peers;
    private final Clock clock;
    private final ObjectMapper json;
    private final Map<String, Lease> leases = new HashMap<>();
    private Instant lastObservedTime;
    private static final class Lease {
        final Descriptor descriptor;
        int used;
        boolean revoked;
        Lease(Descriptor descriptor) { this.descriptor = descriptor; }
    }
    record Descriptor(String generationId, String namespace, String purpose, String origin, String deviceId,
                      Instant issuedAt, Instant expiresAt, int maxUses, String tokenSha256, boolean revoked) {}
    private LocalPreviewVersionAccess(Path descriptor, Path serviceBearer, Set<String> peers, Clock clock, ObjectMapper json) {
        this.descriptor = descriptor; this.serviceBearer = serviceBearer; this.peers = peers; this.clock = clock; this.json = json;
    }
    static LocalPreviewVersionAccess disabled() { return new LocalPreviewVersionAccess(null, null, Set.of(), null, null); }
    static LocalPreviewVersionAccess configured(String profile, String descriptor, String bearer, String peerList, Clock clock, ObjectMapper json) {
        if (profile == null || profile.isBlank()) {
            if (!blank(descriptor) || !blank(bearer) || !blank(peerList)) throw new IllegalArgumentException("preview profile required");
            return disabled();
        }
        if (!profile.equals("local-preview") || blank(descriptor) || blank(bearer) || blank(peerList)) throw new IllegalArgumentException("invalid preview configuration");
        Set<String> peers = new HashSet<>();
        for (String peer : peerList.split(",", -1)) {
            peer = peer.strip();
            if (!privateLiteral(peer)) throw new IllegalArgumentException("preview requires exact private socket peers");
            peers.add(peer);
        }
        return new LocalPreviewVersionAccess(Path.of(descriptor), Path.of(bearer), Set.copyOf(peers), Objects.requireNonNull(clock), Objects.requireNonNull(json));
    }
    boolean enabled() { return descriptor != null; }
    /** Descriptor replacement is observed per request; the lifetime ledger prevents reload/revoke resurrection. */
    synchronized String authorize(String method, String path, String origin, InetSocketAddress peer,
                                  boolean forwardedHeaders, String authorization, Object deviceId) {
        if (!enabled()) return null;
        try {
            if (!"POST".equals(method) || !"/api/v2/version/check".equals(path) || !ORIGIN.equals(origin)
                    || peer == null || peer.getAddress() == null || !peers.contains(peer.getAddress().getHostAddress())
                    || forwardedHeaders || !(deviceId instanceof String device) || device.isBlank()
                    || authorization == null || !authorization.startsWith("Bearer ")) throw rejected();
            Descriptor d = json.readValue(readBounded(descriptor, 8192), Descriptor.class);
            validate(d);
            Instant now = observedTime();
            if (now.isBefore(d.issuedAt()) || !now.isBefore(d.expiresAt())) throw rejected();
            leases.values().removeIf(existing -> !now.isBefore(existing.descriptor.expiresAt()));
            Lease lease = leases.get(d.generationId());
            if (lease == null) {
                if (leases.size() >= 128) throw rejected(); // bounded unexpired generations
                lease = new Lease(d); leases.put(d.generationId(), lease);
            } else if (!sameGrant(lease.descriptor, d)) throw rejected();
            if (d.revoked()) lease.revoked = true;
            if (lease.revoked || now.isBefore(d.issuedAt()) || !now.isBefore(d.expiresAt()) || lease.used >= d.maxUses()
                    || !device.equals(d.deviceId())) throw rejected();
            String ticket = authorization.substring(7);
            if (ticket.length() < 32 || ticket.length() > 512) throw rejected();
            byte[] actual = MessageDigest.getInstance("SHA-256").digest(ticket.getBytes(StandardCharsets.UTF_8));
            if (!MessageDigest.isEqual(actual, HexFormat.of().parseHex(d.tokenSha256()))) throw rejected();
            String service = readBounded(serviceBearer, 4096).strip();
            if (service.length() < 32 || service.contains("\r") || service.contains("\n")
                    || MessageDigest.isEqual(actual, MessageDigest.getInstance("SHA-256").digest(service.getBytes(StandardCharsets.UTF_8)))) throw rejected();
            // Expiry is checked within the same monitor immediately before the irreversible consume.
            if (!observedTime().isBefore(d.expiresAt())) throw rejected();
            lease.used++;
            return "Bearer " + service;
        } catch (SecurityException failure) { throw failure; }
        catch (Exception failure) { throw rejected(); } // no paths, token, descriptor, or secret in the diagnostic
    }
    // Called only under authorize's monitor; reclaimed grants cannot revive on clock rollback.
    private Instant observedTime() {
        Instant now = clock.instant();
        if (lastObservedTime != null && now.isBefore(lastObservedTime)) throw rejected();
        lastObservedTime = now;
        return now;
    }
    /** Redacted, fail-closed predicate view for local diagnostics; never exposes credential values. */
    static Map<String,Object> diagnose(String method, String path, String origin, InetSocketAddress peer,
                                       boolean forwardedHeaders, String authorization, Object deviceId) {
        boolean localPeer = peer != null && peer.getAddress() != null
                && privateLiteral(peer.getAddress().getHostAddress());
        boolean tokenPresent = authorization != null && authorization.startsWith("Bearer ");
        boolean devicePresent = deviceId instanceof String device && !device.isBlank();
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("method", "POST".equals(method));
        result.put("path", "/api/v2/version/check".equals(path));
        result.put("origin", ORIGIN.equals(origin));
        result.put("peer", localPeer);
        result.put("forwarded", !forwardedHeaders);
        result.put("deviceId", devicePresent);
        result.put("tokenHash", tokenPresent);
        result.put("expiry", "UNKNOWN");
        result.put("useLimit", "UNKNOWN");
        return Map.copyOf(result);
    }

    synchronized void revoke(String generationId) {
        Lease lease = leases.get(generationId);
        if (lease != null) lease.revoked = true;
    }
    private static void validate(Descriptor d) {
        if (d == null || blank(d.generationId()) || d.generationId().length() > 128
                || !"aoo-local-preview".equals(d.namespace()) || !"version.check".equals(d.purpose())
                || !ORIGIN.equals(d.origin()) || blank(d.deviceId()) || d.deviceId().length() > 256
                || d.issuedAt() == null || d.expiresAt() == null || !d.expiresAt().isAfter(d.issuedAt())
                || java.time.Duration.between(d.issuedAt(), d.expiresAt()).compareTo(java.time.Duration.ofSeconds(300)) > 0
                || d.maxUses() != 2 || d.tokenSha256() == null || !d.tokenSha256().matches("[0-9a-f]{64}")) throw rejected();
    }
    private static boolean sameGrant(Descriptor a, Descriptor b) {
        return new Descriptor(a.generationId(), a.namespace(), a.purpose(), a.origin(), a.deviceId(), a.issuedAt(), a.expiresAt(), a.maxUses(), a.tokenSha256(), false)
                .equals(new Descriptor(b.generationId(), b.namespace(), b.purpose(), b.origin(), b.deviceId(), b.issuedAt(), b.expiresAt(), b.maxUses(), b.tokenSha256(), false));
    }
    private static String readBounded(Path path, int limit) throws java.io.IOException {
        try (var in = Files.newInputStream(path)) {
            byte[] bytes = in.readNBytes(limit + 1);
            if (bytes.length > limit) throw rejected();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
    private static boolean privateLiteral(String s) {
        if (s.equals("::1")) return true;
        String[] parts = s.split("\\.", -1);
        if (parts.length != 4) return false;
        int[] n = new int[4];
        for (int i = 0; i < 4; i++) {
            if (!parts[i].matches("0|[1-9][0-9]{0,2}")) return false;
            n[i] = Integer.parseInt(parts[i]); if (n[i] > 255) return false;
        }
        return n[0] == 127 || n[0] == 10 || n[0] == 172 && n[1] >= 16 && n[1] <= 31 || n[0] == 192 && n[1] == 168;
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static SecurityException rejected() { return new SecurityException("local preview authorization rejected"); }
}
