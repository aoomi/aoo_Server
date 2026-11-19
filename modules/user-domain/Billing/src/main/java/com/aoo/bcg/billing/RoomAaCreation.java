package com.aoo.bcg.billing;

import java.time.Instant;
import java.util.Objects;

/** Authenticated server AA creation metadata. No creation monetary intent is represented here. */
public final class RoomAaCreation {
    private RoomAaCreation() { }
    public record Key(String requestId,String requestHash,long authenticatedCreatorId) {
        public Key { RoomChargeOperation.requireToken(requestId,128);if(requestId.length()<16)throw new IllegalArgumentException("invalid create request");RoomAaParticipation.hash(requestHash);RoomAaParticipation.positive(authenticatedCreatorId); }
        public String sourceKey(){return RoomChargeOperation.hash("AA_CREATE_SOURCE_V1",requestId);}
    }
    public record Context(long roomId,long gameId,String playVersion,String regionCode,String measureKind,int measureValue,int playerCount,RoomBillingSourceBinding.AuthorityReference authority) {
        public Context { RoomAaParticipation.positive(roomId);new RoomAaParticipation.Context(1,gameId,playVersion,regionCode,measureKind,measureValue,playerCount,authority); }
        public RoomAaParticipation.Context participation(long creator){return new RoomAaParticipation.Context(creator,gameId,playVersion,regionCode,measureKind,measureValue,playerCount,authority);}
        public String contextHash(){return RoomChargeOperation.hash("AA_CREATE_CONTEXT_V1",roomId,gameId,playVersion,regionCode,measureKind,measureValue,playerCount,authority.releaseId(),authority.indexGeneration(),authority.runtimeSourceHash(),authority.classificationRegionCode());}
        String policyKey(){return RoomChargeOperation.hash(gameId,playVersion,regionCode,RoomChargeOperation.Kind.FIXED,RoomChargeOperation.Context.HALL,playerCount,measureKind,measureValue,RoomChargeOperation.Payer.AA,0L);}
    }
    public record PolicyIdentity(long publicationId,String policyKey,JdbcRoomCostPolicyRepository.PolicySnapshot snapshot,String snapshotHash) {
        public PolicyIdentity {
            RoomAaParticipation.positive(publicationId);RoomAaParticipation.hash(policyKey);RoomAaParticipation.hash(snapshotHash);Objects.requireNonNull(snapshot);
            if(publicationId!=snapshot.publicationId()||snapshot.policyVersion()<=0||snapshot.unitCost()<0||snapshot.amount()!=snapshot.unitCost()||snapshot.tierMinor()!=0||!"ROOM_CARD".equals(snapshot.currency())
                    ||snapshot.refundRef()==null||snapshot.refundRef().isBlank()||snapshot.amount()==0&&(snapshot.freeRef()==null||snapshot.freeRef().isBlank()))throw new IllegalArgumentException("invalid AA creation policy");
            RoomAaParticipation.hash(snapshot.contentHash());if(!snapshotHash.equals(hash(snapshot)))throw new IllegalArgumentException("AA creation policy hash conflict");
        }
        public PolicyIdentity(String key,JdbcRoomCostPolicyRepository.PolicySnapshot snapshot){this(snapshot.publicationId(),key,snapshot,hash(snapshot));}
        private static String hash(JdbcRoomCostPolicyRepository.PolicySnapshot s){return RoomChargeOperation.hash("AA_CREATE_POLICY_V1",Objects.requireNonNull(s).json());}
    }
    public record Binding(Key key,Context context,PolicyIdentity policy,String bindingHash) {
        public Binding { Objects.requireNonNull(key);Objects.requireNonNull(context);Objects.requireNonNull(policy);if(!policy.policyKey().equals(context.policyKey())||!fingerprint(key,context,policy).equals(bindingHash))throw new IllegalArgumentException("AA creation binding conflict"); }
        public Binding(Key key,Context context,PolicyIdentity policy){this(key,context,policy,fingerprint(key,context,policy));}
    }
    private static String fingerprint(Key k,Context c,PolicyIdentity p){return RoomChargeOperation.hash("AA_CREATE_BINDING_V1",k.requestId(),k.requestHash(),k.authenticatedCreatorId(),c.contextHash(),p.policyKey(),p.snapshotHash());}
    public record RuntimeProof(String roomInstanceId,long lifecycleEpoch,long fencingToken,long stateVersion,long creatorId,long gameId,String playVersion) {
        public RuntimeProof {RoomAaParticipation.instance(roomInstanceId);RoomAaParticipation.positive(lifecycleEpoch);RoomAaParticipation.positive(fencingToken);RoomAaParticipation.positive(creatorId);RoomAaParticipation.positive(gameId);RoomChargeOperation.requireToken(playVersion,64);if(stateVersion<0)throw new IllegalArgumentException("negative state version");}
        String hash(){return RoomChargeOperation.hash(roomInstanceId,lifecycleEpoch,fencingToken,stateVersion,creatorId,gameId,playVersion);}
    }
    public enum EventType { COMMITTED,ABORTED_BEFORE_COMMIT,DESTROYED }
    public record Receipt(EventType eventType,RuntimeProof proof,Instant createdAt) {
        public Receipt { Objects.requireNonNull(eventType);Objects.requireNonNull(createdAt);if((eventType==EventType.ABORTED_BEFORE_COMMIT)!=(proof==null)||createdAt.getNano()%1_000_000!=0)throw new IllegalArgumentException("invalid AA creation receipt"); }
        public String hash(){return RoomChargeOperation.hash("AA_CREATE_OUTCOME_V1",eventType,proof==null?"":proof.hash());}
    }
}
