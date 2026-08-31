package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_PromotionCalcActiveBatch;

import java.io.IOException;

/**
 * 活跃异常
 */
@Deprecated
public class CClubPromotionCalcActiveBatch extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        CClub_PromotionCalcActiveBatch batch = new Gson().fromJson(message, CClub_PromotionCalcActiveBatch.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getClubPromotionCalcActiveBatch(batch, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
//            ((ClubMember) result.getData()).execClubPromotionCalcActiveBatch(batch.getPromotionCalcActiveItemList());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
