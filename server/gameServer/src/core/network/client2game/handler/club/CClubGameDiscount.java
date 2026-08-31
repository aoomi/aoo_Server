package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.config.DiscountMgr;
import business.player.Player;
import business.player.feature.PlayerFamily;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.GameItem;
import jsproto.c2s.iclass.room.CBase_GameDiscount;

import java.io.IOException;

public class CClubGameDiscount extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CBase_GameDiscount req = new Gson().fromJson(message, CBase_GameDiscount.class);
        // 获取亲友圈信息。
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (null == club) {
            request.error(ErrorCode.NotAllow, "null == club clubId:{%d}", req.getClubId());
            return;
        }
        request.response(new GameItem(req.getGameId(), DiscountMgr.getInstance().getValue(club.getOwnerPlayer().getFeature(PlayerFamily.class).getFamilyIdList(), club.getClubListBO().getClubsign(), 0L, req.getGameId())));

    }
}
