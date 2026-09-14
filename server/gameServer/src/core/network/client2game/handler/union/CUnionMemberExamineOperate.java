package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_MemberExamineOperate;

import java.io.IOException;

/**
 * 赛事成员审核操作
 *
 * @author zaf
 */
public class CUnionMemberExamineOperate extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_MemberExamineOperate req = new Gson().fromJson(message, CUnion_MemberExamineOperate.class);
        SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().getUnionMemberExamineOperate(req.getUnionId(), req.getClubId(), req.getOpPid(), req.getOpClubId(), player.getPid(), req.getType(), req.getOperate());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
