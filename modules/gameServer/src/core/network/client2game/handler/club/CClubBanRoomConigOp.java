package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_BanRoomConfigOp;

import java.io.IOException;

/**
 * 操作赛事禁止玩法列表
 */
public class CClubBanRoomConigOp extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CUnion_BanRoomConfigOp req = new Gson().fromJson(message, CUnion_BanRoomConfigOp.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getClubBanRoomConfigOp(req, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
