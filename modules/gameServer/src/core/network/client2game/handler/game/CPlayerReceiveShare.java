package core.network.client2game.handler.game;

import business.player.Player;
import business.player.feature.achievement.AchievementFeature;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/**
 * 玩家推广分享
 *
 * @author Huaxing
 */
public class CPlayerReceiveShare extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        player.getFeature(AchievementFeature.class).receiveShare(request);
    }


}
