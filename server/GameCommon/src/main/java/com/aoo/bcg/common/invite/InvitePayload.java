package com.aoo.bcg.common.invite;
import java.time.Instant;
public record InvitePayload(long roomId,int gameId,long clubId,String inviteId,String playVersion,Instant expiresAt){public InvitePayload{if(roomId<=0||gameId<=0||clubId<0||inviteId==null||!inviteId.matches("[A-Za-z0-9_-]{16,64}")||playVersion==null||playVersion.isBlank()||expiresAt==null)throw new IllegalArgumentException("invalid invite payload");}}
