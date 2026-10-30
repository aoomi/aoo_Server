package core.network.client2game.handler.game;

import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CPlayer_RealAuthen;

import java.io.IOException;

public class CPlayerRealAuthen extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {

        final CPlayer_RealAuthen req = new Gson().fromJson(message, CPlayer_RealAuthen.class);
        String realName = req.realName;
        String realNumber = req.realNumber;
        if (null == realName || null == realNumber) {
            request.error(ErrorCode.ErrorSysMsg, "CPlayerRealAuthen_NotFindRoom");
            return;
        }
        player.setRealPlayer(realName, realNumber);

        request.response(CPlayer_RealAuthen.make(realName, realNumber));// 无账号情况下返回为空

    }

}
