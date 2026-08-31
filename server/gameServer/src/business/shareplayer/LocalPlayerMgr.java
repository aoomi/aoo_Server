package business.shareplayer;

import com.google.common.collect.Maps;

import java.util.Map;

public class LocalPlayerMgr {
    //全部玩家
    private Map<Long, SharePlayer> localAllPlayerMap = Maps.newHashMap();
    //在线玩家
    private Map<Long, SharePlayer> localOnlinePlayerMap = Maps.newHashMap();

    public Map<Long, SharePlayer> getLocalAllPlayerMap() {
        return localAllPlayerMap;
    }

    public Map<Long, SharePlayer> getLocalOnlinePlayerMap() {
        return localOnlinePlayerMap;
    }

    private LocalPlayerMgr() {
    }

    public static LocalPlayerMgr getInstance() {
        return LocalPlayerMgr.SingletonHolder.instance;
    }

    /**
     * 初始化
     *
     * @param sharePlayerMap
     */
    public void initPlayer(Map<Long, SharePlayer> sharePlayerMap) {
        sharePlayerMap.values().stream().forEach(k -> addAllPlayer(k));
    }

    public void initOnlinePlayer(Map<Long, SharePlayer> sharePlayerMap) {
        sharePlayerMap.values().stream().forEach(k -> addOnlinePlayer(k));
    }

    /**
     * 添加所有玩家
     *
     * @param sharePlayer
     */
    public void addAllPlayer(SharePlayer sharePlayer) {
        SharePlayer sharePlayerOld = this.getLocalAllPlayerMap().get(sharePlayer.getPlayerBO().getId());
        if (sharePlayerOld != null) {
            if (sharePlayerOld.getUpdateTime() < sharePlayer.getUpdateTime()) {
                this.getLocalAllPlayerMap().put(sharePlayer.getPlayerBO().getId(), sharePlayer);
            }
        } else {
            this.getLocalAllPlayerMap().put(sharePlayer.getPlayerBO().getId(), sharePlayer);
        }
    }

    /**
     * 添加在线玩家
     *
     * @param sharePlayer
     */
    public void addOnlinePlayer(SharePlayer sharePlayer) {
        SharePlayer sharePlayerOld = this.getLocalOnlinePlayerMap().get(sharePlayer.getPlayerBO().getId());
        if (sharePlayerOld != null) {
            if (sharePlayerOld.getUpdateTime() < sharePlayer.getUpdateTime()) {
                this.getLocalOnlinePlayerMap().put(sharePlayer.getPlayerBO().getId(), sharePlayer);
            }
        } else {
            this.getLocalOnlinePlayerMap().put(sharePlayer.getPlayerBO().getId(), sharePlayer);
        }
    }


    /**
     * 删除在线玩家
     *
     * @param pid
     */
    public void removeOnlinePlayer(long pid) {
        this.getLocalOnlinePlayerMap().remove(pid);
    }

    /**
     * 获取所有在线玩家
     *
     * @return
     */
    public Map<Long, SharePlayer> onlineSharePlayers() {
        return this.getLocalOnlinePlayerMap();
    }

    /**
     * 获取所有玩家
     *
     * @return
     */
    public Map<Long, SharePlayer> allSharePlayers() {
        return this.getLocalAllPlayerMap();
    }

    /**
     * 玩家是否在线
     *
     * @param pid
     * @return
     */
    public boolean checkSharePlayerByOnline(Long pid) {
        return this.getLocalOnlinePlayerMap().containsKey(pid);
    }


    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static LocalPlayerMgr instance = new LocalPlayerMgr();
    }
}
