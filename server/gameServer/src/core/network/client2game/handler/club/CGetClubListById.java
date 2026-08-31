package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_GetClubInfo;

import java.io.IOException;

/**
 * 获取指定亲友圈信息
 *
 * @authot zaf
 * TODO 2022/06/01 该接口与club.CGetClubListByClubId功能一致客户端要求增加,处理时最好在onGetClubListByPlayerByClubId方法内统一处理
 */

public class CGetClubListById extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_GetClubInfo req = new Gson().fromJson(message, CClub_GetClubInfo.class);
        SData_Result result = ClubMgr.getInstance().onGetClubListByPlayerByClubId(player, req.getClubId());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
