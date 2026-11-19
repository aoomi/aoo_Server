package core.network.client2game.handler.game;

import business.global.GM.MaintainGameMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CMaintainGameNoticeRequest;

import java.io.IOException;

public class CMaintainGameIsGetNotice extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {

        final CMaintainGameNoticeRequest req = new Gson().fromJson(message, CMaintainGameNoticeRequest.class);
        if (null == req) {
            request.error(ErrorCode.NotAllow, "CMaintainGameNoticeRequest not null");
            return;
        }
        MaintainGameMgr.getInstance().notifyFinish(req.gameTypeId, player.getPid());
        request.response();
    }

}
