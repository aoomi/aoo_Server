package com.aoo.bcg.support;

import java.time.Instant;
import java.util.List;

public record SupportLiveSession(long id, long caseId, long playerId, Long agentId, Status status,
                                 long lastMessageId, Instant createdAt, Instant updatedAt,
                                 Instant closedAt, long version, List<Message> messages) {
    public enum Status { QUEUED, ACTIVE, DISCONNECTED, CLOSED }
    public enum Sender { PLAYER, SUPPORT, SYSTEM }
    public record Message(long id, Sender sender, long senderId, String body, String mediaReference,
                          Instant createdAt) {}
}
