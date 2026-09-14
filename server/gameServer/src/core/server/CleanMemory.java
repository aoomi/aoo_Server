package core.server;

import BaseServer._ACleanMemory;
import business.player.PlayerMgr;
import com.ddm.server.common.utils.CommTime;

public class CleanMemory extends _ACleanMemory {

    private static CleanMemory instance = new CleanMemory();

    public static CleanMemory GetInstance() {
        return instance;
    }

    private int featureGCTime = CommTime.DaySec; // 先卸载1天内未激活

    public int getFeatureGCTime() {
        return featureGCTime;
    }

    public void setFeatureGCTime(int featureGCTime) {
        this.featureGCTime = featureGCTime;
    }

    @Override
    public void run() {
        // 更新最近的时间
        CommTime.RecentSec = CommTime.nowSecond();

        // 卸载玩家
        PlayerMgr.getInstance().releasPlayer(featureGCTime);
    }

}
