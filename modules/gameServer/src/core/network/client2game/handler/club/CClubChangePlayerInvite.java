package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_FindPIDAdd;

import java.io.IOException;

/**
 * 修改玩家邀请状态信息
 *
 * @author zaf
 */
public class CClubChangePlayerInvite extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_FindPIDAdd req = new Gson().fromJson(message, CClub_FindPIDAdd.class);
        SData_Result result = ClubMgr.getInstance().getClubListMgr().changePlayerInvite(player.getPid(), req.getType());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
        } else {
            request.error(result.getCode(), result.getMsg());
        }

    }

}
