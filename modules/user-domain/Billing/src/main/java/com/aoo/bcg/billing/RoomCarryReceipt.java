package com.aoo.bcg.billing;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Internal native facts. Neither a price quote nor permission to create a charge. */
public final class RoomCarryReceipt {
    private RoomCarryReceipt() { }
    public enum Kind { INITIAL, CONFIRM, REBUY, RESTART }
    public enum Stage { REQUESTED, APPLIED, USED, TERMINATED_UNUSED }
    public enum WriterKind { COMMAND, SYSTEM_SNAPSHOT }
    static void hash(String s) { if(s==null||!s.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("invalid receipt hash"); }
    static void positive(long x){if(x<=0)throw new IllegalArgumentException("positive identity required");}
    public record Attempt(String attemptId,String originalRequestId,String requestHash,long nativeActorId,
            long roomId,String instanceId,long epoch,int seat,long amount,Kind kind) {
        public Attempt {hash(attemptId);hash(requestHash);RoomChargeOperation.requireToken(originalRequestId,128);
            RoomChargeOperation.requireToken(instanceId,128);positive(nativeActorId);positive(roomId);positive(epoch);
            if(seat<0||amount<0)throw new IllegalArgumentException("resolved native seat/amount required");Objects.requireNonNull(kind);}
    }
    public record Effect(String effectId,Attempt attempt,Stage stage,long ordinal,long stateVersion) {
        public Effect {hash(effectId);Objects.requireNonNull(attempt);Objects.requireNonNull(stage);positive(ordinal);
            if(stateVersion<0)throw new IllegalArgumentException("invalid version");}
    }
    public record Batch(String batchHash,long throughOrdinal,List<Effect> effects) {
        public Batch {hash(batchHash);effects=List.copyOf(effects);if(effects.isEmpty())throw new IllegalArgumentException("empty batch");
            long prior=0;Set<String> ids=new HashSet<>();Attempt first=effects.getFirst().attempt();
            for(Effect e:effects){if(e.ordinal()<=prior||!ids.add(e.effectId())||e.attempt().roomId()!=first.roomId()
                    ||!e.attempt().instanceId().equals(first.instanceId())||e.attempt().epoch()!=first.epoch())throw new IllegalArgumentException("batch identity/order conflict");prior=e.ordinal();}
            if(throughOrdinal!=prior)throw new IllegalArgumentException("batch cursor conflict");}
    }
    public record Proof(WriterKind writerKind,String triggerId,String instanceId,long epoch,long fence,
            long stateVersion,String snapshotHash,String responseHash) {
        public Proof {Objects.requireNonNull(writerKind);RoomChargeOperation.requireToken(triggerId,128);
            RoomChargeOperation.requireToken(instanceId,128);positive(epoch);positive(fence);hash(snapshotHash);
            if(stateVersion<0||(writerKind==WriterKind.COMMAND)!=(responseHash!=null))throw new IllegalArgumentException("writer proof conflict");if(responseHash!=null)hash(responseHash);}
    }
    public record Original(RoomChargeOperation operation,JdbcRoomChargeCoordinator.Subject subject) {
        public Original {Objects.requireNonNull(operation);Objects.requireNonNull(subject);
            if(operation.kind()!=RoomChargeOperation.Kind.CARRY||operation.payerMode()==RoomChargeOperation.Payer.AA)
                throw new IllegalArgumentException("existing CARRY unit required");}
        void matches(Attempt a){if(operation.roomId()!=a.roomId()||operation.carryMinor()!=a.amount()||a.amount()<=0
                ||subject.initiatingActorId()!=a.nativeActorId())throw new IllegalArgumentException("original native association conflict");
            if(operation.context()==RoomChargeOperation.Context.HALL&&(operation.actorId()!=subject.creatorId()
                    ||operation.payerId()!=subject.creatorId()||subject.sourceClubId()!=0))throw new IllegalArgumentException("hall creator must pay");
            if(operation.context()!=RoomChargeOperation.Context.HALL&&subject.sourceClubId()<=0)throw new IllegalArgumentException("organization source required");}
    }
    @FunctionalInterface public interface StoredProofVerifier {void verify(Connection c,Batch batch,Proof proof)throws SQLException;}
    @FunctionalInterface public interface OriginalLinkVerifier {void verify(Connection c,Attempt attempt,Original original)throws SQLException;}
}
