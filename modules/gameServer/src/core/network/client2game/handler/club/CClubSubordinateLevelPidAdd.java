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

public class CClubSubordinateLevelPidAdd extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        CClub_FindPIDAdd req = new Gson().fromJson(message, CClub_FindPIDAdd.class);
        SData_Result result;
        if (req.getType() == 0) {
            result = ClubMgr.getInstance().getClubMemberMgr().getClubSubordinateLevelPidAdd(req, player.getPid());
        } else {
            result = ClubMgr.getInstance().getClubMemberMgr().getClubSubordinateLevelPidAddForUpLevel(req, player.getPid());
        }
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
