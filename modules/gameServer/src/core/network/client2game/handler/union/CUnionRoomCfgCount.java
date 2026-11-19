package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_RoomCfgList;

import java.io.IOException;

/**
 * 房间玩法统计
 *
 * @author zaf
 */
public class CUnionRoomCfgCount extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_RoomCfgList req = new Gson().fromJson(message, CUnion_RoomCfgList.class);
        SData_Result result = UnionMgr.getInstance().getUnionListMgr().getUnionRoomCfgCount(req.getUnionId(), req.getClassType());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
