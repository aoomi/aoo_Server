package core.network.client2game.handler.game;

import business.player.Player;
import business.player.feature.PlayerCurrency;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CPlayer_Exchange;
import jsproto.c2s.iclass.SPlayer_Exchange;

import java.io.IOException;

/**
 * 用户兑换
 *
 * @author Huaxing
 */
public class CPlayerExchange extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CPlayer_Exchange req = new Gson().fromJson(message, CPlayer_Exchange.class);
        int productID = req.productID;

        int DiamondNum = player.getFeature(PlayerCurrency.class).roomCardLeDou(productID);
        if (DiamondNum == 0) {
            request.error(ErrorCode.NotEnough_RoomCard, "CPlayerExchange");
        } else {
            request.response(SPlayer_Exchange.make(player.getPid(), DiamondNum));
        }
    }

}
