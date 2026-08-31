package core.network.client2game.handler.njpdk;

import business.njpdk.c2s.iclass.CNJPDK_CreateRoom;
import business.player.Player;
import business.player.feature.PlayerRoom;
import cenum.PrizeType;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import core.server.njpdk.NJPDKAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.io.IOException;

/**
 * 安岳跑的快创建房间
 */
public class CNJPDKCreateRoom extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        //安岳跑的快房间创建请求
        final CNJPDK_CreateRoom clientPack = new Gson().fromJson(message,
                CNJPDK_CreateRoom.class);
        // 公共房间配置
        BaseRoomConfigure<CNJPDK_CreateRoom> configure = new BaseRoomConfigure<CNJPDK_CreateRoom>(
                PrizeType.RoomCard,
                NJPDKAPP.GameType(),
                clientPack.clone());
        SData_Result resule = player.getFeature(PlayerRoom.class).createRoomAndConsumeCard(configure);
        if (ErrorCode.Success.equals(resule.getCode())) {
            request.response(resule.getData());
        } else {
            request.error(resule.getCode(), resule.getMsg());
        }
    }
}
