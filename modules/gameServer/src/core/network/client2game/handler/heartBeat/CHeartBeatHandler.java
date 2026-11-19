package core.network.client2game.handler.heartBeat;

import business.player.Player;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;


/**
 * 修改心跳，心跳不加锁
 */
public class CHeartBeatHandler extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        if (player.getLastTime() > 0L) {
            player.setLastTime(0L);
        }
        request.response();
    }
}
