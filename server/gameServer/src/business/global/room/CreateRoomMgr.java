package business.global.room;

import BaseThread.BaseMutexManager;
import business.global.room.base.AbsBaseRoom;
import com.ddm.server.common.CommLogD;
import com.ddm.server.protocol.v2.ProtocolRoomTracker;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.lang.reflect.Constructor;

/**
 * 创建房间管理
 *
 * @author Administrator
 */
public class CreateRoomMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static CreateRoomMgr instance = new CreateRoomMgr();
    }

    // 私有化构造方法
    private CreateRoomMgr() {
    }

    // 获取单例
    public static CreateRoomMgr getInstance() {
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


    /**
     * 创建房间
     *
     * @param classPath         类路径
     * @param baseRoomConfigure 公共配置
     * @param ownerID           房主ID
     * @param key               房间key
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends AbsBaseRoom> T createRoom(String classPath, BaseRoomConfigure baseRoomConfigure, long ownerID,
                                                String key) {
        AbsBaseRoom room = null;
        try {
            Class<?> clz = Class.forName(classPath);
            // 以下调用带参的、私有构造函数
            Constructor constructor = clz
                    .getDeclaredConstructor(new Class[]{BaseRoomConfigure.class, String.class, long.class});
            // 私有构造函数中的成员变量为private,故必须进行此操作
            constructor.setAccessible(true);
            room = (AbsBaseRoom) constructor.newInstance(new Object[]{baseRoomConfigure, key, ownerID});
            // key：房间ID,value：房间信息
            RoomMgr.getInstance().roomPut(room.getRoomID(), room);
            LegacyUnifiedRoomBinding.bind(room, baseRoomConfigure,
                    baseRoomConfigure.getBaseCreateRoom().getPlayerNum());
            ProtocolRoomTracker.roomCreated(room.getRoomID());
        } catch (Exception e) {
            CommLogD.error("CreateRoom:[{}] error:{}", classPath, e.getMessage(), e);
        }
        return (T) room;
    }


    /**
     * 创建房间
     *
     * @param baseRoomConfigure 公共配置
     * @param ownerID           房主ID
     * @param key               房间key
     * @return
     */
    @SuppressWarnings("rawtypes")
    public AbsBaseRoom createRoom(BaseRoomConfigure baseRoomConfigure, long ownerID, String key) {
        String path = String.format(ROOM, baseRoomConfigure.getGameType().getType().name().toLowerCase(),
                baseRoomConfigure.getGameType().getName().toLowerCase(), baseRoomConfigure.getGameType().getName());
        return this.createRoom(path, baseRoomConfigure, ownerID, key);
    }
}
