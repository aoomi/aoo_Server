package com.aoo.bcg.admin;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminControlPlaneSecurityTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test void signedAdminIdentityIsBoundToRequestAndCannotBeReplayedOrMixedWithPlayerIdentity() {
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        var auth = new AdminRequestAuthenticator(SECRET, Clock.fixed(now, ZoneOffset.UTC));
        String nonce = "nonce_0123456789abcdef";
        String requestId = "request-1";
        String signature = auth.signForTest(42, now.toEpochMilli(), nonce, requestId,
                "PUT", "/api/v2/admin/users/7");

        assertEquals(42, auth.authenticate("Bearer " + SECRET, "42", Long.toString(now.toEpochMilli()),
                nonce, signature, requestId, "PUT", "/api/v2/admin/users/7", Map.of("X-Admin-Id", "42")));
        assertThrows(SecurityException.class, () -> auth.authenticate("Bearer " + SECRET, "42",
                Long.toString(now.toEpochMilli()), nonce, signature, requestId, "PUT",
                "/api/v2/admin/users/7", Map.of()));

        var fresh = new AdminRequestAuthenticator(SECRET, Clock.fixed(now, ZoneOffset.UTC));
        assertThrows(SecurityException.class, () -> fresh.authenticate("Bearer " + SECRET, "43",
                Long.toString(now.toEpochMilli()), nonce, signature, requestId, "PUT",
                "/api/v2/admin/users/7", Map.of()));
        assertThrows(SecurityException.class, () -> fresh.authenticate("Bearer " + SECRET, "42",
                Long.toString(now.toEpochMilli()), "different_nonce_1234", signature, requestId, "PUT",
                "/api/v2/admin/users/7", Map.of("X-Player-Token", "player-token")));
    }

    @Test void controlPlaneMustBindLoopback() {
        assertDoesNotThrow(() -> AdminRequestAuthenticator.requireLoopbackBoundary(
                new InetSocketAddress("127.0.0.1", 8088)));
        assertThrows(IllegalStateException.class, () -> AdminRequestAuthenticator.requireLoopbackBoundary(
                new InetSocketAddress("0.0.0.0", 8088)));
    }

    @Test void rbacRequiresPermissionScopeAndOneTimeIndependentApprovalForHighRiskAction() {
        var store = new AdminAccessPolicy.InMemoryStore();
        store.grant(new AdminAccessPolicy.Grant(10, "asset-adjustment.apply", "CLUB", "99"));
        store.approve(new AdminAccessPolicy.Approval("approval-1", 10, 11,
                "asset-adjustment.apply", "CLUB", "99", Instant.parse("2026-08-24T10:05:00Z")));
        var policy = new AdminAccessPolicy((operator, permission) -> operator == 10,
                store, store, Clock.fixed(Instant.parse("2026-08-24T10:00:00Z"), ZoneOffset.UTC));

        assertFalse(policy.authorize(10, "asset-adjustment.apply", "CLUB", "98",
                AdminAccessPolicy.Risk.HIGH, "approval-1").allowed());
        assertTrue(policy.authorize(10, "asset-adjustment.apply", "CLUB", "99",
                AdminAccessPolicy.Risk.HIGH, "approval-1").allowed());
        assertEquals("secondary_approval_required", policy.authorize(10, "asset-adjustment.apply",
                "CLUB", "99", AdminAccessPolicy.Risk.HIGH, "approval-1").reason());
    }

    @Test void highRiskHttpApprovalIsShortLivedScopedIndependentAndOneTime() {
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        AdminAuthorizationService grants = (operator, permission) -> operator == 11
                && (permission.equals("game-profile.publish") || permission.equals("scope.GAME.516"));
        var verifier = new AdminSecondaryApprovalVerifier(SECRET, grants, Clock.fixed(now, ZoneOffset.UTC));
        long expires = now.plusSeconds(120).toEpochMilli();
        String signature = verifier.signForTest("approval_0123456789", 10, 11,
                "game-profile.publish", "GAME", "516", expires);
        assertTrue(verifier.verify(10, "game-profile.publish", "GAME", "516",
                "approval_0123456789", "11", Long.toString(expires), signature));
        assertFalse(verifier.verify(10, "game-profile.publish", "GAME", "516",
                "approval_0123456789", "11", Long.toString(expires), signature));
        assertFalse(verifier.verify(10, "game-profile.publish", "GAME", "517",
                "approval_0123456780", "11", Long.toString(expires), signature));
    }

    @Test void sharedNonceStoreRejectsReplayAcrossControlPlaneInstances() {
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        var shared = new InMemoryAdminNonceStore(clock);
        var first = new AdminRequestAuthenticator(SECRET, clock, shared);
        var second = new AdminRequestAuthenticator(SECRET, clock, shared);
        String nonce = "nonce_cross_instance_01";
        String signature = first.signForTest(42, now.toEpochMilli(), nonce, "cross-1", "GET",
                "/api/v2/admin/game-profiles");
        assertEquals(42, first.authenticate("Bearer " + SECRET, "42", Long.toString(now.toEpochMilli()),
                nonce, signature, "cross-1", "GET", "/api/v2/admin/game-profiles", Map.of()));
        assertThrows(SecurityException.class, () -> second.authenticate("Bearer " + SECRET, "42",
                Long.toString(now.toEpochMilli()), nonce, signature, "cross-1", "GET",
                "/api/v2/admin/game-profiles", Map.of()));
    }
}
