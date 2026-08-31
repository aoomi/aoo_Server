package core.network.client2game.handler.njpdk;

import business.njpdk.c2s.iclass.CNJPDK_CreateRoom;
import business.player.Player;
import business.player.feature.PlayerClubRoom;
import cenum.PrizeType;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.server.njpdk.NJPDKAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.io.IOException;

/**
 * 亲友圈房间
 *
 * @author Administrator
 */
public class CNJPDKClubRoom extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws IOException {

        final CNJPDK_CreateRoom clientPack = new Gson().fromJson(message,
                CNJPDK_CreateRoom.class);
        // 公共房间配置
        BaseRoomConfigure<CNJPDK_CreateRoom> configure = new BaseRoomConfigure<CNJPDK_CreateRoom>(
                PrizeType.RoomCard,
                NJPDKAPP.GameType(),
                clientPack.clone());
        player.getFeature(PlayerClubRoom.class).createNoneClubRoom(request, configure);
    }
}
