package core.network.client2game.handler.task;

import business.player.Player;
import business.player.feature.PlayerTask;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/**
 * 任务列表
 *
 * @author Huaxing
 */
public class CTaksTaskList extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws IOException {
        // 获取任务列表
        player.getFeature(PlayerTask.class).taskList(request);
    }
}
