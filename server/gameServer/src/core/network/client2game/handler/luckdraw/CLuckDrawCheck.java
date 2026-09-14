package core.network.client2game.handler.luckdraw;

import business.player.Player;
import business.player.feature.PlayerLuckDraw;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;

import java.io.IOException;

/**
 * 抽奖检查
 * 检查活动是否开启
 */
public class CLuckDrawCheck extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        SData_Result result = player.getFeature(PlayerLuckDraw.class).checkLuckDrawResult();
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
        } else {
            request.error(ErrorCode.Activity_Close, "Luck Draw Close");
        }
    }

}
