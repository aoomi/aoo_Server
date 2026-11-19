package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_GetMemberManageV2;

import java.io.IOException;

public class CClubGetMemberManageV2 extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        CClub_GetMemberManageV2 command = new Gson().fromJson(message, CClub_GetMemberManageV2.class);
        SData_Result<?> result = ClubMgr.getInstance().getClubMemberMgr().getMemberManageCursor(
                command.clubId, player.getPid(), command.afterMemberId, command.limit, command.pageType);
        if (ErrorCode.Success.equals(result.getCode())) request.response(result.getData());
        else request.error(result.getCode(), result.getMsg());
    }
}
