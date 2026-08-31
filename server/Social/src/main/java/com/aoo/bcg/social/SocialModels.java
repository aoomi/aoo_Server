package com.aoo.bcg.social;

import java.time.Instant;

public final class SocialModels {
    private SocialModels(){}
    public record FriendRequest(long id,long requesterId,long recipientId,String status,Instant createdAt,Instant decidedAt){}
    public record Notification(long id,long recipientId,String type,String payload,Instant createdAt){}
    public record Mail(long id,long recipientId,String subject,String body,Instant createdAt){}
    public record Notice(long id,String title,String content,Instant startsAt,Instant endsAt,Instant updatedAt){}
    public record RedDots(long mail,long notification,long notice,long total){}
    public record Presence(long accountId,String state,Long roomId,Instant lastSeenAt){}
    public record Friend(long accountId,String state,Long roomId,Instant lastSeenAt){}
    public record Feed<T>(java.util.List<T> items,long nextCursor,long readCursor){}
}
