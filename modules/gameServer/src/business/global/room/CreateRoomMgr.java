package business.global.room;

import BaseThread.BaseMutexManager;
import business.global.room.base.AbsBaseRoom;
import com.ddm.server.common.CommLogD;
import com.aoo.bcg.gamespi.RoomCreationContext;
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
        return this.createRoom(classPath, baseRoomConfigure, ownerID, key, null);
    }

    /**
     * Creates a legacy room with an explicit immutable Compatibility origin. A null origin is
     * intentionally accepted only to preserve the old call surface; the bind stage rejects it
     * before publication instead of manufacturing a historical identity from gameId.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends AbsBaseRoom> T createRoom(String classPath, BaseRoomConfigure baseRoomConfigure, long ownerID,
                                                String key, RoomCreationContext.CompatibilityOrigin compatibilityOrigin) {
        AbsBaseRoom room = null;
        try {
            Class<?> clz = Class.forName(classPath);
            // 以下调用带参的、私有构造函数
            Constructor constructor = clz
                    .getDeclaredConstructor(new Class[]{BaseRoomConfigure.class, String.class, long.class});
            // 私有构造函数中的成员变量为private,故必须进行此操作
            constructor.setAccessible(true);
            room = (AbsBaseRoom) constructor.newInstance(new Object[]{baseRoomConfigure, key, ownerID});
            final AbsBaseRoom createdRoom = room;
            bindThenPublish(createdRoom,
                    ignored -> LegacyUnifiedRoomBinding.bind(createdRoom, baseRoomConfigure,
                            baseRoomConfigure.getBaseCreateRoom().getPlayerNum(), compatibilityOrigin),
                    ignored -> {
                        // key：房间ID,value：房间信息。只有权威绑定成功后才发布到旧房间表。
                        RoomMgr.getInstance().roomPut(createdRoom.getRoomID(), createdRoom);
                        ProtocolRoomTracker.roomCreated(createdRoom.getRoomID());
                    },
                    ignored -> rollbackPublishedRoom(createdRoom, baseRoomConfigure, compatibilityOrigin));
            return (T) createdRoom;
        } catch (Exception e) {
            CommLogD.error("CreateRoom:[{}] error:{}", classPath, e.getMessage(), e);
            if (e instanceof IllegalStateException rejection && e.getMessage() != null
                    && e.getMessage().startsWith("ROOM_ADMISSION_REJECTED|")) {
                throw rejection;
            }
        }
        // 绑定或发布失败时 fail-closed；调用方不得拿到没有权威运行时的半创建房间。
        return null;
    }

    @FunctionalInterface
    interface CreationStage<T> {
        void run(T value) throws Exception;
    }

    /**
     * Runs the authority bind before the legacy room is published. Any failure
     * invokes rollback and is rethrown so the public create API can fail closed.
     * This small seam is package-visible for the executable lifecycle test.
     */
    static <T> T bindThenPublish(T value, CreationStage<T> bind, CreationStage<T> publish,
                                 CreationStage<T> rollback) throws Exception {
        try {
            bind.run(value);
            publish.run(value);
            return value;
        } catch (Exception failure) {
            try {
                rollback.run(value);
            } catch (Exception cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private static void rollbackPublishedRoom(AbsBaseRoom room, BaseRoomConfigure baseRoomConfigure,
                                              RoomCreationContext.CompatibilityOrigin compatibilityOrigin) {
        // A provider may have installed an authority before reporting failure;
        // always remove that binding, even when the legacy table was not published.
        if (compatibilityOrigin == null) {
            LegacyUnifiedRoomBinding.remove(room.getRoomID());
            return;
        }
        if (RoomMgr.getInstance().getRoom(room.getRoomID()) == room) {
            try {
                RoomMgr.getInstance().removeRoom(room.getRoomID(), baseRoomConfigure.getPrizeType());
            } finally {
                LegacyUnifiedRoomBinding.remove(room.getRoomID());
            }
        } else {
            LegacyUnifiedRoomBinding.remove(room.getRoomID());
        }
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
        return this.createRoom(baseRoomConfigure, ownerID, key, null);
    }

    public AbsBaseRoom createRoom(BaseRoomConfigure baseRoomConfigure, long ownerID, String key,
                                  RoomCreationContext.CompatibilityOrigin compatibilityOrigin) {
        String path = String.format(ROOM, baseRoomConfigure.getGameType().getType().name().toLowerCase(),
                baseRoomConfigure.getGameType().getName().toLowerCase(), baseRoomConfigure.getGameType().getName());
        return this.createRoom(path, baseRoomConfigure, ownerID, key, compatibilityOrigin);
    }
}
