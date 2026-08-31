package business.global.shareroom;

import BaseCommon.CommLog;
import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.config.GameListConfigMgr;
import business.global.room.NormalRoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.RoomImpl;
import business.global.shareclub.ShareClubListMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.rocketmq.bo.MqRoomBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.LocalPlayerMgr;
import business.shareplayer.ShareNode;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.ClassType;
import cenum.RoomSortedEnum;
import cenum.RoomTypeEnum;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.redis.RedisMap;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.BeanUtils;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.JsonUtil;
import com.ddm.server.common.utils.PropertiesUtil;
import com.google.gson.Gson;
import core.db.entity.clarkGame.GameTypeBO;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.RoomCfgCount;
import jsproto.c2s.cclass.club.ClubCreateGameSet;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.RoomInfoItem;
import jsproto.c2s.cclass.room.RoomPosInfoShort;
import jsproto.c2s.cclass.union.UnionCreateGameSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author xsj
 * @date 2020/8/10 16:59
 * @description 共享房间管理类
 */
public class ShareRoomMgr {
    //房间存储KEy
    private static final String SHARE_ROOM_KEY = "shareRoomKey";
    //房间号和房间ID映射关系key
    private static final String SHARE_ROOM_ID_MAPPING_KEY = "shareRoomIdMappingKey";
    //一个亲友圈房间
    private static final String SHARE_ONE_CLUB_ROOM_KEY = "shareOneClubRoomKey";
    //一个赛事房间
    private static final String SHARE_ONE_UNION_ROOM_KEY = "shareOneUnionRoomKey";
    private final String nodeName = Config.nodeName();
    private final String nodeVipAddress = Config.nodeVipAddress();
    private final String nodeIp = Config.nodeIp();
    private final Integer nodePort = Config.nodePort();

    // 私有化构造方法
    private ShareRoomMgr() {
    }

