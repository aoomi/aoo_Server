package core.network.client2game.handler.player;

import business.player.Player;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CPlayer_AppOs;

import java.io.IOException;

public class CPlayerAppOs extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CPlayer_AppOs req = new Gson().fromJson(message, CPlayer_AppOs.class);
        player.getPlayerBO().saveOs(req.type);
        request.response();
    }

}
