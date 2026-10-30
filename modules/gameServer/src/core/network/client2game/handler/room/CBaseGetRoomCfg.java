package core.network.client2game.handler.room;

import business.global.room.NormalRoomMgr;
import business.global.room.base.RoomImpl;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import com.ddm.server.common.Config;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.room.CBase_GetRoomCfg;

import java.io.IOException;
import java.util.Map;

/**
 * 获取房间配置信息
 *
 * @author Administrator
 */
public class CBaseGetRoomCfg extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CBase_GetRoomCfg req = new Gson().fromJson(message, CBase_GetRoomCfg.class);
        if (Config.isShare()) {
            ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(req.getRoomKey());
            if (shareRoom != null && null != shareRoom.getBaseRoomConfigure() && null != shareRoom.getBaseRoomConfigure().getGameType()) {
                request.response(new Gson().fromJson(shareRoom.getBaseRoomConfigure().getShareBaseCreateRoom(), Map.class));
            } else {
                request.error(ErrorCode.NotFind_Room, "NotFind_Room");
            }
        } else {
            RoomImpl roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(req.getRoomKey());
            if (roomImpl != null && null != roomImpl.getBaseRoomConfigure() && null != roomImpl.getBaseRoomConfigure().getGameType()) {
                request.response(roomImpl.getBaseRoomConfigure().getBaseCreateRoom());
            } else {
                request.error(ErrorCode.NotFind_Room, "NotFind_Room");
            }
        }
    }

}
