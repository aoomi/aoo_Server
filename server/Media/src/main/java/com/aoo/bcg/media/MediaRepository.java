package com.aoo.bcg.media;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MediaRepository {
    enum TicketState { OPEN, COMPLETING, READY, FAILED, EXPIRED }
    record Asset(long id,String objectKey,MediaPolicy.Kind kind,String mime,long size,long durationMillis,String sha256) {}
    record Ticket(String id,long ownerId,String objectKey,String storageUploadId,MediaPolicy.Kind kind,String mime,long size,long durationMillis,String sha256,int chunkBytes,int expectedParts,Instant expiresAt,TicketState state,Long assetId) {}
    record Part(int number,long size,String etag,String sha256) {}
    Optional<Asset> findReadyByHash(String sha256,long size,String mime);
    boolean canRead(long assetId,long ownerId);
    void grant(long assetId,long ownerId);
    /** Validates the sender's READY voice asset and atomically grants the current room audience. */
    Asset authorizeVoice(long assetId,long senderId,List<Long> memberIds);
    void create(Ticket ticket);
    Optional<Ticket> ticket(String id);
    void addPart(String ticketId,Part part);
    List<Part> parts(String ticketId);
    boolean transition(String ticketId,TicketState expected,TicketState next);
    long finish(String ticketId,String objectKey,MediaPolicy.Kind kind,String mime,long size,long durationMillis,String sha256,long ownerId);
    void fail(String ticketId,TicketState state,String reason);
    List<Ticket> expired(Instant now,int limit);
    Optional<Asset> asset(long assetId);
}
