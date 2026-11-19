package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_PostTypeUpdate;

import java.io.IOException;

/**
 * 更新职务
 */
public class CUnionPostTypeUpdate extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CUnion_PostTypeUpdate req = new Gson().fromJson(message, CUnion_PostTypeUpdate.class);
        SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execPostTypeUpdate(req, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getCustom());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
