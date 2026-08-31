package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_RoomCfgSave;

import java.io.IOException;

/**
 * 房间玩法勾选保存
 *
 * @author zaf
 */
public class CUnionRoomCfgSave extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_RoomCfgSave req = new Gson().fromJson(message, CUnion_RoomCfgSave.class);
        SData_Result result = UnionMgr.getInstance().getUnionListMgr().saveUnionRoomCfgList(req.getUnionId(), req.getClubId(), player.getPid(), req.getUnionGameList());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
