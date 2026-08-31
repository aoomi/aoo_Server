package core.network.client2game.handler.union;

import business.global.config.DiscountMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
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

public class CUnionGameDiscount extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CBase_GameDiscount req = new Gson().fromJson(message, CBase_GameDiscount.class);
        // 获取亲友圈信息。
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (null == union) {
            request.error(ErrorCode.NotAllow, "null == union unionId:{}", req.getUnionId());
            return;
        }
        request.response(new GameItem(req.getGameId(), DiscountMgr.getInstance().getValue(union.getOwnerPlayer().getFeature(PlayerFamily.class).getFamilyIdList(), 0L, union.getUnionBO().getUnionSign(), req.getGameId())));
    }
}
