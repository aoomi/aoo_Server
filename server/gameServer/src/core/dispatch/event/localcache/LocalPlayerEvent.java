package core.dispatch.event.localcache;

import business.shareplayer.LocalPlayerMgr;
import business.shareplayer.SharePlayer;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;

/**
 * 本地缓存玩家修改
 */
@Data
public class LocalPlayerEvent implements BaseExecutor {
    /**
     * 玩家数据
     */
    private Long pid;
    private SharePlayer sharePlayer;
    //1 增加普通，2增加在线，3删除在线
    private Integer type;

    public LocalPlayerEvent(Long pid, SharePlayer sharePlayer, Integer type) {
        this.pid = pid;
        this.sharePlayer = sharePlayer;
        this.type = type;
    }

    @Override
    public void invoke() {
        if (type == 1) {
            LocalPlayerMgr.getInstance().addAllPlayer(sharePlayer);
        } else if (type == 2) {
            LocalPlayerMgr.getInstance().addOnlinePlayer(sharePlayer);
        } else if (type == 3) {
            LocalPlayerMgr.getInstance().removeOnlinePlayer(pid);
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.LOCAL_PLAYER.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.LOCAL_PLAYER.bufferSize();
    }
}
