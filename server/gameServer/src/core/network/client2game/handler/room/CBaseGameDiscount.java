package core.network.client2game.handler.room;

import business.global.config.DiscountMgr;
import business.player.Player;
import business.player.feature.PlayerFamily;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.GameItem;
import jsproto.c2s.iclass.room.CBase_GameDiscount;

import java.io.IOException;

public class CBaseGameDiscount extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CBase_GameDiscount req = new Gson().fromJson(message, CBase_GameDiscount.class);
        // 设置请求的城市ID
        request.response(new GameItem(req.getGameId(), DiscountMgr.getInstance().getValue(player.getFeature(PlayerFamily.class).getFamilyIdList(), 0L, 0L, req.getGameId())));
    }
}
