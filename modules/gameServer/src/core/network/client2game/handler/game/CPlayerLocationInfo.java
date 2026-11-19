package core.network.client2game.handler.game;

import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CPlayer_LocationInfo;
import jsproto.c2s.iclass.SPlayer_LocationInfo;

import java.io.IOException;

/**
 * 获取玩家定位信息
 *
 * @author Administrator
 */
public class CPlayerLocationInfo extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {

        final CPlayer_LocationInfo req = new Gson().fromJson(message, CPlayer_LocationInfo.class);
        Player toPlayer = PlayerMgr.getInstance().getPlayer(req.pid);
        if (null == toPlayer) {
            request.error(ErrorCode.NotAllow, "req.pid " + req.pid);
        } else {
            request.response(SPlayer_LocationInfo.make(toPlayer.getLocationInfo()));
        }

    }

}
