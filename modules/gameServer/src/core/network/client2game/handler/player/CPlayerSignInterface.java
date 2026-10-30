package core.network.client2game.handler.player;

import business.player.Player;
import cenum.VisitSignEnum;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CPlayer_SignInterface;

import java.io.IOException;

/**
 * 玩家所出界面标识
 */
public class CPlayerSignInterface extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CPlayer_SignInterface req = new Gson().fromJson(message, CPlayer_SignInterface.class);
        VisitSignEnum visitSignEnum = VisitSignEnum.valueOf(req.getSign());
        if (!VisitSignEnum.CLUN_ROOM_MAIN.equals(visitSignEnum)) {
            player.setSignEnum(visitSignEnum);
        }
        request.response();

    }
}
