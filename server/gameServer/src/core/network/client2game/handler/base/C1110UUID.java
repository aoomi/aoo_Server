package core.network.client2game.handler.base;

import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.C1110_UUID;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * 获取玩家UUID
 *
 * @author Administrator
 */
public class C1110UUID extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final C1110_UUID req = new Gson().fromJson(message, C1110_UUID.class);
        String gameName = req.gameName;
        if (StringUtils.isEmpty(gameName)) {
            // 游戏类型为空
            request.error(ErrorCode.NotAllow, "gameName error gameName:{%s}", gameName);
            return;
        }
        request.response(player.getuUID(gameName));
        player.uuidDisconnect();
    }
}
