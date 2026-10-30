package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_RemoveMember;

import java.io.IOException;

/**
 * 赛事移除成员信息
 *
 * @author zaf
 */
public class CUnionRemoveMember extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_RemoveMember req = new Gson().fromJson(message, CUnion_RemoveMember.class);
        SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execUnionRemoveMember(req, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getCustom());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
