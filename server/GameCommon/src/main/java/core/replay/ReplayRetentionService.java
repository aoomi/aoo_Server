package core.replay;

import java.time.Instant;

/** Archives expired replay data before bounded deletion from hot storage. */
public interface ReplayRetentionService {
    int archiveExpiredEvents(Instant expiredBefore, int batchSize);
    default int deleteExpiredEvents(Instant expiredBefore,int batchSize){return archiveExpiredEvents(expiredBefore,batchSize);}

    int deleteOrphanParticipants(Instant expiredBefore, int batchSize);

    /** Permanently removes archived sets only after their manifest deadline and legal-hold check. */
    int deleteExpiredArchives(Instant deleteBefore, int batchSize);
}
