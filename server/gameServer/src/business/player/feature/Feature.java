/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package business.player.feature;

import BaseThread.BaseMutexObject;
import business.player.Player;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommTime;

import java.io.Serializable;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public abstract class Feature implements Serializable {

    protected final Player player;
    private volatile boolean _loaded = false;
    private volatile int lastActiveTime = 0; // 上次激活时间

    final protected BaseMutexObject m_mutex = new BaseMutexObject();

    public Player getPlayer() {
        return player;
    }

    public String getPlayerName() {
        return player.getName();
    }

    public long getPid() {
        return player.getPid();
    }

    public long getRoomID() {
        //共享获取玩家房间ID
        if (Config.isShare()) {
            SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(this.player.getPid());
            if (sharePlayer.getRoomInfo() != null) {
                return sharePlayer.getRoomInfo().getRoomId();
            } else {
                return 0L;
            }
        } else {
            return this.player.getRoomInfo().getRoomId();
        }
    }

    public void setRoomID(long roomID) {
        this.player.getRoomInfo().setRoomId(roomID);
        //更新共享玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this.player, "roomInfo");
        }
    }

    protected void lock() {
        m_mutex.lock();
    }

    protected void unlock() {
        m_mutex.unlock();
    }

    public Feature(Player player) {
        this.player = player;
    }

    public int getLastActiveTime() {
        return lastActiveTime;
    }

    public void updateLastActiveTime() {
        lastActiveTime = CommTime.RecentSec;
    }

    /**
     * 加载数据，初始化逻辑
     */
    public Feature tryLoadDBData() {
        lastActiveTime = CommTime.nowSecond();

        if (_loaded) {
            return this;
        }

        synchronized (this) {
            if (_loaded) {
                return this;
            }

            try {
                loadDB();
            } catch (Exception e) {
                CommLogD.error("Feature.tryLoadDBData", e);
            }
            _loaded = true;
        }
        return this;
    }

    /**
     * 异步载入数据
     */
    public abstract void loadDB();
}
