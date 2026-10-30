package com.aoo.bcg.gamespi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Native facts only. No currency, price, JDBC or wire route. */
public interface CarryBillingEffectSource {
    enum Kind { INITIAL, CONFIRM, REBUY, RESTART }
    enum Stage { REQUESTED, APPLIED, USED, TERMINATED_UNUSED }
    record Proposal(String originalRequestId,long roomId,long authenticatedActorId,int seatId,long carryScore,Kind kind) {
        public Proposal { text(originalRequestId,128); positive(roomId); positive(authenticatedActorId); if(seatId< -1||carryScore<0||carryScore>1_000_000_000L)throw new IllegalArgumentException("invalid carry proposal");Objects.requireNonNull(kind); }
    }
    record Effect(String effectId,String attemptId,String originalRequestId,long roomId,long actorId,int seatId,long carryScore,Kind kind,Stage stage,long ordinal,long stateVersion) {
        public Effect { text(originalRequestId,128);positive(roomId);positive(actorId);positive(ordinal);if(seatId< -1||carryScore<0||carryScore>1_000_000_000L||stateVersion<0)throw new IllegalArgumentException("invalid carry effect");Objects.requireNonNull(kind);Objects.requireNonNull(stage);if(!attemptId.equals(CarryBillingEffectSource.attemptId(roomId,actorId,originalRequestId,kind))||!effectId.equals(CarryBillingEffectSource.effectId(attemptId,stage,ordinal,seatId,carryScore,stateVersion)))throw new IllegalArgumentException("carry effect fingerprint mismatch"); }
        public static Effect create(String request,long room,long actor,int seat,long amount,Kind kind,Stage stage,long ordinal,long version){String attempt=CarryBillingEffectSource.attemptId(room,actor,request,kind);return new Effect(CarryBillingEffectSource.effectId(attempt,stage,ordinal,seat,amount,version),attempt,request,room,actor,seat,amount,kind,stage,ordinal,version);}
    }
    record Batch(long roomId,long throughOrdinal,List<Effect> effects) {
        public Batch {positive(roomId);if(throughOrdinal<0)throw new IllegalArgumentException("invalid carry cursor");effects=List.copyOf(effects);long last=0;Set<String>ids=new HashSet<>();for(Effect e:effects){if(e.roomId()!=roomId||e.ordinal()<=last||e.ordinal()>throughOrdinal||!ids.add(e.effectId()))throw new IllegalArgumentException("invalid carry effect batch");last=e.ordinal();}if(!effects.isEmpty()&&last!=throughOrdinal)throw new IllegalArgumentException("carry cursor must end at final effect");}
        public String hash(){List<String> parts=new ArrayList<>(List.of("CARRY_NATIVE_BATCH_V1",Long.toString(roomId),Long.toString(throughOrdinal)));for(Effect e:effects)parts.add(e.effectId());return digest(parts);}
    }
    Optional<Proposal> proposeCarry(GameCommandRequest request);
    Batch pendingCarryEffects();
    /** Only called by the transaction owner after staging matching receipts, before the final snapshot. */
    void acknowledgeCarryEffects(long throughOrdinal,String batchHash);
    static String attemptId(long room,long actor,String request,Kind kind){return digest(List.of("CARRY_NATIVE_ATTEMPT_V1",Long.toString(room),Long.toString(actor),request,kind.name()));}
    static String effectId(String attempt,Stage stage,long ordinal,int seat,long amount,long version){return digest(List.of("CARRY_NATIVE_EFFECT_V1",attempt,stage.name(),Long.toString(ordinal),Integer.toString(seat),Long.toString(amount),Long.toString(version)));}
    private static String digest(List<String> parts){try{MessageDigest d=MessageDigest.getInstance("SHA-256");for(String p:parts){byte[]b=p.getBytes(StandardCharsets.UTF_8);d.update((b.length+":").getBytes(StandardCharsets.US_ASCII));d.update(b);}return HexFormat.of().formatHex(d.digest());}catch(Exception e){throw new IllegalStateException("carry fingerprint unavailable",e);}}
    private static void text(String v,int max){if(v==null||v.isBlank()||v.length()>max||v.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("invalid carry request token");}
    private static void positive(long v){if(v<=0)throw new IllegalArgumentException("positive carry identity required");}
}
