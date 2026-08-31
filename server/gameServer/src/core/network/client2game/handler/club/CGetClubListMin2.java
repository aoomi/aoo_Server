package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;

import java.io.IOException;

/**
 * 获取俱乐部列表
 * 简化版 2021/11/1傅哥要求
 *
 * @authot zaf
 */

public class CGetClubListMin2 extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        SData_Result result = ClubMgr.getInstance().onGetClubListByPlayerShort2(player);
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
