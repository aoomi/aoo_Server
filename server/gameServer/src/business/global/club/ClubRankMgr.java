package business.global.club;

import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.utils.RoomRecordUtils;
import business.utils.TimeConditionUtils;
import cenum.Page;
import cenum.PrizeType;
import cenum.room.RoomState;
import com.ddm.server.common.ehcache.DataConstants;
import com.ddm.server.common.ehcache.EhCacheFactory;
import com.ddm.server.common.ehcache.configuration.SqlCacheConfiguration;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.db.entity.clarkGame.GameRoomBO;
import core.db.entity.clarkGame.PlayerGameRoomIdBO;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.service.clarkGame.GameRoomBOService;
import core.db.service.clarkGame.PlayerGameRoomIdBOService;
import core.db.service.clarkGame.PlayerRoomAloneBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.QueryRoomIdItem;
import jsproto.c2s.cclass.club.*;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.club.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 亲友圈排行榜
 *
 * @author Administrator
 */
public class ClubRankMgr {


    /**
     * 亲友圈房间id
     *
     * @param roomIdOperation 房间id 选择参数
     * @param pid             玩家pid
     * @return
     */
    public SData_Result getClubRoomIdOperation(CClub_RoomIdOperation roomIdOperation, long pid) {
        if (roomIdOperation.getUnionId() <= 0L) {
            if (ClubMgr.getInstance().getClubMemberMgr().isNotMinister(roomIdOperation.getClubId(), pid)) {
                return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "CLUB_NOTMINISTER");
            }
        } else {
            if (UnionMgr.getInstance().getUnionMemberMgr().isNotManage(pid, roomIdOperation.getClubId(), roomIdOperation.getUnionId())) {
                return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
            }
        }
        // 是否勾选
        if (roomIdOperation.getType() == 1) {
            PlayerGameRoomIdBO.saveIgnore(pid, roomIdOperation.getEndTime(), roomIdOperation.getClubId(), roomIdOperation.getRoomID());
        } else {
            PlayerGameRoomIdBO.delete(pid, roomIdOperation.getClubId(), roomIdOperation.getRoomID());
        }
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 查询亲友圈战绩
     */
    public SData_Result getClubPlayerRecord(CClub_GetRecord req, long pid) {
        // 亲友圈房间战绩
        return SData_Result.make(ErrorCode.Success, ((PlayerRoomAloneBOService) ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class)).findAllE(Restrictions.and(Restrictions.eq("pid", pid), Restrictions.eq("clubId", req.getClubId()), Restrictions.gt("endTime", 0), Restrictions.eq("dateTime", CommTime.getYesterDayStringYMD(req.getGetType()))).groupBy("gameType"), ClubPlayerRecordItem.class, ClubPlayerRecordItem.getItemsName(req.getUnionId())));
    }

    /**
     * 查询亲友圈战绩
     */
    public SData_Result getClubPlayerRecordCount(CClub_GetRecord req, long pid) {
        ClubPlayerRecordItem playerRecordItem = ((PlayerRoomAloneBOService) ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class)).findOneE(Restrictions.and(Restrictions.eq("pid", pid), Restrictions.eq("clubId", req.getClubId()), Restrictions.gt("endTime", 0), Restrictions.eq("dateTime", CommTime.getYesterDayStringYMD(req.getGetType()))), ClubPlayerRecordItem.class, ClubPlayerRecordItem.getItemsNameCount(req.getUnionId()));
        // 亲友圈房间战绩
        return SData_Result.make(ErrorCode.Success, Objects.isNull(playerRecordItem) ? new ClubPlayerRecordItem() : playerRecordItem);
    }

    /**
     * 查询亲友圈战绩
     */
    public SClub_GetRecord getRecord(CClub_GetRecord req, long pid) {
        int getType = req.getGetType() >= TimeConditionUtils.Record_Get_Type.RECORD_GET_TYPE_LAST_THREE_DAYS.value() ? TimeConditionUtils.Record_Get_Type.RECORD_GET_DAY_3.value() : req.getGetType();
        // 时间
        Criteria zeroClockS = TimeConditionUtils.DayZeroClockS(getType);
        // 获取已查看的房间id列表
        List<QueryRoomIdItem> queryRoomIdItems = ((PlayerGameRoomIdBOService) ContainerMgr.get().getComponent(PlayerGameRoomIdBOService.class)).findAllE(Restrictions.and(Restrictions.eq("pid", pid), zeroClockS, Restrictions.eq("clubId", req.getClubId())), QueryRoomIdItem.class, QueryRoomIdItem.getItemsName());
        List<Long> roomIdList = CollectionUtils.isEmpty(queryRoomIdItems) ? Collections.emptyList() : queryRoomIdItems.stream().map(k -> k.getRoomID()).collect(Collectors.toList());
        List<GameRoomBO> roomBOList;
        if (req.getType() <= 0) {
            // 0:所有
            roomBOList = allGetRecord(req, zeroClockS);
        } else {
            // 1:隐藏已查看战绩
            roomBOList = hideGetRecord(roomIdList, req, zeroClockS);
        }
        roomBOList = CollectionUtils.isEmpty(roomBOList) ? Collections.emptyList() : roomBOList;
        List<ClubRecordInfo> clubRecordInfos = roomBOList.stream().filter(k -> Objects.nonNull(k)).map(k -> {
            return new ClubRecordInfo(k.getId(), k.getRoomKey(), k.getDataJsonCfg(), k.getPlayerList(), k.getEndTime(),
                    k.getConsumeValue(), k.getValueType(), k.getGameType(), k.getOwnner(), RoomRecordUtils.getRoomState(k.getEndTime(), k.getRoomState()), k.getRoomSportsConsume(), k.getUnionId(), req.getType() == 0 && roomIdList.contains(k.getId()), k.getSportsDouble(), k.getConfigName());
        }).collect(Collectors.toList());
        return SClub_GetRecord.make(req.getClubId(), req.getUnionId(), getType, CollectionUtils.isEmpty(clubRecordInfos) ? Collections.emptyList() : clubRecordInfos);
    }

    /**
     * 0:所有
     *
     * @param req        参数
     * @param zeroClockS 时间
     * @return
     */
    private List<GameRoomBO> allGetRecord(CClub_GetRecord req, Criteria zeroClockS) {
        List<GameRoomBO> gameRoomBOList;
        if (req.getUnionId() <= 0L) {
            // 亲友圈房间战绩
            List<QueryRoomIdItem> queryRoomIdItems = ((PlayerRoomAloneBOService) ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class)).findAllE(Restrictions.and(Restrictions.eq("clubId", req.getClubId()), zeroClockS, req.getQuery() <= 0 ? null : Restrictions.eq("roomKey", req.getQuery())).groupBy("roomID").desc("endTime").setPageNum(Page.getPageNum(req.getPageNum(), Page.PAGE_SIZE_8)).setPageSize(Page.PAGE_SIZE_8), QueryRoomIdItem.class, QueryRoomIdItem.getItemsName());
            if (CollectionUtils.isEmpty(queryRoomIdItems)) {
                return Collections.emptyList();
            }
            gameRoomBOList = ((GameRoomBOService) ContainerMgr.get().getComponent(GameRoomBOService.class)).findAll(Restrictions.in("id", queryRoomIdItems.stream().map(k -> k.getRoomID()).collect(Collectors.toList())).desc("endTime"), ClubRecordInfo.getItemsName());
        } else {
            // 赛事房间战绩
            Criteria conditions = Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), req.getQuery() <= 0 ? null : Restrictions.eq("roomKey", req.getQuery()), zeroClockS, Restrictions.eq("roomState", RoomState.End.value())).desc("endTime").setPageNum(Page.getPageNum(req.getPageNum(), Page.PAGE_SIZE_8)).setPageSize(Page.PAGE_SIZE_8);
            gameRoomBOList = ((GameRoomBOService) ContainerMgr.get().getComponent(GameRoomBOService.class)).findAll(conditions, ClubRecordInfo.getItemsName());

        }
        //赛事房间战绩添加倍数 需要特殊处理
        for (GameRoomBO con : gameRoomBOList) {
            BaseCreateRoom createRoom = new Gson().fromJson(con.getDataJsonCfg(), BaseCreateRoom.class);
            if (Objects.nonNull(createRoom) || Objects.nonNull(createRoom.getSportsDouble())) {
                con.setSportsDouble(createRoom.getSportsDouble());
            } else {
                con.setSportsDouble(new Double(1.0));
            }
        }
        return gameRoomBOList;
    }

    /**
     * 1:隐藏已查看战绩
     *
     * @param roomIdList 房间列表Id
     * @param req        参数
     * @param zeroClockS 时间
     * @return
     */
    private List<GameRoomBO> hideGetRecord(List<Long> roomIdList, CClub_GetRecord req, Criteria zeroClockS) {
        List<GameRoomBO> gameRoomBOList;
        if (req.getUnionId() <= 0L) {
            // 亲友圈房间战绩
            List<QueryRoomIdItem> queryRoomIdItems = ((PlayerRoomAloneBOService) ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class)).findAllE(Restrictions.and(Restrictions.eq("clubId", req.getClubId()), zeroClockS, CollectionUtils.isEmpty(roomIdList) ? null : Restrictions.notin("roomID", roomIdList)).groupBy("roomID").desc("endTime").setPageNum(Page.getPageNum(req.getPageNum(), Page.PAGE_SIZE_8)).setPageSize(Page.PAGE_SIZE_8), QueryRoomIdItem.class, QueryRoomIdItem.getItemsName());
            if (CollectionUtils.isEmpty(queryRoomIdItems)) {
                return Collections.emptyList();
            }
            gameRoomBOList = ((GameRoomBOService) ContainerMgr.get().getComponent(GameRoomBOService.class)).findAll(Restrictions.in("id", queryRoomIdItems.stream().map(k -> k.getRoomID()).collect(Collectors.toList())).desc("endTime"), ClubRecordInfo.getNotCfgItemsName());
        } else {
            // 赛事房间战绩
            Criteria conditions = Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), zeroClockS, Restrictions.eq("roomState", RoomState.End.value()), CollectionUtils.isEmpty(roomIdList) ? null : Restrictions.notin("id", roomIdList)).desc("endTime").setPageNum(Page.getPageNum(req.getPageNum(), Page.PAGE_SIZE_8)).setPageSize(Page.PAGE_SIZE_8);
            gameRoomBOList = ((GameRoomBOService) ContainerMgr.get().getComponent(GameRoomBOService.class)).findAll(conditions, ClubRecordInfo.getNotCfgItemsName());
        }
        //赛事房间战绩添加倍数 需要特殊处理
        for (GameRoomBO con : gameRoomBOList) {
            BaseCreateRoom createRoom = new Gson().fromJson(con.getDataJsonCfg(), BaseCreateRoom.class);
            if (Objects.nonNull(createRoom) && Objects.nonNull(createRoom.getSportsDouble())) {
                con.setSportsDouble(createRoom.getSportsDouble());
            } else {
                con.setSportsDouble(new Double(1.0));
            }
        }
        return gameRoomBOList;
    }


    /**
     * 查询亲友圈指定玩家战绩
     */
    public void getRecordByPlayer(WebSocketRequest request, CClub_GetRecordByPlayer req) {
        List<Long> gameRoomPlayerBOs = new ArrayList<>();
        Criteria conditions = TimeConditionUtils.DayZeroClockS(req.getType);
        if (Objects.nonNull(conditions)) {
            if (req.unionId <= 0L) {
                // 亲友圈指定玩家战绩条件
                conditions = clubRecordByPlayerConditions(req, conditions);
            } else {
                // 赛事指定玩家战绩条件
                conditions = unionRecordByPlayerConditions(req, conditions);
            }
            gameRoomPlayerBOs = ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class)
                    .findAllE(conditions, ClubPlayerRoomAloneLogPidBO.class, ClubPlayerRoomAloneLogPidBO.getItemsName())
                    .stream().map(k -> k.getRoomID()).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(gameRoomPlayerBOs)) {
            request.error(ErrorCode.NotAllow, "getRecordByPlayer gameRoomPlayerBOs pageNum:{%d}", req.pageNum);
            return;
        }
        List<GameRoomBO> clubGameRoomBOs = ((GameRoomBOService) ContainerMgr.get()
                .getComponent(GameRoomBOService.class)).findAll(Restrictions.in("id", gameRoomPlayerBOs),
                ClubRecordInfo.getItemsName());
        gameRoomPlayerBOs = null;
        if (CollectionUtils.isEmpty(clubGameRoomBOs)) {
            request.error(ErrorCode.NotAllow, "getRecordByPlayer clubGameRoomBOs pageNum:{%d}", req.pageNum);
            return;
        }
        List<ClubRecordInfo> clubRecordInfos = clubGameRoomBOs.stream().filter(k -> null != k).map(k -> {
            return new ClubRecordInfo(k.getId(), k.getRoomKey(), k.getDataJsonCfg(), k.getPlayerList(), k.getEndTime(),
                    k.getConsumeValue(), k.getValueType(), k.getGameType(), k.getOwnner(), RoomRecordUtils.getRoomState(k.getEndTime(), k.getRoomState()), k.getRoomSportsConsume(), k.getUnionId());
        }).sorted(Comparator.comparing(ClubRecordInfo::getEndTime).reversed()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(clubRecordInfos)) {
            request.error(ErrorCode.NotAllow, "getRecordByPlayer clubRecordInfos pageNum:{%d}", req.pageNum);
            return;
        }
        request.response(SClub_GetRecordByPlayer.make(req.clubId, req.getType, req.pid, clubRecordInfos));
        clubRecordInfos = null;
    }

    /**
     * 亲友圈指定玩家战绩条件
     *
     * @param req        参数
     * @param conditions 条件
     * @return
     */
    private Criteria clubRecordByPlayerConditions(CClub_GetRecordByPlayer req, Criteria conditions) {
        return Restrictions.and(Restrictions.eq("clubID", req.clubId), Restrictions.eq("winner", 1),
                Restrictions.eq("pid", req.pid), Restrictions.eq("prizeType", PrizeType.RoomCard.value()),
                conditions).setPageNum(Page.getPageNum(req.pageNum)).setPageSize(Page.PAGE_SIZE);
    }

    /**
     * 赛事指定玩家战绩条件
     *
     * @param req        参数
     * @param conditions 条件
     * @return
     */
    private Criteria unionRecordByPlayerConditions(CClub_GetRecordByPlayer req, Criteria conditions) {
        return Restrictions.and(Restrictions.eq("unionId", req.unionId), Restrictions.eq("winner", 1),
                Restrictions.eq("pid", req.pid), Restrictions.eq("prizeType", PrizeType.RoomCard.value()),
                conditions).setPageNum(Page.getPageNum(req.pageNum)).setPageSize(Page.PAGE_SIZE);
    }


    /**
     * 获取亲友圈战绩统计
     *
     * @param request 返回数据
     * @param req     参数
     */
    public void getClubTotalInfo(WebSocketRequest request, CClub_GetRecord req) {
        request.response(this.clubTotalInfo(req.getClubId(), req.getUnionId(), req.getGetType()));
    }

    /**
     * 亲友圈战绩统计信息
     *
     * @param clubId 亲友圈ID
     * @param type   时间类型
     * @return
     */
    private ClubTotalInfo clubTotalInfo(long clubId, long unionId, int type) {
        int getType = type >= TimeConditionUtils.Record_Get_Type.RECORD_GET_TYPE_LAST_THREE_DAYS.value() ? TimeConditionUtils.Record_Get_Type.RECORD_GET_DAY_3.value() : type;

        // 战绩统计信息缓存key
        String key = clubTotalInfoKey(clubId, unionId, getType);
        ClubTotalInfo clubTotalInfo = new ClubTotalInfo(0, 0);
        //缓存有点问 暂时注释
//                EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).get(key, ClubTotalInfo.class);
//        if (Objects.nonNull(clubTotalInfo)) {
//            return clubTotalInfo;
//        }
        if (unionId <= 0L) {
            // 亲友圈战绩统计信息条件
            clubTotalInfo = this.clubTotalInfoConditions(TimeConditionUtils.DayZeroClockS(getType), clubId);
        } else {
            // 赛事战绩统计信息条件
            clubTotalInfo = this.unionTotalInfoConditions(TimeConditionUtils.DayZeroClockS(getType), unionId);
        }
        if (Objects.nonNull(clubTotalInfo)) {
            int pageNumTotal = clubTotalInfo.getRoomTotalCount() % Page.PAGE_SIZE_8 == 0 ? clubTotalInfo.getRoomTotalCount() / Page.PAGE_SIZE_8 : clubTotalInfo.getRoomTotalCount() / Page.PAGE_SIZE_8 + 1;
            clubTotalInfo.setPageNumTotal(pageNumTotal == 0 ? 1 : pageNumTotal);
//            EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).put(key, new ClubTotalInfo(1,2));
            return clubTotalInfo;
        }
        return new ClubTotalInfo(0, 0);
    }


    /**
     * 战绩统计信息缓存key
     *
     * @param clubId
     * @param unionId
     * @param type
     * @return
     */
    private String clubTotalInfoKey(long clubId, long unionId, int type) {
        if (unionId <= 0L) {
            return String.format(DataConstants.CLUB_TOTAL_INFO, clubId, type);
        } else {
            return String.format(DataConstants.UNION_TOTAL_INFO, unionId, type);
        }
    }

    /**
     * 战绩页码统计信息缓存key
     *
     * @param clubId
     * @param unionId
     * @param type
     * @return
     */
    private String clubTotalInfoKeyPageNum(long clubId, long unionId, int type) {
        if (unionId <= 0L) {
            return String.format(DataConstants.CLUB_TOTAL_PAGE_INFO, clubId, type);
        } else {
            return String.format(DataConstants.CLUB_TOTAL_PAGE_INFO, unionId, type);
        }
    }

    /**
     * 亲友圈战绩统计信息条件
     *
     * @param conditions 条件
     * @param clubId     亲友圈id
     * @return
     */
    private ClubTotalInfo clubTotalInfoConditions(Criteria conditions, long clubId) {
        conditions = Restrictions.and(Restrictions.eq("clubID", clubId), conditions);
        return ((PlayerRoomAloneBOService) ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class)).findOneETest(conditions, ClubTotalInfo.class, ClubTotalInfo.getItemsName());
    }

    /**
     * 赛事战绩统计信息条件
     *
     * @param conditions 条件
     * @param unionId    赛事id
     * @return
     */
    private ClubTotalInfo unionTotalInfoConditions(Criteria conditions, long unionId) {
        conditions = Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.eq("roomState", RoomState.End.value()), conditions);
        return ((GameRoomBOService) ContainerMgr.get().getComponent(GameRoomBOService.class)).findOneETest(conditions, ClubTotalInfo.class, ClubTotalInfo.getUnionItemsName());
    }


    /**
     * 获取指定类型消耗值
     *
     * @param value 值
     * @return
     */
    public int consumeValue(IntSummaryStatistics value) {
        if (Objects.isNull(value)) {
            return 0;
        } else {
            return (int) value.getSum();
        }
    }

    /**
     * 亲友圈战绩统计成绩单。
     *
     * @param clubId     亲友圈ID
     * @param roomIDList 房间列表
     */
    public ClubPlayerRoomAloneData schoolReport(long clubId, long unionId, List<Long> roomIDList, boolean isAll, int getType, int pageNum, String query) {
        return schoolReportTest(clubId, unionId, roomIDList, isAll, getType, pageNum, query);
    }

    /**
     * 亲友圈战绩统计成绩单。
     *
     * @param clubId     亲友圈ID
     * @param roomIDList 房间列表
     */
    public ClubPlayerRoomAloneData schoolReportTest(long clubId, long unionId, List<Long> roomIDList, boolean isAll, int getType, int pageNum, String query) {
        getType = getType >= TimeConditionUtils.Record_Get_Type.RECORD_GET_TYPE_LAST_THREE_DAYS.value() ? TimeConditionUtils.Record_Get_Type.RECORD_GET_DAY_3.value() : getType;
        // 查询Pid
        final long queryPid = queryPlayer(query);
        // 查询列表
        List<ClubPlayerRoomAloneLogBO> logBOs = null;
        // 查询器
        Criteria criteria = null;
        Criteria criteriaCount = null;

        if (unionId <= 0L) {
            criteria = this.clubSchoolReportConditions(clubId, roomIDList, isAll, getType, pageNum, queryPid);
            criteriaCount = this.clubSchoolReportConditionsCount(clubId, roomIDList, isAll, getType, pageNum, queryPid);
        } else {
            criteria = this.unionSchoolReportConditions(unionId, roomIDList, isAll, getType, pageNum, queryPid);
            criteriaCount = this.unionSchoolReportConditionsCount(unionId, roomIDList, isAll, getType, pageNum, queryPid);
        }
        Long totalRecordNum = 0L;
        if (Objects.nonNull(criteria)) {
            logBOs = ((PlayerRoomAloneBOService) ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class))
                    .findAllE(criteria, ClubPlayerRoomAloneLogBO.class, ClubPlayerRoomAloneLogBO.getItemsName());

            totalRecordNum = ((PlayerRoomAloneBOService) ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class)).countTest(criteriaCount, "pid");
        }
        if (Objects.isNull(logBOs)) {
            return new ClubPlayerRoomAloneData(0L, Collections.emptyList());
        }
        // 返回数据
        List<ClubPlayerRoomAloneLogBO> logBOS = logBOs.stream().filter(k -> Objects.nonNull(k)).map(k -> {
            Player player = PlayerMgr.getInstance().getPlayer(k.getPid());
            if (Objects.nonNull(player)) {
                k.setPlayer(player.getShortPlayer());
                return k;
            } else {
                return null;
            }
        }).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
        return new ClubPlayerRoomAloneData(totalRecordNum, logBOS);
    }


    /**
     * 查询玩家
     *
     * @param query
     * @return
     */
    private final long queryPlayer(String query) {
        Player player = null;
        if (StringUtils.isEmpty(query)) {
            return 0L;
        }
        if (StringUtils.isNumeric(query)) {
            // 是纯数字
            final long pid = Long.parseLong(query);
            if (pid > 0L) {
                player = PlayerMgr.getInstance().getPlayer(pid);
                if (Objects.nonNull(player)) {
                    return player.getPid();
                }
            }
        }
        player = PlayerMgr.getInstance().getAllPlayers()
                .stream()
                .filter(k -> Objects.nonNull(k) && k.getShortPlayer().getName().contains(query)).findAny().orElse(null);
        return Objects.nonNull(player) ? player.getPid() : 0L;
    }


    /**
     * 亲友圈战绩统计成绩单
     *
     * @param clubId     亲友圈Id
     * @param roomIDList 房间id列表
     * @param isAll      是否查询所有
     * @param getType    时间
     * @param pageNum    页数
     * @param queryPid   查询pid
     * @return
     */
    private Criteria clubSchoolReportConditions(long clubId, List<Long> roomIDList, boolean isAll, int getType, int pageNum, final long queryPid) {
        if (isAll) {
            // 时间条件
            Criteria conditions = TimeConditionUtils.DayZeroClockS(getType);
            if (Objects.nonNull(conditions)) {
                return Restrictions.and(Restrictions.eq("clubID", clubId), conditions, queryPid > 0L ? Restrictions.eq("pid", queryPid) : null)
                        .groupBy("pid")
                        .descFormat("`size` desc,`pid`")
                        .setPageNum(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
                        .setPageSize(Page.PAGE_SIZE_8);


            }
        } else {
            return Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.in("roomID", roomIDList), queryPid > 0L ? Restrictions.eq("pid", queryPid) : null)
                    .groupBy("pid")
                    .descFormat("`size` desc,`pid`")
                    .setPageNum(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
                    .setPageSize(Page.PAGE_SIZE_8);
        }
        return null;
    }

    /**
     * 亲友圈战绩统计成绩单
     *
     * @param clubId   亲友圈Id
     * @param getType  时间
     * @param type     查询的数据
     * @param gameType 查询的游戏类型
     * @return
     */
    private Criteria clubSchoolReportConditionsZhongZhi(long clubId, int getType, int type, int gameType) {
        // 时间条件
        Criteria conditions = TimeConditionUtils.DayZeroClockSZhongZhiRanked(getType);
        if (Objects.nonNull(conditions)) {
            return Restrictions.and(Restrictions.eq("clubID", clubId), conditions, gameType >= 0 ? Restrictions.eq("gameType", gameType) : null)
                    .groupBy("pid");
        }

        return null;
    }

    /**
     * 亲友圈战绩统计成绩单
     *
     * @param clubId     亲友圈Id
     * @param roomIDList 房间id列表
     * @param isAll      是否查询所有
     * @param getType    时间
     * @param pageNum    页数
     * @param queryPid   查询pid
     * @return
     */
    private Criteria clubSchoolReportConditionsCount(long clubId, List<Long> roomIDList, boolean isAll, int getType, int pageNum, final long queryPid) {
        if (isAll) {
            // 时间条件
            Criteria conditions = TimeConditionUtils.DayZeroClockS(getType);
            if (Objects.nonNull(conditions)) {
                return Restrictions.and(Restrictions.eq("clubID", clubId), conditions, queryPid > 0L ? Restrictions.eq("pid", queryPid) : null)
                        .groupBy("pid");


            }
        } else {
            return Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.in("roomID", roomIDList), queryPid > 0L ? Restrictions.eq("pid", queryPid) : null)
                    .groupBy("pid");
        }
        return null;
    }

    /**
     * 亲友圈战绩统计成绩单
     *
     * @param clubId   亲友圈Id
     * @param getType  时间
     * @param type     查询的数据
     * @param gameType 查询的游戏类型
     * @return
     */
    private Criteria clubSchoolReportConditionsCountZhongZhi(long clubId, int getType, int type, int gameType) {
        // 时间条件
        Criteria conditions = TimeConditionUtils.DayZeroClockSZhongZhiRanked(getType);
        if (Objects.nonNull(conditions)) {
            return Restrictions.and(Restrictions.eq("clubID", clubId), conditions, gameType >= 0 ? Restrictions.eq("gameType", gameType) : null)
                    .groupBy("pid");


        }
        return null;
    }

    /**
     * 赛事战绩统计成绩单
     *
     * @param unionId    赛事Id
     * @param roomIDList 房间id列表
     * @param isAll      是否查询所有
     * @param getType    时间
     * @param pageNum    页数
     * @param queryPid   查询pid
     * @return
     */
    private Criteria unionSchoolReportConditions(long unionId, List<Long> roomIDList, boolean isAll, int getType, int pageNum, final long queryPid) {
        if (isAll) {
            // 时间条件
            Criteria conditions = TimeConditionUtils.DayZeroClockS(getType);
            if (Objects.nonNull(conditions)) {
                return Restrictions.and(Restrictions.eq("unionId", unionId), conditions, queryPid > 0L ? Restrictions.eq("pid", queryPid) : null)
                        .groupBy("pid")
                        .descFormat("`size` desc,`pid`")
                        .setPageNum(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
                        .setPageSize(Page.PAGE_SIZE_8);


            }
        } else {
            return Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.in("roomID", roomIDList), queryPid > 0L ? Restrictions.eq("pid", queryPid) : null)
                    .groupBy("pid")
                    .descFormat("`size` desc,`pid`")
                    .setPageNum(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
                    .setPageSize(Page.PAGE_SIZE_8);
        }
        return null;
    }

    /**
     * 赛事战绩统计成绩单
     *
     * @param unionId    赛事Id
     * @param roomIDList 房间id列表
     * @param isAll      是否查询所有
     * @param getType    时间
     * @param pageNum    页数
     * @param queryPid   查询pid
     * @return
     */
    private Criteria unionSchoolReportConditionsCount(long unionId, List<Long> roomIDList, boolean isAll, int getType, int pageNum, final long queryPid) {
        if (isAll) {
            // 时间条件
            Criteria conditions = TimeConditionUtils.DayZeroClockS(getType);
            if (Objects.nonNull(conditions)) {
                return Restrictions.and(Restrictions.eq("unionId", unionId), conditions, queryPid > 0L ? Restrictions.eq("pid", queryPid) : null)
                        .groupBy("pid");


            }
        } else {
            return Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.in("roomID", roomIDList), queryPid > 0L ? Restrictions.eq("pid", queryPid) : null)
                    .groupBy("pid");
        }
        return null;
    }


    /**
     * 战绩详情列表
     */
    public List<ClubRecordInfo> getPersonalRecord(long pid, long clubId, long unionId, List<Long> roomIDList, boolean isAll, int getType, int pageNum, long roomKey) {
        return getPersonalRecordTest(pid, clubId, unionId, roomIDList, isAll, getType, pageNum, roomKey);
    }

    /**
     * 战绩详情分页统计总数列表
     */
    public ClubTotalInfo getPersonalRecordGetPageNum(long pid, long clubId, long unionId, List<Long> roomIDList, boolean isAll, int getType, int pageNum) {
        return getPersonalRecordTestGetPageNum(pid, clubId, unionId, roomIDList, isAll, getType, pageNum);
    }

    private ClubTotalInfo getPersonalRecordTestGetPageNum(long pid, long clubId, long unionId, List<Long> roomIDList, boolean isAll, int getType, int pageNum) {
        getType = getType >= TimeConditionUtils.Record_Get_Type.RECORD_GET_TYPE_LAST_THREE_DAYS.value() ? TimeConditionUtils.Record_Get_Type.RECORD_GET_DAY_3.value() : getType;
        // 战绩统计信息缓存key
        String key = clubTotalInfoKeyPageNum(clubId, unionId, getType);
        //缓存有点问 暂时注释
//                EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).get(key, ClubTotalInfo.class);
//        if (Objects.nonNull(clubTotalInfo)) {
//            return clubTotalInfo;
//        }
        // 查询器
        Criteria criteria = null;
        if (unionId <= 0L) {
            // 亲友圈战绩详情列表
            criteria = this.clubPersonalRecordConditions(pid, clubId, roomIDList, isAll, getType, 0);
        } else {
            // 赛事战绩详情列表
            criteria = this.unionPersonalRecordConditions(pid, unionId, roomIDList, isAll, getType, 0);
        }
        ClubTotalInfo clubTotalInfoSql = ((PlayerRoomAloneBOService) ContainerMgr.get()
                .getComponent(PlayerRoomAloneBOService.class))
                .findOneETest(criteria, ClubTotalInfo.class, ClubTotalInfo.getItemsName());
        if (Objects.nonNull(clubTotalInfoSql)) {
            int pageNumTotal = clubTotalInfoSql.getRoomTotalCount() % Page.PAGE_SIZE_8 == 0 ? clubTotalInfoSql.getRoomTotalCount() / Page.PAGE_SIZE_8 : clubTotalInfoSql.getRoomTotalCount() / Page.PAGE_SIZE_8 + 1;
            clubTotalInfoSql.setPageNumTotal(pageNumTotal == 0 ? 1 : pageNumTotal);
            EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).put(key, clubTotalInfoSql);
            return clubTotalInfoSql;
        }
        return new ClubTotalInfo(0, 0);


    }

    /**
     * 战绩详情列表
     */
    public List<ClubRecordInfo> getPersonalRecordTest(long pid, long clubId, long unionId, List<Long> roomIDList, boolean isAll, int getType, int pageNum, long roomKey) {
        getType = getType >= TimeConditionUtils.Record_Get_Type.RECORD_GET_TYPE_LAST_THREE_DAYS.value() ? TimeConditionUtils.Record_Get_Type.RECORD_GET_DAY_3.value() : getType;
        // 查询器
        Criteria criteria = null;
        if (unionId <= 0L) {
            // 亲友圈战绩详情列表
            criteria = this.clubPersonalRecordConditions(pid, clubId, roomIDList, isAll, getType, roomKey);
        } else {
            // 赛事战绩详情列表
            criteria = this.unionPersonalRecordConditions(pid, unionId, roomIDList, isAll, getType, roomKey);
        }
        if (Objects.isNull(criteria)) {
            return Collections.emptyList();
        }
        criteria = criteria.setPageNum(Page.getPageNum(pageNum, Page.PAGE_SIZE_8)).setPageSize(Page.PAGE_SIZE_8);
        criteria.desc("createRoomTime");
        List<Long> roomList = ((PlayerRoomAloneBOService) ContainerMgr.get()
                .getComponent(PlayerRoomAloneBOService.class))
                .findAllE(criteria, ClubPlayerRoomAloneLogPidBO.class, ClubPlayerRoomAloneLogPidBO.getItemsName())
                .stream()
                .map(k -> k.getRoomID())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(roomList)) {
            return Collections.emptyList();
        }
        List<GameRoomBO> clubGameRoomBOs = ((GameRoomBOService) ContainerMgr.get()
                .getComponent(GameRoomBOService.class))
                .findAll(Restrictions.in("id", roomList), ClubRecordInfo.getNotCfgItemsName());
        if (CollectionUtils.isEmpty(clubGameRoomBOs)) {
            return Collections.emptyList();
        }
        return clubGameRoomBOs.stream().filter(k -> Objects.nonNull(k)).map(k -> {
            return new ClubRecordInfo(k.getId(), k.getRoomKey(), k.getPlayerList(), k.getEndTime(), k.getConsumeValue(),
                    k.getValueType(), k.getGameType(), k.getOwnner(), RoomRecordUtils.getRoomState(k.getEndTime(), k.getRoomState()), k.getRoomSportsConsume(), k.getUnionId(), k.getConfigName());
        }).sorted(Comparator.comparing(ClubRecordInfo::getEndTime).reversed()).collect(Collectors.toList());
    }


    /**
     * 亲友圈战绩详情列表
     *
     * @param pid        玩家Pid
     * @param clubId     亲友圈id
     * @param roomIDList 房间id列表
     * @param isAll      是否全部
     * @param getType    时间条件
     * @return
     */
    private Criteria clubPersonalRecordConditions(long pid, long clubId, List<Long> roomIDList, boolean isAll, int getType, long roomKey) {
        if (isAll) {
            // 时间条件
            Criteria conditions = TimeConditionUtils.DayZeroClockS(getType);
            if (Objects.nonNull(conditions)) {
                return Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.eq("pid", pid), roomKey <= 0 ? null : Restrictions.eq("roomKey", roomKey), conditions);
            }
        } else if (CollectionUtils.isNotEmpty(roomIDList)) {
            return Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.eq("pid", pid), Restrictions.in("roomID", roomIDList), roomKey <= 0 ? null : Restrictions.eq("roomKey", roomKey));
        }
        return null;
    }

    /**
     * 赛事战绩详情列表
     *
     * @param pid        玩家Pid
     * @param unionId    赛事id
     * @param roomIDList 房间id列表
     * @param isAll      是否全部
     * @param getType    时间条件
     * @return
     */
    private Criteria unionPersonalRecordConditions(long pid, long unionId, List<Long> roomIDList, boolean isAll, int getType, long roomKey) {
        if (isAll) {
            // 时间条件
            Criteria conditions = TimeConditionUtils.DayZeroClockS(getType);
            if (Objects.nonNull(conditions)) {
                return Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.eq("pid", pid), roomKey <= 0 ? null : Restrictions.eq("roomKey", roomKey), conditions);
            }
        } else if (CollectionUtils.isNotEmpty(roomIDList)) {
            return Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.eq("pid", pid), roomKey <= 0 ? null : Restrictions.eq("roomKey", roomKey), Restrictions.in("roomID", roomIDList));
        }
        return null;
    }


    /**
     * 亲友圈推广员下属个人战绩
     *
     * @param clubId     亲友圈ID
     * @param pid        玩家
     * @param partnerPid 合伙人PID
     * @param pageNum    页数
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result getClubPromotionPersonalRecord(long clubId, long pid, long partnerPid, int pageNum) {
        List<Long> roomList = null;
        Criteria conditions = Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.eq("pid", pid), Restrictions.eq("partnerPid", partnerPid)).setPageNum(Page.getPageNumEight(pageNum)).setPageSize(Page.PAGE_SIZE_8);
        conditions.desc("roomID");
        roomList = ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class).findAllE(conditions, ClubPlayerRoomAloneLogPidBO.class, ClubPlayerRoomAloneLogPidBO.getItemsName()).stream().map(k -> k.getRoomID()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(roomList)) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        List<GameRoomBO> clubGameRoomBOs = ((GameRoomBOService) ContainerMgr.get().getComponent(GameRoomBOService.class)).findAll(Restrictions.and(Restrictions.in("id", roomList)), ClubRecordInfo.getNotCfgItemsName());
        if (CollectionUtils.isEmpty(clubGameRoomBOs)) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        List<ClubRecordInfo> clubRecordInfos = clubGameRoomBOs.stream().map(k -> {
            return new ClubRecordInfo(k.getId(), k.getRoomKey(), k.getPlayerList(), k.getEndTime(), k.getConsumeValue(), k.getValueType(), k.getGameType(), k.getOwnner(), RoomRecordUtils.getRoomState(k.getEndTime(), k.getRoomState()), k.getRoomSportsConsume(), k.getUnionId(), k.getConfigName());
        }).sorted(Comparator.comparing(ClubRecordInfo::getEndTime).reversed()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(clubRecordInfos)) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        return SData_Result.make(ErrorCode.Success, clubRecordInfos);
    }

    /**
     * 亲友圈推广员下属个人战绩
     *
     * @param clubId     亲友圈ID
     * @param pid        玩家
     * @param partnerPid 合伙人PID
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result getClubPromotionPersonalCount(long clubId, long pid, long partnerPid) {
        String key = String.format(DataConstants.CLUB_PROMOTION_PERSONAL_COUNT, clubId, pid, partnerPid);
        ClubPlayerRoomAloneLogBO clubPlayerRoomAloneLogBO = EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).get(key, ClubPlayerRoomAloneLogBO.class);
        if (Objects.nonNull(clubPlayerRoomAloneLogBO)) {
            // 获取缓存数据
            return SData_Result.make(ErrorCode.Success, clubPlayerRoomAloneLogBO);
        }
        Player player = PlayerMgr.getInstance().getPlayer(pid);
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "getClubPartnerPersonalCount null == player {%d}", pid);
        }
        Criteria conditions = Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.eq("pid", pid), Restrictions.eq("partnerPid", partnerPid));
        clubPlayerRoomAloneLogBO = ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class).findOneE(conditions,
                ClubPlayerRoomAloneLogBO.class, ClubPlayerRoomAloneLogBO.getItemsName());
        if (Objects.isNull(clubPlayerRoomAloneLogBO)) {
            return SData_Result.make(ErrorCode.NotAllow, "getClubPartnerPersonalCount null == logBOs");
        }
        clubPlayerRoomAloneLogBO.setPlayer(player.getShortPlayer());
        int pageNumTotal = clubPlayerRoomAloneLogBO.getSize() % Page.PAGE_SIZE_8 == 0 ? clubPlayerRoomAloneLogBO.getSize() / Page.PAGE_SIZE_8 : clubPlayerRoomAloneLogBO.getSize() / Page.PAGE_SIZE_8 + 1;
        clubPlayerRoomAloneLogBO.setPageNumTotal(pageNumTotal == 0 ? 1 : pageNumTotal);
        EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).put(key, clubPlayerRoomAloneLogBO);
        return SData_Result.make(ErrorCode.Success, clubPlayerRoomAloneLogBO);

    }

    /**
     * 亲友圈战绩统计成绩单。
     *
     * @param clubId 亲友圈ID
     */
    public SData_Result rankedZhongZhi(long clubId, long unionId, int getType, int pageNum, int type, int gameType, Player player) {
        //权限校验
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "rankedZhongZhi null == unionId {%d}", unionId);
        }
        if (union.getUnionBO().getRankedOpenZhongZhi() == UnionDefine.UNION_CLOSE_OPEN.CLOSE.value()) {
            //关闭情况下 如果不是圈主 和赛事管理员 则权限不足
            ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), clubId);
            if (clubMember.isNotClubCreate() && !clubMember.isUnionMgr()) {
                return SData_Result.make(ErrorCode.UNION_POWER_ERROR, "UNION_POWER_ERROR", unionId);
            }
        }
        // 查询列表
        List<ClubPlayerRoomAloneLogBOZhongZhi> logBOs = null;
        // 查询器
        Criteria criteria = this.clubSchoolReportConditionsZhongZhi(clubId, getType, type, gameType);

        if (Objects.nonNull(criteria)) {
            logBOs = ((PlayerRoomAloneBOService) ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class))
                    .findAllE(criteria, ClubPlayerRoomAloneLogBOZhongZhi.class, ClubPlayerRoomAloneLogBOZhongZhi.getItemsNameZhongZhi(type));
        }
        if (Objects.isNull(logBOs)) {
            return SData_Result.make(ErrorCode.Success, ClubRankedZhongZhi.make(new ArrayList<>(), new ClubRankedZhongZhiSelf(player.getShortPlayer())));
        }
        // 返回数据
        List<ClubPlayerRoomAloneLogBOZhongZhi> logBOS = logBOs.stream().filter(k -> Objects.nonNull(k)).map(k -> {
            Player temPlayer = PlayerMgr.getInstance().getPlayer(k.getPid());
            if (Objects.nonNull(temPlayer)) {
                k.setPlayer(temPlayer.getShortPlayer());
                return k;
            } else {
                return null;
            }
        }).filter(k -> Objects.nonNull(k)).sorted(Comparator.comparing(ClubPlayerRoomAloneLogBOZhongZhi::getItemsValue).reversed()).collect(Collectors.toList());
        //重新赋值客户端所需要的序数id
        int selfId = 0;
        double selfValuye = 0;
        for (int i = 0; i < logBOS.size(); i++) {
            logBOS.get(i).setId(i + 1);
            if (logBOS.get(i).getPid() == player.getPid()) {
                selfId = i + 1;
                selfValuye = logBOS.get(i).getItemsValue();
            }
        }
        logBOS = logBOS.stream().skip(Page.getPageNum(pageNum))
                .limit(Page.PAGE_SIZE).collect(Collectors.toList());

        return SData_Result.make(ErrorCode.Success, ClubRankedZhongZhi.make(logBOS, new ClubRankedZhongZhiSelf(selfId, player.getShortPlayer(), selfValuye)));
    }


}
