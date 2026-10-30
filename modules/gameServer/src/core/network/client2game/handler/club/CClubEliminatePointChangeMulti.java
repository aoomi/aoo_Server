package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_EliminatePointChange;

import java.io.IOException;

/**
 * 生存积分批量变换
 */
public class CClubEliminatePointChangeMulti extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        CUnion_EliminatePointChange sportsPointWarningChange = new Gson().fromJson(message, CUnion_EliminatePointChange.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().eliminatePointChangeMulti(sportsPointWarningChange, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
