package core.network.client2game.handler.base;

import business.player.Player;
import business.player.feature.PlayerBase;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.ClientSession;
import core.network.client2game.handler.BaseHandler;

import java.io.IOException;

public class C1005PlayerInfo extends BaseHandler {

    @Override
    public void handle(WebSocketRequest request, String message) throws IOException {
        ClientSession session = (ClientSession) request.getSession();
        Player player = session.getPlayer();
        if (player != null) {
            request.response(player.getFeature(PlayerBase.class).fullInfo(true));
            return;
        }
        request.response();// 无账号情况下返回为空
    }
}
