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
 * 中至添加队长
 * 下属推广员任命
 */
public class CClubAddCaptionOpZhongZhi extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().clubSubordinateLevelChangeZhongZhi(new Gson().fromJson(message, CClub_FindPIDAdd.class), player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getCode());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
