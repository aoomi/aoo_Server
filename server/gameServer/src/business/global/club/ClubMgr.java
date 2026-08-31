package business.global.club;

import business.global.config.GameListConfigMgr;
import business.global.room.NormalRoomMgr;
import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.RoomImpl;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerClub;
import business.shareplayer.SharePlayerMgr;
import cenum.RoomTypeEnum;
import cenum.VisitSignEnum;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.ehcache.DataConstants;
import com.ddm.server.common.ehcache.EhCacheFactory;
import com.ddm.server.common.ehcache.configuration.SqlCacheConfiguration;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.JsonUtil;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import core.db.service.clarkGame.ClubMemberRelationBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.*;
import jsproto.c2s.cclass.club.Club_define.Club_BASICS;
import jsproto.c2s.cclass.club.Club_define.Club_MINISTER;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.cclass.room.RoomInfoDetails;
import jsproto.c2s.cclass.room.RoomInfoItem;
import jsproto.c2s.cclass.room.RoomInfoItemShortOne;
import jsproto.c2s.cclass.room.RoomPosInfoShort;
import jsproto.c2s.cclass.union.*;
import jsproto.c2s.iclass.club.CClub_Join;
import jsproto.c2s.iclass.club.CClub_PromotionShowList;
import jsproto.c2s.iclass.union.CUnion_Base;
import jsproto.c2s.iclass.union.CUnion_Change;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 亲友圈的总管理操作
 *
 * @author Administrator
 */
@Data
public class ClubMgr {
    /**
     * 亲友圈成员管理
     */
    protected ClubMemberMgr clubMemberMgr;
    /**
     * 亲友圈管理
     */
    protected ClubListMgr clubListMgr;
    /**
     * 亲友圈战绩
     */
    protected ClubRankMgr clubRankMgr;

    public ClubMgr() {
        this.setClubMemberMgr(new ClubMemberMgr());
        this.setClubListMgr(new ClubListMgr());
        this.setClubRankMgr(new ClubRankMgr());
    }

    /**
     * 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
     */
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static ClubMgr instance = new ClubMgr();
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static ClubMgr getInstance() {
        return SingletonHolder.instance;
    }

    public void init() {
        // 初始化亲友圈成员
        this.getClubMemberMgr().init();
        // 初始化亲友圈
        this.getClubListMgr().init();
    }

    /**
     * 游戏关闭返回预开的房卡
     **/
    public void onGiveBackOnGameClose() {
        clubListMgr.onGiveBackOnGameClose();
    }

