package core.network.client2game.handler.scjymj;

import business.player.Player;
import business.player.feature.PlayerRecord;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_GetPlayBackCode;

import java.io.IOException;

/**
 * 根据房间id和局数查询回放码
 *
 * @author Administrator
 */
public class CSCJYMJGetPlayBackCode extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CBase_GetPlayBackCode req = new Gson().fromJson(message, CBase_GetPlayBackCode.class);
        SData_Result result = player.getFeature(PlayerRecord.class).getRoomPlayBackCode(req.getRoomId(), req.getTabId());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
