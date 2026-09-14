package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.union.UnionRoomCfgItem;
import jsproto.c2s.iclass.union.CUnion_RoomCfgUpdate;

import java.io.IOException;

/**
 * 房间玩法状态
 *
 * @author zaf
 */
public class CUnionRoomCfgUpdate extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_RoomCfgUpdate req = new Gson().fromJson(message, CUnion_RoomCfgUpdate.class);
        SData_Result result = UnionMgr.getInstance().getUnionListMgr().updateUnionRoomCfgStopAndUse(req.getUnionId(), req.getClubId(), player.getPid(), req.getUnionRoomCfgId(), req.getStatus());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(((UnionRoomCfgItem) result.getData()).getStatus());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
