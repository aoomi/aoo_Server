package com.aoo.bcg.common.event;
import java.util.List;
public interface OutboxRepository {
    void append(OutboxEvent event);
    List<OutboxClaim> claim(int limit, String workerId, java.time.Instant now, java.time.Duration lease);
    void markPublished(OutboxClaim claim);
    boolean recordFailure(OutboxClaim claim, String error, java.time.Instant nextAttemptAt, int maxAttempts);
    int purgeTerminalBefore(java.time.Instant cutoff, int limit);
    long deadLetterCount();
    OutboxBacklog backlog();
}
