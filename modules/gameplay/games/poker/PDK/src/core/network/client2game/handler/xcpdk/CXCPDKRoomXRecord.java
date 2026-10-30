package core.network.client2game.handler.xcpdk;

import business.global.pk.xcpdk.XCPDKRoom;
import business.global.room.RoomMgr;
import business.global.room.base.AbsRoomPos;
import business.xcpdk.c2s.cclass.XCPDKRoom_RecordPosInfo;
import business.xcpdk.c2s.iclass.SXCPDK_SetInfo;
import business.xcpdk.c2s.iclass.SXCPDK_UserInfo;
import business.xcpdk.c2s.iclass.SXCPDK_XResult;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.room.CBase_GetRoomInfo;

import java.io.IOException;
import java.util.List;

public class CXCPDKRoomXRecord extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CBase_GetRoomInfo req = new Gson().fromJson(message, CBase_GetRoomInfo.class);
        long roomID = req.getRoomID();

        XCPDKRoom room = (XCPDKRoom) RoomMgr.getInstance().getRoom(roomID);
        if (null == room){
            request.error(ErrorCode.NotAllow, "CHBMJRoomEndResult not find room:"+roomID);
            return;
        }
        SXCPDK_XResult result = new SXCPDK_XResult();
        for(AbsRoomPos roomPos: room.getRoomPosMgr().getPosList()){
            result.getUserInfo().put(roomPos.getPosID(),new SXCPDK_UserInfo(roomPos.getName(),roomPos.getPid(),roomPos.getPoint()));
            if(roomPos.getResults()!=null){
                List<Integer> pointList = ((XCPDKRoom_RecordPosInfo) roomPos.getResults()).getPointList();
                for(int i =0;i<pointList.size();i++){
                    if(result.getSetInfo().size()<i+1){
                        result.getSetInfo().add(new SXCPDK_SetInfo(i+1));
                    }
                    SXCPDK_SetInfo setInfo = result.getSetInfo().get(i);
                    setInfo.getPoint().put(roomPos.getPosID(),pointList.get(i));
                }
            }
        }
        request.response(result);
    }
}
