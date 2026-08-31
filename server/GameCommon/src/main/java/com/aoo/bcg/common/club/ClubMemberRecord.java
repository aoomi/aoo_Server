package com.aoo.bcg.common.club;

import java.time.Instant;
import java.util.Map;

public record ClubMemberRecord(long clubId, long playerId, Status status, Role role, boolean online,
        Map<String, Object> profile, Instant joinedAt, Instant updatedAt, long revision) {
    public ClubMemberRecord(long clubId,long playerId,Status status,Role role,boolean online,Map<String,Object>profile,Instant joinedAt,Instant updatedAt){this(clubId,playerId,status,role,online,profile,joinedAt,updatedAt,0);}
    public ClubMemberRecord {
        if (clubId <= 0 || playerId <= 0 || status == null || role == null
                || joinedAt == null || updatedAt == null || revision < 0) throw new IllegalArgumentException("invalid club member");
        profile = Map.copyOf(profile == null ? Map.of() : profile);
    }
    public enum Status { ACTIVE, SUSPENDED, LEFT }
    public enum Role { MEMBER, MANAGER, OWNER }
}
