package core.network.client2game.handler.room;
import business.player.Player; import com.ddm.server.websocket.def.ErrorCode; import com.ddm.server.websocket.handler.requset.WebSocketRequest; import core.network.client2game.handler.PlayerHandler; import java.io.IOException;
/** Retired: all shuffle writes must enter room.shuffle through Protocol V2. */
public abstract class CBaseXiPai extends PlayerHandler {@Override public final void handle(Player p,WebSocketRequest r,String m)throws IOException{r.error(ErrorCode.NotAllow,"ROOM_LEGACY_WRITE_DISABLED");}}
