package core.network.client2game.handler.playback;

import business.global.replay.LegacyPerspectiveReplayReader;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.playback.CPlayer_PerspectiveReplay;

import java.io.IOException;

public class CPlayerPerspectiveReplay extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        try {
            CPlayer_PerspectiveReplay command = new Gson().fromJson(message, CPlayer_PerspectiveReplay.class);
            request.response(LegacyPerspectiveReplayReader.getInstance().read(command.roomId,
                    command.setId, player.getPid(), command.afterSequence, command.limit));
        } catch (SecurityException denied) {
            request.error(ErrorCode.NotAllow, "replay access denied");
        } catch (IllegalArgumentException invalid) {
            request.error(ErrorCode.NotAllow, "invalid replay request");
        }
    }
}
