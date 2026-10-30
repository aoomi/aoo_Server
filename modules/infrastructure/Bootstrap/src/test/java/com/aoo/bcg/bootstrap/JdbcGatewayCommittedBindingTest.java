package com.aoo.bcg.bootstrap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.gamespi.*;
import java.util.Map;
class JdbcGatewayCommittedBindingTest {
    @Test void withReplayCodePreservesBinding() throws Exception {
        var binding=new CommittedCommandBinding(2,"user","move",7,0,"req","A",1,2,3);
        var original=new GameCommandResult("move","req",Map.of()).withCommittedBinding(binding);
        var method=JdbcGatewayGameCommandCommitter.class.getDeclaredMethod("withReplayCode",GameCommandResult.class,core.replay.ReplayCodeRepository.Mapping.class,int.class);
        method.setAccessible(true);
        var result=(GameCommandResult)method.invoke(null,original,new core.replay.ReplayCodeRepository.Mapping("test-code",7,0,"ACTIVE",null),1);
        assertEquals(binding,result.committedBinding());assertEquals("test-code",result.body().asMap().get("replayCode"));
    }
    @Test void rejectsCrossRoomScope() throws Exception {
        var method=JdbcGatewayGameCommandCommitter.class.getDeclaredMethod("requireMatchingRoom",GameRoomHandle.class,GameCommandRequest.class);
        method.setAccessible(true);
        var room=new GameRoomHandle(7,1,"v1",new Object());
        var request=new GameCommandRequest("move","req",1,8,0,"v1","user",0,Map.of());
        var error=assertThrows(java.lang.reflect.InvocationTargetException.class,()->method.invoke(null,room,request));
        assertInstanceOf(IllegalArgumentException.class,error.getCause());
        assertEquals("COMMAND_ROOM_SCOPE_CONFLICT",error.getCause().getMessage());
        assertEquals(8,request.roomId());
    }
    @Test void acceptsMatchingRoomScope() throws Exception {
        var method=JdbcGatewayGameCommandCommitter.class.getDeclaredMethod("requireMatchingRoom",GameRoomHandle.class,GameCommandRequest.class);
        method.setAccessible(true);
        var room=new GameRoomHandle(7,1,"v1",new Object());
        var request=new GameCommandRequest("move","req",1,7,0,"v1","user",0,Map.of());
        assertDoesNotThrow(()->method.invoke(null,room,request));
        assertEquals(7,request.roomId());
    }
    // Canonical transaction A→B and rollback require a separately admitted native MySQL fixture.
}