    /**
     * 亲友圈共享房间列信息转换
     *
     * @param clubID
     * @param room
     * @return
     */
    public RoomInfoItem getRoomInfoItem(long clubID, ShareRoom room) {
        RoomInfoItem roomInfoItem = new RoomInfoItem();
        roomInfoItem.setRoomName("");
        roomInfoItem.setRoomKey(room.getRoomKey());
        roomInfoItem.setGameId(room.getGameId());
        roomInfoItem.setSetCount(room.getSetCount());
        roomInfoItem.setPlayerNum(room.getPlayerNum());
        roomInfoItem.setSort(room.sorted());
        roomInfoItem.setId(clubID);
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
     * 亲友圈共享房间列信息转换
     * 获取信息简化版
     *
     * @param clubID
     * @param room
     * @return
     */
    public RoomInfoItemShortOne getRoomInfoItemShort(long clubID, ShareRoom room) {
        RoomInfoItemShortOne roomInfoItem = new RoomInfoItemShortOne();
        roomInfoItem.setRoomKey(room.getRoomKey());
        roomInfoItem.setGameId(room.getGameId());
        roomInfoItem.setPlayerNum(room.getPlayerNum());
        roomInfoItem.setSort(room.sorted());
        roomInfoItem.setTagId(room.getTagId());
        if (room.isNoneRoom()) {
        } else {
            roomInfoItem.setSetId(room.getSetId());
        }
        return roomInfoItem;
    }

    /**
     * 获取俱乐部房间
     *
     * @param clubID
     * @param room
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RoomInfoItem getRoomInfoItem(long clubID, RoomImpl room) {
        RoomInfoItem roomInfoItem = new RoomInfoItem();
        roomInfoItem.setRoomName("");
        roomInfoItem.setRoomKey(room.getRoomKey());
        roomInfoItem.setGameId(room.getBaseRoomConfigure().getGameType().getId());
        roomInfoItem.setSetCount(room.getBaseRoomConfigure().getBaseCreateRoom().getSetCount());
        roomInfoItem.setPlayerNum(room.getBaseRoomConfigure().getBaseCreateRoom().getPlayerNum());
        roomInfoItem.setSort(room.sorted());
        roomInfoItem.setId(clubID);
        roomInfoItem.setClose(false);
        roomInfoItem.setTagId(room.getBaseRoomConfigure().getTagId());
        roomInfoItem.setPassword(room.getBaseRoomConfigure().getBaseCreateRoom().getPassword());
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
     * 获取俱乐部房间
     * 简化版
     *
     * @param clubID
     * @param room
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RoomInfoItemShortOne getRoomInfoItemShort(long clubID, RoomImpl room) {
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

    @SuppressWarnings({"rawtypes"})
    public List<RoomInfoItem> onClubGetAllRoom(long clubID, int pageNum) {
        if (clubID <= 0L) {
            return Collections.emptyList();
        }
        if (Config.isShare()) {
            return ShareRoomMgr.getInstance().roomClubValues(RoomTypeEnum.CLUB, clubID).stream().map(k -> getRoomInfoItem(clubID, k)).filter(k -> null != k).collect(Collectors.toList());
        } else {
            return NormalRoomMgr.getInstance().roomClubValues(RoomTypeEnum.CLUB, clubID, pageNum).stream().map(k -> getRoomInfoItem(clubID, k)).filter(k -> null != k).collect(Collectors.toList());
        }
    }

    /**
     * 信息简化
     *
     * @param clubID
     * @param pageNum
     * @return
     */
    public List<RoomInfoItem> onClubGetAllRoomData(long clubID, int pageNum, List<Long> roomKey) {
        if (clubID <= 0L) {
            return Collections.emptyList();
        }
        if (Config.isShare()) {
            return ShareRoomMgr.getInstance().roomClubValues(RoomTypeEnum.CLUB, clubID).stream().map(k -> getRoomInfoItem(clubID, k)).filter(k -> null != k && roomKey.contains(Long.valueOf(k.getRoomKey()))).collect(Collectors.toList());
        } else {
            return NormalRoomMgr.getInstance().roomClubValues(RoomTypeEnum.CLUB, clubID, pageNum).stream().map(k -> getRoomInfoItem(clubID, k)).filter(k -> null != k && roomKey.contains(Long.valueOf(k.getRoomKey()))).collect(Collectors.toList());
        }
    }

    /**
     * 获取信息简化版
     *
     * @param clubID
     * @param pageNum
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    public List<RoomInfoItemShortOne> onClubGetAllRoomShort(long clubID, int pageNum) {
        if (clubID <= 0L) {
            return Collections.emptyList();
        }
        if (Config.isShare()) {
            return ShareRoomMgr.getInstance().roomClubValues(RoomTypeEnum.CLUB, clubID).stream().map(k -> getRoomInfoItemShort(clubID, k)).filter(k -> null != k).collect(Collectors.toList());
        } else {
            return NormalRoomMgr.getInstance().roomClubValues(RoomTypeEnum.CLUB, clubID, pageNum).stream().map(k -> getRoomInfoItemShort(clubID, k)).filter(k -> null != k).collect(Collectors.toList());
        }
    }

    /**
     * 操作获取亲友圈列表
     *
     * @param player 玩家
     */
    public SData_Result<?> onGetClubListByPlayer(Player player) {
        List<ClubMember> findClubIdAllClubId = this.getClubMemberMgr().findPidAllGetClubList(player.getPid(), Club_Player_Status.PLAYER_JIARU.value());
        return SData_Result.make(ErrorCode.Success, findClubIdAllClubId.stream().filter(k -> null != k).sorted(Comparator.comparing(ClubMember::getTopTime).reversed()).map(k -> getClubInfo(k, player)).filter(k -> null != k).collect(Collectors.toList()));

    }

    /**
     * 操作获取亲友圈列表
     * 简化版
     *
     * @param player 玩家
     */
    public SData_Result<?> onGetClubListByPlayerShort(Player player) {
        List<ClubMember> findClubIdAllClubId = this.getClubMemberMgr().findPidAllGetClubList(player.getPid(), Club_Player_Status.PLAYER_JIARU.value());
        return SData_Result.make(ErrorCode.Success, findClubIdAllClubId.stream().filter(k -> null != k).sorted(Comparator.comparing(ClubMember::getTopTime).reversed()).map(k -> getClubInfoShort(k, player)).filter(k -> null != k).collect(Collectors.toList()));

    }

    /**
     * 操作获取亲友圈列表
     * 简化版
     *
     * @param player 玩家
     */
    public SData_Result<?> onGetClubListByPlayerShort2(Player player) {
        List<ClubMember> findClubIdAllClubId = this.getClubMemberMgr().findPidAllGetClubList(player.getPid(), Club_Player_Status.PLAYER_JIARU.value());
        return SData_Result.make(ErrorCode.Success, findClubIdAllClubId.stream().filter(k -> null != k).sorted(Comparator.comparing(ClubMember::getTopTime).reversed()).map(k -> getClubInfoShort2(k, player)).filter(k -> null != k).collect(Collectors.toList()));

    }

    /**
     * 操作获取亲友圈列表
     *
     * @param player 玩家
     */
    public SData_Result<?> onGetClubListByPlayerByClubId(Player player, long clubId) {
        ClubMember findClubIdAllClubId = this.getClubMemberMgr().find(player.getPid(), clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == findClubIdAllClubId) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        player.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        player.setSignEnumClubID(clubId);
        //更新共享玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(player, "signEnum", "signEnumClubID");
        }
        return SData_Result.make(ErrorCode.Success, getClubInfo(findClubIdAllClubId, player));
    }

