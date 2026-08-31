package core.network.client2game.handler.family;

import business.player.Player;
import business.player.feature.PlayerFamily;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.family.CFamily_AgentGiveRoomCard;

import java.io.IOException;

/**
 * 代理赠送房卡
 *
 * @author Administrator
 */
public class CAgentGiveRoomCard extends PlayerHandler {
    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CFamily_AgentGiveRoomCard req = new Gson().fromJson(message, CFamily_AgentGiveRoomCard.class);
        SData_Result result = player.getFeature(PlayerFamily.class).agentGiveRoomCard(req.toPid, req.value);
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
