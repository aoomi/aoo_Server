package com.aoo.bcg.location;

import static com.aoo.bcg.location.LocationModels.*;
import static com.aoo.bcg.location.LocationPersistence.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class LocationRiskServiceTest {
    private final Instant now = Instant.parse("2026-08-24T00:00:00Z");
    private final FakeStore store = new FakeStore();
    private final CallerContext caller = new CallerContext("tenant-a", "gateway", Set.of("location.authorization.write", "location.signal.write", "location.risk.assess"), now);
    private final LocationRiskService service = new LocationRiskService(store, store, store, store,
            new LocationRiskPolicy(100, 75, Duration.ofMinutes(2), 80, 20, 15, Duration.ofDays(7)), Clock.fixed(now, ZoneOffset.UTC));

    @Test void assessesClosePlayersAndPersistsResult() {
        var req = new TableRiskRequest("table-7", List.of(11L, 22L), List.of(
                new ParticipantLocation(11, new GeoPoint(30, 104, 5, now.minusSeconds(5))),
                new ParticipantLocation(22, new GeoPoint(30.0001, 104, 5, now.minusSeconds(4)))), now);
        var result = service.assessTable(1, "risk-1", caller, req);
        assertTrue(result.pairs().getFirst().distanceMeters() < 20);
        assertEquals(80, result.maxScore());
        assertEquals(result, store.assessment);
        assertEquals(result, service.assessTable(1, "risk-1", caller, req));
    }

    @Test void authorizationRetryReturnsOriginalReferenceWithoutSecondWrite() {
        var report = new AuthorizationReport("evt-1", 9, AuthorizationStatus.GRANTED, now, "ios", null);
        assertEquals("evt-1", service.reportAuthorization(1, "idem-1", caller, report));
        assertEquals("evt-1", service.reportAuthorization(1, "idem-1", caller, report));
        assertEquals(1, store.authorizationWrites);
    }

    @Test void rejectsScopeVersionAndIdempotencyPayloadConflict() {
        var report = new AuthorizationReport("evt-1", 9, AuthorizationStatus.GRANTED, now, "ios", null);
        var weak = new CallerContext("tenant-a", "caller", Set.of(), now);
        assertEquals(LocationErrorCode.FORBIDDEN, assertThrows(LocationServiceException.class, () -> service.reportAuthorization(1, "x", weak, report)).code());
        assertEquals(LocationErrorCode.UNSUPPORTED_API_VERSION, assertThrows(LocationServiceException.class, () -> service.reportAuthorization(2, "x", caller, report)).code());
        service.reportAuthorization(1, "same", caller, report);
        var changed = new AuthorizationReport("evt-2", 9, AuthorizationStatus.DENIED, now, "ios", null);
        assertEquals(LocationErrorCode.IDEMPOTENCY_KEY_REUSED, assertThrows(LocationServiceException.class, () -> service.reportAuthorization(1, "same", caller, changed)).code());
    }

    private static final class FakeStore implements AuthorizationRepository, RiskSignalRepository, AssessmentRepository, IdempotencyRepository {
        final Map<String,String> fingerprints = new HashMap<>(), responses = new HashMap<>(); int authorizationWrites; TableRiskAssessment assessment;
        public void append(String t, AuthorizationReport r, String a){ authorizationWrites++; }
        public void append(String t, NetworkRiskReport r, String a){}
        public void save(String t, TableRiskAssessment r, String a){ assessment=r; }
        public Optional<TableRiskAssessment> findById(String t,String id){return assessment != null && assessment.assessmentId().equals(id) ? Optional.of(assessment) : Optional.empty();}
        public Claim claim(String t,String o,String k,String f,Instant e){String key=t+o+k, old=fingerprints.putIfAbsent(key,f); if(old!=null&&!old.equals(f))return new Claim(ClaimStatus.CONFLICT,Optional.empty()); if(responses.containsKey(key))return new Claim(ClaimStatus.COMPLETED,Optional.of(responses.get(key))); return new Claim(ClaimStatus.CLAIMED,Optional.empty());}
        public void complete(String t,String o,String k,String r){responses.put(t+o+k,r);}
        public void abandon(String t,String o,String k){fingerprints.remove(t+o+k);}
    }
}
