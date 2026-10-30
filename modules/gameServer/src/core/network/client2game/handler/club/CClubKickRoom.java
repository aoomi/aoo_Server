package core.network.client2game.handler.club;
import business.player.Player; import com.ddm.server.websocket.def.ErrorCode; import com.ddm.server.websocket.handler.requset.WebSocketRequest; import core.network.client2game.handler.PlayerHandler; import java.io.IOException;
/** Retired: Club kick is coordinated after room authority commit. */
public final class CClubKickRoom extends PlayerHandler {@Override public void handle(Player p,WebSocketRequest r,String m)throws IOException{r.error(ErrorCode.NotAllow,"ROOM_LEGACY_WRITE_DISABLED");}}
