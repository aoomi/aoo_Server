package core.network.client2game.handler.game;

import business.player.Player;
import business.player.feature.PlayerSign;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CPlayer_Sign;

import java.io.IOException;

public class CPlayerSign extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {

        final CPlayer_Sign req = new Gson().fromJson(message, CPlayer_Sign.class);
        if (null == req) {
            request.error(ErrorCode.NotAllow, "CPlayer_Sign not null");
            return;
        }
        if (req.type == PlayerSign.Query_Int) {
            player.getFeature(PlayerSign.class).querySignRecord(request);
        } else if (req.type == PlayerSign.Reward_Int) {
            player.getFeature(PlayerSign.class).getSignReward(request);
        } else {
            request.response();
        }
    }

}
