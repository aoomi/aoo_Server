package core.network.client2game.handler.game;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import business.player.feature.PlayerRoom;
import cenum.room.RoomState;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.SGet_GameStatus;

import java.io.IOException;

/*
 * 请求玩家状态
 * */

public class CGetGameStatus extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {

        AbsBaseRoom baseRoom = RoomMgr.getInstance().getRoom(player.getFeature(PlayerRoom.class).getRoomID());
        boolean isPlayingGame = false;
        int gameType = -1;
        long roomID = 0;
        if (null != baseRoom && baseRoom.getRoomState() != RoomState.End) {
            isPlayingGame = true;
            gameType = baseRoom.getBaseRoomConfigure().getGameType().getId();
            roomID = baseRoom.getRoomID();
        }
        request.response(SGet_GameStatus.make(isPlayingGame, gameType, roomID));
    }

}
