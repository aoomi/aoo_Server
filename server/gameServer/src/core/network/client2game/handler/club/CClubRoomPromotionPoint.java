package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_RoomPromotionPoint;

import java.io.IOException;

/**
 * 查看房间分成
 * 在玩家基础上
 */
public class CClubRoomPromotionPoint extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_RoomPromotionPoint req = new Gson().fromJson(message, CClub_RoomPromotionPoint.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getRoomPromotionPointList(req);
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
