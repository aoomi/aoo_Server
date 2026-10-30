package core.network.client2game.handler.player;

import business.player.Player;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

public class CPlayerChanged extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        request.response();
        player.pushProperties();
    }

}
