package com.aoo.bcg.billing;

import java.time.Instant;
import java.util.Objects;

/** Exact server business outcome, not a ledger-derived guess. */
public record RoomBillingBusinessOutcome(EventType eventType,long roomId,long creatorId,String roomInstanceId,
        Long lifecycleEpoch,Long fencingToken,Long stateVersion,long gameId,String playVersion,Instant createdAt) {
    public enum EventType { CREATE_COMMITTED, CREATE_ABORTED_BEFORE_COMMIT, CREATE_DESTROYED }
    public RoomBillingBusinessOutcome {
        Objects.requireNonNull(eventType);Objects.requireNonNull(createdAt);RoomChargeOperation.requireToken(playVersion,64);
        if(roomId<=0||creatorId<=0||gameId<=0||createdAt.getNano()%1_000_000!=0)throw new IllegalArgumentException("invalid business outcome");
        if(eventType==EventType.CREATE_ABORTED_BEFORE_COMMIT) {
            if(roomInstanceId!=null||lifecycleEpoch!=null||fencingToken!=null||stateVersion!=null)throw new IllegalArgumentException("abort has no runtime identity");
        } else if(roomInstanceId==null||roomInstanceId.isBlank()||roomInstanceId.length()>128||roomInstanceId.chars().anyMatch(Character::isISOControl)
                ||lifecycleEpoch==null||lifecycleEpoch<=0||fencingToken==null||fencingToken<=0||stateVersion==null||stateVersion<0)throw new IllegalArgumentException("invalid exact runtime identity");
    }
    public String hash(){return RoomChargeOperation.hash("ROOM_BILLING_OUTCOME_V1",eventType,roomId,creatorId,
            roomInstanceId==null?"":roomInstanceId,lifecycleEpoch==null?"":lifecycleEpoch,fencingToken==null?"":fencingToken,stateVersion==null?"":stateVersion,gameId,playVersion);}
}
