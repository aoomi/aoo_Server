package business.global.room;

import business.global.room.base.AbsBaseRoom;
import business.global.room.key.GoldKeyMgr;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 创建房间管理
 *
 * @author Administrator
 */
public class GoldRoomMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static GoldRoomMgr instance = new GoldRoomMgr();
    }

    // 私有化构造方法
    private GoldRoomMgr() {
    }

    // 获取单例
    public static GoldRoomMgr getInstance() {
        return SingletonHolder.instance;
    }

    // key：房间key,value：房间信息
    private Map<String, AbsBaseRoom> key2rooms = new ConcurrentHashMap<>();

    /**
     * 查询存在空位置的房间。
     */
    @SuppressWarnings("rawtypes")
    public SData_Result queryExistEmptyPos(GameType gameType, long pid, long practiceId) {
        Function<AbsBaseRoom, Integer> function = p -> p.getRoomPosMgr().getEmptyPosCount();
        AbsBaseRoom queryRoom = this.key2rooms.values().parallelStream()
                .filter(room -> this.checkExistRoom(room) && room.getSpecialRoomId() == practiceId && room.getBaseRoomConfigure().getGameType().getId() == gameType.getId()
                        && room.getRoomPosMgr().checkExistEmptyPos())
                .sorted(Comparator.comparing(function)).findFirst().orElse(null);
        // 检查并进入房间
        return this.findAndEnter(queryRoom, pid, false);
    }


    /**
     * 查询存在空位置的房间。
     */
    public List<AbsBaseRoom> queryExistEmptyPos() {
        Function<AbsBaseRoom, Integer> function = p -> p.getRoomPosMgr().getEmptyPosCount();
        List<AbsBaseRoom> queryRoom = this.key2rooms.values().parallelStream()
                .filter(room -> this.checkExistRoom(room) && room.getRoomPosMgr().checkExistEmptyPos())
                .sorted(Comparator.comparing(function)).collect(Collectors.toList());
        return queryRoom;
    }

    /**
     * 检查房间是否存在
     *
     * @param room
     * @return
     */
    private boolean checkExistRoom(AbsBaseRoom room) {
        // 是否存在空房间或游戏中房间
        boolean isExistRoomEmpty = Objects.isNull(room) || room.isEndRoom() || RoomState.Playing.equals(room.getRoomState());
        return isExistRoomEmpty ? false : Objects.nonNull(room.getBaseRoomConfigure());
    }


    /**
     * 查询并进入房间
     *
     * @param room    房间信息
     * @param pid     玩家PID
     * @param isRobot 是否机器人
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result findAndEnter(AbsBaseRoom room, long pid, boolean isRobot) {
        // 房间不存在
        if (null == room) {
            return SData_Result.make(ErrorCode.NotAllow, "GoldRoom findAndEnter null == room error");
        }
        SData_Result result = room.enterRoom(pid, -1, isRobot, false, 0, null);
        if (!ErrorCode.Success.equals(result.getCode())) {
            CommLogD.error("findAndEnter Code:{},Msg:{}", result.getCode(), result.getMsg());
            return result;
        }
        if (isRobot) {
            room.startTrusteeShipTime();
        }
        // 创建房间
        return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
    }

    /**
     * 通过房间key获取房间信息
     *
     * @param key 房间key
     * @return
     */
    public AbsBaseRoom getRoomByKey(String key) {
        return this.key2rooms.get(key);
    }

    /**
     * 通过房间key移除房间信息
     *
     * @param roomKey
     */
    public void remove(String roomKey) {
        this.key2rooms.remove(roomKey);
        GoldKeyMgr.getInstance().giveBackKey(roomKey);
    }

    /**
     * 创建房间
     *
     * @param baseRoomConfigure 公共配置
     * @param pid               pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createRoom(BaseRoomConfigure baseRoomConfigure, long pid) {
        return this.createRoom(baseRoomConfigure, 0L, GoldKeyMgr.getInstance().getNewKey(baseRoomConfigure.getGameType()), pid);
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
    public SData_Result createRoom(BaseRoomConfigure baseRoomConfigure, long ownerID, String key, long pid) {
        // 获取创建房间。
        AbsBaseRoom createRoom = CreateRoomMgr.getInstance().createRoom(baseRoomConfigure, ownerID, key);
        // 创建房间失败
        if (null == createRoom) {
            return SData_Result.make(ErrorCode.Create_Room_Error, "GoldRoomMgr Create_Room_Error {%s}",
                    baseRoomConfigure.getGameType().getName());
        }
        this.key2rooms.put(key, createRoom);
        // 检查并进入房间
        return this.findAndEnter(createRoom, pid, false);
    }

}