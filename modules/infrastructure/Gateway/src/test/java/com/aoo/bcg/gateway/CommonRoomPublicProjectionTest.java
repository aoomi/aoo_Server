package com.aoo.bcg.gateway;

import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.gamespi.*;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class CommonRoomPublicProjectionTest {
    @Test void requiredProviderDecoratesPerspectiveAndMissingProviderFailsClosed() {
        var provider=new ProviderSession();
        Map<String,Object> decorated=GatewayRoomBroadcastHub.withCommonRoom(Map.of("phase","WAITING"),provider,11L,true);
        assertTrue(decorated.containsKey("commonRoom"));
        assertThrows(IllegalStateException.class,()->GatewayRoomBroadcastHub.withCommonRoom(Map.of(),new Session(),11L,true));
    }

    @Test void unregisteredSessionIsUnaffected() {
        Map<String,Object> body=Map.of("legacy",true);
        assertSame(body,GatewayRoomBroadcastHub.withCommonRoom(body,new Session(),11L,false));
    }

    private static class Session implements AuthoritativeGameSession {
        public GameCommandResult execute(GameCommandRequest request){throw new UnsupportedOperationException();}
        public Map<String,Object>viewFor(long viewer){return Map.of();}
        public Map<String,Object>authoritativeState(){return Map.of();}
        public long stateVersion(){return 0;}
        public OperationDeadline operationDeadline(){return OperationDeadline.none();}
        public OperationDeadlineArbiter deadlineArbiter(){return new OperationDeadlineArbiter();}
        public List<String>invariantViolations(){return List.of();}
        public SettlementPayload settlement(int round,String version){throw new UnsupportedOperationException();}
    }
    private static final class ProviderSession extends Session implements CommonRoomPublicViewProvider {
        public CommonRoomPublicView commonRoomPublicView(long viewer){
            return new CommonRoomPublicView(2,0,List.of(
                    new CommonRoomPublicSeat(0,true,viewer,null,null,null),
                    new CommonRoomPublicSeat(1,false,null,null,null,null)));
        }
    }
}
