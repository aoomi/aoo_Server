package core.network.client2game.handler.game;

import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CPlayer_IpAddres;

import java.io.IOException;

/**
 * 用户IP 地址
 *
 * @author
 */
public class CPlayerIpAddress extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CPlayer_IpAddres req = new Gson().fromJson(message, CPlayer_IpAddres.class);
        if (!player.setIp(req.ip)) {
            request.error(ErrorCode.NotAllow, "ip not error");
            return;
        }
        request.response();
    }

}
