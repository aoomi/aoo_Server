package core.network.client2game.handler.scjymj;

import business.player.Player;
import business.player.feature.PlayerUnionRoom;
import business.scjymj.c2s.iclass.CSCJYMJ_CreateRoom;
import cenum.PrizeType;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.server.scjymj.SCJYMJAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.io.IOException;

/**
 * 赛事房间
 *
 * @author Administrator
 */
public class CSCJYMJUnionRoom extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws IOException {

        final CSCJYMJ_CreateRoom clientPack = new Gson().fromJson(message,
                CSCJYMJ_CreateRoom.class);

        // 公共房间配置
        BaseRoomConfigure<CSCJYMJ_CreateRoom> configure = new BaseRoomConfigure<CSCJYMJ_CreateRoom>(
                PrizeType.RoomCard,
                SCJYMJAPP.GameType(),
                clientPack.clone());
        player.getFeature(PlayerUnionRoom.class).createNoneUnionRoom(request, configure);
    }
}
