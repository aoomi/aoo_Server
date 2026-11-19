package core.network.client2game.handler.xcpdk;

import java.io.IOException;

import business.global.pk.xcpdk.XCPDKRoom;
import business.xcpdk.c2s.iclass.CXCPDK_BaDaDui;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import business.global.room.RoomMgr;
import business.player.Player;
import core.network.client2game.handler.PlayerHandler;

/**
 * 打牌
 *
 * @author Huaxing
 */
public class CXCPDKBaDaDui extends PlayerHandler {


    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CXCPDK_BaDaDui req = new Gson().fromJson(message, CXCPDK_BaDaDui.class);
        long roomID = req.roomID;


        XCPDKRoom room = (XCPDKRoom) RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CXCPDK_PiaoHua not find room:" + roomID);
            return;
        }

        room.opBaDaDui(request, player.getId(), req);
    }
}
