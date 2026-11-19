package core.replay;

import java.time.Instant;
import java.util.Optional;

public interface ReplayCodeRepository {
    Optional<Mapping> findShortCode(String code);
    Optional<Mapping> findByTarget(long roomId, int setId);
    Optional<LegacyTarget> findLegacyCode(String code);
    boolean replayReady(long roomId, int setId);
    boolean mayView(long playerId, long roomId, int setId);
    void grantCodeAccess(String code, long playerId);
    long allocatedCount(int length);
    InsertResult insert(String code, long roomId, int setId);

    enum InsertResult { INSERTED, CODE_COLLISION, TARGET_EXISTS }
    record Mapping(String code, long roomId, int setId, String status, Instant expiresAt) {}
    record LegacyTarget(String code, long roomId, int setId, int gameType) {}
}
