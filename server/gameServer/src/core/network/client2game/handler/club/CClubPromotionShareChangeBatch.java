package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.global.club.ClubPromotionLevetShareChangeBatchItem;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_PromotionCalcActiveBatch;

import java.io.IOException;

public class CClubPromotionShareChangeBatch extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        CClub_PromotionCalcActiveBatch batch = new Gson().fromJson(message, CClub_PromotionCalcActiveBatch.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().CClubPromotionLevelShareChangeBatch(batch, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
            ClubPromotionLevetShareChangeBatchItem clubPromotionLevetShareChangeBatchItem = ((ClubPromotionLevetShareChangeBatchItem) result.getData());
            clubPromotionLevetShareChangeBatchItem.getToClubMember().execClubPromotionCalcActiveBatch(batch.getPromotionCalcActiveItemList(), clubPromotionLevetShareChangeBatchItem);
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
