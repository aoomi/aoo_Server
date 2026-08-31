package core.network.client2game.handler.game;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import business.player.feature.PlayerGoldRoom;
import business.shareplayer.SharePlayerMgr;
import cenum.VisitSignEnum;
import com.ddm.server.common.Config;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.C1101_GetRoomID;
import jsproto.c2s.iclass.S1101_GetRoomID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;

public class C1101GetRoomID extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
//		TestEnterUtil.enterRoom(request);
        final C1101_GetRoomID req = new Gson().fromJson(message, C1101_GetRoomID.class);

        if (Config.isShare()) {
            SharePlayerMgr.getInstance().getPlayer(player);
        }
        // 获取房间ID
        long roomID = player.getRoomInfo().getRoomId();
        // 练习场ID
        long practiceId = player.getFeature(PlayerGoldRoom.class).getPracticeId();
        // 游戏类型
        String gameType = "NOT";
        String subjectTopic = "";
        long roomKey = 0;
        if (roomID > 0L) {
            if (Config.isShare()) {
                ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByRoomId(roomID);
                if (null == shareRoom) {
                    roomID = 0L;
                    // 强制清空用户的游戏状态
                    player.onGMExitRoom();
                } else {
                    // 获取游戏类型
                    gameType = shareRoom.getBaseRoomConfigure().getGameType().getName().toUpperCase();
                    player.setSignEnum(VisitSignEnum.ROOM);
                    if (NumberUtils.isDigits(shareRoom.getRoomKey())) {
                        roomKey = Long.parseLong(shareRoom.getRoomKey());
                        if (StringUtils.isNotEmpty(req.getSign())) {
                            subjectTopic = shareRoom.getSubjectTopic();
                        }
                    }
                }
            } else {
                // 获取房间信息
                AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomID);
                if (null == room) {
                    roomID = 0L;
                    // 强制清空用户的游戏状态
                    player.onGMExitRoom();
                } else {
                    // 获取游戏类型
                    gameType = room.getBaseRoomConfigure().getGameType().getName().toUpperCase();
                    player.setSignEnum(VisitSignEnum.ROOM);
                    if (NumberUtils.isDigits(room.getRoomKey())) {
                        roomKey = Long.parseLong(room.getRoomKey());
                    }
                }
            }
        }
        request.response(S1101_GetRoomID.make(roomID, gameType, practiceId, roomKey), subjectTopic);// 无账号情况下返回为空
    }

}
