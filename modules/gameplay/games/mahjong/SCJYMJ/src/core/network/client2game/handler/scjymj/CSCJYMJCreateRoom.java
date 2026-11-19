package core.network.client2game.handler.scjymj;

import business.player.Player;
import business.player.feature.PlayerRoom;
import business.scjymj.c2s.iclass.CSCJYMJ_CreateRoom;
import cenum.PrizeType;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import core.server.scjymj.SCJYMJAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.io.IOException;

/**
 * 济宁创建房间
 *
 * @author Huaxing
 */
public class CSCJYMJCreateRoom extends PlayerHandler {

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
        SData_Result resule = player.getFeature(PlayerRoom.class).createRoomAndConsumeCard(configure);
        if (ErrorCode.Success.equals(resule.getCode())) {
            request.response(resule.getData());
        } else {
            request.error(resule.getCode(), resule.getMsg());
        }

    }
}
