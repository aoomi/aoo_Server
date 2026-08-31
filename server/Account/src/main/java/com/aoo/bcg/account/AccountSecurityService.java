package com.aoo.bcg.account;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;

/**
 * Transactional account-security domain. Persistence adapters must execute each public mutation in one
 * database transaction; this in-memory implementation is deliberately deterministic and suitable as the
 * canonical behavioural contract for adapters.
 */
public final class AccountSecurityService implements AccountService {
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private final ReentrantLock lock = new ReentrantLock();
    private final Clock clock;
    private final SecureRandom random;
    private final PasswordHasher passwords;
    private final Map<String, PlatformCredentialVerifier> platformVerifiers;
    private final Map<Long, Account> accounts = new HashMap<>();
    private final Map<String, Long> loginIndex = new HashMap<>();
    private final Map<String, Token> tokens = new HashMap<>();
    private final List<AuditEvent> audit = new ArrayList<>();
    private final List<Ban> globalBans = new ArrayList<>();
    private final Map<RefreshFlight, CompletedRefresh> refreshFlights = new HashMap<>();
    private long nextId = 1;

    public AccountSecurityService(Clock clock) { this(clock, Map.of()); }
    public AccountSecurityService(Clock clock, Map<String, PlatformCredentialVerifier> platformVerifiers) {
        this(clock, new SecureRandom(), new PasswordHasher(), platformVerifiers);
    }
    AccountSecurityService(Clock clock, SecureRandom random, PasswordHasher passwords) {
        this(clock, random, passwords, Map.of());
    }
    AccountSecurityService(Clock clock, SecureRandom random, PasswordHasher passwords,
                           Map<String, PlatformCredentialVerifier> platformVerifiers) {
        this.clock = Objects.requireNonNull(clock); this.random = Objects.requireNonNull(random); this.passwords = Objects.requireNonNull(passwords);
        Map<String, PlatformCredentialVerifier> normalizedVerifiers = new HashMap<>();
        Objects.requireNonNull(platformVerifiers).forEach((provider, verifier) ->
                normalizedVerifiers.put(normalized(provider), Objects.requireNonNull(verifier)));
        this.platformVerifiers = Map.copyOf(normalizedVerifiers);
    }

    public Registration registerGuest(String recoveryKey, ClientContext client) {
        requireText(recoveryKey, "recoveryKey"); Objects.requireNonNull(client);
        return atomic(() -> {
            String key = "guest:" + digestLookup(recoveryKey);
            Long existing = loginIndex.get(key);
            if (existing != null) return new Registration(existing, true, true);
            long id = nextId++;
            Account account = new Account(id, true, null, client);
            account.guestKey = key;
            accounts.put(id, account); loginIndex.put(key, id);
            audit(id, "GUEST_REGISTERED", client.deviceId());
            return new Registration(id, true, false);
        });
    }

    public TokenPair loginGuest(String recoveryKey, ClientContext client, LoginMode mode) {
        requireText(recoveryKey, "recoveryKey"); Objects.requireNonNull(client); Objects.requireNonNull(mode);
        return atomic(() -> {
            Account a = account(loginIndex.getOrDefault("guest:" + digestLookup(recoveryKey), -1L));
            if (!a.guest) throw new Unauthorized("guest credential was upgraded");
            enforceAllowed(a, client);
            return issue(a, client, mode, null);
        });
    }

    public Registration registerPassword(String login, char[] password, ClientContext client) {
        String normalized = normalizeLogin(login); Objects.requireNonNull(client);
        String hash = passwords.hash(password);
        return atomic(() -> {
            if (loginIndex.containsKey("login:" + normalized)) throw new Conflict("login already registered");
            long id = nextId++;
            Account account = new Account(id, false, hash, client);
            accounts.put(id, account); loginIndex.put("login:" + normalized, id);
            audit(id, "ACCOUNT_REGISTERED", normalized);
            return new Registration(id, false, false);
        });
    }

