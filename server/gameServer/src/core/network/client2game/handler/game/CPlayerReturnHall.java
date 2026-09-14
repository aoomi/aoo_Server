package core.network.client2game.handler.game;

import business.player.Player;
import com.ddm.server.websocket.def.SubscribeEnum;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/**
 * 通知返回大厅
 */
public class CPlayerReturnHall extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        request.response("{}", SubscribeEnum.HALL.name());
        return;
    }
}
