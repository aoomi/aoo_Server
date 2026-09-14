package business.global.room;

import business.global.room.base.AbsBaseRoom;
import business.global.room.key.GoldKeyMgr;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.UnionRoomConfig;

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
public class UnionRoomMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static UnionRoomMgr instance = new UnionRoomMgr();
    }

    // 私有化构造方法
    private UnionRoomMgr() {
    }

    // 获取单例
    public static UnionRoomMgr getInstance() {
        return SingletonHolder.instance;
    }

    // key：房间key,value：房间信息
    private Map<String, AbsBaseRoom> key2rooms = new ConcurrentHashMap<>();

    /**
     * 查询存在空位置的房间。
     */
    @SuppressWarnings("rawtypes")
    public AbsBaseRoom queryExistEmptyPos(GameType gameType, long specialRoomId, long configId) {
        Function<AbsBaseRoom, Integer> function = p -> p.getRoomPosMgr().getEmptyPosCount();
        return this.key2rooms.values().parallelStream()
                .filter(room -> this.checkExistRoom(room) && room.getSpecialRoomId() == specialRoomId && room.getConfigId() == configId && room.getBaseRoomConfigure().getGameType().getId() == gameType.getId() && room.getRoomPosMgr().checkExistEmptyPos())
                .sorted(Comparator.comparing(function)).findFirst().orElse(null);
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
     * 查询并进入房间
     *
     * @param room    房间信息
     * @param pid     玩家PID
     * @param isRobot 是否机器人
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result findAndSeat(AbsBaseRoom room, long pid, boolean isRobot) {
        // 房间不存在
        if (null == room) {
            return SData_Result.make(ErrorCode.NotAllow, "GoldRoom findAndEnter null == room error");
        }
        SData_Result result = room.seatRoom(pid, -1, isRobot, false, 0, null);
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

//    /**
//     * 创建房间
//     *
//     * @param baseRoomConfigure 公共配置
//     * @param pid               pid
//     * @return
//     */
//    @SuppressWarnings("rawtypes")
//    public SData_Result createRoom(BaseRoomConfigure baseRoomConfigure, long pid) {
//        return this.createRoom(baseRoomConfigure, 0L,
//                GoldKeyMgr.getInstance().getNewKey(baseRoomConfigure.getGameType()), pid);
//    }

    /**
     * 创建房间
     *
     * @param baseRoomConfigure 公共配置
     * @param key               房间key
     * @return
     */
    @SuppressWarnings("rawtypes")
    public AbsBaseRoom createRoom(BaseRoomConfigure baseRoomConfigure, String key, long pid, int roomCard, String name) {
        baseRoomConfigure.setUnionRoomConfig(new UnionRoomConfig(pid, key, roomCard, name));
        // 获取创建房间。
        AbsBaseRoom createRoom = CreateRoomMgr.getInstance().createRoom(baseRoomConfigure, 0L, key);
        // 创建房间失败
        if (Objects.isNull(createRoom)) {
            return createRoom;
        }
        this.key2rooms.put(key, createRoom);
        // 检查并进入房间
        return createRoom;
    }


//    /**
//     * 创建空赛事房间
//     *
//     * @param baseRoomConfigure 公共配置
//     * @return
//     */
//    @SuppressWarnings("rawtypes")
//    public SData_Result createNoneUnionRoom(BaseRoomConfigure baseRoomConfigure, String roomKey, long pid, int roomCard, String name) {
//        String key = "";
//        long unionId = baseRoomConfigure.getBaseCreateRoom().getUnionId();
//        long configId = baseRoomConfigure.getBaseCreateRoom().getGameIndex();
//        if (StringUtils.isEmpty(roomKey)) {
//            key = RoomKeyMgr.getInstance().getNewKey();
//        } else {
//            RoomImpl roomImpl = this.key2rooms.get(roomKey);
//            if (Objects.isNull(roomImpl) || roomImpl.isEndRoom()) {
//                // 如果房间不存在或者房间结束,可以拿原来的key
//                CommLog.error("createNoneUnionRoom oldKey pid:{},unionId:{},configId:{},roomKey:{},roomCard:{}",pid,unionId,configId,roomKey,roomCard);
//                key = roomKey;
//            } else {
//                CommLog.error("createNoneUnionRoom newKey pid:{},clubId:{},configId:{},roomKey:{},roomCard:{}",pid,unionId,configId,roomKey,roomCard);
//                key = RoomKeyMgr.getInstance().getNewKey();
//            }
//        }
//        if (StringUtils.isEmpty(key)) {
//            // 最后还是没有key ，则随机获取一个。
//            key = RoomKeyMgr.getInstance().getNewKey();
//        }
//        baseRoomConfigure.setUnionRoomConfig(new UnionRoomConfig(pid, key, roomCard, name));
//        this.key2rooms.put(key, new NoneUnionRoom(unionId, baseRoomConfigure,key));
//        return SData_Result.make(ErrorCode.Success, baseRoomConfigure);
//    }

}