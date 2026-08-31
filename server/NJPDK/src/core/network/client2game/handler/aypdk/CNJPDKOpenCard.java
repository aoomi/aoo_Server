package core.network.client2game.handler.njpdk;

import business.global.pk.njpdk.NJPDKRoom;
import business.global.pk.njpdk.NJPDKRoomSet;
import business.global.room.RoomMgr;
import business.njpdk.c2s.iclass.CNJPDK_OpenCard;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/*
 * 明牌
 * */
public class CNJPDKOpenCard extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CNJPDK_OpenCard clientPack = new Gson().fromJson(message, CNJPDK_OpenCard.class);

        NJPDKRoom room = (NJPDKRoom) RoomMgr.getInstance().getRoom(clientPack.roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CNJPDK_OpenCard not find room:" + clientPack.roomID);
            return;
        }
        NJPDKRoomSet set = (NJPDKRoomSet) room.getCurSet();
        if (null == set) {
            request.error(ErrorCode.NotAllow, "CNJPDK_OpenCard not set room:" + clientPack.roomID);
            return;
        }
        set.onOpenCard(request, clientPack, player.getPid());
    }
}
