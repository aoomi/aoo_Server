package core.network.client2game.handler.union;
import business.player.Player; import com.ddm.server.websocket.def.ErrorCode; import com.ddm.server.websocket.handler.requset.WebSocketRequest; import core.network.client2game.handler.PlayerHandler; import java.io.IOException;
/** Retired: Union kick is coordinated after room authority commit. */
public final class CUnionKickRoom extends PlayerHandler {@Override public void handle(Player p,WebSocketRequest r,String m)throws IOException{r.error(ErrorCode.NotAllow,"ROOM_LEGACY_WRITE_DISABLED");}}
