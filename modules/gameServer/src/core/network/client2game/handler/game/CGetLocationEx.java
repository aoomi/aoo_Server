package core.network.client2game.handler.game;

import business.player.Player;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CGet_LocationEx;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/*
 * 请求玩家获取定位
 * */

public class CGetLocationEx extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CGet_LocationEx req = new Gson().fromJson(message, CGet_LocationEx.class);
        double latitude = req.Latitude;
        double longitude = req.Longitude;
        String address = req.Address;
        boolean isGetError = req.isGetError;
        if (StringUtils.isEmpty(address) || "null".equals(address)) {
            isGetError = true;
        }
        player.setLocationInfo(address, latitude, longitude, isGetError);
        request.response();

    }

}
