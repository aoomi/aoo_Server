package core.network.client2game.handler.union;

import business.global.club.ClubMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_ClubMemberList;

import java.io.IOException;

/**
 * 赛事管理员获取亲友圈成员列表
 */
public class CUnionClubMemberList extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CUnion_ClubMemberList req = new Gson().fromJson(message, CUnion_ClubMemberList.class);
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotUnionManage(player.getPid(), req.getClubId(), req.getUnionId())) {
            request.error(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
            return;
        }
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getUnionMemberManageList(req.getOpClubId(), req.getUnionId(), req.getPageNum(), req.getQuery(), req.getType(), req.getLosePoint());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
