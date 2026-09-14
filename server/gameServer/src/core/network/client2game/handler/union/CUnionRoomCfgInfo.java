package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_RoomCfgInfo;

import java.io.IOException;

/**
 * 获取指定房间配置信息
 *
 * @author zaf
 */
public class CUnionRoomCfgInfo extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_RoomCfgInfo req = new Gson().fromJson(message, CUnion_RoomCfgInfo.class);
        SData_Result result = UnionMgr.getInstance().getUnionListMgr().getUnionRoomCfgInfo(req.getUnionId(), req.getUnionRoomCfgId());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
