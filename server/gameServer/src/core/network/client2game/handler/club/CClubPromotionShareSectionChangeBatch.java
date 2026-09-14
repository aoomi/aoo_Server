package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_PromotionSectionCalcActiveBatch;

import java.io.IOException;

public class CClubPromotionShareSectionChangeBatch extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        CClub_PromotionSectionCalcActiveBatch batch = new Gson().fromJson(message, CClub_PromotionSectionCalcActiveBatch.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().CClubPromotionLevelShareSectionChangeBatch(batch, batch.getOpPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