    /**
     * 获取某一个俱乐部的消息
     */
    public final ClubInfo getClubInfo(final ClubMember member, final Player player) {
        // 获取亲友圈数据
        Club club = this.getClubListMgr().findClub(member.getClubID());
        if (null == club) {
            CommLogD.info("null == club ClubID:{},ClubName:{}", member.getClubID(), member.getStatus());
            return null;
        }
        // 亲友圈信息
        ClubInfo cInfo = club.getClubInfo();
        if (null == cInfo) {
            CommLogD.info("null == cInfo ClubID:{},ClubName:{}", member.getClubID(), member.getStatus());
            return null;
        }
        if (member.isClubCreate()) {
            cInfo.setKicking(1);
            cInfo.setModifyValue(1);
            cInfo.setShowShare(1);
            cInfo.setInvite(1);
        } else if (member.isLevelPromotion()) {
            cInfo.setKicking(member.getClubMemberBO().getKicking());
            cInfo.setModifyValue(member.getClubMemberBO().getModifyValue());
            cInfo.setShowShare(member.getClubMemberBO().getShowShare());
            cInfo.setInvite(member.getClubMemberBO().getInvite());
        }
        cInfo.setLevelPromotion(member.getClubMemberBO().getLevel());
        cInfo.setPromotion(member.getClubMemberBO().getPromotion());
        cInfo.setMinister(member.getClubMemberBO().getIsminister());
        cInfo.setIsPromotionManage(member.getPromotionManage());
        cInfo.setPromotionManagePid(member.getPromotionManagePid());
        cInfo.setExistApply(existApply(cInfo.getMinister(), cInfo.getId()));
        cInfo.setPlayerClubCard(player.getFeature(PlayerClub.class).getPlayerClubRoomCard(cInfo.getAgentsID(), cInfo.getLevel()));
        cInfo = this.setUnionInfo(club, cInfo, player.getPid(), member);
        Player tempPlayer = PlayerMgr.getInstance().getPlayer(club.getOwnerPlayerId());
        if (Objects.nonNull(tempPlayer)) {
            cInfo.setClubCreateName(tempPlayer.getName());
        }
        if (club.isZhongZhiClub()) {
            this.calcGameIdListAndSportsDoubleList(cInfo, club, player.getPid());
        }
        return cInfo;
    }

