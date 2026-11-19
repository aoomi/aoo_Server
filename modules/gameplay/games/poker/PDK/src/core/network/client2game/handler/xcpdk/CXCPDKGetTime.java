package core.network.client2game.handler.xcpdk;

import business.global.pk.xcpdk.XCPDKRoom;
import business.global.pk.xcpdk.XCPDKRoomSet;
import business.global.room.RoomMgr;
import business.xcpdk.c2s.iclass.CXCPDK_AddDouble;
import business.xcpdk.c2s.iclass.SXCPDK_GetTime;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/**
 * @author zhujianming
 * @date 2022-04-22 10:09
 */
public class CXCPDKGetTime extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CXCPDK_AddDouble clientPack = new Gson().fromJson(message, CXCPDK_AddDouble.class);

        XCPDKRoom room = (XCPDKRoom) RoomMgr.getInstance().getRoom(clientPack.roomID);
        if (null == room){
            request.error(ErrorCode.NotAllow, "CXCPDKGetTime not find room:"+clientPack.roomID);
            return;
        }
        XCPDKRoomSet set =  (XCPDKRoomSet) room.getCurSet();
        if(null == set){
            request.error(ErrorCode.NotAllow, "CXCPDKGetTime not set room:"+clientPack.roomID);
            return;
        }

        SXCPDK_GetTime make = SXCPDK_GetTime.make(room.getRoomID(), player.getPid(), clientPack.pos);
        make.secTotal = set.getTime1(clientPack.pos);
        request.response(make);
    }
}
