package core.network.client2game.handler.game;

import business.player.Player;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/**
 * 公告列表
 *
 * @author liyan
 */
public class CSystemNotice extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {

        CommLogD.warn("retired game.CSystemNotice rejected; use authenticated /v1/notices and social.notice.changed");
        request.error(ErrorCode.NotAllow, "LEGACY_ENTRY_DISABLED");
    }

}
