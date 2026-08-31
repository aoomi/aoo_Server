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

/**
 * 根据预留值批量修改固定值
 */
public class CClubFixedShareChangeMulti extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        CClub_PromotionCalcActiveBatch batch = new Gson().fromJson(message, CClub_PromotionCalcActiveBatch.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().changeReservedValueChange(batch, player.getPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
//            request.response(result.getData());
            request.error(result.getCode(), result.getMsg());
            return;
        }
        result = ClubMgr.getInstance().getClubMemberMgr().CClubPromotionLevelShareChangeBatch(batch, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            ClubPromotionLevetShareChangeBatchItem clubPromotionLevetShareChangeBatchItem = ((ClubPromotionLevetShareChangeBatchItem) result.getData());
            result = clubPromotionLevetShareChangeBatchItem.getToClubMember().execClubPromotionCalcActiveBatchByReversedValue(batch.getPromotionCalcActiveItemList(), clubPromotionLevetShareChangeBatchItem, batch.getValue());
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
