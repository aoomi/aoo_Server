package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_MemberExamineList;

import java.io.IOException;

/**
 * 获取赛事成员审核列表
 *
 * @author zaf
 */
public class CUnionMemberExamineList extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_MemberExamineList req = new Gson().fromJson(message, CUnion_MemberExamineList.class);
        SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().getUnionMemberExamineList(req.getUnionId(), req.getClubId(), player.getPid(), req.getPageNum(), req.getType());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
