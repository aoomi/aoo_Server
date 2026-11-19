package business.global.room;

import BaseThread.BaseMutexManager;
import business.global.shareroom.ShareContinueRoomInfoMgr;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommTime;
import jsproto.c2s.cclass.room.ContinueRoomInfo;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContinueRoomInfoMgr {
    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static ContinueRoomInfoMgr instance = new ContinueRoomInfoMgr();
    }

    // 私有化构造方法
    private ContinueRoomInfoMgr() {
    }

    // key：房间key,value：房间信息
    private Map<Long, ContinueRoomInfo> key2rooms = new ConcurrentHashMap<>();

    // 获取单例
    public static ContinueRoomInfoMgr getInstance() {
        return SingletonHolder.instance;
    }

    private final BaseMutexManager _lock = new BaseMutexManager();

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    // 游戏包名
    private final static String ROOM = "business.global.%s.%s.%sRoom";


    public void putContinueRoomInfo(ContinueRoomInfo room) {
        if (room != null) {
            key2rooms.put(room.getRoomID(), room);
            if (Config.isShare()) {
                ShareContinueRoomInfoMgr.getInstance().addShareContinueRoom(room);
            }
        }
    }

    public ContinueRoomInfo getContinueRoomInfo(Long roomID) {
        if (roomID > 0L) {
            if (Config.isShare()) {
                return ShareContinueRoomInfoMgr.getInstance().getShareContinueRoom(roomID);
            } else {
                return key2rooms.get(roomID);
            }
        }
        return null;
    }

    public void continueRoomInfoOutTimeRemove() {
        Iterator<Map.Entry<Long, ContinueRoomInfo>> it = key2rooms.entrySet().iterator();
        while (it.hasNext()) {
            ContinueRoomInfo roomInfo = it.next().getValue();
            if (CommTime.nowSecond() - roomInfo.getRoomEndTime() > 600) {
                it.remove();
                key2rooms.remove(roomInfo.getRoomID());
                if (Config.isShare()) {
                    ShareContinueRoomInfoMgr.getInstance().removeShareContinueRoom(roomInfo.getRoomID());
                }
            }

        }
    }
}
