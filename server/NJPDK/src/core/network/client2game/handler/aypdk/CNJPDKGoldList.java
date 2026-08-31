package core.network.client2game.handler.njpdk;

import business.player.Player;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.config.refdata.RefDataMgr;
import core.config.refdata.ref.RefPractice;
import core.network.client2game.handler.PlayerHandler;
import core.server.njpdk.NJPDKAPP;
import jsproto.c2s.cclass.room.GoldItem;
import jsproto.c2s.iclass.room.SGold_GameList;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 练习场游戏列表
 *
 * @author Huaxing
 */
public class CNJPDKGoldList extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        List<GoldItem> goldItem = RefDataMgr.getAll(RefPractice.class).values().stream()
                .filter(k -> NJPDKAPP.GameType().getName().equals(k.getGameType().toUpperCase()))
                .map(k -> new GoldItem(k.getId(), (int) (Math.random() * 9 + 1) + 100 + (int) (Math.random() * 100 + 1))).collect(Collectors.toList());
        request.response(SGold_GameList.make(goldItem));
    }


}