    public void upgradeGuest(long accountId, String login, char[] password) {
        String normalized = normalizeLogin(login); String hash = passwords.hash(password);
        atomic(() -> { Account a = account(accountId); if (!a.guest) throw new Conflict("account is not a guest");
            if (loginIndex.containsKey("login:" + normalized)) throw new Conflict("login already registered");
            a.guest = false; a.passwordHash = hash; loginIndex.remove(a.guestKey, accountId); a.guestKey = null; loginIndex.put("login:" + normalized, accountId);
            revokeAll(a, "GUEST_UPGRADED"); return null; });
    }

    /** Verifies with the provider adapter before binding; the client can never submit a trusted subject directly. */
    public void bindPlatform(long accountId, String provider, String oneTimeCredential) {
        String normalizedProvider = normalized(provider);
        String verifiedSubject = verifyPlatform(normalizedProvider, oneTimeCredential);
        String key = "platform:" + normalizedProvider + ":" + normalized(verifiedSubject);
        atomic(() -> { Account a = account(accountId); Long owner = loginIndex.get(key);
            if (owner != null && owner != accountId) throw new Conflict("platform identity already bound");
            loginIndex.put(key, accountId); a.platformKeys.add(key); audit(accountId, "PLATFORM_BOUND", provider); return null; });
    }

    public TokenPair loginPlatform(String provider, String oneTimeCredential, ClientContext client, LoginMode mode) {
        String normalizedProvider = normalized(provider);
        String subject = verifyPlatform(normalizedProvider, oneTimeCredential);
        String key = "platform:" + normalizedProvider + ":" + normalized(subject);
        return atomic(() -> {
            Account a = account(loginIndex.getOrDefault(key, -1L));
            enforceAllowed(a, client);
            return issue(a, client, mode, null);
        });
    }

    public TokenPair loginPassword(String login, char[] password, ClientContext client, LoginMode mode) {
        String normalized = normalizeLogin(login); Objects.requireNonNull(client); Objects.requireNonNull(mode);
        return atomic(() -> { Account a = account(loginIndex.getOrDefault("login:" + normalized, -1L));
            enforceAllowed(a, client); if (a.passwordHash == null || !passwords.verify(password, a.passwordHash)) throw new Unauthorized("invalid credentials");
            return issue(a, client, mode, null); });
    }

    public TokenPair refresh(String refreshToken, ClientContext client) {
        return refresh(refreshToken, client, randomToken());
    }

