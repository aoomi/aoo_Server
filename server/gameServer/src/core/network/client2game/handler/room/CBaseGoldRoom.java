package core.network.client2game.handler.room;

import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.config.refdata.RefDataMgr;
import core.config.refdata.ref.RefPractice;
import core.config.server.GameTypeMgr;
import core.network.client2game.ClientAcceptor;
import core.network.client2game.handler.BaseHandler;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.iclass.room.CBase_GoldRoom;

import java.io.IOException;
import java.util.Objects;

/**
 * 练习场进入房间
 *
 * @author Administrator
 */
public class CBaseGoldRoom extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CBase_GoldRoom clientPack = new Gson().fromJson(message, CBase_GoldRoom.class);
        RefPractice data = RefDataMgr.get(RefPractice.class, clientPack.getPracticeId());
        if (Objects.isNull(data)) {
            request.error(ErrorCode.NotAllow, "CBaseGoldRoom do not find practiceId");
            return;
        }
        GameType gameType = GameTypeMgr.getInstance().gameType(data.getGameType());
        if (Objects.nonNull(gameType)) {
            String gameTypeName = gameType.getName().toLowerCase();
            BaseHandler handler = (BaseHandler) ClientAcceptor.getInstance().getHandle(gameTypeName + ".c" + gameTypeName + "goldroom");
            handler.handle(request, message);
            return;
        }
        request.response();
    }
}
