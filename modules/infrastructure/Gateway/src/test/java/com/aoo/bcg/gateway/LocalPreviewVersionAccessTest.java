package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalPreviewVersionAccessTest {
    @TempDir Path directory;
    private static final String TICKET = "synthetic-preview-fixture-".repeat(2);
    private static final String SERVICE = "synthetic-server-only-fixture-".repeat(2);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final MovingClock clock = new MovingClock();
    private LocalPreviewVersionAccess access;
    private LocalPreviewVersionAccess.Descriptor descriptor;
    private static final InetSocketAddress PEER = new InetSocketAddress("127.0.0.1", 12345);
    private void setup() throws Exception {
        Files.writeString(directory.resolve("service"), SERVICE);
        descriptor = new LocalPreviewVersionAccess.Descriptor("generation-1", "aoo-local-preview", "version.check",
                "http://localhost:7456", "device-1", clock.instant(), clock.instant().plusSeconds(300), 2,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(TICKET.getBytes(StandardCharsets.UTF_8))), false);
        write(descriptor);
        access = LocalPreviewVersionAccess.configured("local-preview", directory.resolve("descriptor").toString(),
                directory.resolve("service").toString(), "127.0.0.1", clock, json);
    }
    private void write(LocalPreviewVersionAccess.Descriptor d) throws Exception { json.writeValue(directory.resolve("descriptor").toFile(), d); }
    private String authorize() { return access.authorize("POST", "/api/v2/version/check", "http://localhost:7456", PEER, false, "Bearer " + TICKET, "device-1"); }
    private LocalPreviewVersionAccess.Descriptor generation(String id, Instant issued, Instant expires) {
        return new LocalPreviewVersionAccess.Descriptor(id, descriptor.namespace(), descriptor.purpose(),
                descriptor.origin(), descriptor.deviceId(), issued, expires, 2, descriptor.tokenSha256(), false);
    }
    @Test void expiredGenerationsDoNotExhaustLifetimeCapacityAndCannotReplay() throws Exception {
        setup();
        var original = descriptor;
        for (int i = 0; i < 360; i++) {
            write(generation("rotated-" + i, clock.instant(), clock.instant().plusSeconds(1)));
            assertEquals("Bearer " + SERVICE, authorize());
            clock.now = clock.now.plusSeconds(1);
        }
        write(generation("latest", clock.instant(), clock.instant().plusSeconds(300)));
        assertEquals("Bearer " + SERVICE, authorize());
        write(original);
        assertThrows(SecurityException.class, this::authorize);
        clock.now = original.issuedAt();
        assertThrows(SecurityException.class, this::authorize);
    }
    @Test void unexpiredCapacityRemainsBounded() throws Exception {
        setup();
        for (int i = 0; i < 128; i++) {
            write(generation("live-" + i, clock.instant(), clock.instant().plusSeconds(300)));
            assertEquals("Bearer " + SERVICE, authorize());
        }
        write(generation("overflow", clock.instant(), clock.instant().plusSeconds(300)));
        assertThrows(SecurityException.class, this::authorize);
    }
    @Test void reclamationPreservesRevocationAndConsumedBudget() throws Exception {
        setup();
        var spent = descriptor;
        authorize(); authorize();
        var revoked = generation("revoked", clock.instant(), clock.instant().plusSeconds(300));
        write(revoked); authorize(); access.revoke(revoked.generationId());
        write(generation("short", clock.instant(), clock.instant().plusSeconds(1))); authorize();
        clock.now = clock.now.plusSeconds(1);
        write(generation("after-expiry", clock.instant(), clock.instant().plusSeconds(299))); authorize();
        write(spent); assertThrows(SecurityException.class, this::authorize);
        write(revoked); assertThrows(SecurityException.class, this::authorize);
    }
    @Test void diagnoseRedactsValuesAndReturnsPredicatesOnly() {
        var result = LocalPreviewVersionAccess.diagnose("POST", "/api/v2/version/check",
                "http://localhost:7456", PEER, false, "Bearer secret-token", "device-1");
        assertEquals(true, result.get("method"));
        assertEquals(true, result.get("origin"));
        assertEquals(true, result.get("peer"));
        assertEquals(true, result.get("tokenHash"));
        assertEquals("UNKNOWN", result.get("expiry"));
        assertFalse(result.toString().contains("secret-token"));
        assertFalse(result.toString().contains("device-1"));
    }

    @Test void disabledPreservesOriginalAuthorizationAndExplicitProfileIsRequired() {
        assertNull(LocalPreviewVersionAccess.disabled().authorize(null,null,null,null,false,null,null));
        assertThrows(IllegalArgumentException.class, () -> LocalPreviewVersionAccess.configured("production","a","b","127.0.0.1",clock,json));
        assertThrows(IllegalArgumentException.class, () -> LocalPreviewVersionAccess.configured("","a","b","127.0.0.1",clock,json));
        assertThrows(IllegalArgumentException.class, () -> LocalPreviewVersionAccess.configured("local-preview","a","b","0.0.0.0/0",clock,json));
    }
    @Test void twoUsesOnlyAndDescriptorReloadDoesNotResetBudget() throws Exception {
        setup(); assertEquals("Bearer " + SERVICE, authorize()); write(descriptor);
        assertEquals("Bearer " + SERVICE, authorize()); write(descriptor);
        assertThrows(SecurityException.class, this::authorize);
    }
    @Test void originIsNotIdentityAndWrongPeerForwardingDeviceOrTicketCannotConsume() throws Exception {
        setup();
        assertThrows(SecurityException.class, () -> access.authorize("POST","/api/v2/version/check","http://localhost:7456",new InetSocketAddress("192.168.1.9",99),false,"Bearer "+TICKET,"device-1"));
        assertThrows(SecurityException.class, () -> access.authorize("POST","/api/v2/version/check","http://localhost:7456",PEER,true,"Bearer "+TICKET,"device-1"));
        assertThrows(SecurityException.class, () -> access.authorize("POST","/api/v2/version/check","http://127.0.0.1:7456",PEER,false,"Bearer "+TICKET,"device-1"));
        assertThrows(SecurityException.class, () -> access.authorize("POST","/api/v2/version/check","http://localhost:7456",PEER,false,"Bearer "+TICKET,"other"));
        assertThrows(SecurityException.class, () -> access.authorize("POST","/api/v2/version/check","http://localhost:7456",PEER,false,"Bearer "+SERVICE,"device-1"));
        assertEquals("Bearer "+SERVICE,authorize()); assertEquals("Bearer "+SERVICE,authorize());
    }
    @Test void wrongMethodPathQueryAndDeviceTypeAreDenied() throws Exception {
        setup();
        for (String path : List.of("/api/v2/account/login", "/api/v2/version/check?extra=1"))
            assertThrows(SecurityException.class, () -> access.authorize("POST",path,"http://localhost:7456",PEER,false,"Bearer "+TICKET,"device-1"));
        assertThrows(SecurityException.class, () -> access.authorize("GET","/api/v2/version/check","http://localhost:7456",PEER,false,"Bearer "+TICKET,"device-1"));
        assertThrows(SecurityException.class, () -> access.authorize("POST","/api/v2/version/check","http://localhost:7456",PEER,false,"Bearer "+TICKET,1));
    }
    @Test void expiryIsExclusiveAndFutureIssueIsDenied() throws Exception {
        setup(); clock.now = descriptor.issuedAt().minusNanos(1); assertThrows(SecurityException.class,this::authorize);
        clock.now = descriptor.expiresAt(); assertThrows(SecurityException.class,this::authorize);
    }
    @Test void invalidLifetimePurposeNamespaceAndMissingDescriptorFailClosed() throws Exception {
        setup();
        write(new LocalPreviewVersionAccess.Descriptor("generation-2","aoo-local-preview","version.check",descriptor.origin(),descriptor.deviceId(),descriptor.issuedAt(),descriptor.issuedAt().plusSeconds(301),2,descriptor.tokenSha256(),false));
        assertThrows(SecurityException.class,this::authorize);
        write(new LocalPreviewVersionAccess.Descriptor("generation-2","production","login",descriptor.origin(),descriptor.deviceId(),descriptor.issuedAt(),descriptor.expiresAt(),2,descriptor.tokenSha256(),false));
        assertThrows(SecurityException.class,this::authorize);
        Files.delete(directory.resolve("descriptor")); assertThrows(SecurityException.class,this::authorize);
    }
    @Test void revocationCannotBeUndoneByReloadOrChangedGrant() throws Exception {
        setup(); authorize(); access.revoke(descriptor.generationId()); assertThrows(SecurityException.class,this::authorize);
        write(descriptor); assertThrows(SecurityException.class,this::authorize);
        setup(); authorize();
        write(new LocalPreviewVersionAccess.Descriptor(descriptor.generationId(),descriptor.namespace(),descriptor.purpose(),descriptor.origin(),descriptor.deviceId(),descriptor.issuedAt(),descriptor.expiresAt().minusSeconds(1),2,descriptor.tokenSha256(),false));
        assertThrows(SecurityException.class,this::authorize);
    }
    @Test void descriptorRevocationAndIdenticalServiceCredentialAreDenied() throws Exception {
        setup();
        write(new LocalPreviewVersionAccess.Descriptor(descriptor.generationId(),descriptor.namespace(),descriptor.purpose(),descriptor.origin(),descriptor.deviceId(),descriptor.issuedAt(),descriptor.expiresAt(),2,descriptor.tokenSha256(),true));
        assertThrows(SecurityException.class,this::authorize);write(descriptor);assertThrows(SecurityException.class,this::authorize);
        setup();Files.writeString(directory.resolve("service"),TICKET);assertThrows(SecurityException.class,this::authorize);
    }
    @Test void concurrentRequestsAtomicallyConsumeAtMostTwo() throws Exception {
        setup();var pool=Executors.newFixedThreadPool(8);try {
            List<Callable<Boolean>> tasks=new ArrayList<>();for(int i=0;i<16;i++)tasks.add(()->{try{authorize();return true;}catch(SecurityException denied){return false;}});
            int allowed=0;for(Future<Boolean> f:pool.invokeAll(tasks))if(f.get())allowed++;assertEquals(2,allowed);
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
    }
    private static final class MovingClock extends Clock {
        Instant now=Instant.parse("2026-10-15T00:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}
    }
}
