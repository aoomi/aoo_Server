package core.network.client2game.handler.game;

import business.player.Player;
import business.player.feature.achievement.AchievementFeature;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CPlayer_Page;

import java.io.IOException;

/**
 * 玩家推广列表
 *
 * @author Huaxing
 */
public class CPlayerReceiveList extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CPlayer_Page page = new Gson().fromJson(message, CPlayer_Page.class);
        player.getFeature(AchievementFeature.class).getRefererReceiveList(request, page.pageNum);
    }


}
