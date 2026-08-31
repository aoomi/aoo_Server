package core.network.client2game.handler.xcpdk;

import business.global.pk.xcpdk.XCPDKRoom;
import business.global.pk.xcpdk.XCPDKRoomSet;
import business.global.room.RoomMgr;
import business.xcpdk.c2s.iclass.CXCPDK_FaPaiJieShu;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

public class CXCPDKFaPaiJieShu extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CXCPDK_FaPaiJieShu clientPack = new Gson().fromJson(message, CXCPDK_FaPaiJieShu.class);

        XCPDKRoom room = (XCPDKRoom) RoomMgr.getInstance().getRoom(clientPack.roomID);
        if (null == room){
            request.error(ErrorCode.NotAllow, "CXCPDKOpenCard not find room:"+clientPack.roomID);
            return;
        }
        XCPDKRoomSet set =  (XCPDKRoomSet) room.getCurSet();
        if(null == set){
            request.error(ErrorCode.NotAllow, "CXCPDKOpenCard not set room:"+clientPack.roomID);
            return;
        }
        set.faPaiJieShu(request,  clientPack.pos);
        request.response(ErrorCode.Success);
    }
}
