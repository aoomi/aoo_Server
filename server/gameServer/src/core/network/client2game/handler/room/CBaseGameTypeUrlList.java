package core.network.client2game.handler.room;

import business.global.config.GameListConfigMgr;
import business.player.Player;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/**
 * 获取所有游戏配置连接地址
 */
public class CBaseGameTypeUrlList extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        request.response(GameListConfigMgr.getInstance().getAllList());
    }
}