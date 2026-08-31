package core.network.client2game.handler.union;


import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_MemberList;

import java.io.IOException;

/**
 * 赛事显示信息修改
 *
 * @author Huaxing
 */
public class CUnionChangeZhongZhiShowStatus extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CUnion_MemberList req = new Gson().fromJson(message, CUnion_MemberList.class);
        SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().changeZhongZhiShowStatus(req, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
