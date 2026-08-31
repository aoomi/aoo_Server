package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_GetMemberManage;

import java.io.IOException;

/**
 * 指定亲友圈成员列表
 *
 * @author zaf
 */
public class CClubGetMemberManage extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_GetMemberManage req = new Gson().fromJson(message, CClub_GetMemberManage.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getMemberManageList(req.clubId, player.getPid(), req.pageNum, req.query, req.getType(), req.getPageType(), req.getLosePoint());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
