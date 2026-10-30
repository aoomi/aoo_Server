package com.aoo.bcg.location;

import static com.aoo.bcg.location.LocationModels.*;
import static com.aoo.bcg.location.LocationPersistence.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class LocationRiskService {
    public static final int API_VERSION = 1;
    private final AuthorizationRepository authorizations;
    private final RiskSignalRepository signals;
    private final AssessmentRepository assessments;
    private final IdempotencyRepository idempotency;
    private final LocationRiskPolicy policy;
    private final Clock clock;

    public LocationRiskService(AuthorizationRepository authorizations, RiskSignalRepository signals,
            AssessmentRepository assessments, IdempotencyRepository idempotency,
            LocationRiskPolicy policy, Clock clock) {
        this.authorizations = Objects.requireNonNull(authorizations); this.signals = Objects.requireNonNull(signals);
        this.assessments = Objects.requireNonNull(assessments); this.idempotency = Objects.requireNonNull(idempotency);
        this.policy = Objects.requireNonNull(policy); this.clock = Objects.requireNonNull(clock);
    }

    public String reportAuthorization(int version, String idempotencyKey, CallerContext caller, AuthorizationReport report) {
        check(version, caller, "location.authorization.write");
        return once(caller, "authorization", idempotencyKey, fingerprint(report), report.eventId(),
                () -> authorizations.append(caller.tenantId(), report, caller.subject()));
    }

    public String reportNetworkRisk(int version, String idempotencyKey, CallerContext caller, NetworkRiskReport report) {
        check(version, caller, "location.signal.write");
        return once(caller, "network-risk", idempotencyKey, fingerprint(report), report.eventId(),
                () -> signals.append(caller.tenantId(), report, caller.subject()));
    }

    public TableRiskAssessment assessTable(int version, String idempotencyKey, CallerContext caller, TableRiskRequest request) {
        check(version, caller, "location.risk.assess");
        String assessmentId = UUID.nameUUIDFromBytes((caller.tenantId() + ':' + idempotencyKey).getBytes(StandardCharsets.UTF_8)).toString();
        Claim claim = claim(caller, "table-risk", idempotencyKey, fingerprint(request));
        if (claim.status() == ClaimStatus.COMPLETED) {
            String reference = claim.responseReference().orElseThrow(() -> new LocationServiceException(LocationErrorCode.PERSISTENCE_FAILURE, "completed request has no response reference"));
            return assessments.findById(caller.tenantId(), reference).orElseThrow(() -> new LocationServiceException(LocationErrorCode.PERSISTENCE_FAILURE, "completed assessment is unavailable"));
        }
        Map<Long, GeoPoint> points = new HashMap<>();
        for (ParticipantLocation location : request.locations()) {
            if (!request.participantIds().contains(location.playerId())) throw new LocationServiceException(LocationErrorCode.PARTICIPANT_NOT_FOUND, "location player is not seated");
            if (points.put(location.playerId(), location.point()) != null) LocationModels.invalid("duplicate player location");
        }
        List<PairRisk> pairs = new ArrayList<>(); int max = 0;
        for (int i = 0; i < request.participantIds().size(); i++) for (int j = i + 1; j < request.participantIds().size(); j++) {
            long a = request.participantIds().get(i), b = request.participantIds().get(j); GeoPoint pa = points.get(a), pb = points.get(b);
            EnumSet<RiskReason> reasons = EnumSet.noneOf(RiskReason.class); int score = 0; Double distance = null;
            if (!usable(pa, request.evaluatedAt()) || !usable(pb, request.evaluatedAt())) { reasons.add(RiskReason.LOCATION_UNAVAILABLE); score += policy.missingScore(); }
            else {
                distance = haversineMeters(pa, pb);
                if (pa.accuracyMeters() > policy.maxAccuracyMeters() || pb.accuracyMeters() > policy.maxAccuracyMeters()) { reasons.add(RiskReason.LOW_LOCATION_ACCURACY); score += policy.inaccurateScore(); }
                if (distance <= policy.proximityMeters() + pa.accuracyMeters() + pb.accuracyMeters()) { reasons.add(RiskReason.GPS_PROXIMITY); score += policy.proximityScore(); }
            }
            score = Math.min(100, score); max = Math.max(max, score); pairs.add(new PairRisk(a, b, distance, score, reasons));
        }
        TableRiskAssessment result = new TableRiskAssessment(assessmentId, request.tableId(), API_VERSION, max, pairs, request.evaluatedAt());
        try { assessments.save(caller.tenantId(), result, caller.subject()); idempotency.complete(caller.tenantId(), "table-risk", idempotencyKey, assessmentId); return result; }
        catch (RuntimeException e) { idempotency.abandon(caller.tenantId(), "table-risk", idempotencyKey); throw e; }
    }

    private boolean usable(GeoPoint point, Instant evaluatedAt) { return point != null && !point.capturedAt().isAfter(evaluatedAt.plusSeconds(30)) && Duration.between(point.capturedAt(), evaluatedAt).compareTo(policy.maxLocationAge()) <= 0; }
    static double haversineMeters(GeoPoint a, GeoPoint b) { double r=6371008.8, p1=Math.toRadians(a.latitude()), p2=Math.toRadians(b.latitude()), dp=p2-p1, dl=Math.toRadians(b.longitude()-a.longitude()); double h=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2); return 2*r*Math.asin(Math.sqrt(h)); }
    private void check(int version, CallerContext caller, String scope) { if (version != API_VERSION) throw new LocationServiceException(LocationErrorCode.UNSUPPORTED_API_VERSION, "supported api version: " + API_VERSION); Objects.requireNonNull(caller, "caller").require(scope); }
    private String once(CallerContext caller, String operation, String key, String fingerprint, String reference, Runnable write) { Claim c=claim(caller, operation, key, fingerprint); if(c.status()==ClaimStatus.COMPLETED)return c.responseReference().orElse(reference); try { write.run(); idempotency.complete(caller.tenantId(), operation, key, reference); return reference; } catch(RuntimeException e){ idempotency.abandon(caller.tenantId(), operation, key); throw e; } }
    private Claim claim(CallerContext caller, String operation, String key, String fingerprint) { LocationModels.text(key,"idempotencyKey",128); Claim c=idempotency.claim(caller.tenantId(),operation,key,fingerprint,clock.instant().plus(policy.idempotencyTtl())); if(c.status()==ClaimStatus.CONFLICT)throw new LocationServiceException(LocationErrorCode.IDEMPOTENCY_KEY_REUSED,"idempotency key used with different payload"); if(c.status()==ClaimStatus.IN_PROGRESS)throw new LocationServiceException(LocationErrorCode.REQUEST_IN_PROGRESS,"request is being processed"); return c; }
    private static String fingerprint(Object value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8))); } catch(NoSuchAlgorithmException e){ throw new IllegalStateException(e); } }
}
