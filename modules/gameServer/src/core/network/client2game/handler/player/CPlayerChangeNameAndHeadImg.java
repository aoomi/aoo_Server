package core.network.client2game.handler.player;

import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CPlayer_ChangeNameAndHeadImg;

import java.io.IOException;
import java.util.Objects;

public class CPlayerChangeNameAndHeadImg extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        request.response();
        CPlayer_ChangeNameAndHeadImg req = new Gson().fromJson(message, CPlayer_ChangeNameAndHeadImg.class);
        if (Objects.isNull(player)) {
            request.error(ErrorCode.NotAllow, "player is null");
        } else {
            player.updateHeadImageUrl(req.headImg, req.name);
            request.response(req);
        }
    }

}
