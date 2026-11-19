package com.aoo.bcg.billing;

import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/** Internal authenticated participation contract. None of these objects is a client payment instruction. */
public final class RoomAaParticipation {
    private RoomAaParticipation() { }
    static void hash(String value) { if(value==null||!value.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("invalid AA hash"); }
    static void instance(String value) { if(value==null||value.isEmpty()||value.length()>128||value.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("invalid room instance"); }
    static void positive(long value) { if(value<=0)throw new IllegalArgumentException("positive AA identity required"); }
    public record SessionKey(long roomId,String roomInstanceId,long lifecycleEpoch) {
        public SessionKey { positive(roomId);positive(lifecycleEpoch);instance(roomInstanceId); }
        public String sessionHash() { return RoomChargeOperation.hash("AA_SESSION_V1",roomId,roomInstanceId,lifecycleEpoch); }
        public String roomAttempt() { return "aa:"+Base64.getUrlEncoder().withoutPadding().encodeToString(HexFormat.of().parseHex(sessionHash())); }
    }
    public record Context(long creatorId,long gameId,String playVersion,String regionCode,String measureKind,
            int measureValue,int playerCount,RoomBillingSourceBinding.AuthorityReference authority) {
        public Context {
            positive(creatorId);positive(gameId);positive(measureValue);positive(playerCount);
            RoomChargeOperation.requireToken(playVersion,64);RoomChargeOperation.requireToken(regionCode,32);
            if(!("ROUND_COUNT".equals(measureKind)||"DURATION_MINUTES".equals(measureKind)))throw new IllegalArgumentException("invalid AA measure");
            if(!Objects.requireNonNull(authority).classificationRegionCode().equals(regionCode))throw new IllegalArgumentException("AA authority region mismatch");
        }
        public String contextHash() { return RoomChargeOperation.hash("AA_CONTEXT_V1",creatorId,gameId,playVersion,regionCode,measureKind,measureValue,playerCount,
                authority.classificationRegionCode(),authority.releaseId(),authority.indexGeneration(),authority.runtimeSourceHash()); }
    }
    public record CommandKey(String requestId,String requestHash,long authenticatedActorId) {
        public CommandKey { RoomChargeOperation.requireToken(requestId,128);hash(requestHash);positive(authenticatedActorId); }
        public String commandKey() { return RoomChargeOperation.hash("AA_COMMAND_KEY_V1",requestId); }
    }
    public record PreSeatState(int actorSeatBefore,long fencingToken,long stateVersion,String snapshotHash) {
        public PreSeatState { if(actorSeatBefore< -1||fencingToken<=0||stateVersion<0)throw new IllegalArgumentException("invalid pre-seat state");hash(snapshotHash); }
    }
    public record SeatProof(String requestId,String requestHash,long actorId,int seatId,String roomInstanceId,
            long lifecycleEpoch,long fencingToken,long stateVersion,String snapshotHash,String responseHash) {
        public SeatProof { RoomChargeOperation.requireToken(requestId,128);hash(requestHash);positive(actorId);instance(roomInstanceId);
            if(seatId<0||lifecycleEpoch<=0||fencingToken<=0||stateVersion<=0)throw new IllegalArgumentException("invalid seat proof");hash(snapshotHash);hash(responseHash); }
    }
    public enum Disposition { NEW_ATTEMPT, REUSE_PAID, COMMAND_REPLAY }
    public record Preparation(SessionKey key,Context context,CommandKey command,long sessionId,long attemptNo,
            RoomBillingIntent intent,JdbcRoomChargeCoordinator.Subject subject,JdbcRoomChargeCoordinator.Charge charge,
            PreSeatState preState,Disposition disposition,String preparationHash,SeatReceipt replayReceipt) {
        public Preparation { Objects.requireNonNull(key);Objects.requireNonNull(context);Objects.requireNonNull(command);positive(sessionId);positive(attemptNo);
            Objects.requireNonNull(intent);Objects.requireNonNull(subject);Objects.requireNonNull(charge);Objects.requireNonNull(preState);Objects.requireNonNull(disposition);hash(preparationHash); }
    }
    public record SeatReceipt(long sessionId,long attemptNo,String operationKey,SeatProof proof,Instant createdAt) {
        public SeatReceipt { positive(sessionId);positive(attemptNo);hash(operationKey);Objects.requireNonNull(proof);Objects.requireNonNull(createdAt);
            if(createdAt.getNano()%1_000_000!=0)throw new IllegalArgumentException("AA receipt requires millisecond precision"); }
    }
    static String binding(SessionKey key,Context context,long actor,long attempt,RoomBillingIntent intent,JdbcRoomChargeCoordinator.Subject subject) {
        return RoomChargeOperation.hash("AA_ATTEMPT_V1",key.sessionHash(),context.contextHash(),actor,attempt,intent.operation().operationKey(),subject.commandHash(intent.operation()));
    }
    static String preparation(SessionKey key,Context context,CommandKey command,long attempt,String binding,PreSeatState pre) {
        return RoomChargeOperation.hash("AA_PREPARATION_V1",key.sessionHash(),context.contextHash(),command.requestId(),command.requestHash(),command.authenticatedActorId(),attempt,
                binding,pre.actorSeatBefore(),pre.fencingToken(),pre.stateVersion(),pre.snapshotHash());
    }
}
