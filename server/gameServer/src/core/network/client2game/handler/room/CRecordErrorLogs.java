package core.network.client2game.handler.room;

import BaseCommon.CommLog;
import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.room.C_RecordErrorLogs;

import java.io.IOException;
import java.util.Objects;

public class CRecordErrorLogs extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final C_RecordErrorLogs req = new Gson().fromJson(message, C_RecordErrorLogs.class);
        long roomID = req.roomID;
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "C_RecordErrorLogs not find room:" + roomID);
            return;
        }
        if (null == room.getCurSet()) {
            request.error(ErrorCode.NotAllow, "");
            return;
        }
        if (Objects.isNull(req.context)) {
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("客户出现异常日志");
        stringBuilder.append("     gameName：" + room.getBaseRoomConfigure().getGameType().getName());
        stringBuilder.append("     roomID：" + room.getRoomID());
        stringBuilder.append("     setID：" + room.getCurSetID() );
        stringBuilder.append("     pid：" + player.getPid());
        stringBuilder.append("     context： " + req.context);
        CommLog.error(String.valueOf(stringBuilder));
        request.response();
    }
}
