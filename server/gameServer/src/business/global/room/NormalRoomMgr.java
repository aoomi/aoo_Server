package business.global.room;

import BaseCommon.CommLog;
import business.global.config.GameListConfigMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.NoneClubRoom;
import business.global.room.base.NoneUnionRoom;
import business.global.room.base.RoomImpl;
import business.global.room.key.RoomKeyMgr;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import cenum.RoomTypeEnum;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.EncryptUtils;
import com.ddm.server.websocket.def.ErrorCode;
import core.db.entity.clarkGame.ClubMemberBO;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.ClubRoomConfig;
import jsproto.c2s.cclass.room.UnionRoomConfig;
import jsproto.c2s.iclass.room.SRoom_CreateRoom;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 创建房间管理
 *
 * @author Administrator
 */
public class NormalRoomMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static NormalRoomMgr instance = new NormalRoomMgr();
    }

    // 私有化构造方法
    private NormalRoomMgr() {
    }

    // 获取单例
    public static NormalRoomMgr getInstance() {
        return SingletonHolder.instance;
    }

    // key：房间key,value：房间信息
    private Map<String, RoomImpl> key2rooms = new ConcurrentHashMap<>();

    /**
     * 查询并进入房间
     *
     * @param pid   玩家PID
     * @param key   房间key
     * @param posID 位置
     * @return
     */
    @SuppressWarnings("rawtypes")
    public AbsBaseRoom findAndEnter(long pid, String key, int posID, ClubMemberBO clubMemberBO) {
        // 通过房间key获取房间信息
        AbsBaseRoom room = (AbsBaseRoom) key2rooms.get(key);
        if (Objects.isNull(room)) {
            return room;
        }
        // 检查进入房间。
        SData_Result result = room.enterRoom(pid, posID, false, clubMemberBO);
        if (!ErrorCode.Success.equals(result.getCode())) {
            CommLogD.error("findAndEnter Code:{},Msg:{}", result.getCode(), result.getMsg());
            return null;
        }
        return room;
    }

    /**
     * 通过房间key获取房间信息
     *
     * @param key 房间key
     * @return
     */
    public RoomImpl getNoneRoomByKey(String key) {
        return this.key2rooms.get(key);
    }

    /**
     * 通过房间key获取房间信息
     *
     * @param configId 房间配置Id
     * @return
     */
    public RoomImpl getNoneRoomConfigId(RoomTypeEnum roomTypeEnum, long specialRoomId, long configId) {
        return this.key2rooms.values().stream().filter(k -> RoomState.Init.equals(k.getRoomState()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && specialRoomId == k.getSpecialRoomId() && k.getConfigId() == configId).findAny().orElse(null);

    }

    /**
     * 通过房间key获取房间信息
     *
     * @param key 房间key
     * @return
     */
    public AbsBaseRoom getRoomByKey(String key) {
        return (AbsBaseRoom) this.key2rooms.get(key);
    }

    /**
     * 通过房间key移除房间信息
     *
     * @param roomKey
     */
    public void remove(String roomKey) {
        if (StringUtils.isEmpty(roomKey)) {
            return;
        }
        this.key2rooms.remove(roomKey);
        //移除共享房间
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().removeShareRoom(roomKey);
        }
        RoomKeyMgr.getInstance().giveBackKey(roomKey);
    }

    /**
     * 分组
     *
     * @param roomTypeId 房间类型ID
     * @return
     */
    public Map<RoomState, Long> groupingBy(RoomTypeEnum roomTypeEnum, long roomTypeId) {
        if (Config.isShare()) {
            return ShareRoomMgr.getInstance().groupingBy(roomTypeEnum, roomTypeId);
        } else {
            return this.key2rooms.values().stream().filter(k -> Objects.nonNull(k) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == roomTypeId && !RoomState.End.equals(k.getRoomState())).collect(Collectors.groupingBy(state -> state.getRoomState(), Collectors.counting()));
        }
    }


    /**
     * 是否显示
     *
     * @param room             房间
     * @param unionRoomCfgList 赛事房间配置Id列表
     * @param isHideStartRoom  0全部显示,1隐藏开始中房间 是否隐藏开始房间
     * @return
     */
    private boolean isShow(RoomImpl room, List<Long> unionRoomCfgList, int isHideStartRoom) {
        if (isHideStartRoom >= 1) {
            // 1隐藏开始中房间 是否隐藏开始房间
            if (RoomState.Playing.equals(room.getRoomState()) || RoomState.End.equals(room.getRoomState())) {
                return false;
            }
        }
        if (Objects.isNull(room.getBaseRoomConfigure().getUnionRoomCfg())) {
            return false;
        }
        if (CollectionUtils.isEmpty(unionRoomCfgList)) {
            return true;
        } else {
            return !unionRoomCfgList.contains(room.getConfigId());
        }
    }


    /**
     * 房间类型
     *
     * @return
     */
    public List<RoomImpl> roomValues(RoomTypeEnum roomTypeEnum, long roomTypeId) {
        return this.key2rooms.values().stream().filter(k -> null != k && null != k.getBaseRoomConfigure() && null != k.getBaseRoomConfigure().getClubRoomCfg() && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == roomTypeId).collect(Collectors.toList());
    }


    /**
     * 房间列表
     * 联赛房间列表
     *
     * @param roomTypeEnum     房间类型
     * @param roomTypeId       房间类型Id
     * @param unionRoomCfgList 赛事房间配置Id列表
     * @param isHideStartRoom  是否隐藏开始房间
     * @return
     */
    public List<RoomImpl> roomUnionValues(RoomTypeEnum roomTypeEnum, long roomTypeId, List<Long> unionRoomCfgList, int isHideStartRoom, int pageNum, int sort, Integer gameType) {
        Stream<RoomImpl> stream = this.key2rooms
                .values()
                .stream()
                .filter(k -> Objects.nonNull(k) && Objects.nonNull(k.getBaseRoomConfigure()) && (null == gameType || k.getBaseRoomConfigure().getGameType().getId() == gameType) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == roomTypeId && isShow(k, unionRoomCfgList, isHideStartRoom));
        if (sort <= 0) {
            return stream.sorted(Comparator.comparing(RoomImpl::sorted)).collect(Collectors.toList());
        } else {
            return stream.sorted(Comparator.comparing(RoomImpl::sorted).reversed()).collect(Collectors.toList());
        }
    }


    /**
     * 房间类型
     * 亲友圈房间列表
     *
     * @return
     */
    public List<RoomImpl> roomClubValues(RoomTypeEnum roomTypeEnum, long roomTypeId, int pageNum) {
        return this.key2rooms
                .values()
                .stream()
                .filter(k -> Objects.nonNull(k) && Objects.nonNull(k.getBaseRoomConfigure()) && Objects.nonNull(k.getBaseRoomConfigure().getClubRoomCfg()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == roomTypeId)
                .sorted(Comparator.comparing(RoomImpl::sorted))
                .collect(Collectors.toList());
    }


    /**
     * 值
     *
     * @param map       分组计数
     * @param roomState 房间状态
     * @return
     */
    public static int Value(Map<RoomState, Long> map, RoomState roomState) {
        Long value = map.get(roomState);
        if (Objects.isNull(value)) {
            return 0;
        } else {
            return value.intValue();
        }
    }

    /**
     * 创建房间
     *
     * @param baseRoomConfigure 公共配置
     * @param ownerID           房主ID
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createRoom(BaseRoomConfigure baseRoomConfigure, long ownerID) {
        return this.createRoom(baseRoomConfigure, ownerID, RoomKeyMgr.getInstance().getNewKey());
    }

    /**
     * 创建房间
     *
     * @param baseRoomConfigure 公共配置
     * @param key               房间key
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createRoom(BaseRoomConfigure baseRoomConfigure, String key) {
        return this.createRoom(baseRoomConfigure, 0L, key);
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
    public SData_Result createRoom(BaseRoomConfigure baseRoomConfigure, long ownerID, String key) {
        key = StringUtils.isEmpty(key) ? RoomKeyMgr.getInstance().getNewKey() : key;
        // 获取创建房间。
        AbsBaseRoom createRoom = CreateRoomMgr.getInstance().createRoom(baseRoomConfigure, ownerID, key);
        // 创建房间失败
        if (null == createRoom) {
            return SData_Result.make(ErrorCode.Create_Room_Error, "NormalRoomMgr Create_Room_Error {%s}",
                    baseRoomConfigure.getGameType().getName());
        }
        this.key2rooms.put(key, createRoom);
        //创建共享房间
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().addShareRoom(this.key2rooms.get(key));
            // 创建房间
            ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(key);
            return SData_Result.make(ErrorCode.Success, SRoom_CreateRoom.make(createRoom.getRoomID(),
                    createRoom.getRoomKey(), baseRoomConfigure.getBaseCreateRoom().getCreateType(), createRoom.getBaseRoomConfigure().getGameType().getId(), GameListConfigMgr.getInstance().getByRoom(shareRoom)));
        } else {
            // 创建房间
            return SData_Result.make(ErrorCode.Success, SRoom_CreateRoom.make(createRoom.getRoomID(),
                    createRoom.getRoomKey(), baseRoomConfigure.getBaseCreateRoom().getCreateType(), createRoom.getBaseRoomConfigure().getGameType().getId()));
        }
    }

    /**
     * 创建房间
     *
     * @param baseRoomConfigure 公共配置
     * @param key               房间key
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createNormalRoom(BaseRoomConfigure baseRoomConfigure, long ownerID, String key) {
        key = StringUtils.isEmpty(key) || RoomKeyMgr.getInstance().isExistUse(key) ? null : key;
        return this.createRoom(baseRoomConfigure, ownerID, key);
    }


    @SuppressWarnings("rawtypes")
    public BaseRoomConfigure createNoneClubRoom(BaseRoomConfigure baseRoomConfigure, int roomCard) {
        return (BaseRoomConfigure) createNoneClubRoom(baseRoomConfigure, "", baseRoomConfigure.getClubRoomCfg().getOwnerID(), roomCard, baseRoomConfigure.getClubRoomCfg().getClubName()).getData();
    }

    public BaseRoomConfigure createNoneClubRoomShare(BaseRoomConfigure baseRoomConfigure, String roomKey, int roomCard) {
        return (BaseRoomConfigure) createNoneClubRoom(baseRoomConfigure, roomKey, baseRoomConfigure.getClubRoomCfg().getOwnerID(), roomCard, baseRoomConfigure.getClubRoomCfg().getClubName()).getData();
    }

    /**
     * 创建空亲友圈房间
     *
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createNoneClubRoom(BaseRoomConfigure baseRoomConfigure, String roomKey, long pid, int roomCard, String name) {
        String key = "";
        long clubId = baseRoomConfigure.getBaseCreateRoom().getClubId();
        long configId = baseRoomConfigure.getBaseCreateRoom().getGameIndex();
        if (StringUtils.isEmpty(roomKey)) {
            key = RoomKeyMgr.getInstance().getNewKey();
        } else {
            if (Config.isShare()) {
                ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(roomKey);
                if (Objects.isNull(shareRoom) || RoomState.End.equals(shareRoom.getRoomState())) {
                    // 如果房间不存在或者房间结束,可以拿原来的key
                    CommLog.warn("createNoneClubRoom oldKey pid:{},clubId:{},configId:{},roomKey:{},roomCard:{}", pid, clubId, configId, roomKey, roomCard);
                    key = roomKey;
                } else {
                    CommLog.warn("createNoneClubRoom newKey pid:{},clubId:{},configId:{},roomKey:{},roomCard:{}", pid, clubId, configId, roomKey, roomCard);
                    key = RoomKeyMgr.getInstance().getNewKey();
                }
            } else {
                RoomImpl roomImpl = this.key2rooms.get(roomKey);
                if (Objects.isNull(roomImpl) || roomImpl.isEndRoom()) {
                    // 如果房间不存在或者房间结束,可以拿原来的key
                    CommLog.warn("createNoneClubRoom oldKey pid:{},clubId:{},configId:{},roomKey:{},roomCard:{}", pid, clubId, configId, roomKey, roomCard);
                    key = roomKey;
                } else {
                    CommLog.warn("createNoneClubRoom newKey pid:{},clubId:{},configId:{},roomKey:{},roomCard:{}", pid, clubId, configId, roomKey, roomCard);
                    key = RoomKeyMgr.getInstance().getNewKey();
                }
            }
        }
        if (StringUtils.isEmpty(key)) {
            // 最后还是没有key ，则随机获取一个。
            key = RoomKeyMgr.getInstance().getNewKey();
        }
        baseRoomConfigure.setClubRoomCfg(new ClubRoomConfig(pid, key, roomCard, name));
        this.key2rooms.put(key, new NoneClubRoom(clubId, baseRoomConfigure, key));
        //创建共享房间
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().addShareRoom(this.key2rooms.get(key));
        }
        return SData_Result.make(ErrorCode.Success, baseRoomConfigure);
    }

    @SuppressWarnings("rawtypes")
    public BaseRoomConfigure createNoneUnionRoomShare(BaseRoomConfigure baseRoomConfigure, String roomKey, int roomCard) {
        return (BaseRoomConfigure) createNoneUnionRoom(baseRoomConfigure, roomKey, baseRoomConfigure.getUnionRoomCfg().getOwnerID(), roomCard, baseRoomConfigure.getUnionRoomCfg().getName()).getData();
    }

    @SuppressWarnings("rawtypes")
    public BaseRoomConfigure createNoneUnionRoom(BaseRoomConfigure baseRoomConfigure, int roomCard) {
        return (BaseRoomConfigure) createNoneUnionRoom(baseRoomConfigure, null, baseRoomConfigure.getUnionRoomCfg().getOwnerID(), roomCard, baseRoomConfigure.getUnionRoomCfg().getName()).getData();
    }

    /**
     * 创建空赛事房间
     *
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createNoneUnionRoom(BaseRoomConfigure baseRoomConfigure, String roomKey, long pid, int roomCard, String name) {
        String key = "";
        long unionId = baseRoomConfigure.getBaseCreateRoom().getUnionId();
        long configId = baseRoomConfigure.getBaseCreateRoom().getGameIndex();
        if (StringUtils.isEmpty(roomKey)) {
            key = RoomKeyMgr.getInstance().getNewKey();
        } else {
            if (Config.isShare()) {
                ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(roomKey);
                if (Objects.isNull(shareRoom) || RoomState.End.equals(shareRoom.getRoomState())) {
                    // 如果房间不存在或者房间结束,可以拿原来的key
                    CommLog.warn("createNoneUnionRoom oldKey pid:{},unionId:{},configId:{},roomKey:{},roomCard:{}", pid, unionId, configId, roomKey, roomCard);
                    key = roomKey;
                } else {
                    CommLog.warn("createNoneUnionRoom newKey pid:{},clubId:{},configId:{},roomKey:{},roomCard:{}", pid, unionId, configId, roomKey, roomCard);
                    key = RoomKeyMgr.getInstance().getNewKey();
                }
            } else {
                RoomImpl roomImpl = this.key2rooms.get(roomKey);
                if (Objects.isNull(roomImpl) || roomImpl.isEndRoom()) {
                    // 如果房间不存在或者房间结束,可以拿原来的key
                    CommLog.warn("createNoneUnionRoom oldKey pid:{},unionId:{},configId:{},roomKey:{},roomCard:{}", pid, unionId, configId, roomKey, roomCard);
                    key = roomKey;
                } else {
                    CommLog.warn("createNoneUnionRoom newKey pid:{},clubId:{},configId:{},roomKey:{},roomCard:{}", pid, unionId, configId, roomKey, roomCard);
                    key = RoomKeyMgr.getInstance().getNewKey();
                }
            }

        }
        if (StringUtils.isEmpty(key)) {
            // 最后还是没有key ，则随机获取一个。
            key = RoomKeyMgr.getInstance().getNewKey();
        }
        baseRoomConfigure.setUnionRoomConfig(new UnionRoomConfig(pid, key, roomCard, name));
        this.key2rooms.put(key, new NoneUnionRoom(unionId, baseRoomConfigure, key));
        //创建共享房间
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().addShareRoom(this.key2rooms.get(key));
        }
        return SData_Result.make(ErrorCode.Success, baseRoomConfigure);
    }


    /**
     * 测试快速进入房间专用。
     *
     * @return
     */
    public AbsBaseRoom getDeBugRoomKey() {
        return this.key2rooms.values().stream().filter(k -> !k.isNoneRoom()).map(k -> ((AbsBaseRoom) k)).filter(k -> k.getRoomPosMgr().checkExistEmptyPos()).findFirst().orElse(null);
    }


    /**
     * 获取指定配置指定特殊房间的房间key
     *
     * @param configId      房间配置Id
     * @param specialRoomId 特殊房间Id
     * @return
     */
    public String getSpecifiedConfigurationRoomKey(long configId, long specialRoomId, RoomTypeEnum roomTypeEnum) {
        return this.key2rooms
                .values()
                .stream()
                .filter(k -> RoomState.Init.equals(k.getRoomState()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == specialRoomId && k.getConfigId() == configId && k.checkExistEmptyPos())
                .map(k -> k.getRoomKey())
                .findAny()
                .orElse(null);
    }


    /**
     * 获取指定配置指定特殊房间的房间key
     *
     * @param configIds     房间配置Id列表
     * @param specialRoomId 特殊房间Id
     * @return
     */
    public List<Long> getSpecifiedConfigurationRoomKey(List<Long> configIds, long specialRoomId, RoomTypeEnum roomTypeEnum) {
        List<Long> existIdList = this.key2rooms
                .values()
                .stream()
                .filter(k -> k.isNoneRoom() && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == specialRoomId && configIds.contains(k.getConfigId()))
                .map(k -> k.getConfigId())
                .collect(Collectors.toList());
        // 移除重复
        configIds.removeAll(existIdList);
        return configIds;
    }


    /**
     * 查询存在空位置的房间。
     * 空位置数量
     */
    @SuppressWarnings("rawtypes")
    public AbsBaseRoom queryExistEmptyPosCount(GameType gameType, long specialRoomId, long configId) {
        int count = (int) this.key2rooms.values().parallelStream()
                .filter(room -> !room.isNoneRoom() && this.checkExistRoom(room) && room.getSpecialRoomId() == specialRoomId && room.getConfigId() == configId && room.getBaseRoomConfigure().getGameType().getId() == gameType.getId() && room.checkExistEmptyPos())
                .limit(5).count();
        if (count < 5) {
            return null;
        }
        Function<RoomImpl, Integer> function = p -> p.getEmptyPosCount();
        return (AbsBaseRoom) this.key2rooms.values().parallelStream()
                .filter(room -> !room.isNoneRoom() && this.checkExistRoom(room) && room.getSpecialRoomId() == specialRoomId && room.getConfigId() == configId && room.getBaseRoomConfigure().getGameType().getId() == gameType.getId() && room.checkExistEmptyPos())
                .sorted(Comparator.comparing(function)).findFirst().orElse(null);
    }

    /**
     * 查询存在空位置的房间。
     */
    @SuppressWarnings("rawtypes")
    public AbsBaseRoom queryExistEmptyPos(GameType gameType, long specialRoomId, long configId) {
        Function<RoomImpl, Integer> function = p -> p.getEmptyPosCount();
        return (AbsBaseRoom) this.key2rooms.values().parallelStream()
                .filter(room -> !room.isNoneRoom() && this.checkExistRoom(room) && room.getSpecialRoomId() == specialRoomId && room.getConfigId() == configId && room.getBaseRoomConfigure().getGameType().getId() == gameType.getId() && room.checkExistEmptyPos())
                .sorted(Comparator.comparing(function)).findFirst().orElse(null);
    }

    /**
     * 检查房间是否存在
     *
     * @param room
     * @return
     */
    private boolean checkExistRoom(RoomImpl room) {
        // 是否存在空房间或游戏中房间
        boolean isExistRoomEmpty = Objects.isNull(room) || room.isEndRoom() || RoomState.Playing.equals(room.getRoomState());
        return isExistRoomEmpty ? false : Objects.nonNull(room.getBaseRoomConfigure());
    }


    /**
     * 创建房间
     *
     * @param baseRoomConfigure 公共配置
     * @param key               房间key
     * @return
     */
    @SuppressWarnings("rawtypes")
    public AbsBaseRoom createUnionRoom(BaseRoomConfigure baseRoomConfigure, String key, long pid, int roomCard, String name) {
        BaseRoomConfigure configure = baseRoomConfigure.deepClone();
        configure.setUnionRoomConfig(new UnionRoomConfig(pid, key, roomCard, name));
        // 获取创建房间。
        AbsBaseRoom createRoom = CreateRoomMgr.getInstance().createRoom(configure, 0L, key);
        //创建共享房间
        if (Config.isShare()) {
            if (!StringUtils.isEmpty(createRoom.getBaseRoomConfigure().getBaseCreateRoom().getPassword())) {
                createRoom.getBaseRoomConfigure().getBaseCreateRoom().setPassword(EncryptUtils.encryptDES(createRoom.getBaseRoomConfigure().getBaseCreateRoom().getPassword()));
            }
        }
        // 创建房间失败
        if (Objects.isNull(createRoom)) {
            return createRoom;
        }
        this.key2rooms.put(key, createRoom);
        //创建共享房间
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().addShareRoom(this.key2rooms.get(key));
        }
        // 检查并进入房间
        return createRoom;
    }


    /**
     * 创建房间
     *
     * @param baseRoomConfigure 公共配置
     * @param key               房间key
     * @return
     */
    @SuppressWarnings("rawtypes")
    public AbsBaseRoom createClubRoom(BaseRoomConfigure baseRoomConfigure, String key, long pid, int roomCard, String name) {
        BaseRoomConfigure configure = baseRoomConfigure.deepClone();
        configure.setClubRoomCfg(new ClubRoomConfig(pid, key, roomCard, name));
        // 获取创建房间。
        AbsBaseRoom createRoom = CreateRoomMgr.getInstance().createRoom(configure, 0L, key);
        // 创建房间失败
        if (Objects.isNull(createRoom)) {
            return createRoom;
        }
        //创建共享房间
        if (Config.isShare()) {
            if (!StringUtils.isEmpty(createRoom.getBaseRoomConfigure().getBaseCreateRoom().getPassword())) {
                createRoom.getBaseRoomConfigure().getBaseCreateRoom().setPassword(EncryptUtils.encryptDES(createRoom.getBaseRoomConfigure().getBaseCreateRoom().getPassword()));
            }
        }
        this.key2rooms.put(key, createRoom);
        //创建共享房间
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().addShareRoom(this.key2rooms.get(key));
        }
        // 检查并进入房间
        return createRoom;
    }


    /**
     * 获取指定配置未开始的房间列表
     *
     * @param configId      配置Id
     * @param specialRoomId 特殊id
     * @param roomTypeEnum  房间类型
     * @return
     */
    public List<RoomImpl> getRoomInitList(long configId, long specialRoomId, RoomTypeEnum roomTypeEnum) {
        return this.key2rooms.values().stream().filter(k -> RoomState.Init.equals(k.getRoomState()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == specialRoomId && configId == k.getConfigId()).collect(Collectors.toList());
    }

}
