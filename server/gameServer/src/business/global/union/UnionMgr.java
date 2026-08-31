package business.global.union;

import BaseThread.ThreadManager;
import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.room.NormalRoomMgr;
import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.RoomImpl;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.shareunion.ShareUnionMemberMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerCurrency;
import business.utils.TimeConditionUtils;
import cenum.*;
import com.ddm.server.common.Config;
import com.ddm.server.common.ehcache.DataConstants;
import com.ddm.server.common.ehcache.EhCacheFactory;
import com.ddm.server.common.ehcache.configuration.SqlCacheConfiguration;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.JsonUtil;
import com.ddm.server.common.utils.Lists;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import core.db.entity.clarkGame.UnionDynamicBO;
import core.db.entity.clarkGame.UnionNotifyBO;
import core.db.other.AsyncInfo;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.service.clarkGame.UnionDynamicBOService;
import core.db.service.clarkGame.UnionNotifyBOService;
import core.db.service.clarkLog.ClubLevelRoomCountLogZhongZhiFlowService;
import core.db.service.clarkLog.ClubLevelRoomLogZhongZhiFlowService;
import core.db.service.clarkLog.UnionMatchLogFlowService;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.ClubCompetitionRecord;
import jsproto.c2s.cclass.club.ClubPromotionLevelItem;
import jsproto.c2s.cclass.club.ClubPromotionLevelReportFormItem;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.room.*;
import jsproto.c2s.cclass.union.*;
import jsproto.c2s.iclass.union.*;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class UnionMgr {


    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static UnionMgr instance = new UnionMgr();
    }


    // 获取单例
    public static UnionMgr getInstance() {
        return UnionMgr.SingletonHolder.instance;
    }

    // 赛事管理
    private UnionListMgr unionListMgr;
    /**
     * 赛事成员管理
     */
    private UnionMemberMgr unionMemberMgr;

    // 私有化构造方法
    private UnionMgr() {
        this.setUnionListMgr(new UnionListMgr());
        this.setUnionMemberMgr(new UnionMemberMgr());
    }

    /***
     * 初始化
     */
    public void init() {
        // 初始化赛事管理
        this.getUnionListMgr().init();
        // 初始化赛事成员管理
        this.getUnionMemberMgr().init();
        // 检查计算每个赛事的裁判分
        this.checkSumAllClubSportsPoint();
    }

    private void checkSumAllClubSportsPoint() {
        for (Map.Entry<Long, Union> entry : getUnionListMgr().getUnionMap().entrySet()) {
            if (entry.getValue().getUnionBO().getNewUnionTime() > 0) {
                // 已经有新时间
                continue;
            }
            List<Long> clubIdList;
            if (Config.isShare()) {
                clubIdList = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(entry.getKey()).values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == entry.getKey() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
            } else {
                clubIdList = getUnionMemberMgr().getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == entry.getKey() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
            }
            if (CollectionUtils.isEmpty(clubIdList)) {
                continue;
            }
            double sumAllClubSportsPoint = ClubMgr.getInstance().getClubMemberMgr().sumAllClubSportsPoint(clubIdList);
            entry.getValue().getUnionBO().saveInitSports(sumAllClubSportsPoint);
        }
    }

    /**
     * 检查并执行所有赛事比赛排名
     */
    public void execUnionMatchRanking() {
        ThreadManager.getInstance().regThread(Thread.currentThread().getId());
        Map<Long, Union> unionMap;
        if (Config.isShare()) {
            unionMap = ShareUnionListMgr.getInstance().getAllUnion();
        } else {
            unionMap = getUnionListMgr().getUnionMap();
        }
        for (Map.Entry<Long, Union> entry : unionMap.entrySet()) {
            // 执行赛事比赛排名
            entry.getValue().execUnionMatchRanking();
        }
    }

    @SuppressWarnings({"rawtypes"})
    public List<RoomInfoItem> onUnionGetAllRoom(long unionId, List<Long> unionRoomCfgList, int isHideStartRoom, int pageNum, int sort, Integer gameType) {
        if (unionId <= 0L) {
            return Collections.emptyList();
        }
        if (Config.isShare()) {
            return ShareRoomMgr.getInstance().roomUnionValues(RoomTypeEnum.UNION, unionId, unionRoomCfgList, isHideStartRoom, pageNum, sort, gameType).stream().map(k -> getRoomInfoItem(unionId, k)).collect(Collectors.toList());
        } else {
            return NormalRoomMgr.getInstance().roomUnionValues(RoomTypeEnum.UNION, unionId, unionRoomCfgList, isHideStartRoom, pageNum, sort, gameType).stream().map(k -> getRoomInfoItem(unionId, k)).collect(Collectors.toList());
        }
    }

    /**
     * 获取房间信息
     * 简化版一
     *
     * @param unionId
     * @param unionRoomCfgList
     * @param isHideStartRoom
     * @param pageNum
     * @param sort
     * @return
     */
    public List<RoomInfoItemShortOne> onUnionGetAllRoomShortOne(long unionId, List<Long> unionRoomCfgList, int isHideStartRoom, int pageNum, int sort, Integer gameType) {
        if (unionId <= 0L) {
            return Collections.emptyList();
        }
        if (Config.isShare()) {
            return ShareRoomMgr.getInstance().roomUnionValues(RoomTypeEnum.UNION, unionId, unionRoomCfgList, isHideStartRoom, pageNum, sort, gameType).stream().map(k -> getRoomInfoItemShortOne(unionId, k)).collect(Collectors.toList());
        } else {
            return NormalRoomMgr.getInstance().roomUnionValues(RoomTypeEnum.UNION, unionId, unionRoomCfgList, isHideStartRoom, pageNum, sort, gameType).stream().map(k -> getRoomInfoItemShortOne(unionId, k)).collect(Collectors.toList());
        }
    }

    /**
     * 获取房间信息
     * 简化版二
     *
     * @param unionId
     * @param unionRoomCfgList
     * @param isHideStartRoom
     * @param pageNum
     * @param sort
     * @param gameType
     * @return
     */
    public List<RoomInfoItemShortTwo> onUnionGetAllRoomShortTwo(long unionId, List<Long> unionRoomCfgList, int isHideStartRoom, int pageNum, int sort, List<Long> roomid, Integer gameType) {
        if (unionId <= 0L) {
            return Collections.emptyList();
        }
        if (Config.isShare()) {
            return ShareRoomMgr.getInstance().roomUnionValues(RoomTypeEnum.UNION, unionId, unionRoomCfgList, isHideStartRoom, pageNum, sort, gameType).stream().map(k -> getRoomInfoItemShortTwo(unionId, k)).filter(k -> Objects.nonNull(k.getRoomKey()) && !k.getRoomKey().trim().isEmpty() && roomid.contains(Long.parseLong(k.getRoomKey()))).collect(Collectors.toList());
        } else {
            return NormalRoomMgr.getInstance().roomUnionValues(RoomTypeEnum.UNION, unionId, unionRoomCfgList, isHideStartRoom, pageNum, sort, gameType).stream().map(k -> getRoomInfoItemShortTwo(unionId, k)).filter(k -> Objects.nonNull(k.getRoomKey()) && !k.getRoomKey().trim().isEmpty() && roomid.contains(Long.parseLong(k.getRoomKey()))).collect(Collectors.toList());
        }
    }

    public RoomInfoItem getRoomInfoItem(long clubID, ShareRoom room) {
        RoomInfoItem roomInfoItem = new RoomInfoItem();
        roomInfoItem.setRoomName(room.getRoomName());
        roomInfoItem.setRoomKey(room.getRoomKey());
        roomInfoItem.setGameId(room.getGameId());
        roomInfoItem.setSetCount(room.getSetCount());
        roomInfoItem.setPlayerNum(room.getPlayerNum());
        roomInfoItem.setSort(room.sorted());
        roomInfoItem.setId(clubID);
        roomInfoItem.setClose(false);
        roomInfoItem.setTagId(room.getTagId());
        roomInfoItem.setPassword(room.getPassword());
        roomInfoItem.setRoomSportsThreshold(room.getBaseRoomConfigure().getBaseCreateRoom().getRoomSportsThreshold());
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
     * 获取房间信息实体类
     * 简化版一
     *
     * @param clubID
     * @param room
     * @return
     */
    public RoomInfoItemShortOne getRoomInfoItemShortOne(long clubID, ShareRoom room) {
        RoomInfoItemShortOne roomInfoItem = new RoomInfoItemShortOne();
        roomInfoItem.setRoomKey(room.getRoomKey());
        roomInfoItem.setGameId(room.getGameId());
        roomInfoItem.setPlayerNum(room.getPlayerNum());
        roomInfoItem.setSort(room.sorted());
        roomInfoItem.setTagId(room.getTagId());
        if (Objects.nonNull(room.getBaseRoomConfigure()) && Objects.nonNull(room.getBaseRoomConfigure().getBaseCreateRoom())) {
            roomInfoItem.setSportsDouble(room.getBaseRoomConfigure().getBaseCreateRoom().getSportsDouble());
        }
        if (room.isNoneRoom()) {

        } else {
            roomInfoItem.setSetId(room.getSetId());
        }

        return roomInfoItem;
    }

    /**
     * 获取房间信息实体类
     * 简化版二
     *
     * @param clubID
     * @param room
     * @return
     */
    public RoomInfoItemShortTwo getRoomInfoItemShortTwo(long clubID, ShareRoom room) {
        RoomInfoItemShortTwo roomInfoItem = new RoomInfoItemShortTwo();
        roomInfoItem.setId(clubID);
        roomInfoItem.setRoomName(room.getRoomName());
        roomInfoItem.setRoomKey(room.getRoomKey());
        roomInfoItem.setSetCount(room.getSetCount());
        roomInfoItem.setPassword(room.getPassword());
        roomInfoItem.setRoomSportsThreshold(room.getBaseRoomConfigure().getBaseCreateRoom().getRoomSportsThreshold());
        if (room.isNoneRoom()) {

        } else {

            roomInfoItem.setPosList(JsonUtil.jsonToBeanList(JsonUtil.toJson(room.getPosList()), RoomPosInfoShort.class));
        }
        return roomInfoItem;
    }

    /**
     * 获取赛事房间
     *
     * @param unionId
     * @param room
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RoomInfoItem getRoomInfoItem(long unionId, RoomImpl room) {
        RoomInfoItem roomInfoItem = new RoomInfoItem();
        roomInfoItem.setRoomName(room.getBaseRoomConfigure().getBaseCreateRoom().getRoomName());
        roomInfoItem.setRoomKey(room.getRoomKey());
        roomInfoItem.setGameId(room.getBaseRoomConfigure().getGameType().getId());
        roomInfoItem.setSetCount(room.getBaseRoomConfigure().getBaseCreateRoom().getSetCount());
        roomInfoItem.setPlayerNum(room.getBaseRoomConfigure().getBaseCreateRoom().getPlayerNum());
        roomInfoItem.setSort(room.sorted());
        roomInfoItem.setId(unionId);
        roomInfoItem.setClose(false);
        roomInfoItem.setTagId(room.getBaseRoomConfigure().getTagId());
        roomInfoItem.setPassword(room.getBaseRoomConfigure().getBaseCreateRoom().getPassword());
        roomInfoItem.setRoomSportsThreshold(room.getBaseRoomConfigure().getBaseCreateRoom().getRoomSportsThreshold());
        if (room.isNoneRoom()) {
            roomInfoItem.setCreateTime(CommTime.nowSecond());
        } else {
            AbsBaseRoom absRoom = (AbsBaseRoom) room;
            roomInfoItem.setSetId(absRoom.getCurSetID());
            roomInfoItem.setPosList(absRoom.getRoomPosMgr().getRoomPosInfoShortList());
            roomInfoItem.setCreateTime(absRoom.getTask().getCreateSec());
            roomInfoItem.setRoomId(absRoom.getRoomID());
        }
        return roomInfoItem;
    }

    /**
     * 获取赛事房间
     * <p>
     * 简化版
     *
     * @param unionId
     * @param room
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RoomInfoItemShortOne getRoomInfoItemShortOne(long unionId, RoomImpl room) {
        RoomInfoItemShortOne roomInfoItem = new RoomInfoItemShortOne();
        roomInfoItem.setRoomKey(room.getRoomKey());
        roomInfoItem.setGameId(room.getBaseRoomConfigure().getGameType().getId());
        roomInfoItem.setPlayerNum(room.getBaseRoomConfigure().getBaseCreateRoom().getPlayerNum());
        roomInfoItem.setSort(room.sorted());
        roomInfoItem.setTagId(room.getBaseRoomConfigure().getTagId());
        if (room.isNoneRoom()) {
        } else {
            AbsBaseRoom absRoom = (AbsBaseRoom) room;
            roomInfoItem.setSetId(absRoom.getCurSetID());
        }
        return roomInfoItem;
    }

    /**
     * 获取赛事房间
     * 简化版
     *
     * @param unionId
     * @param room
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RoomInfoItemShortTwo getRoomInfoItemShortTwo(long unionId, RoomImpl room) {
        RoomInfoItemShortTwo roomInfoItem = new RoomInfoItemShortTwo();
        roomInfoItem.setRoomName(room.getBaseRoomConfigure().getBaseCreateRoom().getRoomName());
        roomInfoItem.setSetCount(room.getBaseRoomConfigure().getBaseCreateRoom().getSetCount());
        roomInfoItem.setId(unionId);
        roomInfoItem.setPassword(room.getBaseRoomConfigure().getBaseCreateRoom().getPassword());
        roomInfoItem.setRoomKey(room.getRoomKey());
        if (room.isNoneRoom()) {
        } else {
            AbsBaseRoom absRoom = (AbsBaseRoom) room;
            roomInfoItem.setPosList(absRoom.getRoomPosMgr().getRoomPosInfoShortList());
        }
        return roomInfoItem;
    }

    /**
     * 获取赛事房间详情
     *
     * @param uniondId
     * @param roomKey
     * @return
     */
    public SData_Result getRoomInfoDetails(long uniondId, long clubId, long pid, String roomKey) {
        if (Config.isShare()) {
            if (StringUtils.isEmpty(roomKey)) {
                return SData_Result.make(ErrorCode.Room_NOT_Find, "Room_NOT_Find roomKey:{null}");
            }
            ShareRoom room = ShareRoomMgr.getInstance().getShareRoomByKey(roomKey);
            if (null == room) {
                return SData_Result.make(ErrorCode.Room_NOT_Find, "Room_NOT_Find roomKey:{%s}", roomKey);
            }
            if (!RoomTypeEnum.UNION.equals(room.getRoomTypeEnum()) || room.getSpecialRoomId() != uniondId) {
                return SData_Result.make(ErrorCode.NotAllow, "RoomInfoDetails RoomTypeEnum:{%s},UniondId:{%d}", room.getRoomTypeEnum(), room.getSpecialRoomId());
            }
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(uniondId);
            if (null == union) {
                return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
            }
            return SData_Result.make(ErrorCode.Success, this.getRoomInfoDetails(uniondId, clubId, UnionMgr.getInstance().getUnionMemberMgr().getMinister(pid, clubId, uniondId), union.getUnionBO().getName(), room));
        } else {
            if (StringUtils.isEmpty(roomKey)) {
                return SData_Result.make(ErrorCode.Room_NOT_Find, "Room_NOT_Find roomKey:{null}");
            }
            RoomImpl room = NormalRoomMgr.getInstance().getNoneRoomByKey(roomKey);
            if (null == room) {
                return SData_Result.make(ErrorCode.Room_NOT_Find, "Room_NOT_Find roomKey:{%s}", roomKey);
            }
            if (!RoomTypeEnum.UNION.equals(room.getRoomTypeEnum()) || room.getSpecialRoomId() != uniondId) {
                return SData_Result.make(ErrorCode.NotAllow, "RoomInfoDetails RoomTypeEnum:{%s},UniondId:{%d}", room.getRoomTypeEnum(), room.getSpecialRoomId());
            }
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(uniondId);
            if (null == union) {
                return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
            }
            return SData_Result.make(ErrorCode.Success, this.getRoomInfoDetails(uniondId, clubId, UnionMgr.getInstance().getUnionMemberMgr().getMinister(pid, clubId, uniondId), union.getUnionBO().getName(), room));
        }
    }

    /**
     * 获取赛事房间详情
     *
     * @param unionId
     * @param room
     * @return
     */
    @SuppressWarnings({})
    private RoomInfoDetails getRoomInfoDetails(long unionId, long clubId, int isManage, String name, ShareRoom room) {
        RoomInfoDetails roomInfoDetails = new RoomInfoDetails();
        roomInfoDetails.setName(name);
        roomInfoDetails.setRoomName(room.getRoomName());
        roomInfoDetails.setRoomKey(room.getRoomKey());
        roomInfoDetails.setGameId(room.getGameId());
        roomInfoDetails.setSetCount(room.getSetCount());
        roomInfoDetails.setPlayerNum(room.getPlayerNum());
        roomInfoDetails.setRoomCfg(new Gson().fromJson(room.getBaseRoomConfigure().getShareBaseCreateRoom(), Map.class));
        roomInfoDetails.setRoomStateId(room.getRoomState().value());
        roomInfoDetails.setUnionId(unionId);
        roomInfoDetails.setClubId(clubId);
        roomInfoDetails.setIsManage(isManage);
        roomInfoDetails.setTagId(room.getBaseRoomConfigure().getTagId());
        if (room.isNoneRoom()) {
            roomInfoDetails.setCreateTime(CommTime.nowSecond());
        } else {
            roomInfoDetails.setSetId(room.getSetId());
            roomInfoDetails.setPosList(JsonUtil.jsonToBeanList(JsonUtil.toJson(room.getPosList()), RoomPosInfoShort.class));
            roomInfoDetails.setCreateTime(room.getCreateTime());
        }
        return roomInfoDetails;
    }

    /**
     * 获取赛事房间详情
     *
     * @param unionId
     * @param room
     * @return
     */
    @SuppressWarnings({})
    private RoomInfoDetails getRoomInfoDetails(long unionId, long clubId, int isManage, String name, RoomImpl room) {
        RoomInfoDetails roomInfoDetails = new RoomInfoDetails();
        roomInfoDetails.setName(name);
        roomInfoDetails.setRoomName(room.getBaseRoomConfigure().getBaseCreateRoom().getRoomName());
        roomInfoDetails.setRoomKey(room.getRoomKey());
        roomInfoDetails.setGameId(room.getBaseRoomConfigure().getGameType().getId());
        roomInfoDetails.setSetCount(room.getBaseRoomConfigure().getBaseCreateRoom().getSetCount());
        roomInfoDetails.setPlayerNum(room.getBaseRoomConfigure().getBaseCreateRoom().getPlayerNum());
        roomInfoDetails.setRoomCfg(room.getBaseRoomConfigure().getBaseCreateRoomT());
        roomInfoDetails.setRoomStateId(room.getRoomState().value());
        roomInfoDetails.setUnionId(unionId);
        roomInfoDetails.setClubId(clubId);
        roomInfoDetails.setIsManage(isManage);
        roomInfoDetails.setTagId(room.getBaseRoomConfigure().getTagId());
        if (room.isNoneRoom()) {
            roomInfoDetails.setCreateTime(CommTime.nowSecond());
        } else {
            AbsBaseRoom absRoom = (AbsBaseRoom) room;
            roomInfoDetails.setSetId(absRoom.getCurSetID());
            roomInfoDetails.setPosList(absRoom.getRoomPosMgr().getRoomPosInfoShortList());
            roomInfoDetails.setCreateTime(absRoom.getTask().getCreateSec());
        }
        return roomInfoDetails;
    }

    /**
     * 赛事查询Pid
     * 竞技点动态
     *
     * @param pid
     * @param pageNum
     * @param getType
     * @return
     */
    public List<UnionDynamicItem> unionDynamicByPid(long pid, long clubId, long unoinId, int pageNum, int getType, int chooseType, long ePid) {
        List<Integer> chooseTypeList = getUnionDynamicChooseType(chooseType);
        // 检查指定亲友圈动态数据是否存在
        List<UnionDynamicBO> dynamicBOs = ContainerMgr.get().getComponent(UnionDynamicBOService.class)
                .findListUnion(getPidSqlQuerr(pid, clubId, unoinId, getType, chooseTypeList, false), getPidSqlQuerr(pid, clubId, unoinId, getType, chooseTypeList, true),
                        Restrictions.and(
                                Restrictions.eq("pid", pid),
                                Restrictions.eq("clubId", clubId),
                                Restrictions.eq("unionId", unoinId),
                                Restrictions.in("execType", chooseTypeList),
                                TimeConditionUtils.DayZeroClockS(getType),
                                Restrictions.eq("execPid", pid),
                                Restrictions.eq("clubId", clubId),
                                Restrictions.eq("unionId", unoinId),
                                Restrictions.in("execType", chooseTypeList),
                                TimeConditionUtils.DayZeroClockS(getType)),
                        Restrictions.and().desc("execTime").setPageNum(Page.getPageNum(pageNum, Page.PAGE_SIZE_100)).setPageSize(Page.PAGE_SIZE_100), UnionDynamicBO.class);
        if (CollectionUtils.isEmpty(dynamicBOs)) {
            return Collections.emptyList();
        }
        return getUnionDynamicList(dynamicBOs.stream().distinct().collect(Collectors.toList()), ePid);
    }

    /**
     * 赛事查询Pid
     * 显示该玩家的个人积分变化记录
     * 竞技点动态
     *
     * @param pid
     * @param pageNum
     * @param getType
     * @return
     */
    public List<UnionDynamicItemZhongZhiRecord> unionDynamicRecordByPid(long pid, long clubId, long unoinId, int pageNum, int getType, int chooseType, long ePid) {

        List<Integer> chooseTypeList = getUnionDynamicRecordChooseType(chooseType);
        // 检查指定亲友圈动态数据是否存在
        List<UnionDynamicBO> dynamicBOs = ContainerMgr.get().getComponent(UnionDynamicBOService.class)
                .findListUnion(getPidSqlQuerrRecordZhongZhi(pid, clubId, unoinId, getType, chooseTypeList, false), getPidSqlQuerrRecordZhongZhi(pid, clubId, unoinId, getType, chooseTypeList, true),
                        Restrictions.and(
                                Restrictions.eq("pid", pid),
                                Restrictions.eq("clubId", clubId),
                                Restrictions.eq("unionId", unoinId),
                                Restrictions.in("execType", chooseTypeList),
                                TimeConditionUtils.CLUBDayZeroClockSZhongZhiBetween("execTime", getType),
                                Restrictions.eq("execPid", pid),
                                Restrictions.eq("clubId", clubId),
                                Restrictions.eq("unionId", unoinId),
                                Restrictions.in("execType", chooseTypeList),
                                TimeConditionUtils.CLUBDayZeroClockSZhongZhiBetween("execTime", getType)),
                        Restrictions.and().desc("execTime").setPageNum(Page.getPageNum(pageNum, Page.PAGE_SIZE_100)).setPageSize(Page.PAGE_SIZE_100), UnionDynamicBO.class);
        if (CollectionUtils.isEmpty(dynamicBOs)) {
            return Collections.emptyList();
        }
        return getUnionDynamicListZhongZhiRecord(dynamicBOs.stream().distinct().collect(Collectors.toList()), ePid);
    }

    /**
     * 个人积分变化记录
     *
     * @param player
     * @param clubId
     * @param unoinId
     * @param pageNum
     * @param getType
     * @param chooseType
     * @param ePid
     * @return
     */
    public SData_Result getCompetitionRecord(Player player, long clubId, long unoinId, int pageNum, int getType, int chooseType, long ePid, long pid) {
        List<UnionDynamicItemZhongZhiRecord> unionDynamicItemList = UnionMgr.getInstance().unionDynamicRecordByPid(pid, clubId, unoinId, pageNum, getType, chooseType, pid);
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), clubId);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "CLUB_NOT_EXIST_MEMBER_INFO getCompetitionRecord ");
        }
        ClubCompetitionRecord record = new ClubCompetitionRecord();
        unionDynamicItemList.stream().forEach(k -> changeItemInfo(k));
        record.setPlayer(player.getShortPlayer());
        record.setUnionDynamicItemList(unionDynamicItemList);
        record.setPageNum(pageNum);
        record.setEliminatePoint(clubMember.getClubMemberBO().getEliminatePoint());
        if (getType == 0) {
            ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubGeneral(clubId, clubMember.getId(), clubMember.getClubMemberBO().getUpLevelId(), clubMember.getLevel());
            record.setZhongZhiTotalPoint(CommMath.subDouble(clubPromotionLevelItem.getSportsPointConsume(), clubPromotionLevelItem.getActualEntryFee()));
            record.setPlayerTotalPoint(CommMath.subDouble(clubPromotionLevelItem.getSportsPointConsume(), clubPromotionLevelItem.getActualEntryFee()));
        } else {
            Criteria zeroClockSZhongZhi = TimeConditionUtils.CLUBDayZeroClockSZhongZhi("date_time", getType);
            ClubPromotionLevelReportFormItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", clubId), Restrictions.eq("memberId", clubMember.getId())), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
            record.setZhongZhiTotalPoint(CommMath.subDouble(clubPromotionLevelItem.getSportsPointConsume(), clubPromotionLevelItem.getActualEntryFee()));
            record.setPlayerTotalPoint(CommMath.subDouble(clubPromotionLevelItem.getSportsPointConsume(), clubPromotionLevelItem.getActualEntryFee()));
        }
        return SData_Result.make(ErrorCode.Success, record);

    }

    /**
     * 竞技动态信息处理
     *
     * @param k
     */
    private void changeItemInfo(UnionDynamicItemZhongZhiRecord k) {
        int oldExecType = k.getExecType();
        k.setExecType(getUnionDynamicRecordChooseTypeFenLei(k.getExecType()));
        k.setPidCurValue(String.valueOf(CommMath.addDouble(Double.parseDouble(k.getWinLoseValue()), Double.parseDouble(k.getConsumeValue()))));
        UnionDefine.UNION_EXEC_TYPE chooseTypeList = UnionDefine.UNION_EXEC_TYPE.valueOf(oldExecType);
        switch (chooseTypeList) {
            case UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_MINUS:
            case UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_ADD:
                k.setConsumeValue(k.getWinLoseValue());
                k.setWinLoseValue("0");
                break;
        }
    }

    /**
     * 拼接查询的sql语句
     *
     * @param pid
     * @param clubId
     * @param unoinId
     * @param getType
     * @param chooseTypeList
     * @param execFlag
     * @return
     */
    public String getPidSqlQuerr(long pid, long clubId, long unoinId, int getType, List<Integer> chooseTypeList, boolean execFlag) {
        return ContainerMgr.get().getComponent(UnionDynamicBOService.class).findListUnionSql(Restrictions.and(
                execFlag ? Restrictions.eq("pid", pid) : Restrictions.eq("execPid", pid),
                Restrictions.eq("clubId", clubId),
                Restrictions.eq("unionId", unoinId),
                Restrictions.in("execType", chooseTypeList),
                TimeConditionUtils.DayZeroClockS(getType)));

    }

    public String getPidSqlQuerrRecordZhongZhi(long pid, long clubId, long unoinId, int getType, List<Integer> chooseTypeList, boolean execFlag) {
        return ContainerMgr.get().getComponent(UnionDynamicBOService.class).findListUnionSql(Restrictions.and(
                execFlag ? Restrictions.eq("pid", pid) : Restrictions.eq("execPid", pid),
                Restrictions.eq("clubId", clubId),
                Restrictions.eq("unionId", unoinId),
                Restrictions.in("execType", chooseTypeList),
                TimeConditionUtils.CLUBDayZeroClockSZhongZhiBetween("execTime", getType)));

    }

    /**
     * 根据传进来的类型 筛选出全部的类型
     *
     * @param chooseType
     * @return
     */
    private List<Integer> getUnionDynamicChooseType(int chooseType) {
        List<Integer> chooseTypeList = new ArrayList<>();
        UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE join = UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE.valueOf(chooseType);
        switch (join) {
            case ERROR:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_EXE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_EXE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_OP.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_OP.value());
                break;
            case WINLOSE:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SPORTS_POINT_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_CLUB_EXEC_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_CLUB_EXEC_SPORTS_POINT_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_MINUS.value());
                break;
            case ENTRYFEE:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PROMOTION_SHARE_INCOME.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PROMOTION_SHARE_INCOME_CASEPOINT.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SHARE_INCOME.value());
                break;
            case XiPaiCost:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_QIEPAI_INCOME.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_QIEPAI_CONSUME.value());
                break;

            case FenChengChange:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(),
                        UnionDefine.UNION_EXEC_TYPE.PROMOTION_EXEC_SHARE_SECTION_CHANGE.value(),
                        UnionDefine.UNION_EXEC_TYPE.PROMOTION_EXEC_SHARE_SECTION_ALLOW_CHANGE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SHARE_SECTION.value());
                break;
            case CasePointChange:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.PLAYER_CASE_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.PLAYER_CASE_SPORTS_POINT_SUB.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_CASE_SPORTS_POINT_CLOSE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_CASE_SPORTS_POINT_TICHU.value());
                break;
            case ALIVE_MISSION:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_CLOSE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_OPEN.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_CHANGE.value());
                break;
            case TaoTaiFen:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.CLUB_ELIMINATE_POINT_CHANGE.value());
                break;
            case KuaJi:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.CLUB_KUAJI_SPOINTCHANGE_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.CLUB_KUAJI_SPOINTCHANGE_SUB.value());
                break;
            case RenYuanBianDong:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.CLUB_ZHI_SHU_TICHU.value(),
                        UnionDefine.UNION_EXEC_TYPE.CLUB_ZHI_SHU_JIARU.value(),
                        UnionDefine.UNION_EXEC_TYPE.CLUB_ZHI_SHU_CHANGE_BELONG.value());
                break;
            case ALL:
            default:
                chooseTypeList = Arrays.asList(
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SPORTS_POINT_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_CLUB_EXEC_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_CLUB_EXEC_SPORTS_POINT_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PROMOTION_SHARE_INCOME.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SHARE_INCOME.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_QIEPAI_CONSUME.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_QIEPAI_INCOME.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_ADD_SELF.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS_SELF.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD_SELF.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS_SELF.value(),
                        UnionDefine.UNION_EXEC_TYPE.PLAYER_CASE_SPORTS_POINT_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.PLAYER_CASE_SPORTS_POINT_SUB.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_CASE_SPORTS_POINT_CLOSE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_CASE_SPORTS_POINT_TICHU.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PERSONAL_SPORTS_WARNING_CLOSE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PERSONAL_SPORTS_WARNING_CHANGE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SPORTS_WARNING_CLOSE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SPORTS_WARNING_CHANGE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_EXE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_EXE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_OP.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_OP.value(),
                        UnionDefine.UNION_EXEC_TYPE.PROMOTION_EXEC_SHARE_SECTION_CHANGE.value(),
                        UnionDefine.UNION_EXEC_TYPE.PROMOTION_EXEC_SHARE_SECTION_ALLOW_CHANGE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PROMOTION_SHARE_INCOME_CASEPOINT.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SHARE_SECTION.value(),
                        UnionDefine.UNION_EXEC_TYPE.CLUB_ELIMINATE_POINT_CHANGE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_CLOSE.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_OPEN.value(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_CHANGE.value(),
                        UnionDefine.UNION_EXEC_TYPE.CLUB_KUAJI_SPOINTCHANGE_ADD.value(),
                        UnionDefine.UNION_EXEC_TYPE.CLUB_ZHI_SHU_TICHU.value(),
                        UnionDefine.UNION_EXEC_TYPE.CLUB_ZHI_SHU_JIARU.value(),
                        UnionDefine.UNION_EXEC_TYPE.CLUB_ZHI_SHU_CHANGE_BELONG.value(),
                        UnionDefine.UNION_EXEC_TYPE.CLUB_KUAJI_SPOINTCHANGE_SUB.value());

                break;

        }
        return chooseTypeList;

    }

    /**
     * 根据传进来的类型 筛选出全部的类型
     *
     * @param chooseType
     * @return
     */
    private List<Integer> getUnionDynamicRecordChooseType(int chooseType) {
        List<Integer> chooseTypeList = new ArrayList<>();
        chooseTypeList.addAll(Arrays.asList(
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_ADD.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_EXE.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_EXE.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_OP.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_OP.value()));

        chooseTypeList.addAll(Arrays.asList(
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SPORTS_POINT_ADD.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SPORTS_POINT_MINUS.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_CLUB_EXEC_SPORTS_POINT_ADD.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_CLUB_EXEC_SPORTS_POINT_MINUS.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ADD.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_MINUS.value()));

        chooseTypeList.addAll(Arrays.asList(
                UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_MINUS.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_ADD.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PROMOTION_SHARE_INCOME.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SHARE_INCOME.value()));
        chooseTypeList.addAll(Arrays.asList(
                UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_QIEPAI_INCOME.value(),
                UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_QIEPAI_CONSUME.value()));

        return chooseTypeList;

    }

    /**
     * 根据传进来的类型 筛选出全部的类型
     *
     * @return
     */
    private int getUnionDynamicRecordChooseTypeFenLei(int chooseType) {
        UnionDefine.UNION_EXEC_TYPE chooseTypeList = UnionDefine.UNION_EXEC_TYPE.valueOf(chooseType);
        switch (chooseTypeList) {
            case UNION_EXEC_EMPOWER_SPORTS_POINT_ADD:
            case UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS:
            case UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD:
            case UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS:
            case UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_EXE:
            case UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_EXE:
            case UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_OP:
            case UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_OP:
                return UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE.ERROR.value();
            case UNION_EXEC_SPORTS_POINT_ADD:
            case UNION_EXEC_SPORTS_POINT_MINUS:
            case UNION_CLUB_EXEC_SPORTS_POINT_ADD:
            case UNION_CLUB_EXEC_SPORTS_POINT_MINUS:
            case UNION_ROOM_EXEC_SPORTS_POINT_ADD:
            case UNION_ROOM_EXEC_SPORTS_POINT_MINUS:
                return UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE.WINLOSE.value();
            case UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_MINUS:
            case UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_ADD:
            case UNION_EXEC_PROMOTION_SHARE_INCOME:
            case UNION_EXEC_SHARE_INCOME:
                return UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE.ENTRYFEE.value();
            case UNION_ROOM_QIEPAI_INCOME:
            case UNION_ROOM_QIEPAI_CONSUME:
                return UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE.XiPaiCost.value();

        }
        return 0;

    }

    /**
     * 赛事动态
     *
     * @param clubId  亲友圈Id
     * @param pageNum pageNum 第几页
     */
    public List<UnionDynamicItem> clubDynamic(long clubId, int pageNum, int getType, long pid, long execPid, long unionId, long ePid) {
        // 检查指定亲友圈动态数据是否存在
        List<UnionDynamicBO> dynamicBOs = ContainerMgr.get().getComponent(UnionDynamicBOService.class)
                .findAll(
                        Restrictions.and(Restrictions.eq("clubId", clubId),
                                        unionId <= 0L ? Restrictions.eq("unionId", 0L) : Restrictions.in("unionId", Arrays.asList(0, unionId)),
                                        pid <= 0L ? null : Restrictions.eq("pid", pid),
                                        execPid <= 0L ? null : Restrictions.eq("execPid", execPid),
                                        Restrictions.eq("type", 1),
                                        TimeConditionUtils.DayZeroClockS(getType))
                                .desc("execTime")
                                .setPageNum(Page.getPageNum(1, Page.PAGE_SIZE_400))
                                .setPageSize(Page.PAGE_SIZE_400));
        if (CollectionUtils.isEmpty(dynamicBOs)) {
            return Collections.emptyList();
        }
        return getUnionDynamicList(dynamicBOs, ePid);
    }


    /**
     * 赛事动态
     *
     * @param unionId 赛事Id
     * @param pageNum pageNum 第几页
     */
    public List<UnionDynamicItem> unionDynamic(long unionId, int pageNum, int getType, long pid, long execPid, long ePid) {
        // 检查指定亲友圈动态数据是否存在
        List<UnionDynamicBO> dynamicBOs = ContainerMgr.get().getComponent(UnionDynamicBOService.class)
                .findAll(
                        Restrictions.and(Restrictions.eq("unionId", unionId),
                                        pid <= 0L ? null : Restrictions.eq("pid", pid),
                                        execPid <= 0L ? null : Restrictions.eq("execPid", execPid),
                                        Restrictions.eq("type", 0),
                                        TimeConditionUtils.DayZeroClockS(getType))
                                .desc("execTime")
                                .setPageNum(Page.getPageNum(pageNum, Page.PAGE_SIZE_30))
                                .setPageSize(Page.PAGE_SIZE_30));
        if (CollectionUtils.isEmpty(dynamicBOs)) {
            return Collections.emptyList();
        }
        return getUnionDynamicList(dynamicBOs, ePid);
    }

    /**
     * 普通的亲友圈创建者赛事动态
     * 	普通的亲友圈创建者只能看到自己被修改的消息，而不能看到其他亲友圈创建者被修改的消息；
     *
     * @param unionId 赛事Id
     * @param pageNum pageNum 第几页
     */
    public List<UnionDynamicItem> unionDynamicByClubCreate(long clubId, long unionId, int pageNum, int getType, long pid, long execPid, long ePid) {
        // 检查指定亲友圈动态数据是否存在
        List<UnionDynamicBO> dynamicBOs = ContainerMgr.get().getComponent(UnionDynamicBOService.class)
                .findAll(
                        Restrictions.and(Restrictions.eq("unionId", unionId),
                                        pid <= 0L ? null : Restrictions.eq("pid", pid),
                                        execPid <= 0L ? null : Restrictions.eq("execPid", execPid),
                                        Restrictions.eq("type", 0),
                                        TimeConditionUtils.DayZeroClockS(getType))
                                .desc("execTime")
                                .setPageNum(Page.getPageNum(pageNum, Page.PAGE_SIZE_30))
                                .setPageSize(Page.PAGE_SIZE_30));
        if (CollectionUtils.isEmpty(dynamicBOs)) {
            return Collections.emptyList();
        }
        List<UnionDynamicBO> dynamicBOS = dynamicBOs.stream().filter(k -> k.getClubId() == clubId).collect(Collectors.toList());
        return getUnionDynamicList(dynamicBOS, ePid);
    }

    /**
     * 获取赛事动态列表
     *
     * @param dynamicBOs
     * @return
     */
    public List<UnionDynamicItem> getUnionDynamicList(List<UnionDynamicBO> dynamicBOs, long execPid) {
        return dynamicBOs.stream().map(k -> {
                    Player player = null;
                    if (UnionDefine.UNION_EXEC_TYPE.isShowClubName(k.getExecType())) {
                        // 显示亲友圈名称和亲友圈Id
                        Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getClubId());
                        if (Objects.isNull(club)) {
                            return null;
                        } else {
                            k.setClubName(club.getClubListBO().getName());
                            k.setClubSign(club.getClubListBO().getClubsign());
                        }
                    }
                    if (UnionDefine.UNION_EXEC_TYPE.isShowClubName(k.getExecType())) {
                        // 显示亲友圈名称和亲友圈Id
                        Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getExecClubId());
                        if (Objects.isNull(club)) {
                            return null;
                        } else {
                            k.setExecClubName(club.getClubListBO().getName());
                            k.setExecClubSign(club.getClubListBO().getClubsign());
                        }
                    }
                    if (UnionDefine.UNION_EXEC_TYPE.isPlayerName(k.getExecType())) {
                        player = PlayerMgr.getInstance().getPlayer(k.getPid());
                        if (Objects.isNull(player)) {
                            return null;
                        } else {
                            k.setName(player.getName());
                        }
                    }

                    if (k.getExecPid() > 0L) {
                        player = PlayerMgr.getInstance().getPlayer(k.getExecPid());
                        if (Objects.isNull(player)) {
                            return null;
                        }
                        k.setExecName(player.getName());
                        if (UnionDefine.UNION_EXEC_TYPE.isShowPreValue(k.getExecType()) && k.getExecPid() != execPid) {
                            k.setPreValue(null);
                        }
                    }
                    return new UnionDynamicItem(k.getId(), k.getClubSign(), k.getPid(), k.getExecPid(), k.getExecTime(), k.getExecType(), k.getValue(), k.getName(), k.getExecName(), k.getClubName(), k.getClubId(), k.getCurValue(), k.getPreValue(), k.getRoomKey(), k.getMsg(), k.getExecClubName(), k.getClubSign(), k.getPidCurValue(), k.getPidPreValue(), k.getPidValue(), k.getExecPidCurValue(), k.getExecPidPreValue(), k.getExecPidValue());
                })
                .filter(k -> Objects.nonNull(k))
                .collect(Collectors.toList());
    }

    /**
     * 获取赛事动态列表
     *
     * @param dynamicBOs
     * @return
     */
    public List<UnionDynamicItemZhongZhiRecord> getUnionDynamicListZhongZhiRecord(List<UnionDynamicBO> dynamicBOs, long execPid) {
        return dynamicBOs.stream().map(k -> {
                    Player player = null;
                    if (UnionDefine.UNION_EXEC_TYPE.isShowClubName(k.getExecType())) {
                        // 显示亲友圈名称和亲友圈Id
                        Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getClubId());
                        if (Objects.isNull(club)) {
                            return null;
                        } else {
                            k.setClubName(club.getClubListBO().getName());
                            k.setClubSign(club.getClubListBO().getClubsign());
                        }
                    }
                    if (UnionDefine.UNION_EXEC_TYPE.isShowClubName(k.getExecType())) {
                        // 显示亲友圈名称和亲友圈Id
                        Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getExecClubId());
                        if (Objects.isNull(club)) {
                            return null;
                        } else {
                            k.setExecClubName(club.getClubListBO().getName());
                            k.setExecClubSign(club.getClubListBO().getClubsign());
                        }
                    }
                    if (UnionDefine.UNION_EXEC_TYPE.isPlayerName(k.getExecType())) {
                        player = PlayerMgr.getInstance().getPlayer(k.getPid());
                        if (Objects.isNull(player)) {
                            return null;
                        } else {
                            k.setName(player.getName());
                        }
                    }

                    if (k.getExecPid() > 0L) {
                        player = PlayerMgr.getInstance().getPlayer(k.getExecPid());
                        if (Objects.isNull(player)) {
                            return null;
                        }
                        k.setExecName(player.getName());
                        if (UnionDefine.UNION_EXEC_TYPE.isShowPreValue(k.getExecType()) && k.getExecPid() != execPid) {
                            k.setPreValue(null);
                        }
                    }
                    return new UnionDynamicItemZhongZhiRecord(k.getId(), k.getExecTime(), k.getExecType(), k.getValue(), "0", k.getMsg().isEmpty() ? 0D : Double.parseDouble(k.getMsg()), k.getPidCurValue());
                })
                .filter(k -> Objects.nonNull(k))
                .collect(Collectors.toList());
    }


    /**
     * 亲友圈赛事通知
     *
     * @return
     */
    public List<SUnion_Invited> getUnionNotify(long pid) {
        List<SUnion_Invited> unionInvitedList = new ArrayList<>();
        List<UnionNotifyBO> unionNotifyBOList = ContainerMgr.get().getComponent(UnionNotifyBOService.class).findAll(Restrictions.eq("pid", pid));
        if (CollectionUtils.isEmpty(unionNotifyBOList)) {
            return Lists.newArrayList();
        }
        // 亲友圈
        Club club = null;
        // 操作者
        Player execPlayer = null;
        // 赛事信息
        Union union = null;
        for (UnionNotifyBO notifyBO : unionNotifyBOList) {
            union = UnionMgr.getInstance().getUnionListMgr().findUnion(notifyBO.getUnionId());
            if (null == union) {
                // 跳过找不到赛事
                notifyBO.getBaseService().delete(notifyBO.getId(), new AsyncInfo(notifyBO.getId()));
                continue;
            }
            club = ClubMgr.getInstance().getClubListMgr().findClub(notifyBO.getClubId());
            if (null == club) {
                // 跳过找不到亲友圈
                notifyBO.getBaseService().delete(notifyBO.getId(), new AsyncInfo(notifyBO.getId()));
                continue;
            }
            if (notifyBO.getExecPid() > 0L) {
                execPlayer = PlayerMgr.getInstance().getPlayer(notifyBO.getExecPid());
                if (null == execPlayer) {
                    // 跳过找不到执行者
                    notifyBO.getBaseService().delete(notifyBO.getId(), new AsyncInfo(notifyBO.getId()));
                    continue;
                }
                unionInvitedList.add(SUnion_Invited.make(execPlayer.getPid(), execPlayer.getName(), club.getClubListBO().getName(), club.getClubListBO().getClubsign(), union.getUnionBO().getName(), union.getUnionBO().getUnionSign(), notifyBO.getExecType(), notifyBO.getClubId()));
            } else {
                unionInvitedList.add(SUnion_Invited.make(club.getClubListBO().getName(), club.getClubListBO().getClubsign(), union.getUnionBO().getName(), union.getUnionBO().getUnionSign(), notifyBO.getExecType(), notifyBO.getClubId()));
            }
            notifyBO.getBaseService().delete(notifyBO.getId(), new AsyncInfo(notifyBO.getId()));
        }
        return unionInvitedList;
    }

    /**
     * 获取邀请列表
     */
    public List<SUnion_Invited> onUnionInvited(long pid) {
        return this.getUnionMemberMgr().findPidAll(pid, UnionDefine.UNION_PLAYER_STATUS.PLAYER_YAOQING).stream().filter(k -> null != k).map(k -> {
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(k.getUnionMemberBO().getUnionId());
            if (Objects.isNull(union)) {
                return null;
            }
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getUnionMemberBO().getClubId());
            if (Objects.isNull(club)) {
                return null;
            }
            Player execPlayer = PlayerMgr.getInstance().getPlayer(k.getUnionMemberBO().getInvitationPid());
            if (Objects.isNull(execPlayer)) {
                return null;
            }
            return SUnion_Invited.make(execPlayer.getPid(), execPlayer.getName(), club.getClubListBO().getName(), club.getClubListBO().getClubsign(), union.getUnionBO().getName(), union.getUnionBO().getUnionSign(), UnionDefine.UNION_NOTIFY_TYPE.UNION_NOTIFY_YAOQING.value(), k.getUnionMemberBO().getClubId());
        }).filter(k -> null != k).collect(Collectors.toList());
    }


    /**
     * 切换赛事创建者
     *
     * @param change    切换数据
     * @param newPlayer 新玩家
     * @return
     */
    public SData_Result changeCreate(CUnion_Change change, Player newPlayer) {
        UnionMember unionMember = getUnionMemberMgr().findMinister(change.getPid(), change.getClubId(), change.getUnionId(), UnionDefine.UNION_POST_TYPE.UNION_CREATE);
        if (Objects.isNull(unionMember)) {
            return SData_Result.make(ErrorCode.InvalidParam, "error unionMember");
        }
        Union union = getUnionListMgr().findUnion(change.getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.InvalidParam, "error union");
        }
        union.updateOwnerPlayer(newPlayer);
        unionMember.getUnionMemberBO().saveChangeCreate(change.getClubMemberId(), newPlayer.getPid());
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 执行赛事解散
     *
     * @param dissolve
     * @param player
     * @return
     */
    public SData_Result execUnionDissolve(CUnion_Dissolve dissolve, Player player) {
        // 获取赛事成员
        UnionMember unionMember = getUnionMemberMgr().findMinister(player.getPid(), dissolve.getClubId(), dissolve.getUnionId(), UnionDefine.UNION_POST_TYPE.UNION_CREATE);
        if (Objects.isNull(unionMember)) {
            return SData_Result.make(ErrorCode.UNION_NOT_CREATE, "error UNION_NOT_CREATE");
        }
        // 获取联赛亲友圈统计
        int count = getUnionMemberMgr().getUnionClubCount(dissolve.getUnionId());
        if (count > 1) {
            // 	解散时，如联赛内还有其他亲友圈返回弹窗确认提示：“联赛当前还有其他亲友圈无法解散”
            return SData_Result.make(ErrorCode.UNION_EXIST_CLUB, "error UNION_EXIST_CLUB");
        }
//        if (ClubMgr.getInstance().getClubMemberMgr().checkExistSportsPointNotEqualZero(dissolve.getClubId(), player.getPid())) {
//            // 	解散时，如联赛内还有其他玩家比赛分不为0，返回弹窗确认提示：“联赛有玩家比赛分不为0，无法解散”
//            return SData_Result.make(ErrorCode.UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO, "error UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO");
//        }
        if (RoomMgr.getInstance().checkExistSpecialRoom(RoomTypeEnum.UNION, dissolve.getUnionId())) {
            // 	解散时，如联赛内有正在进行中的房间，返回弹窗确认提示：“联赛当前有房间正在进行，无法解散”
            return SData_Result.make(ErrorCode.UNION_EXIST_PLAYING_ROOM, "error UNION_EXIST_PLAYING_ROOM");
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().findMinister(player.getPid(), dissolve.getClubId(), Club_define.Club_MINISTER.Club_MINISTER_CREATER);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "error CLUB_NOT_CREATE");
        }
        Union union = getUnionListMgr().findUnion(dissolve.getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "error CLUB_NOT_CREATE");
        }
        if (union.getUnionBO().getState() == UnionDefine.UNION_STATE.UNION_STATE_ENABLE.ordinal()) {
            // 总奖励值
            int sum = union.getUnionBO().getRanking() * union.getUnionBO().getValue();
            union.getOwnerPlayer().getFeature(PlayerCurrency.class).gainItemFlow(PrizeType.valueOf(union.getUnionBO().getPrizeType()), sum, ItemFlow.UNION_MATCH_REWARD, ConstEnum.ResOpType.Fallback);
        }
        // 执行竞技点清空
        clubMember.getClubMemberBO().execSportsPointClear(dissolve.getUnionId());
        unionMember.setStatus(player, union, UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value(), player.getPid());
        union.close();
        getUnionListMgr().getUnionMap().remove(dissolve.getUnionId());
        //共享赛事
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().deleteUnion(dissolve.getUnionId());
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 空桌排序在前
     *
     * @param sort   排序
     * @param player 玩家
     * @return
     */
    public SData_Result execUnionSort(CUnion_Sort sort, Player player) {
        // 获取赛事成员
        UnionMember unionMember = getUnionMemberMgr().findMinister(player.getPid(), sort.getClubId(), sort.getUnionId(), UnionDefine.UNION_POST_TYPE.UNION_CREATE);
        ClubMember exeClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(sort.getClubId(), player.getPid());
        if ((Objects.nonNull(exeClubMember) && !exeClubMember.isUnionMgr()) && Objects.isNull(unionMember)) {
            return SData_Result.make(ErrorCode.UNION_NOT_CREATE, "error UNION_NOT_CREATE");
        }
        Union union = getUnionListMgr().findUnion(sort.getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "error CLUB_NOT_CREATE");
        }
        union.getUnionBO().saveSort(sort.getSort());
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 获取赛事排行榜
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result getUnionRankingList(CUnion_RankingList req, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            // 亲友圈不存在
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (club.getClubListBO().getUnionId() != req.getUnionId()) {
            // 不是这个赛事
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            // 赛事不存在
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        if (UnionDefine.UNION_RANKING_TYPE.ROUND_CUR.ordinal() == req.getType()) {
            // 本轮排行
            return SData_Result.make(ErrorCode.Success, getRoundCurRankingList(req, union.getUnionBO().getOwnerId(), union.getUnionBO().getClubId(), pid));
        } else {
            // 上轮排行
            return SData_Result.make(ErrorCode.Success, getRoundUpRankingList(req, union.getUnionBO().getRoundId(), pid));
        }
    }

    /**
     * 本轮排行
     *
     * @param req
     * @return
     */
    private UnionMatchInfo getRoundCurRankingList(CUnion_RankingList req, long ownerPid, long ownerClubId, long pid) {
        return ClubMgr.getInstance().getClubMemberMgr().getRoundCurRankingList(getUnionMemberMgr().getUnionToClubIdList(req.getUnionId()), ownerPid, ownerClubId, pid, req.getClubId());
    }

    /**
     * 上轮排行
     *
     * @param req
     * @return
     */
    private UnionMatchInfo getRoundUpRankingList(CUnion_RankingList req, int roundId, long pid) {
        List<UnionMatchItem> unionMatchLogFlowList = ((UnionMatchLogFlowService) ContainerMgr.get().getComponent(UnionMatchLogFlowService.class.getSimpleName())).findAllE(Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), Restrictions.eq("roundId", roundId - 1)).setLimit(50), UnionMatchItem.class, UnionMatchItem.getItemsName());
        int curRankingId = unionMatchLogFlowList.stream().filter(k -> k.getPid() == pid && k.getClubId() == req.getClubId()).map(k -> k.getRankingId()).findAny().orElse(0);
        return new UnionMatchInfo(CollectionUtils.isEmpty(unionMatchLogFlowList) ? Collections.emptyList() : unionMatchLogFlowList.stream().sorted(Comparator.comparing(UnionMatchItem::getRankingId)).limit(10).collect(Collectors.toList()), curRankingId);
    }


    /**
     * 获取赛事在线人数
     *
     * @return
     */
    public SData_Result execUnionOnlinePlayerCount(CUnion_Base base) {
        String key = String.format(DataConstants.UNION_ONLINE_COUNT, base.getUnionId());
        UnionOnlineCount unionOnlineCount = EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).get(key, UnionOnlineCount.class);
        if (Objects.nonNull(unionOnlineCount)) {
            return SData_Result.make(ErrorCode.Success, unionOnlineCount.getCount());
        }
        int count = ClubMgr.getInstance().getClubMemberMgr().clubIdListToOnlinePlayerCount(getUnionMemberMgr().getUnionToClubIdList(base.getUnionId()));
        EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).put(key, new UnionOnlineCount(count));
        return SData_Result.make(ErrorCode.Success, count);
    }


    /**
     * 检查比赛分总数是否异常
     */
    public void checkSportsPointErrorLog() {
        Map<Long, Union> unionMap;
        if (Config.isShare()) {
            unionMap = ShareUnionListMgr.getInstance().getAllUnion();
        } else {
            unionMap = getUnionListMgr().getUnionMap();
        }
        for (Map.Entry<Long, Union> entry : unionMap.entrySet()) {
            List<Long> clubIdList;
            if (Config.isShare()) {
                clubIdList = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(entry.getKey()).values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == entry.getKey() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
            } else {
                clubIdList = getUnionMemberMgr().getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == entry.getKey() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
            }
            if (CollectionUtils.isEmpty(clubIdList)) {
                continue;
            }
            double sumAllClubSportsPoint = CommMath.FormatDouble(ClubMgr.getInstance().getClubMemberMgr().sumAllClubSportsPoint(clubIdList));
            double initSports = entry.getValue().getUnionBO().getInitSports();
            if (initSports != sumAllClubSportsPoint) {
                FlowLogger.sportsPointErrorLog(entry.getKey(), initSports, sumAllClubSportsPoint);
            }
        }
    }

    /**
     * 设置显示断开连接
     *
     * @param unionSian
     * @return
     */
    public SData_Result showLostConnect(long unionSian, int type) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionSian);
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        union.getUnionBO().saveShowLostConnect(type <= 0 ? 0 : 1);
        return SData_Result.make(ErrorCode.Success, "success");
    }

    /**
     * 凌晨设置每个赛事皮肤修改状态
     */
    public void changeSkinType() {
        Map<Long, Union> unionMap;
        if (Config.isShare()) {
            unionMap = ShareUnionListMgr.getInstance().getAllUnion();
        } else {
            unionMap = getUnionListMgr().getUnionMap();
        }
        for (Map.Entry<Long, Union> entry : unionMap.entrySet()) {
            entry.getValue().setChangeSkinStatus(false);
            if (Config.isShare()) {
                ShareUnionListMgr.getInstance().addUnion(entry.getValue());
            }
        }
    }

}