    // 获取单例
    public static ShareRoomMgr getInstance() {
        return ShareRoomMgr.SingletonHolder.instance;
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

    public void init() {
        //清除节点共享房间,重启的情况
        Map<String, ShareRoom> shareRooms = ShareRoomMgr.getInstance().allShareRooms();
        Map<Long, SharePlayer> sharePlayers = SharePlayerMgr.getInstance().allSharePlayers();
        if (Config.isShareLocal()) {
            LocalRoomMgr.getInstance().init(shareRooms);
            LocalPlayerMgr.getInstance().initPlayer(sharePlayers);
        }
        cleanNodeRoom(shareRooms, sharePlayers);


    }

    /**
     * 增加共享房间
     *
     * @param room
     */
    public void addShareRoom(RoomImpl room) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ROOM_KEY);
        ShareRoom shareRoom = createShareRoom(room);
        redisMap.putJson(room.getRoomKey(), shareRoom);
        if (shareRoom.getRoomId() > 0) {
            //添加映射关系
            addRoomIdMapping(shareRoom.getRoomId(), room.getRoomKey());
        }
        addOneClubShareRoom(shareRoom);
        addOneUnionShareRoom(shareRoom);
        if (Config.isShareLocal()) {
            shareRoom.setUpdateTime(System.nanoTime());
            MqProducerMgr.get().send(MqTopic.LOCAL_ROOM_ADD, new MqRoomBo(shareRoom));
        }
    }

    /**
     * 增加一个亲友圈共享房间
     *
     * @param shareRoom
     */
    public void addOneClubShareRoom(ShareRoom shareRoom) {
        if (RoomTypeEnum.CLUB.equals(shareRoom.getRoomTypeEnum())) {
            RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_ROOM_KEY + shareRoom.getClubId());
            redisMap.putJson(shareRoom.getRoomKey(), shareRoom);
        }
    }

    /**
     * 增加一个赛事共享房间
     *
     * @param shareRoom
     */
    public void addOneUnionShareRoom(ShareRoom shareRoom) {
        if (RoomTypeEnum.UNION.equals(shareRoom.getRoomTypeEnum())) {
            RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_UNION_ROOM_KEY + shareRoom.getUnionId());
            redisMap.putJson(shareRoom.getRoomKey(), shareRoom);
        }
    }

    /**
     * 更新共享房间
     *
     * @param room
     */
    public void updateShareRoom(RoomImpl room) {
        ShareRoom shareRoom = getShareRoomByKey(room.getRoomKey());
        if (shareRoom != null) {
            addShareRoom(room);
        }
    }

    /**
     * 局数变化更新
     *
     * @param roomKey
     * @param setId
     */
    public void updateSetId(String roomKey, int setId) {
        ShareRoom shareRoom = getShareRoomByKey(roomKey);
        if (shareRoom != null) {
            shareRoom.setSetId(setId);
            updateShareRoom(shareRoom);
        }
    }

    /**
     * 获取房间信息
     *
     * @param key
     * @return
     */
    public ShareRoom getShareRoomByKey(String key) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ROOM_KEY);
        ShareRoom shareRoom = redisMap.getObject(key, ShareRoom.class);
        return shareRoom;
    }

    /**
     * 更新房间配置
     *
     * @param baseRoomConfigure
     */
    public void updateBaseRoomConfigure(BaseRoomConfigure baseRoomConfigure) {
        String roomKey = null;
        if (baseRoomConfigure.getClubRoomCfg() != null) {
            roomKey = baseRoomConfigure.getClubRoomCfg().getRoomKey();
        } else if (baseRoomConfigure.getUnionRoomCfg() != null) {
            roomKey = baseRoomConfigure.getUnionRoomCfg().getRoomKey();
        }
        ShareRoom shareRoom = getShareRoomByKey(roomKey);
        BaseCreateRoom baseCreateRoom = new BaseCreateRoom();
        BeanUtils.copyProperties(baseCreateRoom, baseRoomConfigure.getBaseCreateRoom());
        baseCreateRoom.setGameIndex(baseRoomConfigure.getBaseCreateRoom().getGameIndex());
        shareRoom.setRoomCfg(baseCreateRoom);
        shareRoom.setConfigId(baseRoomConfigure.getBaseCreateRoom().getGameIndex());
        shareRoom.setTagId(baseRoomConfigure.getTagId());
        ShareBaseRoomConfigure shareBaseRoomConfigure = new ShareBaseRoomConfigure();
        shareBaseRoomConfigure.setTagId(baseRoomConfigure.getTagId());
        shareBaseRoomConfigure.setArenaRoomCfg(baseRoomConfigure.getArenaRoomCfg());
        shareBaseRoomConfigure.setBaseCreateRoom(baseCreateRoom);
        shareBaseRoomConfigure.setBaseCreateRoomClassType(baseRoomConfigure.getBaseCreateRoom().getClass().getName());
        String shareBaseCreateRoom = setShareBaseCreateRoom(baseRoomConfigure.getShareBaseCreateRoom(), baseRoomConfigure.getBaseCreateRoom(), "gameIndex");
        shareBaseRoomConfigure.setShareBaseCreateRoom(shareBaseCreateRoom);
        shareBaseRoomConfigure.setClubRoomCfg(baseRoomConfigure.getClubRoomCfg());
        shareBaseRoomConfigure.setUnionRoomCfg(baseRoomConfigure.getUnionRoomCfg());
        shareBaseRoomConfigure.setRobotRoomCfg(baseRoomConfigure.getRobotRoomCfg());
        shareBaseRoomConfigure.setPrizeType(baseRoomConfigure.getPrizeType());
        shareBaseRoomConfigure.setGameType(new ShareRoomGameType(baseRoomConfigure.getGameType().getId(), baseRoomConfigure.getGameType().getName(), baseRoomConfigure.getGameType().getType().value()));
        shareRoom.setBaseRoomConfigure(shareBaseRoomConfigure);
        //更新redis
        updateShareRoom(shareRoom);
        //修改配置的共享内容字段
        baseRoomConfigure.setShareBaseCreateRoom(shareBaseCreateRoom);

    }

    private String setShareBaseCreateRoom(String shareBaseCreateRoom, BaseCreateRoom baseCreateRoom, String... fields) {
        Gson gson = new Gson();
        Map<String, Object> shareBaseCreate = gson.fromJson(shareBaseCreateRoom, Map.class);
        for (String field : fields) {
            Object value = PropertiesUtil.invokeGet(baseCreateRoom, field);
            shareBaseCreate.put(field, value);
        }
        return gson.toJson(shareBaseCreate);
    }

    /**
     * 转换成共享房间
     *
     * @param room
     * @return
     */
    private ShareRoom createShareRoom(RoomImpl room) {
        ShareRoom shareRoom = new ShareRoom();
        shareRoom.setRoomKey(room.getRoomKey());
        shareRoom.setGameId(room.getBaseRoomConfigure().getGameType().getId());
        shareRoom.setSetCount(room.getBaseRoomConfigure().getBaseCreateRoom().getSetCount());
        shareRoom.setPlayerNum(room.getBaseRoomConfigure().getBaseCreateRoom().getPlayerNum());
        shareRoom.setSort(room.sorted());
        shareRoom.setId(room.getSpecialRoomId());
        shareRoom.setSpecialRoomId(room.getSpecialRoomId());
        shareRoom.setClose(false);
        shareRoom.setTagId(room.getBaseRoomConfigure().getTagId());
        shareRoom.setPassword(room.getBaseRoomConfigure().getBaseCreateRoom().getPassword());
        shareRoom.setRoomTypeEnum(room.getRoomTypeEnum());
        shareRoom.setRoomState(room.getRoomState());
        BaseCreateRoom baseCreateRoom = new BaseCreateRoom();
        BeanUtils.copyProperties(baseCreateRoom, room.getBaseRoomConfigure().getBaseCreateRoom());
        baseCreateRoom.setGameIndex(room.getBaseRoomConfigure().getBaseCreateRoom().getGameIndex());
        shareRoom.setRoomCfg(baseCreateRoom);
        shareRoom.setConfigId(room.getConfigId());
        ShareBaseRoomConfigure shareBaseRoomConfigure = new ShareBaseRoomConfigure();
        shareBaseRoomConfigure.setTagId(room.getBaseRoomConfigure().getTagId());
        shareBaseRoomConfigure.setArenaRoomCfg(room.getBaseRoomConfigure().getArenaRoomCfg());
        shareBaseRoomConfigure.setBaseCreateRoom(baseCreateRoom);
//        shareBaseRoomConfigure.setBaseCreateRoomT(room.getBaseRoomConfigure().getBaseCreateRoom());
        shareBaseRoomConfigure.setBaseCreateRoomClassType(room.getBaseRoomConfigure().getBaseCreateRoom().getClass().getName());
        shareBaseRoomConfigure.setShareBaseCreateRoom(room.getBaseRoomConfigure().getShareBaseCreateRoom());
        shareBaseRoomConfigure.setClubRoomCfg(room.getBaseRoomConfigure().getClubRoomCfg());
        shareBaseRoomConfigure.setUnionRoomCfg(room.getBaseRoomConfigure().getUnionRoomCfg());
        shareBaseRoomConfigure.setRobotRoomCfg(room.getBaseRoomConfigure().getRobotRoomCfg());
        shareBaseRoomConfigure.setPrizeType(room.getBaseRoomConfigure().getPrizeType());
        shareBaseRoomConfigure.setGameType(new ShareRoomGameType(room.getBaseRoomConfigure().getGameType().getId(), room.getBaseRoomConfigure().getGameType().getName(), room.getBaseRoomConfigure().getGameType().getType().value()));
        shareRoom.setBaseRoomConfigure(shareBaseRoomConfigure);
        GameTypeBO gameTypeBO = GameListConfigMgr.getInstance().getAllConfig().get(shareRoom.getGameId());
        Gson gson = new Gson();
        shareRoom.setShareRoomGameBo(gson.fromJson(gson.toJson(gameTypeBO), ShareRoomGameBo.class));
        shareRoom.setNoneRoom(room.isNoneRoom());
        if (room.isNoneRoom()) {
            shareRoom.setCreateTime(CommTime.nowSecond());
        } else {
            AbsBaseRoom absRoom = (AbsBaseRoom) room;
            shareRoom.setSetId(absRoom.getCurSetID());
            List<RoomPosInfoShort> roomPosInfoShorts = absRoom.getRoomPosMgr().getRoomPosInfoShortList();
            shareRoom.setPosList(JsonUtil.jsonToBeanList(JsonUtil.toJson(roomPosInfoShorts), ShareRoomPosInfoShort.class));
            shareRoom.setCreateTime(absRoom.getTask().getCreateSec());
            shareRoom.setRoomId(absRoom.getRoomID());
            shareRoom.setOwnerID(absRoom.getOwnerID());
        }
        if (RoomTypeEnum.CLUB.equals(room.getRoomTypeEnum())) {
            shareRoom.setClubId(room.getSpecialRoomId());
            shareRoom.setRoomName("");
        }
        if (RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
            shareRoom.setUnionId(room.getSpecialRoomId());
            shareRoom.setRoomName(room.getBaseRoomConfigure().getBaseCreateRoom().getRoomName());
        }
        shareRoom.setCurShareNode(new ShareNode(this.nodeName, this.nodeVipAddress, this.nodeIp, this.nodePort));
        shareRoom.setSubjectTopic(Config.getLocalServerTopic());
//        if(!(room instanceof AbsBaseRoom)){
//            shareRoom.setRoom(room);
//        }
        return shareRoom;
    }

    /**
     * 房间个数
     *
     * @return
     */
    public int shareRoomSize() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ROOM_KEY);
        return redisMap.sizeObject();
    }

    /**
     * 获取所有共享房间
     *
     * @return
     */
    public HashMap<String, ShareRoom> allShareRooms() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ROOM_KEY);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        HashMap<String, ShareRoom> shareRooms = new HashMap<>(allSet.size());
        Gson gson = new Gson();
        allSet.forEach(map -> {
            try {
                ShareRoom shareRoom = gson.fromJson(map.getValue(), ShareRoom.class);
                shareRooms.put(map.getKey(), shareRoom);
//                CommLog.info("哈哈"+JsonUtil.toJson(shareRoom));
            } catch (Exception e) {
                CommLog.error(e.getMessage(), e);
            }
        });
        return shareRooms;
    }

    /**
     * 获取一个亲友圈所有共享房间
     *
     * @return
     */
    public Map<String, ShareRoom> allOneClubShareRooms(Long clubId) {
        if (Config.isShareLocal()) {
            return LocalRoomMgr.getInstance().allShareRoomsByClubId(clubId);
        } else {
            int nowTime = CommTime.nowSecond();
            RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_ROOM_KEY + clubId);
            Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
            HashMap<String, ShareRoom> shareRooms = new HashMap<>(allSet.size());
            Gson gson = new Gson();
            allSet.forEach(map -> {
                try {
                    ShareRoom shareRoom = gson.fromJson(map.getValue(), ShareRoom.class);
                    if (shareRoom != null && nowTime - shareRoom.getCreateTime() > 7200 && shareRoom.getRoomState().equals(RoomState.End)) {
                        CommLogD.error("shareRoom time out room data={}", gson.toJson(shareRoom));
                        makeUpRemoveShareRoom(shareRoom);
                    }
                    shareRooms.put(map.getKey(), shareRoom);
                } catch (Exception e) {
                    CommLog.error(e.getMessage(), e);
                }
            });
            return shareRooms;
        }
    }


    /**
     * 获取一个亲友圈所有共享房间
     *
     * @return
     */
    public Map<String, ShareRoom> allOneUnionShareRooms(Long unionId) {
        if (Config.isShareLocal()) {
            return LocalRoomMgr.getInstance().allShareRoomsByUnionId(unionId);
        } else {
            int nowTime = CommTime.nowSecond();
            RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_UNION_ROOM_KEY + unionId);
            Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
            HashMap<String, ShareRoom> shareRooms = new HashMap<>(allSet.size());
            Gson gson = new Gson();
            allSet.forEach(map -> {
                try {
                    ShareRoom shareRoom = gson.fromJson(map.getValue(), ShareRoom.class);
                    if (shareRoom != null && nowTime - shareRoom.getCreateTime() > 7200 && shareRoom.getRoomState().equals(RoomState.End)) {
                        CommLogD.error("shareRoom time out room data={}", gson.toJson(shareRoom));
                        makeUpRemoveShareRoom(shareRoom);
                    }
                    shareRooms.put(map.getKey(), shareRoom);
                } catch (Exception e) {
                    CommLog.error(e.getMessage(), e);
                }
            });
            return shareRooms;
        }
    }

    /**
     * 检查玩家当前节点
     *
     * @param shareNode
     * @return
     */
    public boolean checkCurNode(ShareNode shareNode) {
        if (shareNode.getName().equals(nodeName) && shareNode.getVipAddress().equals(nodeVipAddress) && shareNode.getIp().equals(nodeIp) && shareNode.getPort().compareTo(nodePort) == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 移除房间
     *
     * @param roomKey
     */
    public void removeShareRoom(String roomKey) {
        //删除房间
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ROOM_KEY);
        ShareRoom shareRoom = redisMap.getObject(roomKey, ShareRoom.class);
        if (shareRoom != null && shareRoom.getRoomId() > 0) {
            //删除映射关系
            removeRoomIdMapping(shareRoom.getRoomId());
        }
        if (shareRoom != null) {
            removeOnClubShareRoom(shareRoom);
            removeOnUnionShareRoom(shareRoom);
        }
        redisMap.remove(roomKey);
//        if(Config.isShareLocal()) {
//            //删除本地缓存数据
//            MqProducerMgr.get().send(MqTopic.LOCAL_ROOM_REMOVE, new MqRoomRemoveBo(roomKey));
//        }


    }

    /**
     * 没有删除完整补充删除
     *
     * @param shareRoom
     */
    public void makeUpRemoveShareRoom(ShareRoom shareRoom) {
        if (shareRoom != null && getShareRoomByKey(shareRoom.getRoomKey()) == null) {
            CommLogD.error("makeUpRemoveShareRoom roomKey={}, roomID={}", shareRoom.getRoomKey(), shareRoom.getRoomId());
            //删除映射关系
            removeRoomIdMapping(shareRoom.getRoomId());
            removeOnClubShareRoom(shareRoom);
            removeOnUnionShareRoom(shareRoom);
        }

    }

    /**
     * 移除一个亲友圈房间
     *
     * @param shareRoom
     */
    public void removeOnClubShareRoom(ShareRoom shareRoom) {
        //删除房间
        if (RoomTypeEnum.CLUB.equals(shareRoom.getRoomTypeEnum())) {
            RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_CLUB_ROOM_KEY + shareRoom.getClubId());
            redisMap.remove(shareRoom.getRoomKey());
//            if(Config.isShareLocal()) {
//                //删除本地缓存数据
//                MqProducerMgr.get().send(MqTopic.LOCAL_ROOM_REMOVE, new MqRoomRemoveBo(shareRoom.getRoomKey()));
//            }
        }

    }

    /**
     * 移除一个赛事房间
     *
     * @param shareRoom
     */
    public void removeOnUnionShareRoom(ShareRoom shareRoom) {
        //删除房间
        if (RoomTypeEnum.UNION.equals(shareRoom.getRoomTypeEnum())) {
            RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ONE_UNION_ROOM_KEY + shareRoom.getUnionId());
            redisMap.remove(shareRoom.getRoomKey());
//            if(Config.isShareLocal()) {
//                //删除本地缓存数据
//                MqProducerMgr.get().send(MqTopic.LOCAL_ROOM_REMOVE, new MqRoomRemoveBo(shareRoom.getRoomKey()));
//            }
        }

    }

    /**
     * 清除节点的房间
     */
    public void cleanNodeRoom(Map<String, ShareRoom> shareRooms, Map<Long, SharePlayer> sharePlayers) {
        shareRooms.forEach((k, shareRoom) -> {
            if (shareRoom != null) {
                ShareNode shareNode = shareRoom.getCurShareNode();
                //同一个节点不是空房间清除掉
                if (checkCurNode(shareNode) && !shareRoom.isNoneRoom()) {
                    if (shareRoom.getClubId() > 0) {
                        Club club = ShareClubListMgr.getInstance().getClub(shareRoom.getClubId());
                        if (club != null) {
                            Map<Long, ClubCreateGameSet> mCreateGamesetMap = club.getMCreateGamesetMap();
                            if (mCreateGamesetMap.get(shareRoom.getConfigId()) != null) {
                                mCreateGamesetMap.get(shareRoom.getConfigId()).subRoomCount();
                            }
                            ShareClubListMgr.getInstance().addClub(club);
                        }

                    }
                    if (shareRoom.getUnionId() > 0) {
                        Union union = ShareUnionListMgr.getInstance().getUnion(shareRoom.getUnionId());
                        if (union != null) {
                            Map<Long, UnionCreateGameSet> roomConfigBOMap = union.getRoomConfigBOMap();
                            if (roomConfigBOMap.get(shareRoom.getConfigId()) != null) {
                                roomConfigBOMap.get(shareRoom.getConfigId()).subRoomCount();
                            }
                            ShareUnionListMgr.getInstance().addUnion(union);
                        }
                    }
                    removeShareRoom(k);
                    //清理掉缓存玩家身上所在的房间信息
                    if (shareRoom.getRoomId() > 0) {
                        SharePlayerMgr.getInstance().cleanRoomId(shareRoom.getRoomId(), sharePlayers);
                    }
                }
            }
        });
    }

    /**
     * 房间类型
     * 亲友圈房间列表
     *
     * @return
     */
    public List<ShareRoom> roomClubValues(RoomTypeEnum roomTypeEnum, long roomTypeId) {
        int nowTime = CommTime.nowSecond();
        List<ShareRoom> list = allOneClubShareRooms(roomTypeId)
                .values()
                .stream()
                .filter(k -> Objects.nonNull(k) && Objects.nonNull(k.getBaseRoomConfigure()) && Objects.nonNull(k.getBaseRoomConfigure().getClubRoomCfg()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == roomTypeId && ((nowTime - k.getCreateTime() < 7200 && k.getRoomState().equals(RoomState.End)) || (!k.getRoomState().equals(RoomState.End))))
                .sorted(Comparator.comparing(ShareRoom::sorted))
                .collect(Collectors.toList());
        return list;
    }

    /**
     * 房间类型
     * 亲友圈房间列表
     *
     * @return
     */
    public List<ShareRoom> roomUnionValues(RoomTypeEnum roomTypeEnum, long roomTypeId) {
        List<ShareRoom> list = allOneUnionShareRooms(roomTypeId)
                .values()
                .stream()
                .filter(k -> Objects.nonNull(k) && Objects.nonNull(k.getBaseRoomConfigure()) && Objects.nonNull(k.getBaseRoomConfigure().getUnionRoomCfg()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == roomTypeId)
                .sorted(Comparator.comparing(ShareRoom::sorted))
                .collect(Collectors.toList());
        return list;
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
    public List<ShareRoom> roomUnionValues(RoomTypeEnum roomTypeEnum, long roomTypeId, List<Long> unionRoomCfgList, int isHideStartRoom, int pageNum, int sort, Integer gameType) {
        int nowTime = CommTime.nowSecond();
        Stream<ShareRoom> stream = allOneUnionShareRooms(roomTypeId)
                .values()
                .stream()
                .filter(k -> Objects.nonNull(k) && Objects.nonNull(k.getBaseRoomConfigure()) && (null == gameType || k.getBaseRoomConfigure().getGameType().getId() == gameType) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == roomTypeId && isShow(k, unionRoomCfgList, isHideStartRoom) && ((nowTime - k.getCreateTime() < 7200 && k.getRoomState().equals(RoomState.End)) || (!k.getRoomState().equals(RoomState.End))));
        if (sort <= 0) {
            return stream.sorted(Comparator.comparing(ShareRoom::sorted)).collect(Collectors.toList());
        } else {
            return stream.sorted(Comparator.comparing(ShareRoom::sorted).reversed()).collect(Collectors.toList());
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
    private boolean isShow(ShareRoom room, List<Long> unionRoomCfgList, int isHideStartRoom) {
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

    public GameType getByShareRoomGameType(ShareRoomGameType shareRoomGameType) {
        GameType gameType = new GameType(shareRoomGameType.getId(), shareRoomGameType.getName(), shareRoomGameType.getType());
        return gameType;
    }

    public BaseRoomConfigure getBaseRoomConfigure(ShareRoom shareRoom) {
        ShareBaseRoomConfigure shareBaseRoomConfigure = shareRoom.getBaseRoomConfigure();
        GameType gameType = getByShareRoomGameType(shareBaseRoomConfigure.getGameType());
        shareBaseRoomConfigure.getBaseCreateRoom().setGameIndex(shareRoom.getConfigId());
        BaseRoomConfigure baseRoomConfigure = new BaseRoomConfigure(shareBaseRoomConfigure.getPrizeType(), gameType, shareBaseRoomConfigure.getBaseCreateRoom(), shareBaseRoomConfigure.getClubRoomCfg());
        baseRoomConfigure.setUnionRoomConfig(shareBaseRoomConfigure.getUnionRoomCfg());
        baseRoomConfigure.setArenaRoomCfg(shareBaseRoomConfigure.getArenaRoomCfg());
        baseRoomConfigure.setRobotRoomCfg(shareBaseRoomConfigure.getRobotRoomCfg());
        baseRoomConfigure.setShareBaseCreateRoom(shareBaseRoomConfigure.getShareBaseCreateRoom());
        baseRoomConfigure.setTagId(shareBaseRoomConfigure.getTagId());
        return baseRoomConfigure;
    }

    /**
     * 根据房间ID获取房间信息
     *
     * @param roomId
     * @return
     */
    public ShareRoom getShareRoomByRoomId(Long roomId) {
        String roomKey = getRoomIdMapping(roomId);
        if (StringUtils.isEmpty(roomKey)) {
            return null;
        } else {
            ShareRoom shareRoom = getShareRoomByKey(roomKey);
            return shareRoom;
        }
    }

    /**
     * 转换获取房间对象
     *
     * @param room
     * @return
     */
    public RoomInfoItem getRoomInfoItem(ShareRoom room) {
        RoomInfoItem roomInfoItem = new RoomInfoItem();
        roomInfoItem.setRoomName(room.getRoomName());
        roomInfoItem.setRoomKey(room.getRoomKey());
        roomInfoItem.setGameId(room.getGameId());
        roomInfoItem.setSetCount(room.getSetCount());
        roomInfoItem.setPlayerNum(room.getPlayerNum());
        roomInfoItem.setSort(room.sorted());
        roomInfoItem.setId(room.getSpecialRoomId());
        roomInfoItem.setClose(false);
        roomInfoItem.setTagId(room.getTagId());
        roomInfoItem.setPassword(room.getPassword());
        if (room.isNoneRoom()) {
            roomInfoItem.setCreateTime(CommTime.nowSecond());
        } else {
            roomInfoItem.setSetId(room.getSetId());
            roomInfoItem.setPosList(JsonUtil.jsonToBeanList(JsonUtil.toJson(room.getPosList()), RoomPosInfoShort.class));
            roomInfoItem.setCreateTime(room.getCreateTime());
            roomInfoItem.setRoomId(room.getRoomId());
        }
        return roomInfoItem;
    }

    /**
     * 分组
     *
     * @param roomTypeId 房间类型ID
     * @return
     */
    public Map<RoomState, Long> groupingBy(RoomTypeEnum roomTypeEnum, long roomTypeId) {
        return this.allOneClubShareRooms(roomTypeId).values().stream().filter(k -> Objects.nonNull(k) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == roomTypeId && !RoomState.End.equals(k.getRoomState())).collect(Collectors.groupingBy(state -> state.getRoomState(), Collectors.counting()));
    }

    /**
     * 获取房间配置统计
     *
     * @return
     */
    public RoomCfgCount getRoomCfgCount(RoomTypeEnum roomTypeEnum, ClassType classType, long unionId) {
        // 统计
        final RoomCfgCount roomCfgCount = new RoomCfgCount();
        this.allOneUnionShareRooms(unionId)
                .values()
                .parallelStream()
                .filter(k -> RoomState.Playing.equals(k.getRoomState()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && unionId == k.getSpecialRoomId() && (ClassType.NONE.equals(classType) || classType.equals(ClassType.valueOf(k.getBaseRoomConfigure().getGameType().getType()))))
                .forEach(k -> {
                    roomCfgCount.addRoomCount();
                    int i = 0;
                    k.getPosList().forEach(shareRoomPosInfoShort -> {
                        if (shareRoomPosInfoShort.getPid() > 0L) {
                            roomCfgCount.addPlayerCount(1);
                        }
                    });

                });
        return roomCfgCount;
    }

    /**
     * 获取某一个房间配置统计
     *
     * @return
     */
    public Integer getOneRoomCfgCount(Map<String, ShareRoom> allShareRooms, Long specialRoomId, Long gameIndex) {
        /**
         * 游戏中房间数
         */
        final AtomicInteger roomCount = new AtomicInteger();
        allShareRooms
                .values()
                .parallelStream()
                .filter(k -> specialRoomId == k.getSpecialRoomId() && k.getBaseRoomConfigure().getBaseCreateRoom().getGameIndex().compareTo(gameIndex) == 0)
                .forEach(k -> {
                    roomCount.incrementAndGet();
                });
        return roomCount.get();
    }

    /**
     * 更新共享房间
     *
     * @param shareRoom
     */
    public void updateShareRoom(ShareRoom shareRoom) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ROOM_KEY);
        redisMap.putJson(shareRoom.getRoomKey(), shareRoom);
        if (shareRoom.getRoomId() > 0) {
            //添加映射关系
            addRoomIdMapping(shareRoom.getRoomId(), shareRoom.getRoomKey());
        }
        addOneClubShareRoom(shareRoom);
        addOneUnionShareRoom(shareRoom);
        if (Config.isShareLocal()) {
            shareRoom.setUpdateTime(System.nanoTime());
            MqProducerMgr.get().send(MqTopic.LOCAL_ROOM_ADD, new MqRoomBo(shareRoom));
        }
    }

    /**
     * 设置房间状态
     *
     * @param roomKey
     */
    public void setRoomState(String roomKey, RoomState roomState) {
        ShareRoom shareRoom = getShareRoomByKey(roomKey);
        if (shareRoom != null) {
            shareRoom.setRoomState(roomState);
            updateShareRoom(shareRoom);
        }
    }

    /**
     * 增加房间key和Id关系
     *
     * @param roomKey
     * @param roomId
     */
    private void addRoomIdMapping(Long roomId, String roomKey) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ROOM_ID_MAPPING_KEY);
        redisMap.put(String.valueOf(roomId), roomKey);
    }

    /**
     * 删除房间key和Id关系
     *
     * @param roomId
     */
    private void removeRoomIdMapping(Long roomId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ROOM_ID_MAPPING_KEY);
        redisMap.remove(String.valueOf(roomId));
    }

    /**
     * 获取房间key和Id关系
     *
     * @param roomId
     */
    private String getRoomIdMapping(Long roomId) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_ROOM_ID_MAPPING_KEY);
        String roomKey = redisMap.getObject(String.valueOf(roomId), String.class);
        return roomKey;
    }

    /**
     * 获取指定配置指定特殊房间的房间key
     *
     * @param configId      房间配置Id
     * @param specialRoomId 特殊房间Id
     * @return
     */
    public String getSpecifiedConfigurationRoomKey(long configId, long specialRoomId, RoomTypeEnum roomTypeEnum) {
        Map<String, ShareRoom> allShareRooms = new HashMap<>();
        if (RoomTypeEnum.CLUB.equals(roomTypeEnum)) {
            allShareRooms = allOneClubShareRooms(specialRoomId);
        } else if (RoomTypeEnum.UNION.equals(roomTypeEnum)) {
            allShareRooms = allOneUnionShareRooms(specialRoomId);
        }
        return allShareRooms
                .values()
                .stream()
                .filter(k -> RoomState.Init.equals(k.getRoomState()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == specialRoomId && k.getConfigId() == configId && k.getFullPosCount() == 0)
                .map(k -> k.getRoomKey())
                .findAny()
                .orElse(null);
    }

    /**
     * 设置标识码
     *
     * @param roomKey
     * @param tagId
     */
    public void setShareTagId(String roomKey, int tagId) {
        ShareRoom shareRoom = getShareRoomByKey(roomKey);
        if (shareRoom != null) {
            shareRoom.getBaseRoomConfigure().setTagId(tagId);
            updateShareRoom(shareRoom);
        }
    }

    /**
     * 获取指定配置未开始的房间列表
     *
     * @param configId      配置Id
     * @param specialRoomId 特殊id
     * @param roomTypeEnum  房间类型
     * @return
     */
    public List<ShareRoom> getRoomInitList(long configId, long specialRoomId, RoomTypeEnum roomTypeEnum) {
        if (RoomTypeEnum.CLUB.equals(roomTypeEnum)) {
            return allOneClubShareRooms(specialRoomId).values().stream().filter(k -> RoomState.Init.equals(k.getRoomState()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == specialRoomId && configId == k.getConfigId()).collect(Collectors.toList());
        } else if (RoomTypeEnum.UNION.equals(roomTypeEnum)) {
            return allOneUnionShareRooms(specialRoomId).values().stream().filter(k -> RoomState.Init.equals(k.getRoomState()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == specialRoomId && configId == k.getConfigId()).collect(Collectors.toList());
        }
        return null;

    }

    public void doDissolveRoom(ShareRoom shareRoom) {
        if (null != shareRoom.getBaseRoomConfigure()) {
            NormalRoomMgr.getInstance().remove(shareRoom.getRoomKey());
            if (RoomTypeEnum.CLUB.equals(shareRoom.getRoomTypeEnum())) {
                shareRoom.getBaseRoomConfigure().getClubRoomCfg().setRoomCard(0);
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(shareRoom.getSpecialRoomId());
                if (null != club) {
                    club.onClubRoomRemove(shareRoom.getBaseRoomConfigure().getBaseCreateRoom().getGameIndex(), shareRoom.getRoomKey(), RoomSortedEnum.NONE_CONFIG.ordinal());
                }
            } else if (RoomTypeEnum.UNION.equals(shareRoom.getRoomTypeEnum())) {
                shareRoom.getBaseRoomConfigure().getUnionRoomCfg().setRoomCard(0);
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(shareRoom.getSpecialRoomId());
                if (null != union) {
                    union.onUnionRoomRemove(shareRoom.getBaseRoomConfigure().getBaseCreateRoom().getGameIndex(), shareRoom.getRoomKey(), RoomSortedEnum.NONE_CONFIG.ordinal());
                }
            }

        }
    }

    /**
     * 获取指定配置指定特殊房间的房间key
     *
     * @param configIds     房间配置Id列表
     * @param specialRoomId 特殊房间Id
     * @return
     */
    public List<Long> getSpecifiedConfigurationRoomKey(List<Long> configIds, long specialRoomId, RoomTypeEnum roomTypeEnum) {
        if (RoomTypeEnum.CLUB.equals(roomTypeEnum)) {
            List<Long> existIdList = allOneClubShareRooms(specialRoomId)
                    .values()
                    .stream()
                    .filter(k -> k.isNoneRoom() && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == specialRoomId && configIds.contains(k.getConfigId()))
                    .map(k -> k.getConfigId())
                    .collect(Collectors.toList());
            // 移除重复
            configIds.removeAll(existIdList);
        } else if (RoomTypeEnum.UNION.equals(roomTypeEnum)) {
            List<Long> existIdList = allOneUnionShareRooms(specialRoomId)
                    .values()
                    .stream()
                    .filter(k -> k.isNoneRoom() && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == specialRoomId && configIds.contains(k.getConfigId()))
                    .map(k -> k.getConfigId())
                    .collect(Collectors.toList());
            // 移除重复
            configIds.removeAll(existIdList);
        }
        return configIds;
    }

    /**
     * 获取指定配置指定特殊房间的房间key
     *
     * @param configIds     房间配置Id列表
     * @param specialRoomId 特殊房间Id
     * @return
     */
    public Map<Long, String> getRoomKeyMapByConfigIds(List<Long> configIds, long specialRoomId, RoomTypeEnum roomTypeEnum) {
        Map<Long, String> roomKeyMap = new HashMap<>();
        List<ShareRoom> list = new ArrayList<>();
        if (RoomTypeEnum.CLUB.equals(roomTypeEnum)) {
            list = allOneClubShareRooms(specialRoomId)
                    .values()
                    .stream()
                    .filter(k -> k.isNoneRoom() && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == specialRoomId)
                    .collect(Collectors.toList());

        } else if (RoomTypeEnum.UNION.equals(roomTypeEnum)) {
            list = allOneUnionShareRooms(specialRoomId)
                    .values()
                    .stream()
                    .filter(k -> k.isNoneRoom() && roomTypeEnum.equals(k.getRoomTypeEnum()) && k.getSpecialRoomId() == specialRoomId)
                    .collect(Collectors.toList());
        }
        for (ShareRoom shareRoom : list) {
            roomKeyMap.put(shareRoom.getConfigId(), shareRoom.getRoomKey());
        }
        return roomKeyMap;
    }

    /**
     * 获取联盟游戏的房间数
     *
     * @param unionId
     * @return
     */
    public long playingRoomByUnionId(Long unionId) {
        Map<String, ShareRoom> shareRoomMap = allOneUnionShareRooms(unionId);
        return shareRoomMap.values().stream().filter(k -> !k.isNoneRoom()).count();
    }

//    public void getRoomKeyMapByConfigIds(List<Long> configIds, long specialRoomId, RoomTypeEnum roomTypeEnum){
//        List<ShareRoom> list = allOneUnionShareRooms(specialRoomId)
//                .values()
//                .stream()
//                .filter(k -> k.isNoneRoom() && roomTypeEnum.equals(k.getRoomTypeEnum()))
//                .collect(Collectors.toList());
//        Gson gson=new Gson();
//        CommLogD.error("getRoomKeyMapByConfigIds-configIds={}",gson.toJson(configIds));
//        CommLogD.error("getRoomKeyMapByConfigIds-specialRoomId={}",gson.toJson(specialRoomId));
//        CommLogD.error("getRoomKeyMapByConfigIds-roomTypeEnum={}",gson.toJson(roomTypeEnum));
//        CommLogD.error("getRoomKeyMapByConfigIds-list={}",gson.toJson(list));
//    }

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static ShareRoomMgr instance = new ShareRoomMgr();
    }


}
