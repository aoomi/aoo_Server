package core.network.client2game.handler.room;
import business.player.Player; import com.ddm.server.websocket.def.ErrorCode; import com.ddm.server.websocket.handler.requset.WebSocketRequest; import core.network.client2game.handler.PlayerHandler; import java.io.IOException;
/** Retired: all kick writes must enter room.kick through Protocol V2. */
public class CBaseGameKickRoom extends PlayerHandler {@Override public final void handle(Player p,WebSocketRequest r,String m)throws IOException{r.error(ErrorCode.NotAllow,"ROOM_LEGACY_WRITE_DISABLED");}}
