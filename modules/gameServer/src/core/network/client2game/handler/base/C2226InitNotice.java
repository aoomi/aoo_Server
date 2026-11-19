package core.network.client2game.handler.base;

import business.player.Player;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

public class C2226InitNotice extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {

//    	request.response(NoticeMgr.getInstance().getNoticeInfoList());
    }
}