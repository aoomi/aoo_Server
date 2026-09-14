package com.aoo.bcg.common.recovery;
import java.util.List;
public record RecoveredRoom<S>(RoomLease lease, RoomSnapshot sourceSnapshot, S state, String stateDigest,
                               List<String> expiredDeadlineKinds) {
    public RecoveredRoom { expiredDeadlineKinds=List.copyOf(expiredDeadlineKinds==null?List.of():expiredDeadlineKinds); }
    public RecoveredRoom(RoomLease lease,RoomSnapshot sourceSnapshot,S state,String stateDigest){this(lease,sourceSnapshot,state,stateDigest,List.of());}
}