    /** A stable request id provides refresh single-flight without making a consumed refresh token reusable. */
    public TokenPair refresh(String refreshToken, ClientContext client, String requestId) {
        Objects.requireNonNull(client);
        requireText(requestId, "requestId");
        return atomic(() -> { RefreshFlight flight = new RefreshFlight(refreshToken, client.deviceId(), requestId);
            Instant now = clock.instant(); refreshFlights.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt));
            CompletedRefresh completed = refreshFlights.get(flight); if (completed != null) return completed.pair;
            Token old = requireToken(refreshToken, TokenKind.REFRESH); Account a = account(old.accountId);
            enforceAllowed(a, client); if (!old.deviceId.equals(client.deviceId())) throw new Unauthorized("device mismatch");
            old.revoked = true; audit(a.id, "REFRESH_ROTATED", old.id);
            TokenPair rotated = issue(a, client, LoginMode.MULTI_DEVICE, old.family);
            refreshFlights.put(flight, new CompletedRefresh(rotated, now.plusSeconds(5)));
            return rotated; });
    }

    public Session authorize(String accessToken, ClientContext client) {
        Objects.requireNonNull(client);
        return atomic(() -> { Token t = requireToken(accessToken, TokenKind.ACCESS); Account a = account(t.accountId);
            enforceAllowed(a, client); if (!t.deviceId.equals(client.deviceId())) throw new Unauthorized("device mismatch");
            return new Session(a.id, t.expiresAt, a.authGeneration, a.profile.revision()); });
    }

    public void logout(String accessToken) {
        atomic(() -> { Token token = tokens.get(accessToken); if (token != null) revokeFamily(token.family, "LOGOUT"); return null; });
    }

    public void changePassword(long accountId, char[] current, char[] replacement) {
        String hash = passwords.hash(replacement);
        atomic(() -> { Account a = account(accountId); if (a.passwordHash == null || !passwords.verify(current, a.passwordHash)) throw new Unauthorized("invalid credentials");
            a.passwordHash = hash; revokeAll(a, "PASSWORD_CHANGED"); return null; });
    }

    public void unbindPlatform(long accountId, String provider, String subject) {
        String key = "platform:" + normalized(provider) + ":" + normalized(subject);
        atomic(() -> { Account a = account(accountId); if (a.platformKeys.remove(key)) loginIndex.remove(key, accountId);
            revokeAll(a, "PLATFORM_UNBOUND"); return null; });
    }

    public void ban(long accountId, BanScope scope, String value, Instant until, String reason) {
        requireText(value, "ban value"); requireText(reason, "reason"); Objects.requireNonNull(scope); Objects.requireNonNull(until);
        atomic(() -> { Account a = account(accountId); Ban ban = new Ban(scope, value, until, reason);
            if (scope == BanScope.DEVICE || scope == BanScope.IP) globalBans.add(ban); else a.bans.add(ban);
            revokeAll(a, "BANNED_" + scope); return null; });
    }

    public void recordRealName(long accountId, Instant birthDate, boolean verified) {
        Objects.requireNonNull(birthDate); atomic(() -> { Account a = account(accountId); a.birthDate = birthDate; a.realNameVerified = verified; audit(accountId, "REAL_NAME_UPDATED", Boolean.toString(verified)); return null; });
    }

    public ComplianceDecision checkPlay(long accountId, Instant now, Duration playedToday) {
        Objects.requireNonNull(now); Objects.requireNonNull(playedToday);
        if (playedToday.isNegative()) throw new IllegalArgumentException("playedToday is negative");
        return atomic(() -> {
            Account a = account(accountId);
            if (!a.realNameVerified) return ComplianceDecision.DENY_UNVERIFIED;
            long years = Duration.between(a.birthDate, now).toDays() / 365;
            if (years < 18 && (now.atZone(java.time.ZoneOffset.UTC).getHour() < 12 || playedToday.compareTo(Duration.ofHours(3)) >= 0)) return ComplianceDecision.DENY_MINOR_LIMIT;
            return ComplianceDecision.ALLOW;
        });
    }

    /** Account-domain payment decision; Billing must call this before committing a charge. */
    public ComplianceDecision checkPayment(long accountId, ClientContext client, long paidThisMonthMinor,
                                             long proposedMinor) {
        if (paidThisMonthMinor < 0 || proposedMinor <= 0) throw new IllegalArgumentException("invalid payment amount");
        return atomic(() -> {
            Account a = account(accountId); enforceAllowedFor(a, client, BanScope.PAYMENT);
            if (!a.realNameVerified) return ComplianceDecision.DENY_UNVERIFIED;
            long years = Duration.between(a.birthDate, clock.instant()).toDays() / 365;
            if (years < 8) return ComplianceDecision.DENY_MINOR_PAYMENT;
            long perCharge = years < 16 ? 5_000 : 10_000;
            long monthly = years < 16 ? 20_000 : 40_000;
            long resultingMonthly;
            try { resultingMonthly = Math.addExact(paidThisMonthMinor, proposedMinor); }
            catch (ArithmeticException overflow) { return ComplianceDecision.DENY_MINOR_PAYMENT; }
            if (years < 18 && (proposedMinor > perCharge || resultingMonthly > monthly)) {
                return ComplianceDecision.DENY_MINOR_PAYMENT;
            }
            return ComplianceDecision.ALLOW;
        });
    }

    public void authorizeScopedAction(long accountId, ClientContext client, BanScope scope) {
        if (scope != BanScope.PAYMENT && scope != BanScope.CHAT) throw new IllegalArgumentException("scope is not an action scope");
        atomic(() -> { enforceAllowedFor(account(accountId), client, scope); return null; });
    }

    public AccountService.PlayerProfile updateProfile(long accountId, long expectedRevision, ProfilePatch patch,
                                                        UnaryOperator<String> contentFilter) {
        Objects.requireNonNull(patch); Objects.requireNonNull(contentFilter);
        return atomic(() -> { Account a = account(accountId); if (a.profile.revision() != expectedRevision) throw new Conflict("stale profile revision");
            Instant now = clock.instant(); if (a.lastProfileUpdate != null && now.isBefore(a.lastProfileUpdate.plusSeconds(30))) throw new RateLimited("profile update too frequent");
            String name = contentFilter.apply(patch.displayName().strip()); if (name.isBlank() || name.length() > 24) throw new IllegalArgumentException("invalid display name");
            a.profile = new AccountService.PlayerProfile(a.id, name, patch.avatarUrl(), expectedRevision + 1); a.lastProfileUpdate = now;
            audit(a.id, "PROFILE_UPDATED", Long.toString(a.profile.revision())); return a.profile; });
    }

    public List<AuditEvent> auditTrail(long accountId) { return atomic(() -> audit.stream().filter(e -> e.accountId == accountId).toList()); }

    @Override public AccountService.PlayerProfile profile(long accountId) {
        return atomic(() -> account(accountId).profile);
    }

    private TokenPair issue(Account a, ClientContext client, LoginMode mode, String existingFamily) {
        if (mode == LoginMode.SINGLE_DEVICE) revokeAll(a, "REPLACED_BY_NEW_LOGIN");
        Instant now = clock.instant(); String family = existingFamily == null ? randomToken() : existingFamily; String access = randomToken(); String refresh = randomToken();
        tokens.put(access, new Token(access, a.id, client.deviceId(), client.ipAddress(), family, TokenKind.ACCESS, now.plus(ACCESS_TTL), a.authGeneration));
        tokens.put(refresh, new Token(refresh, a.id, client.deviceId(), client.ipAddress(), family, TokenKind.REFRESH, now.plus(REFRESH_TTL), a.authGeneration));
        audit(a.id, "SESSION_ISSUED", client.deviceId()); return new TokenPair(access, refresh, now.plus(ACCESS_TTL), now.plus(REFRESH_TTL));
    }
    private Token requireToken(String id, TokenKind kind) {
        Token t = tokens.get(id); if (t == null || t.kind != kind || t.revoked || !clock.instant().isBefore(t.expiresAt)) throw new Unauthorized("invalid or expired token");
        Account a = account(t.accountId); if (t.generation != a.authGeneration) throw new Unauthorized("stale token generation"); return t;
    }
    private void revokeAll(Account a, String reason) { a.authGeneration++; tokens.values().stream().filter(t -> t.accountId == a.id).forEach(t -> t.revoked = true); audit(a.id, reason, "all-sessions"); }
    private void revokeFamily(String family, String reason) { tokens.values().stream().filter(t -> t.family.equals(family)).forEach(t -> t.revoked = true); audit(tokens.values().stream().filter(t -> t.family.equals(family)).findFirst().map(t -> t.accountId).orElse(0L), reason, family); }
    private void enforceAllowed(Account a, ClientContext c) { Instant now = clock.instant();
        for (Ban b : globalBans) if (now.isBefore(b.until) && b.value.equals(valueFor(b.scope, c))) throw new Forbidden("active " + b.scope + " ban");
        for (Ban b : a.bans) if (now.isBefore(b.until) && (b.scope == BanScope.ACCOUNT || b.value.equals(valueFor(b.scope, c)))) throw new Forbidden("active " + b.scope + " ban"); }
    private void enforceAllowedFor(Account a, ClientContext c, BanScope requestedScope) {
        enforceAllowed(a, c);
        Instant now = clock.instant();
        for (Ban b : a.bans) {
            if (b.scope == requestedScope && now.isBefore(b.until)) throw new Forbidden("active " + b.scope + " ban");
        }
    }
    private String verifyPlatform(String provider, String oneTimeCredential) {
        requireText(oneTimeCredential, "oneTimeCredential");
        PlatformCredentialVerifier verifier = platformVerifiers.get(provider);
        if (verifier == null) throw new Unauthorized("unsupported platform");
        String subject = verifier.verify(oneTimeCredential);
        return normalized(subject);
    }
    private static String valueFor(BanScope s, ClientContext c) { return switch (s) { case DEVICE -> c.deviceId; case IP -> c.ipAddress; case PAYMENT, CHAT, ACCOUNT -> ""; }; }
    private Account account(long id) { Account a = accounts.get(id); if (a == null) throw new Unauthorized("unknown account"); return a; }
    private void audit(long id, String action, String detail) { audit.add(new AuditEvent(id, action, detail, clock.instant())); }
    private String randomToken() { byte[] value = new byte[32]; random.nextBytes(value); return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private static String digestLookup(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
    private <T> T atomic(java.util.concurrent.Callable<T> work) { lock.lock(); try { return work.call(); } catch (RuntimeException e) { throw e; } catch (Exception impossible) { throw new IllegalStateException(impossible); } finally { lock.unlock(); } }
    private static String normalizeLogin(String v) { String n = normalized(v); if (n.length() < 3 || n.length() > 64) throw new IllegalArgumentException("invalid login"); return n; }
    private static String normalized(String v) { requireText(v, "value"); return v.strip().toLowerCase(java.util.Locale.ROOT); }
    private static void requireText(String v, String name) { if (v == null || v.isBlank()) throw new IllegalArgumentException("invalid " + name); }

    private static final class Account {
        final long id; final Set<String> platformKeys = new HashSet<>(); final List<Ban> bans = new ArrayList<>(); boolean guest; String passwordHash; long authGeneration; ClientContext originalClient;
        AccountService.PlayerProfile profile; String guestKey; Instant lastProfileUpdate; Instant birthDate; boolean realNameVerified;
        Account(long id, boolean guest, String passwordHash, ClientContext client) { this.id=id; this.guest=guest; this.passwordHash=passwordHash; this.originalClient=client; this.profile=new AccountService.PlayerProfile(id, "Player"+id, null, 0); }
    }
    private static final class Token { final String id; final long accountId; final String deviceId, ipAddress, family; final TokenKind kind; final Instant expiresAt; final long generation; boolean revoked;
        Token(String id,long accountId,String deviceId,String ipAddress,String family,TokenKind kind,Instant expiresAt,long generation){this.id=id;this.accountId=accountId;this.deviceId=deviceId;this.ipAddress=ipAddress;this.family=family;this.kind=kind;this.expiresAt=expiresAt;this.generation=generation;} }
    private record Ban(BanScope scope, String value, Instant until, String reason) {}
    private enum TokenKind { ACCESS, REFRESH }
    public enum LoginMode { SINGLE_DEVICE, MULTI_DEVICE }
    public enum BanScope { ACCOUNT, DEVICE, IP, PAYMENT, CHAT }
    public enum ComplianceDecision { ALLOW, DENY_UNVERIFIED, DENY_MINOR_LIMIT, DENY_MINOR_PAYMENT }
    public record ClientContext(String deviceId, String channel, String clientVersion, String ipAddress) { public ClientContext { requireText(deviceId,"deviceId"); requireText(channel,"channel"); requireText(clientVersion,"clientVersion"); requireText(ipAddress,"ipAddress"); } }
    public record Registration(long accountId, boolean guest, boolean existing) {}
    public record TokenPair(String accessToken,String refreshToken,Instant accessExpiresAt,Instant refreshExpiresAt) {}
    public record Session(long accountId,Instant expiresAt,long generation,long profileRevision) {}
    public record ProfilePatch(String displayName,String avatarUrl) { public ProfilePatch { requireText(displayName,"displayName"); } }
    public record AuditEvent(long accountId,String action,String detail,Instant occurredAt) {}
    @FunctionalInterface public interface PlatformCredentialVerifier { String verify(String oneTimeCredential); }
    private record RefreshFlight(String refreshToken, String deviceId, String requestId) {}
    private record CompletedRefresh(TokenPair pair, Instant expiresAt) {}
    public static class Unauthorized extends SecurityException { public Unauthorized(String m){super(m);} }
    public static class Forbidden extends SecurityException { public Forbidden(String m){super(m);} }
    public static class Conflict extends IllegalStateException { public Conflict(String m){super(m);} }
    public static class RateLimited extends IllegalStateException { public RateLimited(String m){super(m);} }
}
