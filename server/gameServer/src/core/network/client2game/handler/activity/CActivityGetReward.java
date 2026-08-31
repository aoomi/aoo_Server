package core.network.client2game.handler.activity;

import business.global.redBagActivity.Activity;
import business.global.redBagActivity.ActivityManager;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.redactivity.CRedActivity_GetReward;

import java.io.IOException;

/**
 * 改变俱乐部玩家状态
 *
 * @author zaf
 */
public class CActivityGetReward extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CRedActivity_GetReward req = new Gson().fromJson(message, CRedActivity_GetReward.class);


        Activity activity = ActivityManager.getInstance().getActivity(req.activityID);
        if (null == activity) {
            request.error(ErrorCode.NotAllow, "CActivityGetReward not find Activity  activityID = " + req.activityID);
            return;
        }

        activity.getReward(request, player.getPid(), req.getReward);
    }

}
