package core.network.client2game.handler.room;

import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.config.refdata.RefDataMgr;
import core.config.refdata.ref.RefPractice;
import core.config.server.GameTypeMgr;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.GoldItem;
import jsproto.c2s.iclass.CGameType;
import jsproto.c2s.iclass.room.SGold_GameList;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 练习场列表
 *
 * @author Administrator
 */
public class CBaseGoldList extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CGameType clientPack = new Gson().fromJson(message, CGameType.class);
        GameType gameType = GameTypeMgr.getInstance().gameType(clientPack.getGameType());
        if (Objects.nonNull(gameType)) {
            List<GoldItem> goldItem = RefDataMgr.getAll(RefPractice.class).values().stream()
                    .filter(k -> gameType.getName().toUpperCase().equals(k.getGameType().toUpperCase()))
                    .map(k -> new GoldItem((int) k.getId(), (int) (Math.random() * 9 + 1) + 100 + (int) (Math.random() * 100 + 1))).collect(Collectors.toList());
            request.response(SGold_GameList.make(goldItem));
            return;
        } else {
            request.error(ErrorCode.NotAllow, "error gametype");
        }


    }
}