    /**
     * 中至计算已经有的房间信息
     *
     * @param cInfo
     * @param club
     */
    private void calcGameIdListAndSportsDoubleList(ClubInfo cInfo, Club club, long pid) {
        ClubMember clubMember = this.getClubMemberMgr().find(pid, club.getClubListBO().getId());
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.nonNull(clubMember) && Objects.nonNull(union)) {
            List<UnionRoomConfigItem> roomConfigItems = union.getUnionRoomConfigItemListZhongZhi(clubMember);
            List<Integer> gameIdList = roomConfigItems.stream().map(k -> k.getGameId()).distinct().collect(Collectors.toList());
            List<Double> sportsDoubleList = roomConfigItems.stream().map(k -> k.getSportsDouble()).distinct().collect(Collectors.toList());
            cInfo.setGameIdList(gameIdList);
            cInfo.setSportsDoubleList(sportsDoubleList);
        }
    }

    /**
     * 获取某一个俱乐部的消息
     * 简化版
     */
    public final ClubInfoShort getClubInfoShort(final ClubMember member, final Player player) {
        // 获取亲友圈数据
        Club club = this.getClubListMgr().findClub(member.getClubID());
        if (null == club) {
            CommLogD.info("null == club ClubID:{},ClubName:{}", member.getClubID(), member.getStatus());
            return null;
        }
        // 亲友圈信息
        ClubInfoShort cInfo = club.getClubInfoShort();
        if (null == cInfo) {
            CommLogD.info("null == cInfo ClubID:{},ClubName:{}", member.getClubID(), member.getStatus());
            return null;
        }
        cInfo.setPlayerClubCard(player.getFeature(PlayerClub.class).getPlayerClubRoomCard(cInfo.getAgentsID(), cInfo.getLevel()));
        cInfo = this.setUnionInfoShort(club, cInfo, player.getPid(), member);
        return cInfo;
    }

    /**
     * 获取某一个俱乐部的消息
     * 简化版2
     */
    public final ClubInfoShort2 getClubInfoShort2(final ClubMember member, final Player player) {
        // 获取亲友圈数据
        Club club = this.getClubListMgr().findClub(member.getClubID());
        if (null == club) {
            CommLogD.info("null == club ClubID:{},ClubName:{}", member.getClubID(), member.getStatus());
            return null;
        }
        // 亲友圈信息
        ClubInfoShort2 cInfo = club.getClubInfoShort2();
        if (null == cInfo) {
            CommLogD.info("null == cInfo ClubID:{},ClubName:{}", member.getClubID(), member.getStatus());
            return null;
        }
        cInfo = this.setUnionInfoShort2(club, cInfo, player.getPid(), member);
        return cInfo;
    }

    /**
     * 存在申请 1存在，0不存在
     *
     * @param minister 权限
     * @param clubId   亲友圈Id
     * @return
     */
    private Integer existApply(int minister, long clubId) {
        if (minister != Club_MINISTER.Club_MINISTER_GENERAL.value()) {
            return ClubMgr.getInstance().getClubMemberMgr().isExistApply(clubId) ? 1 : 0;
        }
        return null;
    }

    /**
     * 获取某一个俱乐部的消息
     */
    public ClubInfo getClubInfo(Club club, Player player) {
        ClubInfo cInfo = club.getClubInfo();
        ClubMember member = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), club.getClubListBO().getId(), Club_Player_Status.PLAYER_JIARU);
        cInfo.setLevelPromotion(member.getClubMemberBO().getLevel());
        cInfo.setPromotion(member.getClubMemberBO().getPromotion());
        cInfo.setMinister(member.getClubMemberBO().getIsminister());
        cInfo.setExistApply(existApply(cInfo.getMinister(), cInfo.getId()));
        cInfo.setPlayerClubCard(player.getFeature(PlayerClub.class).getPlayerClubRoomCard(cInfo.getAgentsID(), cInfo.getLevel()));
        cInfo = this.setUnionInfo(club, cInfo, player.getPid(), member);
        return cInfo;
    }

    /**
     * 设置赛事联盟信息
     *
     * @param club  亲友圈
     * @param cInfo 亲友圈信息
     * @param pid   pid
     * @return
     */
    public ClubInfo setUnionInfo(Club club, ClubInfo cInfo, long pid, ClubMember member) {
        UnionInfo unionInfo = club.getUnionInfo(pid);
        if (Objects.nonNull(unionInfo)) {
            cInfo.setUnionId(unionInfo.getUnionId());
            cInfo.setUnionName(unionInfo.getUnionName());
            cInfo.setShowUplevelId(unionInfo.getShowUplevelId());
            cInfo.setShowClubSign(unionInfo.getShowClubSign());
            cInfo.setUnionPostType(unionInfo.getUnionPostType());
            cInfo.setUnionSign(unionInfo.getUnionSign());
            cInfo.setSportsPoint(member.getClubMemberBO().getSportsPoint());
            cInfo.setUnionType(unionInfo.getUnionType());
            cInfo.setEliminatePoint(member.getClubMemberBO().getEliminatePoint());
            //如果个人预警值有打开的话
            if (member.getClubMemberBO().getPersonalWarnStatus() == 1) {
                cInfo.setPersonalSportsPointWarning(member.getClubMemberBO().getPersonalSportsPointWarning());
            }
            cInfo.setOwnerClubName(unionInfo.getOwnerClubName());
            cInfo.setUnionStateType(unionInfo.getUnionStateType());
            cInfo.setSort(unionInfo.getSort());
            cInfo.setTableNum(unionInfo.getTableNum());
            cInfo.setSkinTable(unionInfo.getSkinTable());
            cInfo.setSkinBackColor(unionInfo.getSkinBackColor());
            cInfo.setZhongZhiShowStatus(unionInfo.getZhongZhiShowStatus());
            cInfo.setExistRound(unionInfo.getRoundId() == member.getClubMemberBO().getRoundId() ? UnionDefine.UNION_ROUND_STATE.ROUND_CUR.ordinal() : UnionDefine.UNION_ROUND_STATE.ROUND_UP.ordinal());
            if (cInfo.getExistRound().intValue() == UnionDefine.UNION_ROUND_STATE.ROUND_UP.ordinal()) {
                // 提示上回合奖励
                String rankingReward = member.getClubMemberBO().getRankingReward();
                if (StringUtils.isNotEmpty(rankingReward)) {
                    cInfo.setUnionRankingItem(new Gson().fromJson(rankingReward.trim(), UnionRankingItem.class));
                }
            }
            if (Objects.nonNull(unionInfo.getUnionPostType()) && UnionDefine.UNION_POST_TYPE.UNION_CREATE.value() == unionInfo.getUnionPostType().intValue()) {
                cInfo.setUnionState(member.getClubMemberBO().getUnionState());
            } else {
                cInfo.setUnionState(member.getClubMemberBO().getUnionState(unionInfo.getOutSportsPoint(), unionInfo.getUnionId()));
            }
            cInfo.setOutSportsPoint(unionInfo.getOutSportsPoint());
            cInfo.setEndRoundTime(unionInfo.getEndRoundTime());
            cInfo.setShowLostConnect(unionInfo.getShowLostConnect() == 1 || member.isMinister() ? 0 : 1);
            cInfo.setCaseStatus(unionInfo.getCaseStatus());
            cInfo.setRankedOpenZhongZhi(unionInfo.getRankedOpenZhongZhi());
            cInfo.setRankedOpenEntryZhongZhi(unionInfo.getRankedOpenEntryZhongZhi());
        } else {
            cInfo.setShowLostConnect(club.getClubListBO().getShowLostConnect() == 1 || member.isMinister() ? 0 : 1);
        }
        return cInfo;
    }

    /**
     * 设置赛事联盟信息
     *
     * @param club  亲友圈
     * @param cInfo 亲友圈信息
     * @param pid   pid
     * @return
     */
    public ClubInfoShort setUnionInfoShort(Club club, ClubInfoShort cInfo, long pid, ClubMember member) {
        UnionInfo unionInfo = club.getUnionInfo(pid);
        if (Objects.nonNull(unionInfo)) {
            cInfo.setUnionId(unionInfo.getUnionId());
            cInfo.setShowUplevelId(unionInfo.getShowUplevelId());
            cInfo.setShowClubSign(unionInfo.getShowClubSign());
        } else {
        }
        return cInfo;
    }

    /**
     * 设置赛事联盟信息
     *
     * @param club  亲友圈
     * @param cInfo 亲友圈信息
     * @param pid   pid
     * @return
     */
    public ClubInfoShort2 setUnionInfoShort2(Club club, ClubInfoShort2 cInfo, long pid, ClubMember member) {
        UnionInfo unionInfo = club.getUnionInfo(pid);
        if (Objects.nonNull(unionInfo)) {
            cInfo.setUnionId(unionInfo.getUnionId());
//            cInfo.setShowUplevelId(unionInfo.getShowUplevelId());
//            cInfo.setShowClubSign(unionInfo.getShowClubSign());
        } else {
        }
        return cInfo;
    }

    /**
     * 加入亲友圈
     *
     * @param player 玩家信息
     * @param data   加入key
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result onJoinClub(Player player, CClub_Join data) {
        // 查找指定的亲友圈信息
        Club club = this.getClubListMgr().findClub(data.clubSign);
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "error clubSign:{%d}", data.clubSign);
        }
        // 已经加入亲友圈
        ClubMember member = this.getClubMemberMgr().find(player.getPid(), club.getClubListBO().getId());
        if (null != member) {
            if (member.getStatus(Club_Player_Status.PLAYER_JIARU.value())) {
                // 玩家已加入本亲友圈
                return SData_Result.make(ErrorCode.CLUB_EXIST_JIARU, "onJoinClub CLUB_EXIST_JIARU");
            } else if (member.getStatus(Club_Player_Status.PLAYER_WEIPIZHUN.value())) {
                // 玩家已申请加入亲友圈,等待管理员批准中
                return SData_Result.make(ErrorCode.CLUB_PLAYER_WEIPIZHUN, "onJoinClub CLUB_PLAYER_WEIPIZHUN");
            }
        } else {
            if (UnionMgr.getInstance().getUnionMemberMgr().checkExistInUnionOtherClub(club.getClubListBO().getId(), player.getPid())) {
                // 同赛事不同亲友圈不能重复拉人
                return SData_Result.make(ErrorCode.CLUB_PLAYER_EXIT_IN_OTHER_UNION, "onJoinClub CLUB_PLAYER_EXIT_IN_OTHER_UNION");
            }
            if (this.getClubMemberMgr().checkClubMemberUpperLimit(club.getClubListBO().getId())) {
                // 俱乐部人数已满
                return SData_Result.make(ErrorCode.CLUB_MEMBER_UPPER_LIMIT, "onJoinClub CLUB_MEMBER_UPPER_LIMIT");
            } else if (this.getClubMemberMgr().checkPlayerClubUpperLimit(player.getPid())) {
                // 自己加入的俱乐部数达到上限
                return SData_Result.make(ErrorCode.CLUB_PLAYER_UPPER_LIMIT, "onJoinClub CLUB_PARTNER_UPPER_LIMIT");
            }
        }

        SData_Result result = ClubMember.checkExistJoinOrQuitTimeLimit(player.getPid(), club.getClubListBO().getId(), Club_Player_Status.PLAYER_JIARU.value(), true);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 获取亲友圈配置
        ClubConfig config = club.getClubListBO().getClubConfigJson();
        Club_define.CLUB_JOIN join = Club_define.CLUB_JOIN.valueOf(club.getClubListBO().getJoin());
        if (Club_define.CLUB_JOIN.CLUB_JOIN_NEED_AUDIT.equals(join) && (null == config || config.getBasics().contains(Club_BASICS.APPROVAL.ordinal()))) {
            // 进入亲友圈需审批
            if (this.getClubMemberMgr().onJoin(player, club.getClubListBO().getId(), club.clubListBO.getOwnerID())) {
                // 未受到批准
                return SData_Result.make(ErrorCode.Success, Club_Player_Status.PLAYER_WEIPIZHUN.value());
            }
        } else {
            // 只要输入亲友圈key就可以加入亲友圈
            if (this.getClubMemberMgr().onInsertClubMember(player, club, Club_MINISTER.Club_MINISTER_GENERAL.value(), club.clubListBO.getOwnerID())) {
                // 已加入亲友圈
                return SData_Result.make(ErrorCode.Success, Club_Player_Status.PLAYER_JIARU.value());
            }
        }
        return SData_Result.make(ErrorCode.NotAllow, "onJoinClub error");
    }


    /**
     * 获取邀请列表
     */
    public List<ClubInvitedInfo> onClubInvited(long pid) {
        return this.getClubMemberMgr().findPidAll(pid, Club_Player_Status.PLAYER_YAOQING.value()).stream().filter(k -> null != k && k.getClubMemberBO().getStatus() == Club_Player_Status.PLAYER_YAOQING.value()).map(k -> {
            Club club = this.getClubListMgr().findClub(k.getClubID());
            if (null != club) {
                return new ClubInvitedInfo(k.getClubID(), club.getClubListBO().getClubsign(), club.getClubListBO().getName());
            }
            return null;
        }).filter(k -> null != k).collect(Collectors.toList());
    }

    /**
     * 查询加入的亲友圈id
     *
     * @param pid
     * @return
     */
    public Optional<List<Long>> findAllJoinClubByPid(long pid) {
        List<ClubMember> clubMembers = clubMemberMgr.findAll(pid, Club_Player_Status.PLAYER_JIARU.value(), true);
        if (clubMembers != null && !clubMembers.isEmpty()) {
            return Optional.of(clubMembers.stream().map(member -> member.getClubID()).collect(Collectors.toList()));
        }
        return Optional.ofNullable(null);
    }

    /**
     * 解算房间
     */
    public void dissolveRoom(long clubID, String roomKey, boolean isClose, boolean isNotifyRoomCountChange, Club_define.Club_DISSOLVEROOM_STATUS clubDissloveRoom) {
        AbsBaseRoom roomDelegateAbstract = NormalRoomMgr.getInstance().getRoomByKey(roomKey);
        if (null == roomDelegateAbstract) {
            CommLogD.error("null == roomDelegateAbstract roomKey:{}", roomKey);
            return;
        }
        if (null != roomDelegateAbstract && roomDelegateAbstract.getRoomState() == RoomState.Init) {
            roomDelegateAbstract.doDissolveRoom(clubDissloveRoom.value());
        }
        // 移除房间ID和房间Key
        RoomMgr.getInstance().removeRoom(roomDelegateAbstract.getRoomID(), roomDelegateAbstract.getBaseRoomConfigure().getPrizeType());
        roomDelegateAbstract = null;
    }


    /**
     * 获取亲友圈房间详情
     *
     * @param clubId
     * @param roomKey
     * @return
     */
    public SData_Result getRoomInfoDetails(long clubId, long pid, String roomKey) {
        if (StringUtils.isEmpty(roomKey)) {
            return SData_Result.make(ErrorCode.Room_NOT_Find, "Room_NOT_Find roomKey:{null}");
        }
        //从共享房间获取信息
        if (Config.isShare()) {
            ShareRoom room = ShareRoomMgr.getInstance().getShareRoomByKey(roomKey);
            if (null == room) {
                return SData_Result.make(ErrorCode.Room_NOT_Find, "Room_NOT_Find roomKey:{%s}", roomKey);
            }
            if (!RoomTypeEnum.CLUB.equals(room.getRoomTypeEnum()) || room.getSpecialRoomId() != clubId) {
                return SData_Result.make(ErrorCode.NotAllow, "RoomInfoDetails RoomTypeEnum:{%s},UniondId:{%d}", room.getRoomTypeEnum(), room.getSpecialRoomId());
            }
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
            if (null == club) {
                return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
            }
            return SData_Result.make(ErrorCode.Success, this.getRoomInfoDetails(clubId, ClubMgr.getInstance().getClubMemberMgr().getMinister(clubId, pid), club.getClubListBO().getName(), room));
        } else {
            RoomImpl room = NormalRoomMgr.getInstance().getNoneRoomByKey(roomKey);
            if (null == room) {
                return SData_Result.make(ErrorCode.Room_NOT_Find, "Room_NOT_Find roomKey:{%s}", roomKey);
            }
            if (!RoomTypeEnum.CLUB.equals(room.getRoomTypeEnum()) || room.getSpecialRoomId() != clubId) {
                return SData_Result.make(ErrorCode.NotAllow, "RoomInfoDetails RoomTypeEnum:{%s},UniondId:{%d}", room.getRoomTypeEnum(), room.getSpecialRoomId());
            }
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
            if (null == club) {
                return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
            }
            return SData_Result.make(ErrorCode.Success, this.getRoomInfoDetails(clubId, ClubMgr.getInstance().getClubMemberMgr().getMinister(clubId, pid), club.getClubListBO().getName(), room));
        }

    }

    /**
     * 获取赛事共享房间详情
     *
     * @param clubId
     * @param room
     * @return
     */
    @SuppressWarnings({})
    private RoomInfoDetails getRoomInfoDetails(long clubId, int isManage, String name, ShareRoom room) {
        RoomInfoDetails roomInfoDetails = new RoomInfoDetails();
        roomInfoDetails.setName(name);
        roomInfoDetails.setRoomName("");
        roomInfoDetails.setRoomKey(room.getRoomKey());
        roomInfoDetails.setGameId(room.getGameId());
        roomInfoDetails.setSetCount(room.getSetCount());
        roomInfoDetails.setPlayerNum(room.getPlayerNum());
        roomInfoDetails.setRoomCfg(new Gson().fromJson(room.getBaseRoomConfigure().getShareBaseCreateRoom(), Map.class));
        roomInfoDetails.setRoomStateId(room.getRoomState().value());
        roomInfoDetails.setIsManage(isManage);
        roomInfoDetails.setClubId(clubId);
        roomInfoDetails.setTagId(room.getTagId());
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
     * @param clubId
     * @param room
     * @return
     */
    @SuppressWarnings({})
    private RoomInfoDetails getRoomInfoDetails(long clubId, int isManage, String name, RoomImpl room) {
        RoomInfoDetails roomInfoDetails = new RoomInfoDetails();
        roomInfoDetails.setName(name);
        roomInfoDetails.setRoomName("");
        roomInfoDetails.setRoomKey(room.getRoomKey());
        roomInfoDetails.setGameId(room.getBaseRoomConfigure().getGameType().getId());
        roomInfoDetails.setSetCount(room.getBaseRoomConfigure().getBaseCreateRoom().getSetCount());
        roomInfoDetails.setPlayerNum(room.getBaseRoomConfigure().getBaseCreateRoom().getPlayerNum());
        roomInfoDetails.setRoomCfg(room.getBaseRoomConfigure().getBaseCreateRoomT());
        roomInfoDetails.setRoomStateId(room.getRoomState().value());
        roomInfoDetails.setIsManage(isManage);
        roomInfoDetails.setClubId(clubId);
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
     * 切换圈主
     *
     * @param clubSign    亲友圈Id
     * @param oldPlayer   旧玩家
     * @param newPlayer   新玩家
     * @param isUnionClub 是否联盟赛事
     * @return
     */
    public SData_Result changeCreate(int clubSign, Player oldPlayer, Player newPlayer, boolean isUnionClub) {
        Club club = getClubListMgr().findClub(clubSign);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.InvalidParam, "error club");
        }
        if (isUnionClub && club.getClubListBO().getUnionId() <= 0) {
            return SData_Result.make(ErrorCode.InvalidParam, "error club");
        } else if (!isUnionClub && club.getClubListBO().getUnionId() > 0) {
            return SData_Result.make(ErrorCode.InvalidParam, "error club");
        }
        ClubMember oldClubMember = getClubMemberMgr().findMinister(oldPlayer.getPid(), club.getClubListBO().getId(), Club_MINISTER.Club_MINISTER_CREATER);
        if (Objects.isNull(oldClubMember)) {
            return SData_Result.make(ErrorCode.InvalidParam, "error oldClubMember Pid:{%d},ClubId:{%d}", oldPlayer.getPid(), club.getClubListBO().getId());
        }
        ClubMember newClubMember = getClubMemberMgr().find(newPlayer.getPid(), club.getClubListBO().getId());
        if (Objects.isNull(newClubMember)) {
            newClubMember = getClubMemberMgr().insertCreaterMember(club.getClubListBO().getId(), newPlayer.getPid());
        }
        if (Objects.isNull(newClubMember)) {
            return SData_Result.make(ErrorCode.InvalidParam, "error newClubMember Pid:{%d},ClubId:{%d}", newPlayer.getPid(), club.getClubListBO().getId());
        }
        //成为盟主的那个人 不能是推广员
        if (newClubMember.isPromotion() || newClubMember.isLevelPromotion()) {
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        //更新旧的关联表数据
        changeCreateLeader(oldClubMember.getId(), newClubMember.getId());
        //删除无用的数据
        deleteNewCreateLeader(newClubMember.getId());
        //变更
        Map<Long, ClubMember> allClubMember = ShareClubMemberMgr.getInstance().getAllOneClubMember(club.getClubListBO().getId());
        for (Map.Entry<Long, ClubMember> con : allClubMember.entrySet()) {
            if (con.getValue().getClubMemberBO().getUpLevelId() == oldClubMember.getId()) {
                con.getValue().getClubMemberBO().saveUpLevelId(newClubMember.getId());
            }
        }
        // 切换为创建者
        club.updateOwnerPlayer(newPlayer);
        newClubMember.getClubMemberBO().changeCreate();
        // 切换为普通成员
        oldClubMember.getClubMemberBO().changeGeneral();
        //分成修改
        changePromotionValue(oldClubMember, newClubMember);
        return SData_Result.make(ErrorCode.Success, new CUnion_Change(club.getClubListBO().getUnionId(), club.getClubListBO().getId(), oldPlayer.getPid(), newClubMember.getId()));
    }

    /**
     * 新旧圈主 分成信息修改
     *
     * @param oldClubMember
     * @param newClubMember
     */
    private void changePromotionValue(ClubMember oldClubMember, ClubMember newClubMember) {
        int oldType = oldClubMember.getClubMemberBO().getShareType();
        double oldShareValue = oldClubMember.getClubMemberBO().getShareValue();
        double oldShareFixedValue = oldClubMember.getClubMemberBO().getShareFixedValue();
        oldClubMember.getClubMemberBO().saveShareType(newClubMember.getClubMemberBO().getShareType());
        oldClubMember.getClubMemberBO().saveShareValue(newClubMember.getClubMemberBO().getShareValue());
        oldClubMember.getClubMemberBO().saveShareFixedValue(newClubMember.getClubMemberBO().getShareFixedValue());
        newClubMember.getClubMemberBO().saveShareType(oldType);
        newClubMember.getClubMemberBO().saveShareValue(oldShareValue);
        newClubMember.getClubMemberBO().saveShareFixedValue(oldShareFixedValue);
    }

    /**
     * 更新旧的无用的数据
     *
     * @param uid
     * @param puid
     */
    public void changeCreateLeader(Long uid, Long puid) {
        String sql = String.format("UPDATE `ClubMemberRelation` SET puid = " + puid + "  WHERE  puid=" + uid);
        ContainerMgr.get().getComponent(ClubMemberRelationBOService.class).getDefaultDao().execute(sql);

    }

    /**
     * 删除新的无用数据
     *
     * @param uid
     */
    public void deleteNewCreateLeader(Long uid) {
        String sql = String.format("DELETE FROM `ClubMemberRelation`  WHERE  uid=" + uid);
        ContainerMgr.get().getComponent(ClubMemberRelationBOService.class).getDefaultDao().execute(sql);

    }

    /**
     * 获取亲友圈在线人数
     *
     * @return
     */
    public SData_Result execClubOnlinePlayerCount(CUnion_Base base, Player player) {
        String key = String.format(DataConstants.CLUB_ONLINE_COUNT, base.getClubId());
        UnionOnlineCount unionOnlineCount = EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).get(key, UnionOnlineCount.class);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(base.getClubId());
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), base.getClubId());
        if (Objects.nonNull(unionOnlineCount)) {
            if (Objects.nonNull(club) && club.getClubListBO().getShowOnlinePlayerNum() == Club_define.CLUB_PLAYERNUM_SHOW.PROMOTION_NOT_SEE.value() &&
                    Objects.nonNull(clubMember) && !clubMember.isMinister()) {
                unionOnlineCount.setCount(-1);
                //传-1  客户端处理
            }
            return SData_Result.make(ErrorCode.Success, unionOnlineCount.getCount());
        }
        int count = ClubMgr.getInstance().getClubMemberMgr().clubIdToOnlinePlayerCount(base.getClubId());
        if (Objects.nonNull(club) && club.getClubListBO().getShowOnlinePlayerNum() == Club_define.CLUB_PLAYERNUM_SHOW.PROMOTION_NOT_SEE.value() &&
                Objects.nonNull(clubMember) && !clubMember.isMinister()) {
            count = -1;
            //传-1  客户端处理
        }
        EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).put(key, new UnionOnlineCount(count));
        return SData_Result.make(ErrorCode.Success, count);
    }


    /**
     * 清空玩家合伙人绑定
     *
     * @return
     */
    public SData_Result clearPartnerPid(int clubSian, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubSian);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(pid, club.getClubListBO().getId(), Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        clubMember.getClubMemberBO().savePartnerPid(0L);
        return SData_Result.make(ErrorCode.Success, "success");
    }

    /**
     * 清空合伙人
     *
     * @param clubSian
     * @param pid
     * @return
     */
    public SData_Result clearPartner(int clubSian, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubSian);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(pid, club.getClubListBO().getId(), Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        clubMember.getClubMemberBO().clearPartner();
        return SData_Result.make(ErrorCode.Success, "success");

    }


    /**
     * 设置显示断开连接
     *
     * @param clubSian
     * @return
     */
    public SData_Result showLostConnect(long clubSian, int type) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubSian);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        club.getClubListBO().saveShowLostConnect(type <= 0 ? 0 : 1);
        return SData_Result.make(ErrorCode.Success, "success");
    }

    public SData_Result savePromotionShowList(CClub_PromotionShowList promotionShowList, Long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionShowList.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        ClubMember doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(promotionShowList.getClubId(), pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", promotionShowList.getClubId(), pid);
        }
        if (!doClubMember.isClubCreate()) {
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE doClubMember ClubId:{},Pid:{}", promotionShowList.getClubId(), pid);
        }
        if (promotionShowList.getShowList().size() > 9) {
            return SData_Result.make(ErrorCode.CLUB_PROMOTION_SHOW_THAN_NINE, "promotionShowList is than 9");
        }
        ClubPromotionShowConfig config = new ClubPromotionShowConfig(promotionShowList.getShowList(), promotionShowList.getShowListSecond());
        club.getClubListBO().savePromotionShowConfig(config);
        return SData_Result.make(ErrorCode.Success);
    }
}
