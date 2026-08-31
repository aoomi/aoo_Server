package core.network.client2game.handler.union;


import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_SkinInfo;

import java.io.IOException;

/**
 * 赛事显示信息修改
 *
 * @author Huaxing
 */
public class CUnionChangeSkinShowInfo extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CUnion_SkinInfo req = new Gson().fromJson(message, CUnion_SkinInfo.class);
        SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().changeUnionSkinShowInfo(req, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
