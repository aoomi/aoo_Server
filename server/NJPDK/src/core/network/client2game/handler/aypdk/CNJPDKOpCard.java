package core.network.client2game.handler.njpdk;

import business.global.pk.njpdk.NJPDKRoom;
import business.global.room.RoomMgr;
import business.njpdk.c2s.iclass.CNJPDK_OpCard;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/**
 * 安岳跑的快打牌
 */
public class CNJPDKOpCard extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CNJPDK_OpCard clientPack = new Gson().fromJson(message, CNJPDK_OpCard.class);

        NJPDKRoom room = (NJPDKRoom) RoomMgr.getInstance().getRoom(clientPack.roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CNJPDKOpCard not find room:" + clientPack.roomID);
            return;
        }
        room.onOpCard(request, clientPack);
    }
}
