package com.aoo.bcg.location;

import com.aoo.bcg.location.LocationModels.AuthorizationReport;
import com.aoo.bcg.location.LocationModels.NetworkRiskReport;
import com.aoo.bcg.location.LocationModels.TableRiskAssessment;
import java.time.Instant;
import java.util.Optional;

/** Transaction-capable ports. Implementations should key every operation by tenant. */
public final class LocationPersistence {
    private LocationPersistence() {}

    public interface AuthorizationRepository { void append(String tenantId, AuthorizationReport report, String actor); }
    public interface RiskSignalRepository { void append(String tenantId, NetworkRiskReport report, String actor); }
    public interface AssessmentRepository {
        void save(String tenantId, TableRiskAssessment assessment, String actor);
        Optional<TableRiskAssessment> findById(String tenantId, String assessmentId);
    }

    public enum ClaimStatus { CLAIMED, COMPLETED, IN_PROGRESS, CONFLICT }
    public record Claim(ClaimStatus status, Optional<String> responseReference) {
        public Claim { responseReference = responseReference == null ? Optional.empty() : responseReference; }
    }

    /** claim must atomically compare (tenant, operation, key, fingerprint); completed results outlive client retries. */
    public interface IdempotencyRepository {
        Claim claim(String tenantId, String operation, String key, String fingerprint, Instant expiresAt);
        void complete(String tenantId, String operation, String key, String responseReference);
        void abandon(String tenantId, String operation, String key);
    }
}
