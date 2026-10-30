package core.network.client2game.handler.room;

import business.global.config.GameListConfigMgr;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.room.CBase_GameTypeUrl;

import java.io.IOException;

/**
 * 获取一个游戏配置连接地址
 */
public class CBaseGameTypeUrl extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CBase_GameTypeUrl req = new Gson().fromJson(message, CBase_GameTypeUrl.class);
        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(player.getPid());
        //获取玩家当前的roomId
        long roomID = sharePlayer.getRoomInfo().getRoomId();
        if (roomID > 0) {
            ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByRoomId(roomID);
            if (shareRoom == null) {
                request.error(ErrorCode.Room_NOT_Find, "Room_NOT_Find");
            } else {
                request.response(GameListConfigMgr.getInstance().getByRoom(shareRoom));
            }
        } else {
            request.response(GameListConfigMgr.getInstance().getByGameType(req.getGametype()));
        }
    }
}