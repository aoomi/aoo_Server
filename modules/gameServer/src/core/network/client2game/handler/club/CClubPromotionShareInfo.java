package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_PromotionShareChange;

import java.io.IOException;

/**
 * 查询推广员身上的分成信息
 */
public class CClubPromotionShareInfo extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        CClub_PromotionShareChange cClub_promotionShareChange = new Gson().fromJson(message, CClub_PromotionShareChange.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getPromotionListShareInfo(cClub_promotionShareChange, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
