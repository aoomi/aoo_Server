package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameCommandRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ReplayRecordingGateTest {
    @Test void waitingRoomActionsDoNotCreateReplayParticipants(){
        assertFalse(JdbcGatewayGameCommandCommitter.shouldRecordReplay(
                Map.of("phase","WAITING","started",false),request("ready")));
        assertFalse(JdbcGatewayGameCommandCommitter.shouldRecordReplay(
                Map.of("phase","WAITING","started",false),request("join")));
    }

    @Test void formallyStartedRoomRecordsGameplayButNotReadOnlyRequests(){
        assertTrue(JdbcGatewayGameCommandCommitter.shouldRecordReplay(
                Map.of("phase","PLAYING","started",true),request("play")));
        assertFalse(JdbcGatewayGameCommandCommitter.shouldRecordReplay(
                Map.of("phase","PLAYING","started",true),request("state")));
    }

    private static GameCommandRequest request(String action){
        return new GameCommandRequest("poker.action","request-1",1,100001,0,"1.0.0","11",0,
                Map.of("action",action));
    }
}
