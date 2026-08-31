package core.network.client2game.handler.room;

import business.player.Player;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.config.server.GameTypeMgr;
import core.network.client2game.ClientAcceptor;
import core.network.client2game.handler.BaseHandler;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.iclass.CGameType;

import java.io.IOException;
import java.util.Objects;

public class CBaseUnionRoom extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CGameType clientPack = new Gson().fromJson(message, CGameType.class);
        GameType gameType = GameTypeMgr.getInstance().gameType(clientPack.getGameType());
        if (Objects.nonNull(gameType)) {
            String gameTypeName = gameType.getName().toLowerCase();
            BaseHandler handler = (BaseHandler) ClientAcceptor.getInstance().getHandle(gameTypeName + ".c" + gameTypeName + "unionroom");
            handler.handle(request, message);
            return;
        }
        request.response();
    }

}
