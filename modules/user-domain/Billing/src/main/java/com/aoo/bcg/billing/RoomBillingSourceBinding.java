package com.aoo.bcg.billing;

import java.util.Objects;

/** Immutable server source envelope; its fingerprint is not authentication or business success. */
public final class RoomBillingSourceBinding {
    private RoomBillingSourceBinding() {}
    public record SourceKey(String namespace,String requestId,String requestHash,long authenticatedActor) {
        public SourceKey {
            if(!"HALL_CREATE".equals(namespace)||requestId==null||!requestId.matches("[A-Za-z0-9_.:-]{16,128}")
                    ||!hash(requestHash)||authenticatedActor<=0)throw new IllegalArgumentException("invalid billing source key");
        }
    }
    public record AuthorityReference(long releaseId,long indexGeneration,String runtimeSourceHash,String classificationRegionCode) {
        public AuthorityReference {
            if(releaseId<=0||indexGeneration<=0||!hash(runtimeSourceHash))throw new IllegalArgumentException("invalid billing authority");
            RoomChargeOperation.requireToken(classificationRegionCode,32);
        }
    }
    public record BoundIntent(SourceKey key,RoomBillingIntent intent,JdbcRoomChargeCoordinator.Subject subject,
                              AuthorityReference authority,String bindingHash) {
        public BoundIntent {
            Objects.requireNonNull(key);Objects.requireNonNull(intent);Objects.requireNonNull(subject);Objects.requireNonNull(authority);
            var op=intent.operation();
            if(key.authenticatedActor()!=subject.initiatingActorId()||key.authenticatedActor()!=subject.creatorId()||!authority.classificationRegionCode().equals(op.regionCode())
                    ||(op.context()==RoomChargeOperation.Context.HALL)!=(subject.sourceClubId()==0))
                throw new IllegalArgumentException("billing source context mismatch");
            if(!fingerprint(key,intent,subject,authority).equals(bindingHash))throw new IllegalArgumentException("billing source fingerprint mismatch");
        }
        public BoundIntent(SourceKey key,RoomBillingIntent intent,JdbcRoomChargeCoordinator.Subject subject,AuthorityReference authority) {
            this(key,intent,subject,authority,fingerprint(key,intent,subject,authority));
        }
    }
    private static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private static String fingerprint(SourceKey k,RoomBillingIntent i,JdbcRoomChargeCoordinator.Subject s,AuthorityReference a) {
        Objects.requireNonNull(k);Objects.requireNonNull(i);Objects.requireNonNull(s);Objects.requireNonNull(a);
        return RoomChargeOperation.hash("ROOM_BILLING_SOURCE_V1",k.namespace(),k.requestId(),k.requestHash(),k.authenticatedActor(),
                i.operation().commandHash(),s.commandHash(i.operation()),a.releaseId(),a.indexGeneration(),a.runtimeSourceHash(),a.classificationRegionCode());
    }
}
