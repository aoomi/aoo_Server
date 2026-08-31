package business.global.shareroom;

import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;

public class LocalRoomMgr {

    private Map<String, ShareRoom> localOneRoomMap = Maps.newHashMap();
    //已经删除的房间好
    private Map<String, Long> localRemoveRoomKey = Maps.newHashMap();

    public Map<String, ShareRoom> getLocalOneRoomMap() {
        return localOneRoomMap;
    }

    private LocalRoomMgr() {
    }

    public static LocalRoomMgr getInstance() {
        return LocalRoomMgr.SingletonHolder.instance;
    }

    /**
     * 初始化数据
     *
     * @param shareRoomMap
     */
    public void init(Map<String, ShareRoom> shareRoomMap) {
//        Map<String, ShareRoom> shareRooms = shareRoomMap.values().stream().filter(k-> ShareRoomMgr.getInstance().checkCurNode(k.getCurShareNode()) && !k.isNoneRoom()).collect(HashMap::new, (m, e) -> m.put(e.getRoomKey(), e), HashMap::putAll);
        shareRoomMap.values().stream().forEach(k -> addShareRoom(k));
    }

    /**
     * 添加房间本地缓存
     *
     * @param shareRoom
     */
    public void addShareRoom(ShareRoom shareRoom) {
        ShareRoom shareRoomOld = this.getLocalOneRoomMap().get(shareRoom.getRoomKey());
        if (shareRoomOld != null) {
            if (shareRoomOld.getUpdateTime() < shareRoom.getUpdateTime()) {
                this.getLocalOneRoomMap().put(shareRoom.getRoomKey(), shareRoom);
            }
        } else {
            Long delTime = localRemoveRoomKey.get(shareRoom.getRoomKey());
            if (delTime == null || System.currentTimeMillis() - delTime > 1000) {
                this.getLocalOneRoomMap().put(shareRoom.getRoomKey(), shareRoom);
            }
        }

    }

    /**
     * 移除房间
     *
     * @param roomKey
     */
    public void removeShareRoom(String roomKey) {
        localRemoveRoomKey.put(roomKey, System.currentTimeMillis());
        this.getLocalOneRoomMap().remove(roomKey);
    }

    /**
     * 亲友圈房间
     *
     * @param clubId
     * @return
     */
    public Map<String, ShareRoom> allShareRoomsByClubId(Long clubId) {
        Map<String, ShareRoom> shareRooms = this.getLocalOneRoomMap().values().stream().filter(k -> k.getClubId() == clubId).collect(HashMap::new, (m, e) -> m.put(e.getRoomKey(), e), HashMap::putAll);
        return shareRooms;
    }

    /**
     * 赛事房间
     *
     * @param unionId
     * @return
     */
    public Map<String, ShareRoom> allShareRoomsByUnionId(Long unionId) {
        Map<String, ShareRoom> shareRooms = this.getLocalOneRoomMap().values().stream().filter(k -> k.getUnionId() == unionId).collect(HashMap::new, (m, e) -> m.put(e.getRoomKey(), e), HashMap::putAll);
        return shareRooms;
    }


    /**
     * 获取所有房间
     *
     * @return
     */
    public Map<String, ShareRoom> allShareRooms() {
        return this.getLocalOneRoomMap();
    }


    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static LocalRoomMgr instance = new LocalRoomMgr();
    }
}
