package com.aoo.bcg.account;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AccountSecurityServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-24T13:00:00Z");
    private static final AccountSecurityService.ClientContext PHONE = new AccountSecurityService.ClientContext("phone-1","appstore","1.2.3","127.0.0.1");
    private AccountSecurityService service() { return new AccountSecurityService(Clock.fixed(NOW, ZoneOffset.UTC)); }

    @Test void guestIsStableAndCanUpgradeWithoutChangingIdentity() {
        var s=service(); var first=s.registerGuest("recovery-secret",PHONE); var second=s.registerGuest("recovery-secret",PHONE);
        assertEquals(first.accountId(),second.accountId()); assertTrue(second.existing());
        var guestSession=s.loginGuest("recovery-secret",PHONE,AccountSecurityService.LoginMode.SINGLE_DEVICE);
        assertEquals(first.accountId(),s.authorize(guestSession.accessToken(),PHONE).accountId());
        s.upgradeGuest(first.accountId(),"alice","correct horse battery staple".toCharArray());
        assertThrows(AccountSecurityService.Unauthorized.class,()->s.loginGuest("recovery-secret",PHONE,AccountSecurityService.LoginMode.SINGLE_DEVICE));
        var pair=s.loginPassword("ALICE","correct horse battery staple".toCharArray(),PHONE, AccountSecurityService.LoginMode.SINGLE_DEVICE);
        assertEquals(first.accountId(),s.authorize(pair.accessToken(),PHONE).accountId());
    }

    @Test void refreshRotatesAndReplayIsRejected() {
        var s=service(); s.registerPassword("alice","correct horse battery staple".toCharArray(),PHONE);
        var pair=s.loginPassword("alice","correct horse battery staple".toCharArray(),PHONE, AccountSecurityService.LoginMode.MULTI_DEVICE);
        var rotated=s.refresh(pair.refreshToken(),PHONE); assertNotEquals(pair.refreshToken(),rotated.refreshToken());
        assertThrows(AccountSecurityService.Unauthorized.class,()->s.refresh(pair.refreshToken(),PHONE));
    }

    @Test void concurrentRefreshIsSingleFlightOnlyForTheSameRequest() throws Exception {
        var s=service(); s.registerPassword("alice","correct horse battery staple".toCharArray(),PHONE);
        var pair=s.loginPassword("alice","correct horse battery staple".toCharArray(),PHONE, AccountSecurityService.LoginMode.MULTI_DEVICE);
        var ready=new java.util.concurrent.CountDownLatch(2); var start=new java.util.concurrent.CountDownLatch(1);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<AccountSecurityService.TokenPair> refresh=()->{ready.countDown();start.await();return s.refresh(pair.refreshToken(),PHONE,"refresh-request-1");};
        try { var first=executor.submit(refresh);var second=executor.submit(refresh);ready.await();start.countDown();assertEquals(first.get(),second.get()); }
        finally { executor.shutdownNow(); }
        assertThrows(AccountSecurityService.Unauthorized.class,()->s.refresh(pair.refreshToken(),PHONE,"refresh-request-2"));
    }

    @Test void singleDeviceLoginAndPasswordChangeRevokeOldTokens() {
        var s=service(); long id=s.registerPassword("alice","correct horse battery staple".toCharArray(),PHONE).accountId();
        var first=s.loginPassword("alice","correct horse battery staple".toCharArray(),PHONE, AccountSecurityService.LoginMode.SINGLE_DEVICE);
        var phone2=new AccountSecurityService.ClientContext("phone-2","appstore","1.2.3","127.0.0.2");
        var second=s.loginPassword("alice","correct horse battery staple".toCharArray(),phone2, AccountSecurityService.LoginMode.SINGLE_DEVICE);
        assertThrows(AccountSecurityService.Unauthorized.class,()->s.authorize(first.accessToken(),PHONE));
        s.changePassword(id,"correct horse battery staple".toCharArray(),"a newer correct password".toCharArray());
        assertThrows(AccountSecurityService.Unauthorized.class,()->s.authorize(second.accessToken(),phone2));
    }

    @Test void banRevokesExistingSessionAndBlocksNewLogin() {
        var s=service(); long id=s.registerPassword("alice","correct horse battery staple".toCharArray(),PHONE).accountId();
        var pair=s.loginPassword("alice","correct horse battery staple".toCharArray(),PHONE, AccountSecurityService.LoginMode.MULTI_DEVICE);
        s.ban(id,AccountSecurityService.BanScope.ACCOUNT,"alice",NOW.plusSeconds(60),"fraud");
        assertThrows(AccountSecurityService.Unauthorized.class,()->s.authorize(pair.accessToken(),PHONE));
        assertThrows(AccountSecurityService.Forbidden.class,()->s.loginPassword("alice","correct horse battery staple".toCharArray(),PHONE, AccountSecurityService.LoginMode.MULTI_DEVICE));
    }

    @Test void profileIsRevisionCheckedFilteredAndRateLimited() {
        var s=service(); long id=s.registerGuest("recover",PHONE).accountId();
        var p=s.updateProfile(id,0,new AccountSecurityService.ProfilePatch(" Alice ","https://avatar"),String::toUpperCase);
        assertEquals("ALICE",p.displayName()); assertEquals(1,p.revision());
        assertThrows(AccountSecurityService.Conflict.class,()->s.updateProfile(id,0,new AccountSecurityService.ProfilePatch("Bob",null),String::toUpperCase));
        assertThrows(AccountSecurityService.RateLimited.class,()->s.updateProfile(id,1,new AccountSecurityService.ProfilePatch("Bob",null),String::toUpperCase));
    }

    @Test void realNameAndMinorLimitsAreAuthoritative() {
        var s=service(); long id=s.registerGuest("recover",PHONE).accountId();
        assertEquals(AccountSecurityService.ComplianceDecision.DENY_UNVERIFIED,s.checkPlay(id,NOW,Duration.ZERO));
        s.recordRealName(id,NOW.minus(Duration.ofDays(3650)),true);
        assertEquals(AccountSecurityService.ComplianceDecision.DENY_MINOR_LIMIT,s.checkPlay(id,NOW,Duration.ofHours(3)));
    }

    @Test void platformCredentialIsServerVerifiedBeforeBindingAndLogin() {
        var s=new AccountSecurityService(Clock.fixed(NOW,ZoneOffset.UTC),java.util.Map.of("wechat", credential -> {
            if (!credential.equals("provider-one-time-code")) throw new AccountSecurityService.Unauthorized("provider rejected");
            return "provider-user-42";
        }));
        long id=s.registerGuest("recover",PHONE).accountId();
        assertThrows(AccountSecurityService.Unauthorized.class,()->s.bindPlatform(id,"wechat","client-subject"));
        s.bindPlatform(id,"wechat","provider-one-time-code");
        var pair=s.loginPlatform("wechat","provider-one-time-code",PHONE,AccountSecurityService.LoginMode.MULTI_DEVICE);
        assertEquals(id,s.authorize(pair.accessToken(),PHONE).accountId());
    }

    @Test void deviceBanIsGlobalAndPaymentBanHasAnIndependentGate() {
        var s=service(); long alice=s.registerPassword("alice","correct horse battery staple".toCharArray(),PHONE).accountId();
        s.registerPassword("bob","another correct battery staple".toCharArray(),PHONE);
        s.ban(alice,AccountSecurityService.BanScope.DEVICE,PHONE.deviceId(),NOW.plusSeconds(60),"compromised device");
        assertThrows(AccountSecurityService.Forbidden.class,()->s.loginPassword("bob","another correct battery staple".toCharArray(),PHONE,AccountSecurityService.LoginMode.MULTI_DEVICE));

        var other=new AccountSecurityService.ClientContext("safe-phone","appstore","1.2.3","127.0.0.9");
        long carol=s.registerGuest("carol-recover",other).accountId();
        s.recordRealName(carol,NOW.minus(Duration.ofDays(365L*30)),true);
        assertEquals(AccountSecurityService.ComplianceDecision.ALLOW,s.checkPayment(carol,other,0,100));
        s.ban(carol,AccountSecurityService.BanScope.PAYMENT,Long.toString(carol),NOW.plusSeconds(60),"chargeback");
        assertThrows(AccountSecurityService.Forbidden.class,()->s.checkPayment(carol,other,0,100));
    }

    @Test void minorPaymentTotalCannotOverflow() {
        var s=service(); long id=s.registerGuest("minor-recover",PHONE).accountId();
        s.recordRealName(id,NOW.minus(Duration.ofDays(365L*15)),true);
        assertEquals(AccountSecurityService.ComplianceDecision.DENY_MINOR_PAYMENT,
                s.checkPayment(id,PHONE,Long.MAX_VALUE,1));
    }
}
