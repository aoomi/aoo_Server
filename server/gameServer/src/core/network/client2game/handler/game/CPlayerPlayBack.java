package core.network.client2game.handler.game;

import business.player.Player;
import business.player.feature.PlayerRecord;
import business.global.replay.ReplayAuthorization;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.CPlayer_PlayBack;

import java.io.IOException;

/**
 * 玩家回放
 *
 * @author Huaxing
 */
public class CPlayerPlayBack extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CPlayer_PlayBack req = new Gson().fromJson(message, CPlayer_PlayBack.class);
        if (!ReplayAuthorization.mayRead(player.getPid(), req.getPlayBackCode())) {
            request.error(ErrorCode.NotAllow, "replay access denied");
            return;
        }
        SData_Result result = player.getFeature(PlayerRecord.class).playerPlayBack(req.getPlayBackCode(), req.isChekcPlayBackCode());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
