package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_RoomConfigPrizePoolItem;

import java.io.IOException;

/**
 * 赛事经营列表
 */
public class CUnionRoomConfigPrizePoolList extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().getUnionRoomConfigPrizePoolList(new Gson().fromJson(message, CUnion_RoomConfigPrizePoolItem.class), player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
