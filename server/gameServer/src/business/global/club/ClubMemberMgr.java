package business.global.club;

import BaseCommon.CommLog;
import business.global.room.NormalRoomMgr;
import business.global.shareclub.LocalClubMemberMgr;
import business.global.shareclub.ShareClubListMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.shareclub.SharePromotionSectionMgr;
import business.global.sharegm.ShareInitMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerCurrency;
import business.rocketmq.bo.MqClubMemberNotifyBo;
import business.rocketmq.bo.MqClubMemberUpdateNotifyBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.LocalPlayerMgr;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import business.utils.ClubMemberUtils;
import business.utils.TimeConditionUtils;
import cenum.ItemFlow;
import cenum.Page;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.GameConfig;
import com.ddm.server.common.ehcache.DataConstants;
import com.ddm.server.common.ehcache.EhCacheFactory;
import com.ddm.server.common.ehcache.configuration.DefaultCacheConfiguration;
import com.ddm.server.common.ehcache.configuration.UnionZhongZhiCountInfoCacheConfiguration;
import com.ddm.server.common.ehcache.configuration.WarningSportsCacheConfiguration;
import com.ddm.server.common.redis.DistributedRedisLock;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.TypeUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import core.db.dao.clarkGame.ClubMemberBODao;
import core.db.entity.clarkGame.*;
import core.db.entity.clarkLog.ExamineLogFlow;
import core.db.entity.clarkLog.RoomPromotionPointLogFlow;
import core.db.other.AsyncInfo;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.service.clarkGame.*;
import core.db.service.clarkLog.*;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.club.ClubPromotionSectionChangeEvent;
import core.dispatch.event.promotion.PromotionLevelChangeEvent;
import core.dispatch.event.promotion.PromotionLevelDeleteEvent;
import core.dispatch.event.promotion.PromotionLevelInsertEvent;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import core.network.client2game.handler.club.ClubPersonalSportsPointWarningItem;
import core.network.client2game.handler.club.ClubReservedValueItem;
import core.network.client2game.handler.club.ClubSportsPointWarningItem;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.Player.ShortPlayer;
import jsproto.c2s.cclass.PlayerHeadImageUrl;
import jsproto.c2s.cclass.club.*;
import jsproto.c2s.cclass.club.Club_define.Club_MINISTER;
import jsproto.c2s.cclass.club.Club_define.Club_PARTNER;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.union.*;
import jsproto.c2s.iclass.club.*;
import jsproto.c2s.iclass.union.*;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 亲友圈成员管理操作
 *
 * @author Administrator
 */
@Data
public class ClubMemberMgr {
    // 俱乐部成员管理map
    protected Map<Long, ClubMember> clubMemberMap = Maps.newConcurrentMap();
    private ClubMemberBOService clubMemberBOService;

    public ClubMemberMgr() {
        clubMemberBOService = ContainerMgr.get().getComponent(ClubMemberBOService.class);
    }

    /**
     * 初始化
     */
    public void init() {
        CommLogD.info("[ClubMemberBO.init] load ClubMemberBO begin...]");
        //初始化redis没有数据才需要读数据库
        if (ShareInitMgr.getInstance().getShareDataInit()) {
            List<ClubMemberBO> arg0 = clubMemberBOService.findAll(null);
            int status = Club_Player_Status.PLAYER_YAOQING.value() | Club_Player_Status.PLAYER_WEIPIZHUN.value()
                    | Club_Player_Status.PLAYER_JIARU.value();
            for (ClubMemberBO clubMemberBO : arg0) {
                if ((clubMemberBO.getStatus() & status) > 0) {
                    getClubMemberMap().put(clubMemberBO.getId(), new ClubMember(clubMemberBO));
                    //共享数据
                    if (Config.isShare() && ShareInitMgr.getInstance().getShareDataInit()) {
                        if (!ShareClubMemberMgr.getInstance().existClubMember(clubMemberBO.getId())) {
                            CommLogD.info("[ClubMemberBO.init] load ClubMemberBO add {}]", clubMemberBO.getId());
                            ShareClubMemberMgr.getInstance().addClubMember(getClubMemberMap().get(clubMemberBO.getId()));
                            this.PromotionShareSectionInit(clubMemberBO);
                        }
                    }
                }
            }
            arg0 = null;
        } else {
            //从redis读取数据到内存
            onUpdateAllMemberShare();
        }
        CommLogD.info("[ClubMemberBO.init] load ClubMemberBO end]");
    }

    /**
     * 初始化亲友圈成员区间分成redis
     *
     * @param clubMemberBO
     * @return
     */
    private void PromotionShareSectionInit(ClubMemberBO clubMemberBO) {
        if (UnionDefine.UNION_SHARE_TYPE.SECTION.ordinal() == clubMemberBO.getShareType()) {
            List<PromotionShareSectionItem> promotionShareSectionItems = ((PromotionShareSectionBOService) ContainerMgr.get().getComponent(PromotionShareSectionBOService.class)).findAllE(Restrictions.and(
                    Restrictions.eq("pid", clubMemberBO.getPlayerID()), Restrictions.eq("clubId", clubMemberBO.getClubID())), PromotionShareSectionItem.class, PromotionShareSectionItem.getItemsName());
            SharePromotionSectionMgr.getInstance().addClubMemberPromotionSection(getClubMemberMap().get(clubMemberBO.getId()),
                    new SharePromotionSection(promotionShareSectionItems));
        }
    }

    /**
     * 更新亲友圈玩家数据
     */
    public void onUpdateMemberShare(long id) {
        ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
        if (clubMember != null) {
            getClubMemberMap().put(id, clubMember);
        } else {
            CommLogD.error("ClubMember[{}] is null", id);
        }
    }

    /**
     * 更新亲友圈玩家状态
     */
    public void onUpdateMemberStatusShare(long id) {
        ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
        if (clubMember != null) {
            if (getClubMemberMap().containsKey(id)) {
                ClubMember clubMemberLocal = getClubMemberMap().get(id);
                clubMemberLocal.getClubMemberBO().setStatus(clubMember.getStatus());
            } else {
                getClubMemberMap().put(id, clubMember);
                CommLogD.error("ClubMember[{}] local is exist", id);
            }

        } else {
            CommLogD.error("ClubMember[{}] is null", id);
        }
    }

    /**
     * 添加玩家会员
     *
     * @param id
     */
    public void onInsertMemberLocal(long id) {
        ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
        if (clubMember != null && !getClubMemberMap().containsKey(id)) {
            getClubMemberMap().put(id, clubMember);
        } else {
            CommLogD.error("insert ClubMember[{}] is null", id);
        }
    }

    /**
     * 更新亲友圈玩家数据
     */
    public void onDeleteMemberLocal(long id) {
        getClubMemberMap().remove(id);
    }

    /**
     * 更新亲友圈所有玩家数据在服务切换的时候
     */
    public void onUpdateAllMemberShare() {
        long startTime = System.currentTimeMillis();
        Map<Long, ClubMember> clubMemberMap = ShareClubMemberMgr.getInstance().getAllClubMember();
        setClubMemberMap(clubMemberMap);
        if (Config.isShareLocal()) {
            LocalClubMemberMgr.getInstance().initClubMember(clubMemberMap);
        }
//        ShareClubMemberMgr.getInstance().initShareOneClubMemberArrayKey(clubMemberMap);
        CommLogD.info("reload local ClubMemberMap time:{}", System.currentTimeMillis() - startTime);
    }

//    /**
//     * 更新中至数据
//     * @param clubMemberMap
//     */
//    public void upDateZhongLevle( Map<Long, ClubMember> clubMemberMap){
//        clubMemberMap.entrySet().stream().forEach(k->{
//            Club club=ClubMgr.getInstance().getClubListMgr().findClub(k.getValue().getClubID());
//            if(Objects.nonNull(club)&&club.isZhongZhiClub()){
//                k.getValue().getClubMemberBO().saveLevelZhongZhi(k.getValue().getClubMemberBO().getLevel());
//            }
//        });
//    }

    /**
     * 修改职务
     *
     * @param id
     * @param isMinister
     * @return
     */
    public boolean onUpdateIsMinister(long id, int isMinister) {
        ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
        if (Objects.isNull(clubMember)) {
            return false;
        }
        clubMember.getClubMemberBO().saveIsminister(isMinister);
        return true;
    }

//    /**
//     * 更新玩家
//     */
//    public boolean onUpdateStatus(long id) {
//        ClubMemberBO clubMemberBO = getClubMemberBOService().findOne(id, null);
//        if (Objects.isNull(clubMemberBO)) {
//            return false;
//        }
//        ClubMember clubMember = null;
//        if(Config.isShare()){
//            clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
//        } else {
//            clubMember = getClubMemberMap().get(id);
//        }
//        if (Objects.isNull(clubMember)) {
//            clubMember = new ClubMember(clubMemberBO);
//            if (((clubMemberBO.getStatus() & Club_Player_Status.PLAYER_YAOQING.value()) > 0)
//                    || (clubMemberBO.getStatus() & Club_Player_Status.PLAYER_JIARU.value()) > 0
//                    || ((clubMemberBO.getStatus() & Club_Player_Status.PLAYER_WEIPIZHUN.value()) > 0)) {
//                getClubMemberMap().put(clubMemberBO.getId(), clubMember);
//                //共享数据
//                if(Config.isShare()) {
//                    ShareClubMemberMgr.getInstance().addClubMember(clubMember);
//                }
//            }
//        } else {
//            clubMember.onUpdateStatus(clubMemberBO);
//            //共享数据
//            if(Config.isShare()) {
//                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
//            }
//            if (clubMember.getStatus(Club_Player_Status.PLAYER_JUJIE.value()
//                    | Club_Player_Status.PLAYER_JUJIEYAOQING.value() | Club_Player_Status.PLAYER_TICHU.value()
//                    | Club_Player_Status.PLAYER_TICHU_CLOSE.value() | Club_Player_Status.PLAYER_TUICHU.value())) {
//                getClubMemberMap().remove(clubMemberBO.getId());
//                //共享数据
//                if(Config.isShare()) {
//                    ShareClubMemberMgr.getInstance().deleteClubMember(clubMemberBO.getId());
//                }
//            }
//        }
//        // 获取玩家信息
//        Player player = PlayerMgr.getInstance().getPlayer(clubMember.getClubMemberBO().getPlayerID());
//        if (Objects.isNull(player)) {
//            // 找不到玩家信息
//            CommLogD.error("onUpdateStatus null == player Pid:{}", clubMember.getClubMemberBO().getPlayerID());
//            return false;
//        }
//        // 获取亲友圈信息
//        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubMember.getClubID());
//        if (Objects.isNull(club)) {
//            // 找不到亲友圈信息
//            CommLogD.error("onUpdateStatus null == club ClubID:{}", clubMember.getClubID());
//            return false;
//        }
//        this.onGiveReward(player, club, clubMember.getStatus());
//        // 通知玩家本身和所有的管理员
//        this.notify2AllClubMemberAndPid(player, club, clubMember);
//        return this.invitedPlayer(club, player, clubMember.getStatus());
//    }

    /**
     * 是否存在申请加入亲友圈的玩家
     *
     * @return
     */
    public boolean isExistApply(long clubId) {
        return this.getClubMemberMap().values().stream().anyMatch(k -> k.getClubID() == clubId && (k.getStatus(Club_Player_Status.PLAYER_WEIPIZHUN.value()) || k.getStatus(Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value())));
//        if (Config.isShare()) {
//            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().anyMatch(k -> k.getClubID() == clubId && (k.getStatus(Club_Player_Status.PLAYER_WEIPIZHUN.value()) || k.getStatus(Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value())));
//        } else {
//            return this.getClubMemberMap().values().stream().anyMatch(k -> k.getClubID() == clubId && (k.getStatus(Club_Player_Status.PLAYER_WEIPIZHUN.value()) || k.getStatus(Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value())));
//        }
    }

    /**
     * 查询指定亲友圈ID玩家
     *
     * @param clubID 亲友圈ID
     * @param status 状态
     * @return
     */
    public List<ClubMember> findClubIdAllClubMember(long clubID, int status) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status)).collect(Collectors.toList());
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Million-member safe response shape. New protocols must use this cursor API
     * instead of pageNum/OFFSET or findClubIdAllClubMember.
     */
    public core.club.ClubMemberPage<ClubMember> findClubMemberPage(
            core.club.ClubMemberCursor cursor) {
        Collection<ClubMember> source = Config.isShare()
                ? ShareClubMemberMgr.getInstance().getAllOneClubMember(cursor.clubId()).values()
                : this.getClubMemberMap().values();
        int candidateLimit = cursor.limit() + 1;
        PriorityQueue<ClubMember> candidates = new PriorityQueue<>(candidateLimit,
                Comparator.comparingLong((ClubMember member) -> member.getClubMemberBO().getId()).reversed());
        for (ClubMember member : source) {
            if (member.getClubID() != cursor.clubId() || !member.getStatus(cursor.status())
                    || member.getClubMemberBO().getId() <= cursor.afterMemberId()) continue;
            candidates.offer(member);
            if (candidates.size() > candidateLimit) candidates.poll();
        }
        List<ClubMember> selected = new ArrayList<>(candidates);
        selected.sort(Comparator.comparingLong(member -> member.getClubMemberBO().getId()));
        boolean hasMore = selected.size() > cursor.limit();
        if (hasMore) selected = new ArrayList<>(selected.subList(0, cursor.limit()));
        long nextMemberId = selected.isEmpty() ? cursor.afterMemberId()
                : selected.get(selected.size() - 1).getClubMemberBO().getId();
        return new core.club.ClubMemberPage<>(selected, nextMemberId, hasMore);
    }

    public SData_Result<?> getMemberManageCursor(long clubId, long operatorPid,
            long afterMemberId, int limit, int pageType) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "club not found");
        }
        ClubMember operator = this.getClubMember(clubId, operatorPid);
        if (Objects.isNull(operator)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "member not found");
        }
        if (!this.isMinister(clubId, operatorPid)) {
            return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "permission denied");
        }
        int status = Club_Player_Status.PLAYER_JIARU.value();
        if (pageType == 1) status = Club_Player_Status.PLAYER_WEIPIZHUN.value();
        else if (pageType == 2) status = Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value();
        else if (pageType != 0) return SData_Result.make(ErrorCode.NotAllow, "invalid page type");

        core.club.ClubMemberPage<ClubMember> page;
        try {
            page = this.findClubMemberPage(new core.club.ClubMemberCursor(clubId, status, afterMemberId, limit));
        } catch (IllegalArgumentException error) {
            return SData_Result.make(ErrorCode.NotAllow, "invalid cursor");
        }
        List<ClubPlayerInfo> items = page.items().stream().map(member -> {
            Player target = PlayerMgr.getInstance().getPlayer(member.getClubMemberBO().getPlayerID());
            if (Objects.isNull(target)) return null;
            return member.getClubPlayerInfo(target, club.getClubListBO().getAgentsID(),
                    club.getClubListBO().getLevel(), member.isBanGame(),
                    member.getClubMemberBO().getPromotion(), member.getSportsPoint(club));
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return SData_Result.make(ErrorCode.Success,
                new jsproto.c2s.cclass.club.ClubMemberCursorPage(items, page.nextMemberId(), page.hasMore()));
    }

//    /**
//     * 查询指定亲友圈ID玩家
//     *
//     * @param clubID 亲友圈ID
//     * @param status 状态
//     * @return
//     */
//    public List<ClubMember> findClubIdAllClubMemberLosePoint(long clubID, int status, int losePoint) {
//        if (Config.isShare()) {
//            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && (losePoint == 1 ? k.getSportsPoint() < 0D : true))
//                    .collect(Collectors.toList());
//        } else {
//            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && (losePoint == 1 ? k.getSportsPoint() < 0D : true))
//                    .collect(Collectors.toList());
//        }
//    }

//    /**
//     * 查询指定亲友圈ID玩家
//     * 只显示在线
//     *
//     * @param clubID 亲友圈ID
//     * @param status 状态
//     * @return
//     */
//    public List<ClubMember> findClubIdAllClubMemberOnline(long clubID, int status, int losePoint) {
//        if (Config.isShare()) {
//            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && (losePoint == 1 ? k.getSportsPoint() < 0D : true) && PlayerMgr.getInstance().checkExistOnlinePlayerByPid(k.getClubMemberBO().getPlayerID()))
//                    .collect(Collectors.toList());
//        } else {
//            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && (losePoint == 1 ? k.getSportsPoint() < 0D : true) && PlayerMgr.getInstance().checkExistOnlinePlayerByPid(k.getClubMemberBO().getPlayerID()))
//                    .collect(Collectors.toList());
//        }
//    }

//    /**
//     * 查询指定亲友圈ID玩家
//     * 只显示在线
//     *
//     * @param clubID 亲友圈ID
//     * @param status 状态
//     * @return
//     */
//    public List<ClubMember> findClubIdAllClubMemberOnline(long clubID, int status) {
//        if (Config.isShare()) {
//            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && PlayerMgr.getInstance().checkExistOnlinePlayerByPid(k.getClubMemberBO().getPlayerID()))
//                    .collect(Collectors.toList());
//        } else {
//            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && PlayerMgr.getInstance().checkExistOnlinePlayerByPid(k.getClubMemberBO().getPlayerID()))
//                    .collect(Collectors.toList());
//        }
//    }

    /**
     * 查询指定亲友圈ID玩家
     * 只显示在线
     *
     * @param clubIdList 亲友圈列表
     * @return
     */
    public List<Long> findClubIdAllClubMemberOnline(final List<Long> clubIdList) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().findClubIdAllClubMemberOnline(clubIdList);
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && !(k.isBanGame() || k.isUnionBanGame())).map(k -> k.getClubMemberBO().getPlayerID()).distinct().collect(Collectors.toList());
        }
    }

//    /**
//     * 根据亲友圈Id查询成员
//     *
//     * @param clubID 亲友圈Id
//     * @param status 状态
//     * @param getType   0 所有玩家，1只显示在线玩家
//     * @return
//     */
//    public List<ClubMember> findClubIdAllClubMember(long clubID, int status, int getType) {
//        if (getType <= 0) {
//            return findClubIdAllClubMember(clubID, status);
//        }
//        return findClubIdAllClubMemberOnline(clubID, status);
//    }


//    /**
//     * 根据亲友圈Id查询成员
//     *
//     * @param clubID    亲友圈Id
//     * @param status    状态
//     * @param getType      0 所有玩家，1只显示在线玩家
//     * @param losePoint 0: 所有分数，1只显示输分
//     * @return
//     */
//    public List<ClubMember> findClubIdAllClubMember(long clubID, int status, int getType, int losePoint) {
//        if (getType <= 0) {
//            return findClubIdAllClubMemberLosePoint(clubID, status, losePoint);
//        }
//        return findClubIdAllClubMemberOnline(clubID, status, losePoint);
//    }

    /**
     * 根据亲友圈Id查询成员
     *
     * @param clubMembers 亲友圈成员
     * @param clubID      亲友圈Id
     * @param status      状态
     * @param type        0 所有玩家，1只显示在线玩家
     * @param losePoint   0: 所有分数，1只显示输分
     * @return
     */
    public List<ClubMember> findClubIdAllClubMemberShare(Map<Long, ClubMember> clubMembers, long clubID, int status, int type, int losePoint) {
        if (type <= 0) {
            return clubMembers.values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && (losePoint == 1 ? k.getSportsPoint() < 0D : true))
                    .collect(Collectors.toList());
        }
        return clubMembers.values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && (losePoint == 1 ? k.getSportsPoint() < 0D : true) && PlayerMgr.getInstance().checkExistOnlinePlayerByPid(k.getClubMemberBO().getPlayerID()))
                .collect(Collectors.toList());
    }

    /**
     * 根据亲友圈Id查询成员
     *
     * @param clubMembers 亲友圈成员
     * @param clubID      亲友圈Id
     * @param status      状态
     * @param type        0 所有玩家，1只显示在线玩家
     * @param losePoint   0: 所有分数，1只显示输分
     * @return
     */
    public List<ClubMember> findClubIdAllClubMemberShareZhongZhi(Map<Long, ClubMember> clubMembers, long clubID, int status, int type, int losePoint) {
        return clubMembers.values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && (losePoint == 1 ? k.getSportsPoint() < 0D : true))
                .collect(Collectors.toList());
    }

    /**
     * 根据亲友圈Id查询成员
     *
     * @param clubMembers 亲友圈成员
     * @param clubID      亲友圈Id
     * @param status      状态
     * @return
     */
    public List<ClubMember> findClubIdAllClubMemberShareAddCaptionZhongZhi(Map<Long, ClubMember> clubMembers, long clubID, int status) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(club.getOwnerPlayer().getPid(), clubID);
        return clubMembers.values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) &&
                        (clubMember.getId() == k.getClubMemberBO().getUpLevelId()) && k.isNotLevelPromotion() && k.isNotClubCreate())
                .collect(Collectors.toList());
    }

    /**
     * 根据亲友圈Id查询成员
     * 查出不是圈主的其他人
     *
     * @param clubMembers 亲友圈成员
     * @param clubID      亲友圈Id
     * @param status      状态
     * @return
     */
    public List<ClubMember> findClubIdAllClubMemberShareChangeAlivePointZhongZhi(Map<Long, ClubMember> clubMembers, long clubID, int status) {
        return clubMembers.values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && !k.isClubCreate())
                .collect(Collectors.toList());
    }

    /**
     * 根据亲友圈Id查询成员
     *
     * @param clubMembers 亲友圈成员
     * @param clubID      亲友圈Id
     * @param status      状态
     * @return
     */
    public List<ClubMember> findClubIdAllClubMemberShareChangePromotionZhongZhi(Map<Long, ClubMember> clubMembers, long clubID, int status, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(club.getOwnerPlayer().getPid(), clubID);
//        ClubMember upClubmember=ClubMgr.getInstance().getClubMemberMgr().find(pid,clubID);
        return clubMembers.values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status) && !k.isClubCreate() && k.isNotLevelPromotion() &&
                        (clubMember.getId() == k.getClubMemberBO().getUpLevelId()
//                        ||upClubmember.getId()==k.getClubMemberBO().getUpLevelId()
                        ))
                .collect(Collectors.toList());
    }

    /**
     * 根据亲友圈Id查询成员
     *
     * @param clubMembers 亲友圈成员
     * @param clubID      亲友圈Id
     * @param status      状态
     * @param type        0 所有玩家，1只显示在线玩家
     * @param losePoint   0: 所有分数，1只显示输分
     * @return
     */
    public List<String[]> findClubIdAllClubMemberShare(List<String[]> clubMembers, long clubID, int status, int type, int losePoint) {
        if (type <= 0) {
            return clubMembers.stream().filter(k -> ClubMemberUtils.getArrayValueLong(k, "clubID") == clubID && ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), status) && (losePoint == 1 ? ClubMemberUtils.getArrayValueDouble(k, "sportsPoint") < 0D : true))
                    .collect(Collectors.toList());
        }
        if (Config.isShareLocal()) {
            Map<Long, SharePlayer> shareOnlinePlayer = LocalPlayerMgr.getInstance().getLocalOnlinePlayerMap();
            return clubMembers.stream().filter(k -> ClubMemberUtils.getArrayValueLong(k, "clubID") == clubID && ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), status) && (losePoint == 1 ? ClubMemberUtils.getArrayValueDouble(k, "sportsPoint") < 0D : true) && shareOnlinePlayer.containsKey(ClubMemberUtils.getArrayValueLong(k, "playerID")))
                    .collect(Collectors.toList());
        } else {
            Set<String> onlinePlayerIds = SharePlayerMgr.getInstance().onlineSharePlayerIds();
            return clubMembers.stream().filter(k -> ClubMemberUtils.getArrayValueLong(k, "clubID") == clubID && ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), status) && (losePoint == 1 ? ClubMemberUtils.getArrayValueDouble(k, "sportsPoint") < 0D : true) && onlinePlayerIds.contains(ClubMemberUtils.getArrayValueString(k, "playerID")))
                    .collect(Collectors.toList());
        }

    }

    /**
     * 根据亲友圈Id查询成员
     *
     * @param clubMembers 亲友圈成员
     * @param clubID      亲友圈Id
     * @param status      状态
     * @param type        0 所有玩家，1只显示在线玩家
     * @param losePoint   0: 所有分数，1只显示输分
     * @return
     */
    public List<String[]> findClubIdAllClubMemberShareZhongZhi(List<String[]> clubMembers, long clubID, int status, int type, int losePoint) {
        return clubMembers.stream().filter(k -> ClubMemberUtils.getArrayValueLong(k, "clubID") == clubID && ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), status) && (losePoint == 1 ? ClubMemberUtils.getArrayValueDouble(k, "sportsPoint") < 0D : true))
                .collect(Collectors.toList());

    }

    public List<String[]> clubMembersToList(Map<String, String> clubMembers) {
        List<String[]> list = clubMembers.values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).collect(Collectors.toList());
        return list;
    }

    /**
     * 俱乐部人数已满
     *
     * @param clubID 亲友圈ID
     * @return
     */
    public boolean checkClubMemberUpperLimit(long clubID) {
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream()
                        .filter(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                        .count() >= GameConfig.ClubMemberUpperLimit();
            } else {
                return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k))
                        .filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()))
                        .count() >= GameConfig.ClubMemberUpperLimit();
            }

        } else {
            return this.getClubMemberMap().values().stream()
                    .filter(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                    .count() >= GameConfig.ClubMemberUpperLimit();
        }
    }

    /**
     * 自己加入的俱乐部数达到上限
     *
     * @param pid 玩家PID
     * @return
     */
    public boolean checkPlayerClubUpperLimit(long pid) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOnePlayerClubMember(pid).values().stream().filter(
                            k -> k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                    .count() >= GameConfig.ClubPlayerJoinUpperLimit();
        } else {
            return this.getClubMemberMap().values().stream().filter(
                            k -> k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                    .count() >= GameConfig.ClubPlayerJoinUpperLimit();
        }
    }


    public List<Long> playerClubList(long pid) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOnePlayerClubMember(pid).values().stream().filter(k -> k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).map(k -> k.getClubID()).collect(Collectors.toList());
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).map(k -> k.getClubID()).collect(Collectors.toList());
        }
    }

    /**
     * 检查是否管理员上限
     *
     * @param clubID 亲友圈ID
     * @return
     */
    public boolean checkClubMinisterUpperLimit(long clubID) {
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().filter(
                                k -> k.getClubID() == clubID && k.isMinister() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                        .count() >= GameConfig.ClubMinisterUpperLimit();
            } else {
                return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k))
                        .filter(k -> Club_MINISTER.Club_MINISTER_GENERAL.value() != ClubMemberUtils.getArrayValueInteger(k, "isminister") && ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()))
                        .count() >= GameConfig.ClubMinisterUpperLimit();
            }

        } else {
            return this.getClubMemberMap().values().stream().filter(
                            k -> k.getClubID() == clubID && k.isMinister() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                    .count() >= GameConfig.ClubMinisterUpperLimit();
        }
    }

    /**
     * 检查亲友圈成员状态是否存在
     *
     * @return
     */
    public boolean checkClubMemberStateExist(long clubID, long pid, int state) {
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubID, pid);
            if (clubMember != null && clubMember.getStatus(state)) {
                return true;
            } else {
                return false;
            }
        } else {
            return this.getClubMemberMap().values().stream()
                    .filter(k -> k.getClubID() == clubID && k.getClubMemberBO().getPlayerID() == pid && k.getStatus(state))
                    .findAny().isPresent();
        }
    }

    /**
     * 是否赛事成员
     *
     * @param clubId 亲友圈id
     * @param pid    玩家Pid
     * @return
     */
    public boolean anyMatch(long clubId, long pid) {
        Map<Long, ClubMember> clubMemberMap;
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, pid);
            return Objects.nonNull(clubMember) && clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value());
        } else {
            clubMemberMap = this.getClubMemberMap();
        }
        return clubMemberMap
                .values()
                .stream()
                .anyMatch(k -> k.getClubID() == clubId && k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()));
    }


    /**
     * 是否赛事成员
     * 并且没有被禁止游戏
     *
     * @param clubId 亲友圈id
     * @param pid    玩家Pid
     * @return
     */
    public boolean anyMatchNotBanGame(long clubId, long pid) {
        Map<Long, ClubMember> clubMemberMap;
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, pid);
            return Objects.nonNull(clubMember) && clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && !clubMember.isBanGame();
        } else {
            clubMemberMap = this.getClubMemberMap();
        }
        return clubMemberMap
                .values()
                .stream()
                .anyMatch(k -> k.getClubID() == clubId && k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && !k.isBanGame());
    }

    /**
     * 推广员列表
     *
     * @param clubId     亲友圈id
     * @param partnerPid 推广员pid
     * @return
     */
    public List<ClubMember> getPromotionList(long clubId, long partnerPid) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().filter(k -> k.getClubID() == clubId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isAppointPromotion() && (partnerPid <= 0L ? true : partnerPid == k.getClubMemberBO().getPartnerPid())).collect(Collectors.toList());
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isAppointPromotion() && (partnerPid <= 0L ? true : partnerPid == k.getClubMemberBO().getPartnerPid())).collect(Collectors.toList());
        }
    }

    /**
     * 推广员列表
     *
     * @param clubId 亲友圈id
     * @return
     */
    public List<ClubMember> getPromotionList(long clubId) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().filter(k -> k.getClubID() == clubId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isPromotion()).collect(Collectors.toList());
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isPromotion()).collect(Collectors.toList());
        }
    }

    /**
     * 推广员列表
     *
     * @param clubId 亲友圈id
     * @return
     */
    public List<String[]> getPromotionLevelListShare(long clubId, long id, int pageNum) {
        return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k -> (ClubMemberUtils.getArrayValueLong(k, "upLevelId") == id || (id <= 0L ? false : ClubMemberUtils.getArrayValueLong(k, "id") == id)) && (ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value())))
                .sorted(Comparator.comparing((String[] h) -> ClubMemberUtils.getIsLevel(ClubMemberUtils.getArrayValueInteger(h, "level"), ClubMemberUtils.getArrayValueInteger(h, "isminister")))
                        .thenComparing((String[] h) -> ClubMemberUtils.getTime(ClubMemberUtils.getArrayValueInteger(h, "deletetime"),
                                ClubMemberUtils.getArrayValueInteger(h, "creattime"),
                                ClubMemberUtils.getArrayValueInteger(h, "updatetime"),
                                ClubMemberUtils.getArrayValueInteger(h, "status"))))
                //傅哥前端考虑全部收消息 滑动渲染 服务端取消分页
//                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
//                .limit(Page.PAGE_SIZE_8)
                .collect(Collectors.toList());

    }

    /**
     * 推广员列表
     *
     * @param clubId 亲友圈id
     * @return
     */
    public List<String[]> geClubTeamListShare(long clubId, long id, int pageNum, List<Integer> queryList, long pid) {
        return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).
                filter(k -> (ClubMemberUtils.getArrayValueLong(k, "upLevelId") == id || (id <= 0L ? false : ClubMemberUtils.getArrayValueLong(k, "id") == id)) &&
                        (ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()))
                        && (pid > 0l ? true : ClubMemberUtils.getArrayValueInteger(k, "level") > 0) && CollectionUtils.isEmpty(queryList) ? true : queryList.contains(ClubMemberUtils.getArrayValueInteger(k, "levelZhongZhi")))
                .sorted(Comparator.comparing((String[] h) -> ClubMemberUtils.getIsLevel(ClubMemberUtils.getArrayValueInteger(h, "level"), ClubMemberUtils.getArrayValueInteger(h, "isminister")))
                        .thenComparing((String[] h) -> ClubMemberUtils.getTime(ClubMemberUtils.getArrayValueInteger(h, "deletetime"),
                                ClubMemberUtils.getArrayValueInteger(h, "creattime"),
                                ClubMemberUtils.getArrayValueInteger(h, "updatetime"),
                                ClubMemberUtils.getArrayValueInteger(h, "status"))))
                //傅哥前端考虑全部收消息 滑动渲染 服务端取消分页
//                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
//                .limit(Page.PAGE_SIZE_8)
                .collect(Collectors.toList());

    }

    /**
     * 推广员列表
     *
     * @param clubId 亲友圈id
     * @return
     */
    public List<String[]> getCompetitionRankedShare(long clubId, long id, int pageNum) {
        CommLogD.error("555555555555555555555");
        return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k))
                .filter(k -> ((ClubMemberUtils.getArrayValueLong(k, "upLevelId") == id || (id <= 0L ? false : ClubMemberUtils.getArrayValueLong(k, "id") == id)) && (ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()))))
                .sorted(Comparator.comparing((String[] h) -> ClubMemberUtils.getIsLevel(ClubMemberUtils.getArrayValueInteger(h, "level"), ClubMemberUtils.getArrayValueInteger(h, "isminister")))
                        .thenComparing((String[] h) -> ClubMemberUtils.getTime(ClubMemberUtils.getArrayValueInteger(h, "deletetime"),
                                ClubMemberUtils.getArrayValueInteger(h, "creattime"),
                                ClubMemberUtils.getArrayValueInteger(h, "updatetime"),
                                ClubMemberUtils.getArrayValueInteger(h, "status"))))
                //傅哥前端考虑全部收消息 滑动渲染 服务端取消分页
//                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
//                .limit(Page.PAGE_SIZE_8)
                .collect(Collectors.toList());

    }

    /**
     * 推广员列表
     *
     * @param clubId 亲友圈id
     * @param query  模糊搜索字段
     * @return
     */
    public List<String[]> getPromotionLevelListShare(long clubId, long id, final long qPid, int pageNum, String query) {
        // 查询pid
        return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k ->
                        (qPid == ClubMemberUtils.getArrayValueLong(k, "playerID") || (PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID")) != null && PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID")).getName().contains(query))) && ClubMemberUtils.getArrayValueLong(k, "upLevelId") == id && ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()))
                .sorted(Comparator.comparing((String[] h) -> ClubMemberUtils.getIsLevel(ClubMemberUtils.getArrayValueInteger(h, "level"), ClubMemberUtils.getArrayValueInteger(h, "isminister")))
                        .thenComparing((String[] h) -> ClubMemberUtils.getTime(ClubMemberUtils.getArrayValueInteger(h, "deletetime"),
                                ClubMemberUtils.getArrayValueInteger(h, "creattime"),
                                ClubMemberUtils.getArrayValueInteger(h, "updatetime"),
                                ClubMemberUtils.getArrayValueInteger(h, "status"))))
                //傅哥前端考虑全部收消息 滑动渲染 服务端取消分页
//                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
//                .limit(Page.PAGE_SIZE_8)
                .collect(Collectors.toList());

    }

    /**
     * 推广员列表
     *
     * @param clubId 亲友圈id
     * @param query  模糊搜索字段
     * @return
     */
    public List<String[]> getPromotionLevelListShareZhongZhi(long clubId, long id, final long qPid, int pageNum, String query, List<Integer> levelList, long pid) {
//        // 查询pid
        return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k ->
                        (qPid == ClubMemberUtils.getArrayValueLong(k, "playerID") || (PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID")) != null && PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID")).getName().contains(query))
                        ) && (pid > 0l ? true : ClubMemberUtils.getArrayValueInteger(k, "level") > 0L) && levelList.contains(ClubMemberUtils.getArrayValueInteger(k, "levelZhongZhi")) && ClubMemberUtils.getArrayValueLong(k, "upLevelId") == id && ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()))
                .sorted(Comparator.comparing((String[] h) -> ClubMemberUtils.getIsLevel(ClubMemberUtils.getArrayValueInteger(h, "level"), ClubMemberUtils.getArrayValueInteger(h, "isminister")))
                        .thenComparing((String[] h) -> ClubMemberUtils.getTime(ClubMemberUtils.getArrayValueInteger(h, "deletetime"),
                                ClubMemberUtils.getArrayValueInteger(h, "creattime"),
                                ClubMemberUtils.getArrayValueInteger(h, "updatetime"),
                                ClubMemberUtils.getArrayValueInteger(h, "status"))))
                //傅哥前端考虑全部收消息 滑动渲染 服务端取消分页
//                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
//                .limit(Page.PAGE_SIZE_8)
                .collect(Collectors.toList());

    }

    /**
     * 推广员列表
     *
     * @param clubId 亲友圈id
     * @return
     */
    public List<ClubMember> getPromotionLevelList(long clubId, long id, int pageNum) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().filter(k -> k.getClubID() == clubId && (k.getClubMemberBO().getUpLevelId() == id || (id <= 0L ? false : k.getId() == id)) && (k.getStatus(Club_Player_Status.PLAYER_JIARU.value())))
                    .sorted(Comparator.comparing(ClubMember::getIsLevel)
                            .thenComparing(ClubMember::getTime))
                    //傅哥前端考虑全部收消息 滑动渲染 服务端取消分页
//                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
//                .limit(Page.PAGE_SIZE_8)
                    .collect(Collectors.toList());
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubId && (k.getClubMemberBO().getUpLevelId() == id || (id <= 0L ? false : k.getId() == id)) && (k.getStatus(Club_Player_Status.PLAYER_JIARU.value())))
                    .sorted(Comparator.comparing(ClubMember::getIsLevel)
                            .thenComparing(ClubMember::getTime))
                    //傅哥前端考虑全部收消息 滑动渲染 服务端取消分页
//                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
//                .limit(Page.PAGE_SIZE_8)
                    .collect(Collectors.toList());
        }

    }

    /**
     * 推广员列表
     *
     * @param clubId 亲友圈id
     * @param query  模糊搜索字段
     * @return
     */
    public List<ClubMember> getPromotionLevelList(long clubId, long id, final long qPid, int pageNum, String query) {
        // 查询pid
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().filter(k ->
                            k.getClubID() == clubId && (qPid == k.getClubMemberBO().getPlayerID() || (PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()) != null && PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()).getName().contains(query))) && k.getClubMemberBO().getUpLevelId() == id && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                    .sorted(Comparator.comparing(ClubMember::getIsLevel)
                            .thenComparing(ClubMember::getTime))
                    //傅哥前端考虑全部收消息 滑动渲染 服务端取消分页
//                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
//                .limit(Page.PAGE_SIZE_8)
                    .collect(Collectors.toList());
        } else {
            return this.getClubMemberMap().values().stream().filter(k ->
                            k.getClubID() == clubId && (qPid == k.getClubMemberBO().getPlayerID() || (PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()) != null && PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()).getName().contains(query))) && k.getClubMemberBO().getUpLevelId() == id && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                    .sorted(Comparator.comparing(ClubMember::getIsLevel)
                            .thenComparing(ClubMember::getTime))
                    //傅哥前端考虑全部收消息 滑动渲染 服务端取消分页
//                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
//                .limit(Page.PAGE_SIZE_8)
                    .collect(Collectors.toList());
        }

    }

    /**
     * 推广员列表
     *
     * @param clubId 亲友圈id
     * @param query  模糊搜索字段
     * @return
     */
    public List<ClubMember> getPromotionLevelListIncludeAll(long clubId, long id, final long qPid, int pageNum, String query, List<Long> uidList) {
        List<ClubMember> clubMembers;
        if (Config.isShare()) {
            clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().filter(k -> uidList.contains(k.getClubMemberBO().getId())).collect(Collectors.toList());
        } else {
            clubMembers = this.getClubMemberMap().values().stream().filter(k -> uidList.contains(k.getClubMemberBO().getId())).collect(Collectors.toList());
        }
        return clubMembers.stream().filter(k -> k.getClubID() == clubId && (qPid == k.getClubMemberBO().getPlayerID() || (PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()) != null && PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()).getName().contains(query))) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                .sorted(Comparator.comparing(ClubMember::getIsLevel)
                        .thenComparing(ClubMember::getTime))
                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
                .limit(Page.PAGE_SIZE_8)
                .collect(Collectors.toList());


//        // 查询pid
//        return this.getClubMemberMap().values().stream().filter(k ->
//                k.getClubID() == clubId && (qPid == k.getClubMemberBO().getPlayerID()||(PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID())!=null&&PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()).getName().contains(query)))
//                        && k.getClubMemberBO().getUpLevelId() == id && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
//                .sorted(Comparator.comparing(ClubMember::getIsLevel)
//                        .thenComparing(ClubMember::getTime))
//                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_8))
//                .limit(Page.PAGE_SIZE_8)
//                .collect(Collectors.toList());

    }

    /**
     * 推广员列表下属
     *
     * @param clubId 亲友圈id
     * @return
     */
    @Deprecated
    public List<ClubMember> getSubordinateList(long clubId, long partnerPid) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().filter(k -> k.getClubID() == clubId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getPartnerPid() == partnerPid).collect(Collectors.toList());
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getPartnerPid() == partnerPid).collect(Collectors.toList());
        }
    }


    /**
     * 是推广员
     *
     * @param clubId 亲友圈id
     * @param pid    玩家Pid
     * @return
     */
    @Deprecated
    public ClubMember getPromotion(long clubId, long pid) {
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, pid);
            if (clubMember != null && clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && clubMember.isPromotion()) {
                return clubMember;
            } else {
                return null;
            }
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubId && k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isPromotion()).findAny().orElse(null);
        }
    }

    /**
     * 是推广员
     *
     * @param clubId 亲友圈id
     * @param pid    玩家Pid
     * @return
     */
    @Deprecated
    public boolean isPromotion(long clubId, long pid) {
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, pid);
            if (clubMember != null && clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && clubMember.isPromotion()) {
                return true;
            } else {
                return false;
            }
        } else {
            return this.getClubMemberMap().values().stream().anyMatch(k -> k.getClubID() == clubId && k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isPromotion());
        }
    }


    /**
     * 不是推广员
     *
     * @param clubId 亲友圈id
     * @param pid    玩家Pid
     * @return
     */
    @Deprecated
    public boolean isNotPromotion(long clubId, long pid) {
        return !isPromotion(clubId, pid);
    }


    /**
     * 是推广员
     *
     * @param clubId 亲友圈id
     * @param pid    玩家Pid
     * @return
     */
    public boolean isLevelPromotion(long clubId, long pid) {
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, pid);
            if (clubMember != null && clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && clubMember.isLevelPromotion()) {
                return true;
            } else {
                return false;
            }
        } else {
            return this.getClubMemberMap().values().stream().anyMatch(k -> k.getClubID() == clubId && k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isLevelPromotion());
        }
    }

    /**
     * 是推广员
     *
     * @param clubId 亲友圈id
     * @param pid    玩家Pid
     * @return
     */
    public boolean isNotLevelPromotion(long clubId, long pid) {
        return !isLevelPromotion(clubId, pid);
    }


    /**
     * 指定推广员的下属数量不为0
     *
     * @return
     */
    public boolean checkExistPromotionSubordinateNumberNotEqualZero(long clubID, long partnerPid) {
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().anyMatch(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isNotPromotion() && k.getClubMemberBO().getPartnerPid() == partnerPid);
            } else {
                return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).anyMatch(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()) && Club_define.Club_PROMOTION.CheckExpectedValue(Club_define.Club_PROMOTION.NOT, ClubMemberUtils.getArrayValueInteger(k, "promotion")) && ClubMemberUtils.getArrayValueLong(k, "partnerPid") == partnerPid);
            }
        } else {
            return this.getClubMemberMap().values().stream().anyMatch(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isNotPromotion() && k.getClubMemberBO().getPartnerPid() == partnerPid);
        }
    }

//    /**
//     * 查询指定亲友圈ID玩家
//     *
//     * @param clubID 亲友圈ID
//     * @param status 状态
//     * @return
//     */
//    public List<PlayerHeadImageUrl> findClubIdLimitHeadImage(long clubID, int status, int limit) {
//        if (Config.isShare()) {
//            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status))
//                    .map(k -> getHeadImageUrl(k.getClubMemberBO().getPlayerID())).filter(k -> null != k).limit(limit)
//                    .collect(Collectors.toList());
//        } else {
//            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(status))
//                    .map(k -> getHeadImageUrl(k.getClubMemberBO().getPlayerID())).filter(k -> null != k).limit(limit)
//                    .collect(Collectors.toList());
//        }
//    }

    /**
     * 获取玩家头像信息
     *
     * @param playerID
     * @return
     */
    private PlayerHeadImageUrl getHeadImageUrl(long playerID) {
        Player player = PlayerMgr.getInstance().getPlayer(playerID);
        if (Objects.isNull(player)) {
            return null;
        }
        return new PlayerHeadImageUrl(player.getPid(), player.getHeadImageUrl());
    }

    /**
     * 查询指定亲友圈ID玩家
     *
     * @param pid    亲友圈ID
     * @param status 状态
     * @return
     */
    public List<Long> findPidToClubId(long pid, int status) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOnePlayerClubMember(pid).values().stream()
                    .filter(k -> k.getClubMemberBO().getPlayerID() == pid && k.getStatus(status)).map(k -> k.getClubID())
                    .collect(Collectors.toList());
        } else {
            return this.getClubMemberMap().values().stream()
                    .filter(k -> k.getClubMemberBO().getPlayerID() == pid && k.getStatus(status)).map(k -> k.getClubID())
                    .collect(Collectors.toList());
        }
    }

    /**
     * 查询指定玩家
     *
     * @param pid    玩家Pid
     * @param status 状态
     * @return
     */
    public List<ClubMember> findPidAll(long pid, int status) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOnePlayerClubMember(pid).values().stream().filter(k -> k.checkPidEqual(pid) && k.getStatus(status))
                    .collect(Collectors.toList());
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.checkPidEqual(pid) && k.getStatus(status))
                    .collect(Collectors.toList());
        }

    }

    /**
     * 查询指定玩家
     * 获取亲友圈信息页面
     *
     * @param pid    玩家Pid
     * @param status 状态
     * @return
     */
    public List<ClubMember> findPidAllGetClubList(long pid, int status) {
        if (Config.isShare()) {
            return ShareClubMemberMgr.getInstance().getAllOnePlayerClubMember(pid).values().stream().filter(k -> k.checkPidEqual(pid) && k.getStatusGetClubList(status))
                    .collect(Collectors.toList());
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.checkPidEqual(pid) && k.getStatusGetClubList(status))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取亲友圈成员
     *
     * @param pid    玩家PID
     * @param clubID 亲友圈ID
     * @return
     */
    public ClubMember find(long pid, long clubID) {
        return this.find(pid, clubID, Club_Player_Status.PLAYER_ALL);
    }


    public ClubMember findCreate(long clubId) {
        if (Config.isShare()) {
            Club club = ShareClubListMgr.getInstance().getClub(clubId);
            if (Objects.isNull(club)) {
                CommLogD.error("findCreate clubId:{}", clubId);
                return null;
            }
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, club.getClubListBO().getOwnerID());
            return Objects.nonNull(clubMember) && clubMember.isClubCreate() ? clubMember : null;
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> null != k && k.getClubID() == clubId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getIsminister() == Club_MINISTER.Club_MINISTER_CREATER.value()).findAny().orElse(null);
        }
    }

    public double sumSportsPoint(long clubID) {
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                return CommMath.FormatDouble(ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).mapToDouble(ClubMember::getSportsPoint).sum());
            } else {
                return CommMath.FormatDouble(ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value())).mapToDouble(k -> ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")).sum());
            }
        } else {
            return CommMath.FormatDouble(this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).mapToDouble(ClubMember::getSportsPoint).sum());
        }
    }

    public double sumTotalSportsPoint(long clubID) {
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                return CommMath.FormatDouble(ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).mapToDouble(ClubMember::getTotalSportsPoint).sum());
            } else {
                return CommMath.FormatDouble(ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value())).mapToDouble(k -> ClubMemberUtils.getArrayValueDouble(k, "sportsPoint") + ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")).sum());
            }
        } else {
            return CommMath.FormatDouble(this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).mapToDouble(ClubMember::getTotalSportsPoint).sum());
        }

    }

    public double sumSportsPoint(List<Long> memberIdList) {
        // 总比赛分
        double sumSportsPoint = 0D;
        for (Long memberId : memberIdList) {
            ClubMember clubMember = null;
            if (Config.isShare()) {
                clubMember = ShareClubMemberMgr.getInstance().getClubMember(memberId);
            } else {
                clubMember = this.getClubMemberMap().get(memberId);
            }
            if (Objects.nonNull(clubMember)) {
                sumSportsPoint = CommMath.addDouble(sumSportsPoint, clubMember.getSportsPoint());
            }
        }
        return sumSportsPoint;
    }

    /**
     * 获取所有人的比赛分数  包括保险箱内的
     *
     * @param memberIdList
     * @return
     */
    public double sumTotalSportsPoint(List<Long> memberIdList) {
        // 总比赛分
        double sumSportsPoint = 0D;
        for (Long memberId : memberIdList) {
            ClubMember clubMember = null;
            if (Config.isShare()) {
                clubMember = ShareClubMemberMgr.getInstance().getClubMember(memberId);
            } else {
                clubMember = this.getClubMemberMap().get(memberId);
            }
            if (Objects.nonNull(clubMember)) {
                sumSportsPoint = CommMath.addDouble(sumSportsPoint, clubMember.getTotalSportsPoint());
            }
        }
        return sumSportsPoint;
    }

    /**
     * 赛事成员统计
     *
     * @param clubId 亲友圈id
     * @return
     */
    public Map<String, Object> unionMemberStatisticsMap(long clubId) {
        // 统计
        int count = 0;
        // 总比赛分
        double sumSportsPoint = 0D;
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                for (ClubMember clubMember : ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values()) {
                    if (clubId == clubMember.getClubID() && clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value())) {
                        count += 1;
                        sumSportsPoint = CommMath.addDouble(sumSportsPoint, clubMember.getTotalSportsPoint());
                    }
                }
            } else {
                for (String clubMemberString : ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values()) {
//                StringTokenizer stringTokenizer = new StringTokenizer(clubMemberString, ",");
//                int size = ShareClubMemberMgr.getInstance().getClubMemberIndexMap().size();
//                String[] clubMember = new String[47];
//                int i=0;
//                while(stringTokenizer.hasMoreTokens()){
//                    clubMember[i]=stringTokenizer.nextToken();
//                    i++;
//                }
//                System.out.println(i);
//                for (int i = 0; i < size; i++) {
//                    System.out.println(i);
//                    clubMember[i] = stringTokenizer.nextToken();
//                }
//                String[] clubMember=new String[51];
//                int i=0;
//                while (clubMemberString.indexOf(",")!=-1){
//                    String ss=clubMemberString.substring(0,clubMemberString.indexOf(","));
//                    clubMember[i]=ss;
//                    clubMemberString=clubMemberString.substring(clubMemberString.indexOf(",")+1);
////                    System.out.println(clubMemberString);
//                    i++;
//                }
                    String[] clubMember = ClubMemberUtils.stringSwitchArray(clubMemberString);
                    if (clubId == ClubMemberUtils.getArrayValueLong(clubMember, "clubID") && ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(clubMember, "status"), Club_Player_Status.PLAYER_JIARU.value())) {
                        count += 1;
                        sumSportsPoint = CommMath.addDouble(sumSportsPoint, ClubMemberUtils.getArrayValueDouble(clubMember, "sportsPoint") + ClubMemberUtils.getArrayValueDouble(clubMember, "caseSportsPoint"));
                    }
                }
            }

        } else {
            for (ClubMember clubMember : this.getClubMemberMap().values()) {
                if (clubId == clubMember.getClubID() && clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value())) {
                    count += 1;
                    sumSportsPoint = CommMath.addDouble(sumSportsPoint, clubMember.getTotalSportsPoint());
                }
            }
        }
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
        map.put("COUNT", count);
        map.put("SUM", sumSportsPoint);
        return map;
    }


    /**
     * 获取亲友圈人数
     *
     * @param clubID
     * @return
     */
    public int clubPeopleNum(long clubID) {
        //状态有通知每个服务变更所有用本地应该没问题
//        if (Config.isShare()) {
//            return (int) ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().filter(
//                    k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
//                    .count();
//        } else {
        return (int) this.getClubMemberMap().values().stream().filter(
                        k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                .count();
//        }
    }

    /**
     * 获取亲友圈成员
     *
     * @param pid    玩家PID
     * @param clubID 亲友圈ID
     * @param status 状态
     * @return
     */
    public ClubMember find(long pid, long clubID, Club_Player_Status status) {
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubID, pid);
            if (clubMember != null && clubMember.getStatus(status.value())) {
                return clubMember;
            }
            return null;
        } else {
            return this.getClubMemberMap().values().stream()
                    .filter(k -> k.getClubID() == clubID && k.checkPidEqual(pid) && k.getStatus(status.value())).findAny()
                    .orElse(null);
        }
    }

    /**
     * 获取共享亲友圈成员
     *
     * @param pid    玩家PID
     * @param clubID 亲友圈ID
     * @param status 状态
     * @return
     */
    public ClubMember findShare(long pid, long clubID, Club_Player_Status status) {
//        return ShareClubMemberMgr.getInstance().getAllClubMember().values().stream()
//                .filter(k -> k.getClubID() == clubID && k.checkPidEqual(pid) && k.getStatus(status.value())).findAny()
//                .orElse(null);
        ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubID, pid);
        if (clubMember != null && clubMember.getStatus(status.value())) {
            return clubMember;
        } else {
            return null;
        }
    }

    /**
     * 获取亲友圈成员
     *
     * @param pid      玩家PID
     * @param clubID   亲友圈ID
     * @param minister 状态
     * @return
     */
    public ClubMember findMinister(long pid, long clubID, Club_MINISTER minister) {
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubID, pid);
            if (clubMember != null && clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && clubMember.getClubMemberBO().getIsminister() == minister.value()) {
                return clubMember;
            } else {
                return null;
            }
        } else {
            return this.getClubMemberMap().values().stream()
                    .filter(k -> k.getClubID() == clubID && k.checkPidEqual(pid) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getIsminister() == minister.value()).findAny()
                    .orElse(null);
        }
    }


    /**
     * 检查存在比赛分不等于0的
     *
     * @return
     */
    public boolean checkExistSportsPointNotEqualZero(long clubID) {
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                List<Long> list = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().filter(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getSportsPoint() != 0D).map(k -> k.getClubMemberBO().getId()).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(list)) {
                    CommLogD.error("11111111111111111111111111111111111111CollectionUtils.isNotEmpty(list)" + list.toString());
                }
                return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().anyMatch(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getSportsPoint() != 0D);
            } else {
//            Arrays<String> list=
                List<Object> list = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()) && ClubMemberUtils.getArrayValueDouble(k, "sportsPoint") != 0D).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(list)) {
                    CommLogD.error("11111111111111111111111111111111111111CollectionUtils.isNotEmpty(list)" + list.toString());
                }
                return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).anyMatch(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()) && ClubMemberUtils.getArrayValueDouble(k, "sportsPoint") != 0D);
            }

        } else {
            return this.getClubMemberMap().values().stream().anyMatch(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getSportsPoint() != 0D);
        }
    }

    /**
     * 检查存在保险箱分不等于0的
     *
     * @return
     */
    public boolean checkExistCaseSportsPointNotEqualZero(long clubID) {
        if (Config.isShareLocal()) {
            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().anyMatch(k -> k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getCaseSportsPoint() != 0D);
        } else {
            return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).anyMatch(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()) && ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint") != 0D);
        }
//        Map<Long, ClubMember> clubMemberMap = new HashMap<>();
//        if (Config.isShare()) {
//            ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
//        } else {
//            clubMemberMap = this.getClubMemberMap();
//        }
//        return clubMemberMap.values().stream().anyMatch(k -> k.getClubID() == clubID && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getCaseSportsPoint() != 0D);
    }


    /**
     * 检查存在比赛分不等于0的
     *
     * @return
     */
    public boolean checkExistSportsPointNotEqualZero(long clubID, long ownerId) {
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream().anyMatch(k -> k.getClubID() == clubID && k.getClubMemberBO().getPlayerID() != ownerId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getSportsPoint() != 0D);
            } else {
                return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).anyMatch(k -> ClubMemberUtils.getArrayValueLong(k, "playerID") != ownerId && ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()) && ClubMemberUtils.getArrayValueDouble(k, "sportsPoint") != 0D);
            }
        } else {
            return this.getClubMemberMap().values().stream().anyMatch(k -> k.getClubID() == clubID && k.getClubMemberBO().getPlayerID() != ownerId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getSportsPoint() != 0D);
        }
    }

    /**
     * 获取当前俱乐部的管理ID链表
     */
    public List<Long> getMinisterList(long clubID) {
        if (Config.isShareLocal()) {
//            Map<Long, ClubMember> clubMemberMap;
//            if (Config.isShare()) {
//                clubMemberMap = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
//            } else {
//                clubMemberMap = this.getClubMemberMap();
//            }
            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID)
                    .values().stream().filter(k -> clubID == k.getClubID()
                            && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
                    .map(k -> k.getClubMemberBO().getPlayerID()).collect(Collectors.toList());
        } else {
            return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID)
                    .values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()) && Club_MINISTER.Club_MINISTER_GENERAL.value() != ClubMemberUtils.getArrayValueInteger(k, "isminister"))
                    .map(k -> ClubMemberUtils.getArrayValueLong(k, "playerID")).collect(Collectors.toList());
        }
    }

    /**
     * 判断指定玩家是否指定亲友圈的管理员
     *
     * @param clubID 亲友圈ID
     * @param pid    玩家ID
     * @return
     */
    public boolean isMinister(long clubID, long pid) {
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubID, pid);
            if (clubMember != null && clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && clubMember.isMinister()) {
                return true;
            } else {
                return false;
            }
        } else {
            return this.getClubMemberMap().values().stream()
                    .filter(k -> clubID == k.getClubID() && k.checkPidEqual(pid)
                            && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
                    .findAny().isPresent();
        }
    }

    public boolean isNotMinister(long clubID, long pid) {
        return !isMinister(clubID, pid);
    }

    /**
     * 是亲友圈创建者
     *
     * @param clubID 亲友圈id
     * @param pid    玩家pid
     * @return
     */
    public boolean isClubCreate(long clubID, long pid) {
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubID, pid);
            if (clubMember != null && clubMember.isClubCreate()) {
                return true;
            } else {
                return false;
            }
        } else {
            return this.getClubMemberMap().values().stream().anyMatch(k -> k.getClubID() == clubID && k.getClubMemberBO().getPlayerID() == pid && k.isClubCreate());
        }
    }


    /**
     * 获取亲友圈创建者成员Id
     *
     * @param clubID 亲友圈id
     * @return
     */
    public long getClubCreateMemberId(long clubID) {
        if (Config.isShare()) {
            Club club = ShareClubListMgr.getInstance().getClub(clubID);
            if (Objects.isNull(club)) {
                CommLogD.error("getClubCreateMemberId clubId:{}", clubID);
                return 0L;
            }
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubID, club.getClubListBO().getOwnerID());
            return Objects.nonNull(clubMember) && clubMember.isClubCreate() ? clubMember.getId() : 0L;
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubID && k.isClubCreate()).map(k -> k.getId()).findFirst().orElse(0L);
        }
    }

    /**
     * 获取亲友圈创建者成员Id
     *
     * @param clubID 亲友圈id
     * @return
     */
    public long getClubCreatePid(long clubID) {
        if (Config.isShare()) {
            Club club = ShareClubListMgr.getInstance().getClub(clubID);
            if (Objects.isNull(club)) {
                CommLogD.error("getClubCreatePid clubId:{}", clubID);
                return 0L;
            }
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubID, club.getClubListBO().getOwnerID());
            return Objects.nonNull(clubMember) && clubMember.isClubCreate() ? clubMember.getClubMemberBO().getPlayerID() : 0L;
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubID && k.isClubCreate()).map(k -> k.getClubMemberBO().getPlayerID()).findFirst().orElse(0L);
        }
    }

    /**
     * 获取亲友圈创建者成员Id
     *
     * @param clubMembers 亲友圈成员
     * @return
     */
    public long getClubCreatePidShare(Map<Long, ClubMember> clubMembers) {
        return clubMembers.values().stream().filter(k -> k.isClubCreate()).map(k -> k.getClubMemberBO().getPlayerID()).findFirst().orElse(0L);
    }


    /**
     * 不是亲友圈创建者
     *
     * @param clubID 亲友圈id
     * @param pid    玩家pid
     * @return
     */
    public boolean isNotClubCreate(long clubID, long pid) {
        return !this.isClubCreate(clubID, pid);
    }


    /**
     * 获取亲友圈创建者成员Id
     *
     * @param UpLevelId 上级玩家
     * @return
     */
    public boolean checkUpLevelIdClubCreate(long UpLevelId) {
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(UpLevelId);
            if (clubMember != null && clubMember.isClubCreate()) {
                return true;
            } else {
                return false;
            }
        } else {
            return this.getClubMemberMap().values().stream().anyMatch(k -> k.getClubMemberBO().getId() == UpLevelId && k.isClubCreate());
        }
    }

    /**
     * 判断指定玩家是否指定亲友圈的管理员
     *
     * @param clubID 亲友圈ID
     * @param pid    玩家ID
     * @return
     */
    public int getMinister(long clubID, long pid) {
        ClubMember clubMember;
        if (Config.isShare()) {
            clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubID, pid);
            if (!clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value())) {
                clubMember = null;
            }
        } else {
            clubMember = this.getClubMemberMap().values().stream().filter(k -> clubID == k.getClubID() && k.checkPidEqual(pid) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).findAny().orElse(null);
        }
        return Objects.isNull(clubMember) ? 0 : clubMember.getClubMemberBO().getIsminister();
    }

    /**
     * 判断指定玩家是否指定亲友圈的管理员共享
     *
     * @param clubID 亲友圈ID
     * @param pid    玩家ID
     * @return
     */
    public int getMinisterShare(long clubID, long pid) {
//        ClubMember clubMember = ShareClubMemberMgr.getInstance().getAllClubMember().values().stream().filter(k -> clubID == k.getClubID() && k.checkPidEqual(pid) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).findAny().orElse(null);
//        return Objects.isNull(clubMember) ? 0 : clubMember.getClubMemberBO().getIsminister();
        ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubID, pid);
        return Objects.isNull(clubMember) ? 0 : clubMember.getClubMemberBO().getIsminister();
    }


    /**
     * 检查指定赛事成员是否存在
     *
     * @param clubIdList
     * @param pid
     * @return
     */
    public boolean checkExistByPidMember(final List<Long> clubIdList, long pid) {
        if (Config.isShare()) {
            for (Long clubId : clubIdList) {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, pid);
                if (clubMember != null && clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value())) {
                    return true;
                }
            }
            return false;
        } else {
            return this.getClubMemberMap().values().stream().anyMatch(k -> clubIdList.contains(k.getClubID()) && k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()));
        }
    }


    /**
     * 获取指定亲友圈列表的在线人数
     *
     * @param clubIdList 亲友圈id列表
     * @return
     */
    public int clubIdListToOnlinePlayerCount(final List<Long> clubIdList) {
        int count = 0;
        for (Long clubId : clubIdList) {
            count += clubIdToOnlinePlayerCount(clubId);
        }
        return count;
//        Map<Long, ClubMember> clubMemberMap = new HashMap<>();
//        if (Config.isShare()) {
//            for (Long clubId : clubIdList) {
//                clubMemberMap.putAll(ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId));
//            }
//        } else {
//            clubMemberMap = this.getClubMemberMap();
//        }
//        return (int) clubMemberMap
//                .values()
//                .stream()
//                .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
//                .map(k -> k.getClubMemberBO().getPlayerID())
//                .distinct()
//                .filter(k -> Objects.nonNull(k) && PlayerMgr.getInstance().checkExistOnlinePlayerByPid(k)).count();
    }


    /**
     * 获取指定亲友圈列表的在线人数
     *
     * @param clubId 亲友圈id
     * @return
     */
    public int clubIdToOnlinePlayerCount(long clubId) {
        if (Config.isShareLocal()) {
            Map<Long, SharePlayer> shareOnlinePlayer = LocalPlayerMgr.getInstance().getLocalOnlinePlayerMap();
            return (int) ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId)
                    .values()
                    .parallelStream()
                    .filter(k -> clubId == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                    .map(k -> k.getClubMemberBO().getPlayerID())
                    .distinct()
                    .filter(k -> Objects.nonNull(k) && shareOnlinePlayer.containsKey(k)).count();
        } else {
            Set<String> onlinePlayerIds = SharePlayerMgr.getInstance().onlineSharePlayerIds();
            return (int) ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId)
                    .values()
                    .parallelStream()
                    .map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()))
                    .map(k -> ClubMemberUtils.getArrayValueLong(k, "playerID"))
                    .distinct()
                    .filter(k -> Objects.nonNull(k) && onlinePlayerIds.contains(String.valueOf(k))).count();
        }
//        Map<Long, ClubMember> clubMemberMap;
//        if (Config.isShare()) {
//            clubMemberMap = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId);
//        } else {
//            clubMemberMap = this.getClubMemberMap();
//        }
//        return (int) clubMemberMap
//                .values()
//                .parallelStream()
//                .filter(k -> clubId == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
//                .map(k -> k.getClubMemberBO().getPlayerID())
//                .distinct()
//                .filter(k -> Objects.nonNull(k) && PlayerMgr.getInstance().checkExistOnlinePlayerByPid(k)).count();
    }


    /**
     * 获取指定亲友圈列表的人数
     *
     * @param clubIdList 亲友圈id列表
     * @return
     */
    public int clubIdListToCount(final List<Long> clubIdList) {
        return (int) this.getClubMemberMap()
                .values()
                .stream()
                .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).count();
    }


    /**
     * 检查是否存在赛事比赛申请操作
     **/
    public boolean checkExistUnionMatchApply(final List<Long> clubIdList) {
        if (CollectionUtils.isEmpty(clubIdList)) {
            // 没有亲友圈
            return false;
        }
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                return LocalClubMemberMgr.getInstance().getAllClubMemberByClubIds(clubIdList).values().stream().anyMatch(k -> k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.checkExistApply());
            } else {
                for (Long clubId : clubIdList) {
                    return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).anyMatch(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()) && ClubMemberUtils.checkExistApply(ClubMemberUtils.getArrayValueInteger(k, "unionState")));
                    //                return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().anyMatch(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.checkExistApply());
                }
            }
            return false;
        } else {
            return this.getClubMemberMap().values().stream().anyMatch(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.checkExistApply());
        }
    }

    /**
     * 获取亲友圈创建者下属列表
     *
     * @param clubId 亲友圈id
     * @return
     */
    private List<Long> getClubCreateSubordinateLevelIdList(long clubId) {
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().filter(k -> k.getClubMemberBO().getLevel() <= 0L && k.getClubMemberBO().getUpLevelId() <= 0L && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).map(k -> k.getId()).collect(Collectors.toList());
            } else {
                return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k -> ClubMemberUtils.getArrayValueLong(k, "level") <= 0L && ClubMemberUtils.getArrayValueLong(k, "upLevelId") <= 0L && ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value())).map(k -> ClubMemberUtils.getArrayValueLong(k, "id")).collect(Collectors.toList());
            }
        } else {
            return this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == clubId && k.getClubMemberBO().getLevel() <= 0L && k.getClubMemberBO().getUpLevelId() <= 0L && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).map(k -> k.getId()).collect(Collectors.toList());
        }
    }

    /**
     * 获取退赛审核、重赛审核列表
     *
     * @param clubIdList 亲友圈Id列表
     * @param pageNum    页数
     * @param state      状态
     * @return
     */
    public List<UnionMemberExamineItem> getUnionMatchApplyExamineList(final List<Long> clubIdList, int pageNum, long clubId, long pid, final int type, UnionDefine.UNION_MATCH_STATE state) {
        Map<Long, ClubMember> clubMemberMap = new HashMap<>();
        if (Config.isShare()) {
            for (Long clubId1 : clubIdList) {
                clubMemberMap.putAll(ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId1));
            }
        } else {
            clubMemberMap = this.getClubMemberMap();
        }
        return clubMemberMap
                .values()
                .stream()
                .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getUnionState() == state.value() && !(k.getClubMemberBO().getClubID() == clubId && k.getClubMemberBO().getPlayerID() == pid))
                .sorted(Comparator.comparing(ClubMember::getResetTime).reversed()).map(k -> {
                    Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getClubID());
                    if (Objects.isNull(club)) {
                        return null;
                    }
                    Player player = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                    if (Objects.isNull(player)) {
                        return null;
                    }
                    return new UnionMemberExamineItem(club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), type, k.getClubMemberBO().getSportsPoint(), club.getClubListBO().getClubsign());
                }).filter(k -> Objects.nonNull(k)).skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).collect(Collectors.toList());
    }

    /**
     * 获取退赛审核、重赛审核列表
     *
     * @param clubIdList 亲友圈Id列表
     * @param pageNum    页数
     * @param state      状态
     * @return
     */
    public List<UnionMemberExamineItemZhongZhi> getUnionMatchApplyExamineListZhongZhi(final List<Long> clubIdList, int pageNum, long clubId, long pid, final int type, UnionDefine.UNION_MATCH_STATE state, String query) {
        Map<Long, ClubMember> clubMemberMap = new HashMap<>();
        if (Config.isShare()) {
            for (Long clubId1 : clubIdList) {
                clubMemberMap.putAll(ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId1));
            }
        } else {
            clubMemberMap = this.getClubMemberMap();
        }
        // 查询pid
        final long qPid = TypeUtils.StringTypeLong(query);
        return clubMemberMap
                .values()
                .stream()
                .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getUnionState() == state.value() && !(k.getClubMemberBO().getClubID() == clubId && k.getClubMemberBO().getPlayerID() == pid))
                .sorted(Comparator.comparing(ClubMember::getResetTime).reversed()).map(k -> {
                    Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getClubID());
                    if (Objects.isNull(club)) {
                        return null;
                    }
                    Player player = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                    if (Objects.isNull(player)) {
                        return null;
                    }
                    ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(k.getClubMemberBO().getUpLevelId());
                    if (Objects.isNull(clubMember)) {
                        return null;
                    }
                    Player upPlayer = PlayerMgr.getInstance().getPlayer(clubMember.getClubMemberBO().getPlayerID());
                    if (Objects.isNull(upPlayer)) {
                        return null;
                    }
                    if (StringUtils.isEmpty(query) || (qPid == player.getPid())) {
                        return new UnionMemberExamineItemZhongZhi(player.getShortPlayer(), club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), type, k.getClubMemberBO().getSportsPoint(),
                                club.getClubListBO().getClubsign(), upPlayer.getName(), k.getClubMemberBO().getEliminatePoint());
                    }
                    if (player.getName().contains(query)) {
                        return new UnionMemberExamineItemZhongZhi(player.getShortPlayer(), club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), type, k.getClubMemberBO().getSportsPoint(),
                                club.getClubListBO().getClubsign(), upPlayer.getName(), k.getClubMemberBO().getEliminatePoint());
                    }
                    return null;
                }).filter(k -> Objects.nonNull(k)).skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).collect(Collectors.toList());
    }

    /**
     * 通知俱乐部玩家
     *
     * @param clubID 亲友圈ID
     * @param msg    通知消息
     */
    public void notify2AllByClub(long clubID, BaseSendMsg msg) {
        if (Config.isShare()) {
            mqNotifyMessage(MqTopic.CLUB_ALL_BY_CLUB_NOTIFY, clubID, null, true, null, msg);
        } else {
            this.getClubMemberMap().values().stream().filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
                if (null != k) {
                    k.pushProto(msg);
                }
            });
        }
    }

    /**
     * 通知俱乐部玩家
     *
     * @param clubID 亲友圈ID
     * @param msg    通知消息
     */
    public void unionNotify2ClubAllMember(long clubID, long pid, BaseSendMsg msg) {
        if (Config.isShare()) {
            mqNotifyMessage(MqTopic.UNION_CLUB_ALL_MEMBER_NOTIFY, clubID, pid, true, null, msg);
        } else {
            SUnion_ClubChange sUnionClubChange = (SUnion_ClubChange) msg;
//        Map<Long, ClubMember> clubMemberMap;
//        if (Config.isShare()) {
//            clubMemberMap = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
//        } else {
//            clubMemberMap = this.getClubMemberMap();
//        }
            this.getClubMemberMap()
                    .values()
                    .stream()
                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
                    .forEach(k -> {
                        Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID());
                        if (Objects.nonNull(player)) {
                            if (sUnionClubChange.getUnionId() <= 0L) {
                                sUnionClubChange.setSportsPoint(0D);
                                sUnionClubChange.setUnionPostType(UnionDefine.UNION_POST_TYPE.UNION_GENERAL.value());
                            } else {
//                            sUnionClubChange.setSportsPoint(k.getClubMemberBO().getSportsPoint());
                                sUnionClubChange.setSportsPoint(ShareClubMemberMgr.getInstance().getClubMember(k.getId()).getClubMemberBO().getSportsPoint());
                                if (pid == player.getPlayerBO().getId()) {
                                    sUnionClubChange.setUnionPostType(UnionDefine.UNION_POST_TYPE.UNION_CREATE.value());
                                } else {
                                    sUnionClubChange.setUnionPostType(UnionDefine.UNION_POST_TYPE.UNION_GENERAL.value());
                                }
                            }
                            player.pushProto(sUnionClubChange);
                        }

                    });
        }
    }


    /**
     * 通知俱乐部玩家
     *
     * @param clubID 亲友圈ID
     * @param msg    通知消息
     */
    public void notify2AllMinisterByClub(long clubID, BaseSendMsg msg) {
        //指通知大厅
        if (Config.isShare()) {
            mqNotifyMessage(MqTopic.CLUB_ALL_MINISTER_BY_CLUB_NOTIFY, clubID, null, true, null, msg);
        } else {
            this.getClubMemberMap().values().stream()
                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())
                            && k.isMinister())
                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
                        if (null != k) {
                            k.pushProto(msg);
                        }
                    });
        }
//        if (Config.isShare()) {
//            ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream()
//                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())
//                            && k.isMinister())
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (null != k) {
//                    k.pushProtoMq(msg);
//                }
//            });
//        }
//        else {
//            this.getClubMemberMap().values().stream()
//                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())
//                            && k.isMinister())
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (null != k && k.notExistRoom()) {
//                    k.pushProto(msg);
//                }
//            });
//        }
    }

//    /**
//     * 通知俱乐部管理玩家 玩家不在房间里面 并且在亲友圈页面
//     *
//     * @param clubID 亲友圈ID
//     * @param msg    通知消息
//     */
//    @Deprecated
//    public void notify2AllMinisterByClubInClubSign(long clubID, BaseSendMsg msg) {
//        if (Config.isShare()) {
//            ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream()
//                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())
//                            && k.isMinister())
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (null != k && k.getSignEnumClubID() == clubID) {
//                    k.pushProtoMq(msg);
//                }
//            });
//        } else {
//            this.getClubMemberMap().values().stream()
//                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())
//                            && k.isMinister())
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (null != k && k.notExistRoom() && k.getSignEnumClubID() == clubID) {
//                    k.pushProto(msg);
//                }
//            });
//        }
//    }

//    /**
//     * 通知联赛管理员
//     *
//     * @param clubID 亲友圈ID
//     * @param msg    通知消息
//     */
//    @Deprecated
//    public void notify2UnionMinisterByClubInClubSign(long clubID, BaseSendMsg msg) {
//        if (Config.isShare()) {
//            ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream()
//                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())
//                            && k.isUnionMgr())
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (null != k && k.getSignEnumClubID() == clubID) {
//                    k.pushProtoMq(msg);
//                }
//            });
//        } else {
//            this.getClubMemberMap().values().stream()
//                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())
//                            && k.isUnionMgr())
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (null != k && k.notExistRoom() && k.getSignEnumClubID() == clubID) {
//                    k.pushProto(msg);
//                }
//            });
//        }
//    }

//    /**
//     * 通知俱乐部玩家 玩家不在房间里面 并且在亲友圈页面
//     *
//     * @param clubID 亲友圈ID
//     * @param msg    通知消息
//     */
//    @Deprecated
//    public void notify2AllByClubInClubSign(long clubID, BaseSendMsg msg) {
//        if (Config.isShare()) {
//            ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream()
//                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())
//                    )
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (null != k && k.getSignEnumClubID() == clubID) {
//                    k.pushProtoMq(msg);
//                }
//            });
//        } else {
//            this.getClubMemberMap().values().stream()
//                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())
//                    )
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (null != k && k.notExistRoom() && k.getSignEnumClubID() == clubID) {
//                    k.pushProto(msg);
//                }
//            });
//        }
//    }

    /**
     * 通知亲友圈管理员和玩家本身
     *
     * @param clubID 亲友圈ID
     * @param msg    通知消息
     */
    public void notify2AllMinisterAndPidByClub(long clubID, List<Long> pidList, BaseSendMsg msg) {
        //只通知大厅玩家
        mqNotifyMessage(MqTopic.CLUB_ALL_MINISTER_AND_PID_BY_CLUB_NOTIFY, clubID, null, pidList, true, null, msg);


    }

    /**
     * 通知亲友圈管理员和玩家本身
     *
     * @param clubID 亲友圈ID
     * @param msg    通知消息
     */
    public void notify2AllMinisterAndPidByClub(long clubID, long pid, BaseSendMsg msg) {
        //只通知大厅玩家
        if (Config.isShare()) {
            mqNotifyMessage(MqTopic.CLUB_ALL_MINISTER_AND_PID_BY_CLUB_NOTIFY, clubID, pid, true, null, msg);
        } else {
            this.getClubMemberMap().values().stream()
                    .filter(k -> clubID == k.getClubID()
                            && ((k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
                            || (k.getClubMemberBO().getPlayerID() == pid)))
                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
                        if (null != k) {
                            k.pushProto(msg);
                        }
                    });
        }
//        if (Config.isShare()) {
//            ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream()
//                    .filter(k -> clubID == k.getClubID()
//                            && ((k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
//                            || (k.getClubMemberBO().getPlayerID() == pid)))
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (null != k) {
//                    k.pushProtoMq(msg);
//                }
//            });
//        } else {
//            this.getClubMemberMap().values().stream()
//                    .filter(k -> clubID == k.getClubID()
//                            && ((k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
//                            || (k.getClubMemberBO().getPlayerID() == pid)))
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (null != k && k.notExistRoom()) {
//                    k.pushProto(msg);
//                }
//            });
//        }

    }


    /**
     * 通知俱乐部玩家 只通知管理或者创建者
     **/
    public void notify2AllByClubMinister(long clubID, BaseSendMsg msg) {
        //指通知大厅玩家
        if (Config.isShare()) {
            mqNotifyMessage(MqTopic.CLUB_ALL_BY_CLUB_MINISTER_NOTIFY, clubID, null, true, null, msg);
        } else {
            this.getClubMemberMap().values().stream()
                    // 筛选亲友圈ID和成员状态
                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
                        if (Objects.nonNull(k)) {
                            k.pushProto(msg);
                        }
                    });
        }
//        if (Config.isShare()) {
//            ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID).values().stream()
//                    // 筛选亲友圈ID和成员状态
//                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (Objects.nonNull(k)) {
//                    k.pushProtoMq(msg);
//                }
//            });
//        } else {
//            this.getClubMemberMap().values().stream()
//                    // 筛选亲友圈ID和成员状态
//                    .filter(k -> clubID == k.getClubID() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
//                    .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                if (Objects.nonNull(k) && k.notExistRoom()) {
//                    k.pushProto(msg);
//                }
//            });
//        }

    }

    /**
     * 通知指定亲友圈列表的所有玩家，并且指定配置
     **/
    public void notify2AllByClub(final List<Long> clubIdList, long unionGameCfgId, BaseSendMsg msg) {
        //只通知大厅玩家
        if (CollectionUtils.isEmpty(clubIdList) || Objects.isNull(msg)) {
            // 没有亲友圈
            return;
        }
        this.getClubMemberMap().values().stream()
                // 筛选亲友圈ID和成员状态
                .filter(k -> {
                    if (k.getClubMemberBO().getBanGame() > 0 || k.getClubMemberBO().getUnionBanGame() > 0) {
                        return false;
                    }
                    return clubIdList.contains(k.getClubID()) && k.getStatusIncludeTuiChuWeiPiZhun() && k.getClubMemberBO().isUnionNotify2Room(unionGameCfgId);
                })
                .map(k -> k.getClubMemberBO().getPlayerID())
                .distinct().forEach(k -> {
                    Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
                    if (null != player) {
//                CommLogD.info("通知指定亲友圈列表的所有玩家[{}]", new Gson().toJson(msg));
                        player.pushProto(msg);
                    }
                });
//        if(Config.isShare()){
//            clubIdList.forEach(clubId->{
//                ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream()
//                        // 筛选亲友圈ID和成员状态
//                        .filter(k -> {
//                            if (k.getClubMemberBO().getBanGame() > 0 || k.getClubMemberBO().getUnionBanGame() > 0) {
//                                return false;
//                            }
//                            return clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().isUnionNotify2Room(unionGameCfgId);
//                        })
//                        .map(k -> k.getClubMemberBO().getPlayerID())
//                        .distinct().forEach(k -> {
//                    Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
//                    if (null != player) {
//    //                CommLogD.info("通知指定亲友圈列表的所有玩家[{}]", new Gson().toJson(msg));
//                        player.pushProtoMq(msg);
//                    }
//                });
//            });
//        } else {
//        this.getClubMemberMap().values().stream()
//                // 筛选亲友圈ID和成员状态
//                .filter(k -> {
//                    if (k.getClubMemberBO().getBanGame() > 0 || k.getClubMemberBO().getUnionBanGame() > 0) {
//                        return false;
//                    }
//                    return clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().isUnionNotify2Room(unionGameCfgId);
//                })
//                .map(k -> k.getClubMemberBO().getPlayerID())
//                .distinct().forEach(k -> {
//            Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
//            if (null != player && player.notExistRoom()) {
////                CommLogD.info("通知指定亲友圈列表的所有玩家[{}]", new Gson().toJson(msg));
//                player.pushProto(msg);
//            }
//        });
//        }
    }

    /**
     * 通知指定亲友圈列表的所有玩家
     **/
    public void notify2AllByClub(final List<Long> clubIdList, BaseSendMsg msg) {
        //只通知大厅玩家
        if (CollectionUtils.isEmpty(clubIdList) || Objects.isNull(msg)) {
            // 没有亲友圈
            return;
        }
        this.getClubMemberMap().values().stream()
                // 筛选亲友圈ID和成员状态
                .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatusIncludeTuiChuWeiPiZhun())
                .map(k -> k.getClubMemberBO().getPlayerID())
                .distinct().forEach(k -> {
                    Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
                    if (null != player) {
                        player.pushProto(msg);
                    }
                });
//        if(Config.isShare()){
//            clubIdList.forEach(clubId -> {
//                ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream()
//                        // 筛选亲友圈ID和成员状态
//                        .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
//                        .map(k -> k.getClubMemberBO().getPlayerID())
//                        .distinct().forEach(k -> {
//                    Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
//                    if (null != player) {
//                        player.pushProtoMq(msg);
//                    }
//                });
//            });
//        } else {
//        this.getClubMemberMap().values().stream()
//                // 筛选亲友圈ID和成员状态
//                .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()))
//                .map(k -> k.getClubMemberBO().getPlayerID())
//                .distinct().forEach(k -> {
//            Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
//            if (null != player && player.notExistRoom()) {
//                player.pushProto(msg);
//            }
//        });
//        }
    }

    /**
     * 通知指定亲友圈列表的所有管理员
     **/
    @Deprecated
    public void notify2AllByClubMinister(final List<Long> clubIdList, BaseSendMsg msg) {
        if (CollectionUtils.isEmpty(clubIdList) || Objects.isNull(msg)) {
            // 没有亲友圈
            return;
        }
        if (Config.isShare()) {
//            clubIdList.forEach(clubId -> {
//                ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream()
//                        // 筛选亲友圈ID和成员状态
//                        .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
//                        .map(k -> k.getClubMemberBO().getPlayerID())
//                        .distinct().forEach(k -> {
//                    Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
//                    if (null != player) {
//                        player.pushProtoMq(msg);
//                    }
//                });
//            });
        } else {
            this.getClubMemberMap().values().stream()
                    // 筛选亲友圈ID和成员状态
                    .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
                    .map(k -> k.getClubMemberBO().getPlayerID())
                    .distinct().forEach(k -> {
                        Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
                        if (null != player && player.notExistRoom()) {
                            player.pushProto(msg);
                        }
                    });
        }
    }

    /**
     * 通知修改设置
     *
     * @param clubID                亲友圈ID
     * @param clubCreateGameSetInfo 修改的配置
     * @param memberCreationRoom    俱乐部成员创建房间 0:成员不可以创建 1:成员可以创建
     */
    @SuppressWarnings("rawtypes")
    public void notifyRoomCountChange(long clubID, long pid, ClubCreateGameSetInfo clubCreateGameSetInfo,
                                      int memberCreationRoom, boolean isCreate) {
        // 亲友圈房间类型分组
        Map<RoomState, Long> map = NormalRoomMgr.getInstance().groupingBy(RoomTypeEnum.CLUB, clubID);
        // 消息通知
        this.notify2AllByClubMinister(clubID, SClub_CreateGameSetChange.make(clubID, pid, isCreate, clubCreateGameSetInfo,
                NormalRoomMgr.Value(map, RoomState.Init),
                NormalRoomMgr.Value(map, RoomState.Playing), memberCreationRoom));
    }

    /**
     * 检查加入亲友圈房间
     *
     * @param club    亲友圈ID
     * @param pid     玩家PId
     * @param pidList
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result checkJoinClub(Club club, long pid, List<Long> pidList, BaseCreateRoom baseCreateRoom) {
        // 查询
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, ErrorCode.CLUB_NOT_EXIST.name());
        }

        SData_Result result = checkExistUnion(club);
        if (!ErrorCode.NotAllow.equals(result.getCode())) {
            return result;
        }

        ClubMember clubMember = null;
        //共享获取
        if (Config.isShare()) {
            clubMember = this.findShare(pid, club.getClubListBO().getId(), Club_Player_Status.PLAYER_JIARU);
        } else {
            clubMember = this.find(pid, club.getClubListBO().getId(), Club_Player_Status.PLAYER_JIARU);
        }
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_ENTER_NOT_CLUBMEMBER, new Gson().toJson(club.getClubInvitedInfo()));
        }
        if (clubMember.getClubMemberBO().getBanGame() > 0) {
            // 被管理员禁止加入房间。
            return SData_Result.make(ErrorCode.CLUB_BAN_GAME, "您已被禁止该游戏，请联系管理");
        }


        // 检查分组禁令
        String name = club.checkGroupingBan(pid, pidList);
        if (StringUtils.isNotEmpty(name)) {
            return SData_Result.make(ErrorCode.CLUB_GROUPING, "您与@" + name + "处于禁止同桌游戏状态，请联系亲友圈管理");
        }
        return SData_Result.make(ErrorCode.Success, clubMember.getClubMemberBO());
    }

    /**
     * 操作添加亲友圈成员
     *
     * @param playerID   玩家信息
     * @param clubID     亲友圈ID
     * @param isminister 是否管理员
     * @param status     成员状态
     * @return
     */
    public boolean onInsertClubMember(long playerID, long clubID, int isminister, int status, long exePid) {
        return this.onInsertClubMember(null, null, clubID, playerID, 0, 0, isminister, status, exePid, 0, 0L);
    }

    /**
     * 操作添加亲友圈成员
     *
     * @param player     玩家信息
     * @param clubID     亲友圈ID
     * @param partner    0,1 是否合伙人
     * @param partnerPid 合作人PID
     * @param status     成员状态
     * @return
     */
    public boolean onInsertClubMember(Player player, long clubID, int partner, long partnerPid, int status, long exePid, int level, long upLevelId) {
        return this.onInsertClubMember(player, null, clubID, 0L, partner, partnerPid,
                Club_MINISTER.Club_MINISTER_GENERAL.value(), status, exePid, level, upLevelId);
    }

    /**
     * 操作添加亲友圈成员（已加入）
     *
     * @param player     玩家信息
     * @param club       亲友圈信息
     * @param isminister 成员身份(0,1,2)
     * @return
     */
    public boolean onInsertClubMember(Player player, Club club, int isminister, long exePid) {
        return this.onInsertClubMember(player, club, 0L, 0L, Club_PARTNER.Club_PARTNER_NULL.value(), 0, isminister,
                Club_Player_Status.PLAYER_JIARU.value(), exePid, 0, 0L);
    }

    /**
     * 操作添加亲友圈成员
     *
     * @param player     玩家信息
     * @param club       亲友圈信息
     * @param clubID     亲友圈ID
     * @param playerID   玩家PID
     * @param partner    0,1 是否合伙人
     * @param partnerPid 合作人PID
     * @param isminister 成员身份(0,1,2)
     * @param status     成员状态
     * @return
     */
    public boolean onInsertClubMember(Player player, Club club, long clubID, long playerID, int partner,
                                      long partnerPid, int isminister, int status, long exePid, int level, long upLevelId) {
        // 获取玩家信息
        player = Objects.isNull(player) ? PlayerMgr.getInstance().getPlayer(playerID) : player;
        if (Objects.isNull(player)) {
            // 找不到玩家信息
            CommLogD.error("null == player Pid:{}", playerID);
            return false;
        }
        // 获取亲友圈信息
        club = Objects.isNull(club) ? ClubMgr.getInstance().getClubListMgr().findClub(clubID) : club;
        if (Objects.isNull(club)) {
            // 找不到亲友圈信息
            CommLogD.error("null == club ClubID:{}", clubID);
            return false;
        }

        // 获取亲友圈成员
        ClubMember clubMember = this.find(player.getPid(), club.getClubListBO().getId());
        if (Objects.nonNull(clubMember)) {
            if (clubMember.getStatus(status)) {
                // 新旧状态设置一样
                return false;
            } else if (clubMember.getStatus(Club_Player_Status.PLAYER_YAOQING.value() | Club_Player_Status.PLAYER_WEIPIZHUN.value())) {
                // 成员本身状态为 已邀请 或者 未批准状态则更新。
                clubMember.setStatus(player, club, status, exePid, false);
                return true;
            } else if (clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value())) {
                return false;
            }
            return false;
        }

        if (UnionMgr.getInstance().getUnionMemberMgr().checkExistInUnionOtherClub(clubID, player.getPid())) {
            return false;
        }
        ClubMemberBO clubMemberInfo = new ClubMemberBO();
        clubMemberInfo.setClubID(club.getClubListBO().getId());
        clubMemberInfo.setPlayerID(player.getPid());
        clubMemberInfo.setCreattime(CommTime.nowSecond());
        if (status == Club_Player_Status.PLAYER_JIARU.value()) {
            clubMemberInfo.setUpdatetime(CommTime.nowSecond());
        } else if (status == Club_Player_Status.PLAYER_YAOQING.value()) {
            // 设置邀请人Pid
            clubMemberInfo.setInvitationPid(exePid);
        }
        clubMemberInfo.setPromotion(partner);
        clubMemberInfo.setPartnerPid(partnerPid);
        clubMemberInfo.setIsminister(isminister);
        clubMemberInfo.setStatus(status);
        clubMemberInfo.setClubRoomCard(0);
        clubMemberInfo.setLevel(level);
        clubMemberInfo.setUpLevelId(upLevelId);
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.nonNull(union)) {
            if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
                clubMemberInfo.setEliminatePoint(union.getUnionBO().getOutSports());
            }
        }
        boolean flag = ((ClubMemberBOService) clubMemberInfo.getBaseService()).saveIgnoreOrUpDate(clubMemberInfo) > 0L;
        if (flag) {
            clubMember = new ClubMember(clubMemberInfo);
            this.getClubMemberMap().put(clubMemberInfo.getId(), clubMember);
            //共享数据
            if (Config.isShare()) {
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                //推送到MQ通知其他节点更新亲友圈数据
                MqProducerMgr.get().send(MqTopic.CLUB_INSERT_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(clubMemberInfo.getId(), Config.nodeName()));
            }
            // 首次加入亲友圈赠送圈卡
            this.onGiveReward(player, club, status);
            // 通知玩家本身和所有的管理员
            this.notify2AllClubMemberAndPid(player, club, clubMember);
            if (Club_MINISTER.Club_MINISTER_CREATER.value() == isminister) {
                // 亲友圈创建者
                clubMember.insertClubDynamicBO(player.getPid(), club.getClubListBO().getId(), UnionDefine.UNION_EXEC_TYPE.Club_EXEC_CREATER, clubMember.getClubMemberBO(), 0L);
            } else {
                if (status == Club_Player_Status.PLAYER_JIARU.value()) {
                    // 添加亲友圈流水
                    clubMember.insertClubDynamicBO(player.getPid(), club.getClubListBO().getId(), UnionDefine.UNION_EXEC_TYPE.Club_EXEC_JIARU, clubMember.getClubMemberBO(), exePid);
                    if (Club_define.Club_PROMOTION_LEVEL.APPOINT.ordinal() == level || upLevelId > 0L) {
                        DispatcherComponent.getInstance().publish(new PromotionLevelInsertEvent(clubMemberInfo.getId(), upLevelId));
                    }
                }
                if (Club_MINISTER.Club_MINISTER_MGR.value() == isminister) {
                    // 加入亲友圈就设置为管理员
                    clubMember.insertClubDynamicBO(player.getPid(), club.getClubListBO().getId(), UnionDefine.UNION_EXEC_TYPE.Club_EXEC_BECOME_MGR, clubMember.getClubMemberBO(), exePid);
                }
            }
            return invitedPlayer(club, player, status);
        } else {
            CommLogD.error("forceJoinClubMember insert ClubMemberBO fail");
            return false;
        }
    }


    /**
     * 添加创建者
     *
     * @param clubId
     * @param pid
     * @return
     */
    public ClubMember insertCreaterMember(long clubId, long pid) {
        ClubMember clubMember = null;
        ClubMemberBO clubMemberInfo = new ClubMemberBO();
        clubMemberInfo.setClubID(clubId);
        clubMemberInfo.setPlayerID(pid);
        clubMemberInfo.setCreattime(CommTime.nowSecond());
        clubMemberInfo.setUpdatetime(CommTime.nowSecond());
        clubMemberInfo.setIsminister(Club_MINISTER.Club_MINISTER_CREATER.value());
        clubMemberInfo.setStatus(Club_Player_Status.PLAYER_JIARU.value());
        boolean flag = ((ClubMemberBOService) clubMemberInfo.getBaseService()).saveIgnoreOrUpDate(clubMemberInfo) > 0L;
        if (flag) {
            clubMember = new ClubMember(clubMemberInfo);
            this.getClubMemberMap().put(clubMemberInfo.getId(), clubMember);
            //共享数据
            if (Config.isShare()) {
                ShareClubMemberMgr.getInstance().addClubMember(clubMember);
                //推送到MQ通知其他节点更新亲友圈数据
                MqProducerMgr.get().send(MqTopic.CLUB_INSERT_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(clubMemberInfo.getId(), Config.nodeName()));
            }
            return clubMember;
        }
        CommLog.error("insertCreaterMember clubId:{},pid:{}", clubId, pid);
        return clubMember;
    }


    /***
     * 通知玩家本身和所有的管理员
     *
     * @param player
     *            玩家信息
     * @param club
     *            亲友圈信息
     * @param clubMember
     *            亲友圈成员
     */
    public void notify2AllClubMemberAndPid(Player player, Club club, ClubMember clubMember) {
        BaseSendMsg msg = SClub_PlayerInfoChange.make(clubMember.getClubID(), club.getClubListBO().getName(),
                clubMember.getClubPlayerInfo(player, club.getClubListBO().getAgentsID(),
                        club.getClubListBO().getLevel(), clubMember.isBanGame(),
                        clubMember.getClubMemberBO().getPromotion(), clubMember.getSportsPoint(club)));
        this.notify2AllMinisterAndPidByClub(clubMember.getClubID(), player.getPid(), msg);
    }

    /***
     * 通知玩家本身和所有的管理员
     *
     * @param player
     *            玩家信息
     * @param status
     *            玩家状态
     * @param club
     *            亲友圈信息
     * @param clubMember
     *            亲友圈成员
     */
    public void notify2AllClubMemberAndPid(Player player, int status, Club club, ClubMember clubMember) {
        BaseSendMsg msg = SClub_PlayerInfoChange.make(clubMember.getClubID(), club.getClubListBO().getName(),
                clubMember.getClubPlayerInfo(player, status, club.getClubListBO().getAgentsID(),
                        club.getClubListBO().getLevel(), clubMember.isBanGame(),
                        clubMember.getClubMemberBO().getPromotion(), clubMember.getSportsPoint(club)));
        this.notify2AllMinisterAndPidByClub(clubMember.getClubID(), player.getPid(), msg);
    }

    /**
     * 更新状态 没有时插入
     **/
    public boolean onJoin(Player player, long clubID, long ownerID) {
        ClubMember clubMember = this.find(player.getPid(), clubID);
        if (Objects.isNull(clubMember)) {
            // 添加亲友圈成员(申请加入亲友圈)
            return this.onInsertClubMember(player, clubID, Club_PARTNER.Club_PARTNER_NULL.value(), 0L,
                    Club_Player_Status.PLAYER_WEIPIZHUN.value(), 0L, 0, 0L);
        } else {
            int status = 0;
            long exePid = 0L;
            if (clubMember.getStatus(Club_Player_Status.PLAYER_YAOQING.value())) {
                // 如果受到邀请就可以直接进入亲友圈
                status = Club_Player_Status.PLAYER_JIARU.value();
                // 邀请人
                exePid = clubMember.getClubMemberBO().getInvitationPid();
            } else if (clubMember.getStatus(Club_Player_Status.PLAYER_WEIPIZHUN.value())) {
                // 未批准
                return false;
            } else if (clubMember.getStatus(Club_Player_Status.PLAYER_JIARU.value())) {
                // 已加入亲友圈了
                return false;
            } else {
                // 否则未批准状态
                status = Club_Player_Status.PLAYER_WEIPIZHUN.value();
            }
            clubMember.setStatus(status, exePid);
        }
        return true;
    }


    /**
     * 邀请指定玩家通知
     *
     * @param club   亲友圈信息
     * @param player 玩家信息
     * @param status 成员状态
     * @return
     */
    public boolean invitedPlayer(Club club, Player player, int status) {
        if (Club_Player_Status.PLAYER_YAOQING.value() == status) {
            ClubInvitedInfo invitedInfo = new ClubInvitedInfo();
            invitedInfo.setClubId(club.getClubListBO().getId());
            invitedInfo.setClubsign(club.getClubListBO().getClubsign());
            invitedInfo.setClubName(club.getClubListBO().getName());
            SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(player.getPid());
            sharePlayer.pushProtoMq(SClub_Invited.make(invitedInfo));
        }
        return true;
    }

    /**
     * 首次加入亲友圈赠送圈卡
     *
     * @param player 玩家信息
     * @param club   亲友圈信息
     * @param status 状态
     */
    public void onGiveReward(Player player, Club club, int status) {
        if (Objects.isNull(player) || Objects.isNull(club)) {
            return;
        }
        if (Club_Player_Status.PLAYER_JIARU.value() != status) {
            return;
        }
        // 俱乐部奖励是否赠送0:没有赠送1:已赠送
        if (player.getSendClubReward() > 0) {
            return;
        }
        player.getFeature(PlayerWallet.class).gainItemFlow(GameConfig.ClubFirstJoinValue(), ItemFlow.ClubFirstRewardClubCard);
        player.saveSendClubReward(club.getClubListBO().getId());
    }


    /**
     * 删除某一条
     */
    public void removeClubMenber(long id) {
        this.getClubMemberMap().remove(id);
        //共享数据
        if (Config.isShare()) {
            ShareClubMemberMgr.getInstance().deleteClubMember(id);
            //推送到MQ删除成员
            MqProducerMgr.get().send(MqTopic.CLUB_DELETE_MEMBER_BO_NOTIFY, new MqClubMemberUpdateNotifyBo(id, Config.nodeName()));
        }
        // 并且删除
        // 进入日志
    }


    /**
     * 邀请好友列表，只显示在线并未在游戏
     *
     * @param clubId 亲友圈Id
     * @param number 查询数
     * @param query  查询
     * @return
     */
    public SData_Result<?> getClubMemberRoomInvitationItemList(long clubId, int number, int pageNum, String query) {
        if (clubId <= 0L || number <= 0) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        // 查询pid
        final long qPid = TypeUtils.StringTypeLong(query);
        //TODO 如果玩家在线人多在考虑缓存本地
        Map<Long, SharePlayer> sharePlayers = SharePlayerMgr.getInstance().onlineSharePlayers();
        if (Config.isShareLocal()) {
            List<ShortPlayer> clubPlayerInfos = this.findClubIdAllClubMember(clubId, Club_Player_Status.PLAYER_JIARU.value()).stream()
                    .map(k -> {
                        if (k.isBanGame()) {
                            return null;
                        }
                        SharePlayer onlinePlayer = sharePlayers.get(k.getClubMemberBO().getPlayerID());
                        if (Objects.nonNull(onlinePlayer) && onlinePlayer.getRoomInfo().getRoomId() <= 0L) {
                            if (onlinePlayer.getSignEnumClubID() != clubId) {
                                return null;
                            }
                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == onlinePlayer.getPlayerBO().getId())) {
                                return onlinePlayer.getShortPlayer();
                            }
                            if (onlinePlayer.getPlayerBO().getName().contains(query)) {
                                return onlinePlayer.getShortPlayer();
                            }
                        }
                        return null;
                    }).filter(k -> Objects.nonNull(k)).skip(Page.getPageNum(pageNum, number)).limit(number).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                return SData_Result.make(ErrorCode.Success, Collections.emptyList());
            }
            return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
        } else {
            List<ShortPlayer> clubPlayerInfos = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k))
                    .map(k -> {
                        if (ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0) {
                            return null;
                        }
                        SharePlayer onlinePlayer = sharePlayers.get(ClubMemberUtils.getArrayValueLong(k, "playerID"));
                        if (Objects.nonNull(onlinePlayer) && onlinePlayer.getRoomInfo().getRoomId() <= 0L) {
                            if (onlinePlayer.getSignEnumClubID() != clubId) {
                                return null;
                            }
                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == onlinePlayer.getPlayerBO().getId())) {
                                return onlinePlayer.getShortPlayer();
                            }
                            if (onlinePlayer.getPlayerBO().getName().contains(query)) {
                                return onlinePlayer.getShortPlayer();
                            }
                        }
                        return null;
                    }).filter(k -> Objects.nonNull(k)).skip(Page.getPageNum(pageNum, number)).limit(number).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                return SData_Result.make(ErrorCode.Success, Collections.emptyList());
            }
            return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
        }


//        List<ShortPlayer> clubPlayerInfos = this.findClubIdAllClubMember(clubId, Club_Player_Status.PLAYER_JIARU.value(), 1).stream()
//                .map(k -> {
//                    if (k.isBanGame()) {
//                        return null;
//                    }
//                    SharePlayer onlinePlayer = sharePlayers.get(k.getClubMemberBO().getPlayerID());
//                    if (Objects.nonNull(onlinePlayer) && onlinePlayer.getRoomInfo().getRoomId() <= 0L) {
//                        if (onlinePlayer.getSignEnumClubID() != clubId) {
//                            return null;
//                        }
//                        // 没有查询信息 或者
//                        if (StringUtils.isEmpty(query) || (qPid == onlinePlayer.getPlayerBO().getId())) {
//                            return onlinePlayer.getShortPlayer();
//                        }
//                        if (onlinePlayer.getPlayerBO().getName().contains(query)) {
//                            return onlinePlayer.getShortPlayer();
//                        }
//                    }
//                    return null;
//                }).filter(k -> Objects.nonNull(k)).skip(Page.getPageNum(pageNum, number)).limit(number).collect(Collectors.toList());
    }


//    /**
//     * 获取玩家身上的成员管理列表
//     *
//     * @param clubID  亲友圈ID
//     * @param pid     玩家PID
//     * @param pageNum 页数
//     * @return
//     */
//    public SData_Result<?> getMemberManageList(long clubID, long pid, int pageNum, String query, int getType, int pageType, int losePoint) {
//        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
//        if (Objects.isNull(club)) {
//            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "null == club ClubID:{%d}", clubID);
//        }
//        int queryStatus = Club_Player_Status.PLAYER_JIARU.value();
//        if (pageType == 1) {
//            queryStatus = Club_Player_Status.PLAYER_WEIPIZHUN.value();
//        } else if (pageType == 2) {
//            queryStatus = Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value();
//        }
//        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, pid);
//        if (Objects.isNull(clubMember)) {
//            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "null == clubMember ClubID:{%d} pid:{%d}", clubID, pid);
//        }
//        // 检查是否亲友圈管理员
//        if (this.isMinister(clubID, pid)) {
//            // 查询pid
//            final long qPid = TypeUtils.StringTypeLong(query);
//            Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
//            List<ClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShare(clubMembers, clubID,
//                    queryStatus, getType, losePoint).parallelStream()
//                    .map(k -> {
//                        Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
//                        if (Objects.isNull(tempPlayer)) {
//                            return null;
//                        }
//                        // 没有查询信息 或者
//                        if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {
//                            return k.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
//                                    club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
//                        }
//                        if (tempPlayer.getName().contains(query)) {
//                            return k.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
//                                    club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
//                        }
//                        return null;
//                    }).filter(k -> Objects.nonNull(k))
//                    .sorted(Comparator.comparing(ClubPlayerInfo::getWaiPiZhun)
//                            .thenComparing(ClubPlayerInfo::getUnionMgrSorted).reversed()
//                            .thenComparing(ClubPlayerInfo::getTime))
//
//                    .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
//            if (CollectionUtils.isEmpty(clubPlayerInfos)) {
//                return SData_Result.make(ErrorCode.Success, Collections.emptyList());
//            }
//            return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
//        } else if ((Objects.nonNull(clubMember) && clubMember.isLevelPromotion()) || clubMember.isPromotionManage()) {
//            if (clubMember.isPromotionManage()) {
//                if (Config.isShare()) {
//                    clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubMember.getClubMemberBO().getUpLevelId());
//                } else {
//                    clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(clubMember.getClubMemberBO().getUpLevelId());
//                }
//            }
//            if (Objects.isNull(clubMember)) {
//                return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", clubID, pid);
//            }
//            //查出自己的所有下线
//            List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
//            // 推广员id列表
//            List<Long> uidList = Lists.newArrayList();
//            //手动添加自己
//            uidList.add(clubMember.getId());
//            if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
//                // 查询我的所有下线（包括我）：
//                uidList.addAll(queryUidOrPidItemList.stream().map(k -> k.getUid()).collect(Collectors.toList()));
//            }
//            // 查询pid
//            final long qPid = TypeUtils.StringTypeLong(query);
//            Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
//            List<ClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShare(clubMembers, clubID,
//                    queryStatus, getType, losePoint).parallelStream()
//                    .filter(m -> uidList.contains(m.getId())).map(k -> {
//
//                        Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
//                        if (Objects.isNull(tempPlayer)) {
//                            return null;
//                        }
//
//                        // 没有查询信息 或者
//                        if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {
//                            return k.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
//                                    club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
//                        }
//                        if (tempPlayer.getName().contains(query)) {
//                            return k.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
//                                    club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
//                        }
//                        return null;
//                    }).filter(k -> Objects.nonNull(k))
//                    .sorted(Comparator.comparing(ClubPlayerInfo::getWaiPiZhun)
//                            .thenComparing(ClubPlayerInfo::getUnionMgrSorted).reversed()
//                            .thenComparing(ClubPlayerInfo::getTime))
//
//                    .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
//            if (CollectionUtils.isEmpty(clubPlayerInfos)) {
//                return SData_Result.make(ErrorCode.Success, Collections.emptyList());
//            }
//            return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
//        }
//
//
//        return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", clubID, pid);
//
//    }

    /**
     * 获取玩家身上的成员管理列表
     *
     * @param clubID  亲友圈ID
     * @param pid     玩家PID
     * @param pageNum 页数
     * @return
     */
    public SData_Result<?> getMemberManageList(long clubID, long pid, int pageNum, String query, int type, int pageType, int losePoint) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "null == club ClubID:{%d}", clubID);
        }
        int queryStatus = Club_Player_Status.PLAYER_JIARU.value();
        if (pageType == 1) {
            queryStatus = Club_Player_Status.PLAYER_WEIPIZHUN.value();
        } else if (pageType == 2) {
            queryStatus = Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value();
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, pid);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "null == clubMember ClubID:{%d} pid:{%d}", clubID, pid);
        }
        // 检查是否亲友圈管理员
        if (this.isMinister(clubID, pid)) {
            // 查询pid
            final long qPid = TypeUtils.StringTypeLong(query);
            if (Config.isShareLocal()) {
                Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
                List<ClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShare(clubMembers, clubID,
                                queryStatus, type, losePoint).parallelStream()
                        .map(k -> {
                            Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                            if (Objects.isNull(tempPlayer)) {
                                return null;
                            }
                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {
                                return k.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
                            }
                            if (tempPlayer.getName().contains(query)) {
                                return k.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
                            }
                            return null;
                        }).filter(k -> Objects.nonNull(k))
                        .sorted(Comparator.comparing(ClubPlayerInfo::getWaiPiZhun)
                                .thenComparing(ClubPlayerInfo::getUnionMgrSorted).reversed()
                                .thenComparing(ClubPlayerInfo::getTime))

                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            } else {
                Map<String, String> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID);
                List<String[]> clubMemberList = this.clubMembersToList(clubMembers);
                List<ClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShare(clubMemberList, clubID,
                                queryStatus, type, losePoint).parallelStream()
                        .map(k -> {
                            Player tempPlayer = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));
                            if (Objects.isNull(tempPlayer)) {
                                return null;
                            }
                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {//int isminister, int promotionManage, int deletetime, int creattime, int updatetime
                                return ClubMemberUtils.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMemberList, ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "upLevelId")), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0, ClubMemberUtils.getArrayValueInteger(k, "promotion"), ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "status"), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"), ClubMemberUtils.getArrayValueInteger(k, "deletetime"), ClubMemberUtils.getArrayValueInteger(k, "creattime"), ClubMemberUtils.getArrayValueInteger(k, "updatetime"));
                            }
                            if (tempPlayer.getName().contains(query)) {
                                return ClubMemberUtils.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMemberList, ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "upLevelId")), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0, ClubMemberUtils.getArrayValueInteger(k, "promotion"), ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "status"), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"), ClubMemberUtils.getArrayValueInteger(k, "deletetime"), ClubMemberUtils.getArrayValueInteger(k, "creattime"), ClubMemberUtils.getArrayValueInteger(k, "updatetime"));
                            }
                            return null;
                        }).filter(k -> Objects.nonNull(k))
                        .sorted(Comparator.comparing(ClubPlayerInfo::getWaiPiZhun)
                                .thenComparing(ClubPlayerInfo::getUnionMgrSorted).reversed()
                                .thenComparing(ClubPlayerInfo::getTime))

                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            }
        } else if ((Objects.nonNull(clubMember) && clubMember.isLevelPromotion()) || clubMember.isPromotionManage()) {
            if (clubMember.isPromotionManage()) {
                if (Config.isShare()) {
                    clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubMember.getClubMemberBO().getUpLevelId());
                } else {
                    clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(clubMember.getClubMemberBO().getUpLevelId());
                }
            }
            if (Objects.isNull(clubMember)) {
                return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", clubID, pid);
            }
            //查出自己的所有下线
            List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
            // 推广员id列表
            List<Long> uidList = Lists.newArrayList();
            //手动添加自己
            uidList.add(clubMember.getId());
            if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
                // 查询我的所有下线（包括我）：
                uidList.addAll(queryUidOrPidItemList.stream().map(k -> k.getUid()).collect(Collectors.toList()));
            }
            // 查询pid
            final long qPid = TypeUtils.StringTypeLong(query);
            if (Config.isShareLocal()) {
                Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
                List<ClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShare(clubMembers, clubID,
                                queryStatus, type, losePoint).parallelStream()
                        .filter(m -> uidList.contains(m.getId())).map(k -> {

                            Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                            if (Objects.isNull(tempPlayer)) {
                                return null;
                            }

                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {
                                return k.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
                            }
                            if (tempPlayer.getName().contains(query)) {
                                return k.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
                            }
                            return null;
                        }).filter(k -> Objects.nonNull(k))
                        .sorted(Comparator.comparing(ClubPlayerInfo::getWaiPiZhun)
                                .thenComparing(ClubPlayerInfo::getUnionMgrSorted).reversed()
                                .thenComparing(ClubPlayerInfo::getTime))

                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            } else {
                Map<String, String> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID);
                List<String[]> clubMemberList = this.clubMembersToList(clubMembers);
                List<ClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShare(clubMemberList, clubID,
                                queryStatus, type, losePoint).parallelStream()
                        .filter(m -> uidList.contains(ClubMemberUtils.getArrayValueLong(m, "id"))).map(k -> {

                            Player tempPlayer = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));
                            if (Objects.isNull(tempPlayer)) {
                                return null;
                            }

                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {
                                return ClubMemberUtils.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMemberList, ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "upLevelId")), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0, ClubMemberUtils.getArrayValueInteger(k, "promotion"), ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "status"), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"), ClubMemberUtils.getArrayValueInteger(k, "deletetime"), ClubMemberUtils.getArrayValueInteger(k, "creattime"), ClubMemberUtils.getArrayValueInteger(k, "updatetime"));
                            }
                            if (tempPlayer.getName().contains(query)) {
                                return ClubMemberUtils.getClubPlayerInfo(tempPlayer, getUpShortPlayerShare(clubMemberList, ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "upLevelId")), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0, ClubMemberUtils.getArrayValueInteger(k, "promotion"), ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "status"), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"), ClubMemberUtils.getArrayValueInteger(k, "deletetime"), ClubMemberUtils.getArrayValueInteger(k, "creattime"), ClubMemberUtils.getArrayValueInteger(k, "updatetime"));
                            }
                            return null;
                        }).filter(k -> Objects.nonNull(k))
                        .sorted(Comparator.comparing(ClubPlayerInfo::getWaiPiZhun)
                                .thenComparing(ClubPlayerInfo::getUnionMgrSorted).reversed()
                                .thenComparing(ClubPlayerInfo::getTime))

                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            }
        }


        return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", clubID, pid);

    }

    /**
     * 获取玩家身上的成员管理列表
     *
     * @param clubID  亲友圈ID
     * @param pid     玩家PID
     * @param pageNum 页数
     * @return
     */
    public SData_Result<?> getMemberManageListZhongZhi(long clubID, long pid, int pageNum, String query, int type, int pageType, int losePoint) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "null == club ClubID:{%d}", clubID);
        }
        int queryStatus = Club_Player_Status.PLAYER_JIARU.value();
        if (pageType == 1) {
            queryStatus = Club_Player_Status.PLAYER_WEIPIZHUN.value();
        } else if (pageType == 2) {
            queryStatus = Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value();
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, pid);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "null == clubMember ClubID:{%d} pid:{%d}", clubID, pid);
        }
        // 检查是否亲友圈管理员
        if (this.isMinister(clubID, pid)) {
            // 查询pid
            final long qPid = TypeUtils.StringTypeLong(query);
            if (Config.isShareLocal()) {
                Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
                List<ClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShareZhongZhi(clubMembers, clubID,
                                queryStatus, type, losePoint).parallelStream()
                        .map(k -> {
                            Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                            if (Objects.isNull(tempPlayer)) {
                                return null;
                            }
                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {
                                return k.getClubPlayerInfoZhongZhi(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
                            }
                            if (tempPlayer.getName().contains(query)) {
                                return k.getClubPlayerInfoZhongZhi(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
                            }
                            return null;
                        }).filter(k -> Objects.nonNull(k))
                        .sorted(Comparator.comparing(ClubPlayerInfo::getWaiPiZhun)
                                .thenComparing(ClubPlayerInfo::getUnionMgrSorted).reversed()
                                .thenComparing(ClubPlayerInfo::getTime))

                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            } else {
                Map<String, String> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID);
                List<String[]> clubMemberList = this.clubMembersToList(clubMembers);
                List<ClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShareZhongZhi(clubMemberList, clubID,
                                queryStatus, type, losePoint).parallelStream()
                        .map(k -> {
                            Player tempPlayer = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));
                            if (Objects.isNull(tempPlayer)) {
                                return null;
                            }
                            boolean onlineFlag = PlayerMgr.getInstance().checkExistOnlinePlayerByPid(tempPlayer.getPid());
                            String lastLogin = CommTime.getSecToYMDStr2(tempPlayer.getPlayerBO().getLastLogin());
                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {//int isminister, int promotionManage, int deletetime, int creattime, int updatetime
                                return ClubMemberUtils.getClubPlayerInfoZhongZhi(tempPlayer, getUpShortPlayerShare(clubMemberList, ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "upLevelId")), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0, ClubMemberUtils.getArrayValueInteger(k, "promotion"), ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "status"), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"), ClubMemberUtils.getArrayValueInteger(k, "deletetime"), ClubMemberUtils.getArrayValueInteger(k, "creattime"), ClubMemberUtils.getArrayValueInteger(k, "updatetime"), onlineFlag, lastLogin);
                            }
                            if (tempPlayer.getName().contains(query)) {
                                return ClubMemberUtils.getClubPlayerInfoZhongZhi(tempPlayer, getUpShortPlayerShare(clubMemberList, ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "upLevelId")), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0, ClubMemberUtils.getArrayValueInteger(k, "promotion"), ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "status"), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"), ClubMemberUtils.getArrayValueInteger(k, "deletetime"), ClubMemberUtils.getArrayValueInteger(k, "creattime"), ClubMemberUtils.getArrayValueInteger(k, "updatetime"), onlineFlag, lastLogin);
                            }
                            return null;
                        }).filter(k -> Objects.nonNull(k))
                        .sorted(Comparator.comparing(ClubPlayerInfo::getWaiPiZhun)
                                .thenComparing(ClubPlayerInfo::getUnionMgrSorted).reversed()
                                .thenComparing(ClubPlayerInfo::getTime))

                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            }
        } else if ((Objects.nonNull(clubMember) && clubMember.isLevelPromotion()) || clubMember.isPromotionManage()) {
            if (clubMember.isPromotionManage()) {
                if (Config.isShare()) {
                    clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubMember.getClubMemberBO().getUpLevelId());
                } else {
                    clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(clubMember.getClubMemberBO().getUpLevelId());
                }
            }
            if (Objects.isNull(clubMember)) {
                return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", clubID, pid);
            }
            //查出自己的所有下线
            List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
            // 推广员id列表
            List<Long> uidList = Lists.newArrayList();
            //手动添加自己
            uidList.add(clubMember.getId());
            if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
                // 查询我的所有下线（包括我）：
                uidList.addAll(queryUidOrPidItemList.stream().map(k -> k.getUid()).collect(Collectors.toList()));
            }
            // 查询pid
            final long qPid = TypeUtils.StringTypeLong(query);
            if (Config.isShareLocal()) {
                Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
                List<ClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShareZhongZhi(clubMembers, clubID,
                                queryStatus, type, losePoint).parallelStream()
                        .filter(m -> uidList.contains(m.getId())).map(k -> {

                            Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                            if (Objects.isNull(tempPlayer)) {
                                return null;
                            }

                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {
                                return k.getClubPlayerInfoZhongZhi(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
                            }
                            if (tempPlayer.getName().contains(query)) {
                                return k.getClubPlayerInfoZhongZhi(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
                            }
                            return null;
                        }).filter(k -> Objects.nonNull(k))
                        .sorted(Comparator.comparing(ClubPlayerInfo::getWaiPiZhun)
                                .thenComparing(ClubPlayerInfo::getUnionMgrSorted).reversed()
                                .thenComparing(ClubPlayerInfo::getTime))

                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            } else {
                Map<String, String> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID);
                List<String[]> clubMemberList = this.clubMembersToList(clubMembers);
                List<ClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShareZhongZhi(clubMemberList, clubID,
                                queryStatus, type, losePoint).parallelStream()
                        .filter(m -> uidList.contains(ClubMemberUtils.getArrayValueLong(m, "id"))).map(k -> {
                            Player tempPlayer = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));
                            if (Objects.isNull(tempPlayer)) {
                                return null;
                            }
                            boolean onlineFlag = PlayerMgr.getInstance().checkExistOnlinePlayerByPid(tempPlayer.getPid());
                            String lastLogin = CommTime.getSecToYMDStr2(tempPlayer.getPlayerBO().getLastLogin());
                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {
                                return ClubMemberUtils.getClubPlayerInfoZhongZhi(tempPlayer, getUpShortPlayerShare(clubMemberList, ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "upLevelId")), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0, ClubMemberUtils.getArrayValueInteger(k, "promotion"), ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "status"), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"), ClubMemberUtils.getArrayValueInteger(k, "deletetime"), ClubMemberUtils.getArrayValueInteger(k, "creattime"), ClubMemberUtils.getArrayValueInteger(k, "updatetime"), onlineFlag, lastLogin);
                            }
                            if (tempPlayer.getName().contains(query)) {
                                return ClubMemberUtils.getClubPlayerInfoZhongZhi(tempPlayer, getUpShortPlayerShare(clubMemberList, ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "upLevelId")), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0, ClubMemberUtils.getArrayValueInteger(k, "promotion"), ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "status"), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"), ClubMemberUtils.getArrayValueInteger(k, "deletetime"), ClubMemberUtils.getArrayValueInteger(k, "creattime"), ClubMemberUtils.getArrayValueInteger(k, "updatetime"), onlineFlag, lastLogin);
                            }
                            return null;
                        }).filter(k -> Objects.nonNull(k))
                        .sorted(Comparator.comparing(ClubPlayerInfo::getWaiPiZhun)
                                .thenComparing(ClubPlayerInfo::getUnionMgrSorted).reversed()
                                .thenComparing(ClubPlayerInfo::getTime))

                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            }
        }


        return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", clubID, pid);

    }

    /**
     * 获取比赛劵发放界面成员信息
     *
     * @param clubID  亲友圈ID
     * @param pid     玩家PID
     * @param pageNum 页数
     * @return
     */
    public SData_Result<?> getMemberCompetitionManageListZhongZhi(long clubID, long pid, int pageNum, String query, int type, int pageType, int losePoint) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "null == club ClubID:{%d}", clubID);
        }
        int queryStatus = Club_Player_Status.PLAYER_JIARU.value();
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, pid);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "null == clubMember ClubID:{%d} pid:{%d}", clubID, pid);
        }
        // 检查是否亲友圈管理员
        if (this.isMinister(clubID, pid)) {
            // 查询pid
            final long qPid = TypeUtils.StringTypeLong(query);
            if (Config.isShareLocal()) {
                Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
                List<ClubPlayerInfoZhongZhi> clubPlayerInfos = this.findClubIdAllClubMemberShareZhongZhi(clubMembers, clubID,
                                queryStatus, type, losePoint).parallelStream()
                        .map(k -> {
                            Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                            if (Objects.isNull(tempPlayer)) {
                                return null;
                            }
                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {
                                return k.getClubPlayerInfoComPetitionZhongZhi(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
                            }
                            if (tempPlayer.getName().contains(query)) {
                                return k.getClubPlayerInfoComPetitionZhongZhi(tempPlayer, getUpShortPlayerShare(clubMembers, k.getClubID(), k.getClubMemberBO().getUpLevelId()), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), k.isBanGame(), k.getClubMemberBO().getPromotion(), k.getSportsPoint(club));
                            }
                            return null;
                        }).filter(k -> Objects.nonNull(k))
                        .sorted(Comparator.comparing(ClubPlayerInfoZhongZhi::getMinister).reversed()
                                .thenComparing(ClubPlayerInfoZhongZhi::getTime))
                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            } else {
                Map<String, String> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID);
                List<String[]> clubMemberList = this.clubMembersToList(clubMembers);
                List<ClubPlayerInfoZhongZhi> clubPlayerInfos = this.findClubIdAllClubMemberShareZhongZhi(clubMemberList, clubID,
                                queryStatus, type, losePoint).parallelStream()
                        .map(k -> {
                            Player tempPlayer = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));
                            if (Objects.isNull(tempPlayer)) {
                                return null;
                            }
                            boolean onlineFlag = PlayerMgr.getInstance().checkExistOnlinePlayerByPid(tempPlayer.getPid());
                            String lastLogin = CommTime.getSecToYMDStr2(tempPlayer.getPlayerBO().getLastLogin());
                            // 没有查询信息 或者
                            if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {//int isminister, int promotionManage, int deletetime, int creattime, int updatetime
                                return ClubMemberUtils.getClubPlayerInfoCompetitionZhongZhi(tempPlayer, getUpShortPlayerShare(clubMemberList, ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "upLevelId")), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0, ClubMemberUtils.getArrayValueInteger(k, "promotion"), ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "status"), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"), ClubMemberUtils.getArrayValueInteger(k, "deletetime"), ClubMemberUtils.getArrayValueInteger(k, "creattime"), ClubMemberUtils.getArrayValueInteger(k, "updatetime"), onlineFlag, lastLogin, ClubMemberUtils.getArrayValueInteger(k, "eliminatePoint"), ClubMemberUtils.getArrayValueInteger(k, "alivePoint"));
                            }
                            if (tempPlayer.getName().contains(query)) {
                                return ClubMemberUtils.getClubPlayerInfoCompetitionZhongZhi(tempPlayer, getUpShortPlayerShare(clubMemberList, ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "upLevelId")), club.getClubListBO().getAgentsID(),
                                        club.getClubListBO().getLevel(), ClubMemberUtils.getArrayValueInteger(k, "banGame") > 0, ClubMemberUtils.getArrayValueInteger(k, "promotion"), ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "status"), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"), ClubMemberUtils.getArrayValueInteger(k, "deletetime"), ClubMemberUtils.getArrayValueInteger(k, "creattime"), ClubMemberUtils.getArrayValueInteger(k, "updatetime"), onlineFlag, lastLogin, ClubMemberUtils.getArrayValueInteger(k, "eliminatePoint"), ClubMemberUtils.getArrayValueInteger(k, "alivePoint"));
                            }
                            return null;
                        }).filter(k -> Objects.nonNull(k))
                        .sorted(Comparator.comparing(ClubPlayerInfoZhongZhi::getMinister).reversed()
                                .thenComparing(ClubPlayerInfoZhongZhi::getTime))

                        .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_15)).limit(Page.PAGE_SIZE_15).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            }
        }
        return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", clubID, pid);
    }

    /**
     * 查询玩家的所属推广员 链表 全部的
     *
     * @param clubID 亲友圈ID
     * @param pid    玩家PID
     * @param
     * @return
     */
    public SData_Result<?> getMemberPromotionList(long clubID, long pid, String query) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        final long qPid = TypeUtils.StringTypeLong(query);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "null == club ClubID:{%d}", clubID);
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, qPid);
        ClubMember doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, pid);
        // 检查是否亲友圈管理员
        if (Objects.nonNull(clubMember) && ((Objects.nonNull(doClubMember) && (doClubMember.isLevelPromotion())) || this.isMinister(clubID, pid))) {
            //查出自己的所有上线
            List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
            // 推广员id列表
            List<Long> uidList = Lists.newArrayList();
            uidList.add(clubMember.getId());
            if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
                // 查询我的所有上线（包括我）：
                uidList.addAll(queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId).reversed()).map(k -> k.getPuid()).collect(Collectors.toList()));
            }
            if (Config.isShareLocal()) {
                List<ShortPlayer> clubPlayerInfos;
                Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubID);
                if (Config.isShare()) {
                    clubPlayerInfos = uidList.stream().filter(k -> Objects.nonNull(ShareClubMemberMgr.getInstance().getClubMember(k.longValue()))).map(k -> {
                        ClubMember proMemeber = ShareClubMemberMgr.getInstance().getClubMember(k.longValue());
                        return getUpShortPlayerShare(clubMembers, clubID, proMemeber.getId());
                    }).collect(Collectors.toList());
                } else {
                    clubPlayerInfos = uidList.stream().filter(k -> Objects.nonNull(this.getClubMemberMap().get(k.longValue()))).map(k -> {
                        ClubMember proMemeber = this.getClubMemberMap().get(k.longValue());
                        return getUpShortPlayer(clubID, proMemeber.getId());
                    }).collect(Collectors.toList());
                }

                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    clubPlayerInfos.add(getUpShortPlayerShare(clubMembers, clubID, qPid));//如果为空的话  手动加入一个圈主的信息
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            } else {
                List<ShortPlayer> clubPlayerInfos;
                Map<String, String> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID);
                List<String[]> clubMemberList = clubMembersToList(clubMembers);
                if (Config.isShare()) {
                    clubPlayerInfos = uidList.stream().filter(k -> Objects.nonNull(ShareClubMemberMgr.getInstance().getClubMember(k.longValue()))).map(k -> {
                        ClubMember proMemeber = ShareClubMemberMgr.getInstance().getClubMember(k.longValue());
                        return getUpShortPlayerShare(clubMemberList, clubID, proMemeber.getId());
                    }).collect(Collectors.toList());
                } else {
                    clubPlayerInfos = uidList.stream().filter(k -> Objects.nonNull(this.getClubMemberMap().get(k.longValue()))).map(k -> {
                        ClubMember proMemeber = this.getClubMemberMap().get(k.longValue());
                        return getUpShortPlayer(clubID, proMemeber.getId());
                    }).collect(Collectors.toList());
                }

                if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                    clubPlayerInfos.add(getUpShortPlayerShare(clubMemberList, clubID, qPid));//如果为空的话  手动加入一个圈主的信息
                    return SData_Result.make(ErrorCode.Success, Collections.emptyList());
                }
                return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
            }
        }


        return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister and not Promotion ClubID:{%d},Pid:{%d}", clubID, pid);

    }

    /**
     * 查询玩家的所属推广员 链表 全部的
     *
     * @param
     * @param pid 玩家PID
     * @param
     * @return
     */
    public SData_Result<?> getUnionMemberPromotionList(CUnion_GetMemberManage req, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getQueryClubId());
        final long qPid = req.getQueryPid();
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "null == club ClubID:{%d}", req.getQueryClubId());
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.getQueryClubId(), req.getQueryPid());
        //查出自己的所有上线
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        // 推广员id列表
        List<Long> uidList = Lists.newArrayList();
        uidList.add(clubMember.getId());
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            // 查询我的所有上线（包括我）：
            uidList.addAll(queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId).reversed()).map(k -> k.getPuid()).collect(Collectors.toList()));
        }
        if (Config.isShareLocal()) {
            List<ShortPlayer> clubPlayerInfos;
            Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(req.getQueryClubId());
            if (Config.isShare()) {
                clubPlayerInfos = uidList.stream().filter(k -> Objects.nonNull(ShareClubMemberMgr.getInstance().getClubMember(k.longValue()))).map(k -> {
                    ClubMember proMemeber = ShareClubMemberMgr.getInstance().getClubMember(k.longValue());
                    return getUpShortPlayerShare(clubMembers, req.getQueryClubId(), proMemeber.getId());
                }).collect(Collectors.toList());
            } else {
                clubPlayerInfos = uidList.stream().filter(k -> Objects.nonNull(this.getClubMemberMap().get(k.longValue()))).map(k -> {
                    ClubMember proMemeber = this.getClubMemberMap().get(k.longValue());
                    return getUpShortPlayer(req.getQueryClubId(), proMemeber.getId());
                }).collect(Collectors.toList());
            }

            if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                clubPlayerInfos.add(getUpShortPlayerShare(clubMembers, req.getQueryClubId(), qPid));//如果为空的话  手动加入一个圈主的信息
                return SData_Result.make(ErrorCode.Success, Collections.emptyList());
            }
            return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
        } else {
            List<ShortPlayer> clubPlayerInfos;
            Map<String, String> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(req.getQueryClubId());
            List<String[]> clubMemberList = clubMembersToList(clubMembers);
            if (Config.isShare()) {
                clubPlayerInfos = uidList.stream().filter(k -> Objects.nonNull(ShareClubMemberMgr.getInstance().getClubMember(k.longValue()))).map(k -> {
                    ClubMember proMemeber = ShareClubMemberMgr.getInstance().getClubMember(k.longValue());
                    return getUpShortPlayerShare(clubMemberList, req.getQueryClubId(), proMemeber.getId());
                }).collect(Collectors.toList());
            } else {
                clubPlayerInfos = uidList.stream().filter(k -> Objects.nonNull(this.getClubMemberMap().get(k.longValue()))).map(k -> {
                    ClubMember proMemeber = this.getClubMemberMap().get(k.longValue());
                    return getUpShortPlayer(req.getQueryClubId(), proMemeber.getId());
                }).collect(Collectors.toList());
            }
            if (CollectionUtils.isEmpty(clubPlayerInfos)) {
                clubPlayerInfos.add(getUpShortPlayerShare(clubMemberList, req.getQueryClubId(), qPid));//如果为空的话  手动加入一个圈主的信息
                return SData_Result.make(ErrorCode.Success, Collections.emptyList());
            }
            return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
        }


    }

    /**
     * 获取赛事身上的成员管理列表
     *
     * @param clubID  亲友圈ID
     * @param pageNum 页数
     * @return
     */
    public SData_Result<?> getUnionMemberManageList(long clubID, long unionId, int pageNum, String query, int type, int losePoint) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "null == club ClubID:{%d}", clubID);
        }
        if (club.getClubListBO().getUnionId() != unionId) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR Club UnionId:{},UnionId:{}", club.getClubListBO().getUnionId(), unionId);
        }

        // 查询pid
        final long qPid = TypeUtils.StringTypeLong(query);
        Map<String, String> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubID);
        List<String[]> clubMemberList = this.clubMembersToList(clubMembers);
        List<UnionClubPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShare(clubMemberList, clubID, Club_Player_Status.PLAYER_JIARU.value(), type, losePoint).stream()
                .map(k -> {
                    Player tempPlayer = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));
                    if (Objects.isNull(tempPlayer)) {
                        return null;
                    }
                    // 没有查询信息 或者
                    if (StringUtils.isEmpty(query) || (qPid == tempPlayer.getPid())) {
                        return ClubMemberUtils.getUnionClubPlayerInfo(tempPlayer, this.getUpShortPlayerShare(clubMemberList, clubID, ClubMemberUtils.getArrayValueLong(k, "upLevelId")), ClubMemberUtils.getArrayValueInteger(k, "unionBanGame") > 0, ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                    }

                    if (tempPlayer.getName().contains(query)) {
                        return ClubMemberUtils.getUnionClubPlayerInfo(tempPlayer, this.getUpShortPlayerShare(clubMemberList, clubID, ClubMemberUtils.getArrayValueLong(k, "upLevelId")), ClubMemberUtils.getArrayValueInteger(k, "unionBanGame") > 0, ClubMemberUtils.getSportsPoint(club, ClubMemberUtils.getArrayValueDouble(k, "sportsPoint")), ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                    }
                    return null;
                }).filter(k -> Objects.nonNull(k))
                .sorted(Comparator.comparing(UnionClubPlayerInfo::getSportsPoint).reversed())
                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(clubPlayerInfos)) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        return SData_Result.make(ErrorCode.Success, clubPlayerInfos);
    }

    /**
     * 获取上级
     *
     * @param clubId    亲友圈id
     * @param upLevelId 上级
     * @return
     */
    public ShortPlayer getUpShortPlayer(long clubId, long upLevelId) {
        long upid = upLevelId;
        Player player = null;
        ClubMember clubMember;
        if (Config.isShare()) {
            clubMember = ShareClubMemberMgr.getInstance().getClubMember(upLevelId);
        } else {
            clubMember = this.getClubMemberMap().get(upLevelId);
        }
        if (Objects.nonNull(clubMember)) {
            player = PlayerMgr.getInstance().getPlayer(clubMember.getClubMemberBO().getPlayerID());
            if (Objects.nonNull(player)) {
                return player.getUpShortPlayer();
            }
        }
        player = PlayerMgr.getInstance().getPlayer(getClubCreatePid(clubId));
        if (Objects.nonNull(player)) {
            return player.getUpShortPlayer();
        }
        return null;
    }

    /**
     * 获取上级
     *
     * @param clubId    亲友圈id
     * @param upLevelId 上级
     * @return
     */
    public ShortPlayer getUpShortPlayerShare(Map<Long, ClubMember> clubMembers, long clubId, long upLevelId) {
        long upid = upLevelId;
        Player player = null;
        ClubMember clubMember = clubMembers.values().stream().filter(k -> k.getClubMemberBO().getId() == upLevelId).findAny().orElse(null);
        if (Objects.nonNull(clubMember)) {
            player = PlayerMgr.getInstance().getPlayer(clubMember.getClubMemberBO().getPlayerID());
            if (Objects.nonNull(player)) {
                return player.getUpShortPlayer();
            }
        }
        player = PlayerMgr.getInstance().getPlayer(getClubCreatePidShare(clubMembers));
        if (Objects.nonNull(player)) {
            return player.getUpShortPlayer();
        }
        return null;
    }

    /**
     * 获取上级
     *
     * @param clubId    亲友圈id
     * @param upLevelId 上级
     * @return
     */
    public ShortPlayer getUpShortPlayerShare(List<String[]> clubMembers, long clubId, long upLevelId) {
        long upid = upLevelId;
        Player player = null;
        String[] clubMember = clubMembers.stream().filter(k -> ClubMemberUtils.getArrayValueLong(k, "id") == upLevelId).findAny().orElse(null);
        if (Objects.nonNull(clubMember)) {
            player = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(clubMember, "playerID"));
            if (Objects.nonNull(player)) {
                return player.getUpShortPlayer();
            }
        }
        player = PlayerMgr.getInstance().getPlayer(getClubCreatePidShare(clubMembers));
        if (Objects.nonNull(player)) {
            return player.getUpShortPlayer();
        }
        return null;
    }

    /**
     * 获取亲友圈创建者成员Id
     *
     * @param clubMembers 亲友圈成员
     * @return
     */
    public long getClubCreatePidShare(List<String[]> clubMembers) {
        return clubMembers.stream().filter(k -> Club_define.Club_MINISTER.Club_MINISTER_CREATER.value() == ClubMemberUtils.getArrayValueInteger(k, "isminister")).map(k -> ClubMemberUtils.getArrayValueLong(k, "playerID")).findFirst().orElse(0L);
    }


    /**
     * 获取玩家所属下级 是自己的下级
     *
     * @param pid      当前查看的玩家
     * @param clubId   亲友圈id
     * @param playerId 当前玩家自己的idgetNextPlayerByPromotion
     * @return
     */
    public ShortPlayer getNextPlayerByPid(long pid, long clubId, long playerId) {
        ClubMember exeClubMember = ClubMgr.getInstance().getClubMemberMgr().find(playerId, clubId);
        Player player = null;
        if (Objects.nonNull(exeClubMember)) {
            long upLevelId = exeClubMember.getClubMemberBO().getUpLevelId();
            long upid = upLevelId;
            ClubMember clubMember;
            if (Config.isShare()) {
                clubMember = ShareClubMemberMgr.getInstance().getClubMember(upLevelId);
            } else {
                clubMember = this.getClubMemberMap().get(upLevelId);
            }
            if (Objects.nonNull(clubMember)) {
                player = PlayerMgr.getInstance().getPlayer(clubMember.getClubMemberBO().getPlayerID());
                if (Objects.nonNull(player)) {
                    return player.getUpShortPlayer();
                }
            }
        }
        player = PlayerMgr.getInstance().getPlayer(getClubCreatePid(clubId));
        if (Objects.nonNull(player)) {
            return player.getUpShortPlayer();
        }
        return null;
//        //单前查询玩家的信息
//
//        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
//        ClubMember clubMember=ClubMgr.getInstance().getClubMemberMgr().find(pid,clubId);
//        ClubMember exeClubMember=ClubMgr.getInstance().getClubMemberMgr().find(playerId,clubId);
//        if(playerId==getClubCreatePid(clubId)){
//            Player player = PlayerMgr.getInstance().getPlayer(getClubCreatePid(clubId));
//            if (Objects.nonNull(player)) {
//                return player.getUpShortPlayer();
//            }
//        }
//        if(Objects.nonNull(clubMember)&&Objects.nonNull(exeClubMember)){
//            //查出自己的所有上线线
//            List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid",exeClubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
//            QueryUidOrPuidItem next=null ;
//            if (queryUidOrPidItemList.size() == 1) {
//                next=queryUidOrPidItemList.get(0);
//
//            }else if(queryUidOrPidItemList.size()>1){
//                next=queryUidOrPidItemList.stream().filter(k->k.getPuid()!=clubMember.getId()).max(Comparator.comparingLong(QueryUidOrPuidItem::getId).reversed()).orElse(null);
//            }
//            if(Objects.nonNull(next)){
//                ClubMember nextClubMember = this.getClubMemberMap().get(next.getPuid());
//                Player player = null;
//                if (Objects.nonNull(nextClubMember)) {
//                    player = PlayerMgr.getInstance().getPlayer(nextClubMember.getClubMemberBO().getPlayerID());
//                    if (Objects.nonNull(player)) {
//                        return player.getUpShortPlayer();
//                    }
//                }
//                player = PlayerMgr.getInstance().getPlayer(getClubCreatePid(clubId));
//                if (Objects.nonNull(player)) {
//                    return player.getUpShortPlayer();
//                }
//            }
//        }
//        return club.getOwnerPlayer().getUpShortPlayer();
    }

    /**
     * 获取玩家所属下级 是自己的下级  如果是推广员进来的话 就只看推广员信息就好了
     *
     * @return
     */
    public ShortPlayer getNextPlayerByPromotion(Player player) {
        //单前查询玩家的信息
        return player.getUpShortPlayer();
    }

    /**
     * 关闭俱乐部时提出玩家
     *
     * @param clubID 亲友圈ID
     */
    public void onTuiChuInCloseClub(long clubID) {
        List<ClubMember> clubMembers = findClubIdAllClubMember(clubID,
                Club_Player_Status.PLAYER_JIARU.value() | Club_Player_Status.PLAYER_WEIPIZHUN.value());
        for (ClubMember clubMember : clubMembers) {
            if (clubMember.getClubMemberBO().getIsminister() == Club_MINISTER.Club_MINISTER_CREATER.value()) {
                clubMember.closeClubStatus(Club_Player_Status.PLAYER_TUICHU.value());
            } else {
                clubMember.closeClubStatus(Club_Player_Status.PLAYER_TICHU_CLOSE.value());
            }
        }
    }


    /**
     * 查询亲友圈指定玩家的信息
     *
     * @param clubID 亲友圈ID
     * @param pid    玩家PID
     * @return
     */
    public ClubMember getClubMember(long clubID, long pid) {
        // or like this
        ClubMember result;
        if (Config.isShare()) {
            result = ShareClubMemberMgr.getInstance().getClubMember(clubID, pid);
            if (result != null && result.getStatus(Club_Player_Status.PLAYER_JIARU.value())) {
                return result;
            } else {
                return null;
            }
        } else {
            // 检查亲友圈成员数据是否存在。
            if (MapUtils.isEmpty(this.getClubMemberMap())) {
                return null;
            }
            result = this.getClubMemberMap().values().stream().filter(x -> {
                if (x.getClubMemberBO().getPlayerID() == pid && x.getClubID() == clubID
                        && x.getStatus(Club_Player_Status.PLAYER_JIARU.value())) {
                    return true;
                }
                return false;
            }).findAny().orElse(null);
        }
        return result;
    }


    /**
     * 查找
     **/
    public ArrayList<ClubMember> findAll(long pid, int status, boolean isPid) {
        ArrayList<ClubMember> clubMembers = new ArrayList<ClubMember>();
        for (ClubMember clubMember : clubMemberMap.values()) {
            if (isPid && clubMember.checkPidEqual(pid) && clubMember.getStatus(status) || (!isPid && clubMember.getStatus(status))) {
                clubMembers.add(clubMember);
            }
        }
        return clubMembers;
    }

    /**
     * 查找
     **/
    public ArrayList<ClubMember> findAllMember(long pid) {
        ArrayList<ClubMember> clubMembers = new ArrayList<ClubMember>();
        for (ClubMember clubMember : clubMemberMap.values()) {
            if (clubMember.checkPidEqual(pid)) {
                clubMembers.add(clubMember);
            }
        }
        return clubMembers;
    }


    /**
     * 清空亲友圈成员赛事信息
     *
     * @param clubId 亲友圈Id
     */
    public void clearUnionInfo(long clubId, int roundId) {
        List<ClubMember> clubMemberList = findClubIdAllClubMember(clubId, Club_Player_Status.PLAYER_JIARU.value());
        if (CollectionUtils.isEmpty(clubMemberList)) {
            return;
        }
        clubMemberList.stream().forEach(k -> k.getClubMemberBO().clearUnionInfo(roundId));
    }


    /**
     * 检查亲友圈是否加入赛事
     *
     * @param club
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result checkExistUnion(Club club) {
        if (club.getClubListBO().getUnionId() <= 0L) {
            return SData_Result.make(ErrorCode.NotAllow);
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (null != union) {
            return SData_Result.make(ErrorCode.CLUB_EXIST_UNION, "您所在的亲友圈@%s，已成功加入联盟%s，该亲友圈的房间列表已切换为联盟房间", club.getClubListBO().getName(), union.getUnionBO().getName());
        } else {
            return SData_Result.make(ErrorCode.NotAllow);
        }
    }


    /**
     * 检查赛事成员权利
     *
     * @param exePid 操作者id
     * @param opPid  被操作者Pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    private SData_Result checkUnionRights(long clubId, long exePid, long opPid) {
        // 获取当前操作者的成员信息。
        ClubMember execClubMember = find(exePid, clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == execClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "execClubMember CLUB_NOTCLUBMEMBER");
        }
        // 检查当前操作者是否有基本权限。
        if (!execClubMember.isMinister()) {
            return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "CLUB_NOTMINISTER");
        }
        // 获取被操作者的成员信息。
        ClubMember opClubMember = find(opPid, clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == opClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "opClubMember CLUB_NOTCLUBMEMBER");
        }
        //加入赛事管理 权限判断需要修改
        //如果执行者是创建者的话 优先级最高  不是创建者的话才进行操作权限判断
        // 检查操作者和被操作者权限是否一致。
        if (!execClubMember.isClubCreate() && opClubMember.getClubMemberBO().getIsminister() >= execClubMember.getClubMemberBO().getIsminister()) {
            // 被操作者的权力 >= 操作者的权力
            return SData_Result.make(ErrorCode.CLUB_MEMBER_SAME_POST_TYPE, "CLUB_MEMBER_SAME_POST_TYPE");
        }
        return SData_Result.make(ErrorCode.Success, opClubMember.isMinister());
    }

    /**
     * 检查赛事推广员成员权利
     *
     * @param exePid 操作者id
     * @param opPid  被操作者Pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    private SData_Result checkUnionPromotionRights(long clubId, long exePid, long opPid) {
        // 获取当前操作者的成员信息。
        ClubMember execClubMember = find(exePid, clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == execClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "execClubMember CLUB_NOTCLUBMEMBER");
        }
        //获取赛事类型
        UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(execClubMember.getClubID());
        if (Objects.nonNull(club)) {
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
            if (Objects.nonNull(union)) {
                unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
            }
        }
        if (execClubMember.isMinister()) {
            // 获取被操作者的成员信息。
            ClubMember opClubMember = find(opPid, clubId, Club_Player_Status.PLAYER_JIARU);
            if (null == opClubMember) {
                return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "opClubMember CLUB_NOTCLUBMEMBER");
            }
            //加入赛事管理 权限判断需要修改
            //如果执行者是创建者的话 优先级最高  不是创建者的话才进行操作权限判断
            // 检查操作者和被操作者权限是否一致。

            if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                if (!execClubMember.isClubCreate() && opClubMember.getClubMemberBO().getIsminister() >= execClubMember.getClubMemberBO().getIsminister()) {
                    // 被操作者的权力 >= 操作者的权力
                    return SData_Result.make(ErrorCode.CLUB_MEMBER_SAME_POST_TYPE, "CLUB_MEMBER_SAME_POST_TYPE");
                }
            } else if (UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType)) {
                if (!execClubMember.isClubCreate() && opClubMember.getClubMemberBO().getIsminister() > execClubMember.getClubMemberBO().getIsminister()) {
                    // 被操作者的权力 >= 操作者的权力
                    return SData_Result.make(ErrorCode.CLUB_MEMBER_SAME_POST_TYPE, "CLUB_MEMBER_SAME_POST_TYPE");
                }
            }
            return SData_Result.make(ErrorCode.Success, opClubMember.isMinister());
        } else if (execClubMember.isLevelPromotion()) {
            // 获取被操作者的成员信息。
            ClubMember opClubMember = find(opPid, clubId, Club_Player_Status.PLAYER_JIARU);
            if (null == opClubMember) {
                return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "opClubMember CLUB_NOTCLUBMEMBER");
            }
            if (opClubMember.isMinister()) {
                return SData_Result.make(ErrorCode.CLUB_MEMBER_SAME_POST_TYPE, "CLUB_MEMBER_SAME_POST_TYPE");
            }
            if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                // 检查操作者和被操作者权限推广员等级 level越大  等级越低
                if (opClubMember.getClubMemberBO().getLevel() != 0 && opClubMember.getClubMemberBO().getLevel() <= execClubMember.getClubMemberBO().getLevel()) {
                    return SData_Result.make(ErrorCode.CLUB_MEMBER_SAME_POST_TYPE, "CLUB_MEMBER_SAME_POST_TYPE");
                }
            } else if (UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType)) {
                if (opClubMember.getClubMemberBO().getLevel() != 0 && opClubMember.getClubMemberBO().getLevel() < execClubMember.getClubMemberBO().getLevel()) {
                    return SData_Result.make(ErrorCode.CLUB_MEMBER_SAME_POST_TYPE, "CLUB_MEMBER_SAME_POST_TYPE");
                }
            }
            return SData_Result.make(ErrorCode.Success, opClubMember.isMinister());

        } else if (execClubMember.isPromotionManage()) {
            if (Config.isShare()) {
                execClubMember = ShareClubMemberMgr.getInstance().getClubMember(execClubMember.getClubMemberBO().getUpLevelId());
            } else {
                execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(execClubMember.getClubMemberBO().getUpLevelId());
            }
            if (null == execClubMember) {
                return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "opClubMember CLUB_NOTCLUBMEMBER");
            }
            // 获取被操作者的成员信息。
            ClubMember opClubMember = find(opPid, clubId, Club_Player_Status.PLAYER_JIARU);
            if (null == opClubMember) {
                return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "opClubMember CLUB_NOTCLUBMEMBER");
            }
            if (opClubMember.isMinister()) {
                return SData_Result.make(ErrorCode.CLUB_MEMBER_SAME_POST_TYPE, "CLUB_MEMBER_SAME_POST_TYPE");
            }

            if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                // 检查操作者和被操作者权限推广员等级 level越大  等级越低
                if (opClubMember.getClubMemberBO().getLevel() != 0 && opClubMember.getClubMemberBO().getLevel() <= execClubMember.getClubMemberBO().getLevel()) {
                    return SData_Result.make(ErrorCode.CLUB_MEMBER_SAME_POST_TYPE, "CLUB_MEMBER_SAME_POST_TYPE");
                }
            } else if (UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType)) {
                // 检查操作者和被操作者权限推广员等级 level越大  等级越低
                if (opClubMember.getClubMemberBO().getLevel() != 0 && opClubMember.getClubMemberBO().getLevel() < execClubMember.getClubMemberBO().getLevel()) {
                    return SData_Result.make(ErrorCode.CLUB_MEMBER_SAME_POST_TYPE, "CLUB_MEMBER_SAME_POST_TYPE");
                }
            }
            return SData_Result.make(ErrorCode.Success, opClubMember.isMinister());
        }
        return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "CLUB_NOTMINISTER");
    }

    /**
     * 执行比赛分
     *
     * @param req
     * @param exePid 操作Pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result execSportsPointUpdate(CClub_SportsPointUpdate req, long exePid, Player player) {
        String uuid = UUID.randomUUID().toString();
        try {
            //redis分布式锁
            if (DistributedRedisLock.acquireGiveUp("createRoomAndConsumeCard" + req.getOpPid(), uuid)) {
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
                if (null == club) {
                    return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
                }
                if (club.getClubListBO().getUnionId() <= 0L) {
                    return SData_Result.make(ErrorCode.CLUB_NOT_JOIN_UNION, "CLUB_NOT_JOIN_UNION");
                }
                Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
                if (Objects.isNull(union)) {
                    return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
                }
                SData_Result result = this.checkUnionPromotionRights(req.getClubId(), exePid, req.getOpPid());
//                if(UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))){
//                    // 获取被操作者的成员信息。
//                    ClubMember opClubMember = find(req.getOpPid(), req.getClubId(), Club_Player_Status.PLAYER_JIARU);
//                    if (null == opClubMember) {
//                        return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "opClubMember CLUB_NOTCLUBMEMBER");
//                    }
//                    result= SData_Result.make(ErrorCode.Success, opClubMember.isMinister());
//                }
                if (!ErrorCode.Success.equals(result.getCode())) {
                    return result;
                }
                boolean isManage = (boolean) result.getData();
                UnionDefine.UNION_SPORTS_POINT sportsPoint = UnionDefine.UNION_SPORTS_POINT.valueOf(req.getType());
                if (req.getValue() <= 0) {
                    if (UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_MINUS.equals(sportsPoint)) {
                        req.setValue(Math.abs(req.getValue()));
                        sportsPoint = UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_ADD;
                    } else {
                        return SData_Result.make(ErrorCode.NotAllow, "execSportsPointUpdate value:{%s}", req.getValue());
                    }
                }
                double pidValue;
                if (UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_ADD.equals(sportsPoint)) {
                    pidValue = req.getValue();
                    // 比赛分增加
                    result = execSportsPointAdd(req, club.getClubListBO().getUnionId(), exePid, club.getClubListBO().getId(), union.getUnionBO().getOutSports(), ItemFlow.CLUB_SPORTS_POINT_CHANGE);
                } else {
                    pidValue = -req.getValue();
                    // 比赛分减少
                    result = execSportsPointMinus(req, club.getClubListBO().getUnionId(), exePid, club.getClubListBO().getId(), union.getUnionBO().getOutSports(), ItemFlow.CLUB_SPORTS_POINT_CHANGE);
                }
                if (!ErrorCode.Success.equals(result.getCode())) {
                    return result;
                }
                // 比赛分增加/减少
                UnionDefine.UNION_EXEC_TYPE itemFlow = UnionDefine.UNION_EXEC_TYPE.getUnionExecTypeGaneral(isManage, req.getType() <= 0);
                // 修改前后值
                UnionSportsPointItem item = (UnionSportsPointItem) result.getData();
                // 比赛分值修改
                UnionDynamicBO.insertSportsPoint(req.getOpPid(), club.getClubListBO().getUnionId(), req.getClubId(), exePid, req.getClubId(), CommTime.nowSecond(), itemFlow.value(), String.valueOf(req.getValue()), 1, String.valueOf(item.getCurValue()), String.valueOf(item.getPreValue()), String.valueOf(item.getPidCurValue()),
                        String.valueOf(item.getPidPreValue()), String.valueOf(pidValue), String.valueOf(item.getExecPidCurValue()), String.valueOf(item.getExecPidPreValue()), String.valueOf(-pidValue));
                //判断是不是直属上下级

                ClubMember execClubMember = find(exePid, req.getClubId(), Club_Player_Status.PLAYER_JIARU);
                ClubMember opClubMember = find(req.getOpPid(), req.getClubId(), Club_Player_Status.PLAYER_JIARU);
                //找不到的话不会进行到这边 不用空判断
                if (opClubMember.getClubMemberBO().getUpLevelId() != execClubMember.getClubMemberBO().getId()) {
                    //不能加字段  把没用的 曲线救国
                    // pidPreValue 存pid
                    // execPidPreValue存 name
                    Player playerTo = PlayerMgr.getInstance().getPlayer(opClubMember.getClubMemberBO().getPlayerID());
                    ClubMember upMember = ShareClubMemberMgr.getInstance().getClubMember(opClubMember.getClubMemberBO().getUpLevelId());
                    Player upPlayer = null;
                    if (Objects.nonNull(upMember)) {
                        upPlayer = PlayerMgr.getInstance().getPlayer(upMember.getClubMemberBO().getPlayerID());
                    }
                    if (Objects.nonNull(playerTo) && Objects.nonNull(upPlayer)) {
                        // 比赛分值修改
                        UnionDynamicBO.insertSportsPoint(upPlayer.getPid(), club.getClubListBO().getUnionId(), req.getClubId(), 0, req.getClubId(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.getUnionExecTypeGaneralKuaJi(req.getType() <= 0).value(), String.valueOf(req.getValue()), 0,
                                String.valueOf(item.getCurValue()), String.valueOf(item.getPreValue()), String.valueOf(item.getPidCurValue()),
                                String.valueOf(playerTo.getPid()), String.valueOf(pidValue), String.valueOf(item.getExecPidCurValue()), String.valueOf(playerTo.getName()), String.valueOf(-pidValue), player.getName(), String.valueOf(player.getPid()));

                    }
                }
                return SData_Result.make(ErrorCode.Success, CUnion_ChangeSportPoint.make(req.getType(), req.getValue(), item.getPidCurValue()));
            } else {
                return SData_Result.make(ErrorCode.NotAllow);
            }
        } finally {
            DistributedRedisLock.release("createRoomAndConsumeCard" + req.getOpPid(), uuid);
        }
    }

    /**
     * 执行比赛分
     *
     * @param req
     * @param exePid 操作Pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result execSportsPointExamine(CClub_SportsPointExamine req, long exePid, boolean autoFlag) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (club.getClubListBO().getUnionId() <= 0L) {
            return SData_Result.make(ErrorCode.CLUB_NOT_JOIN_UNION, "CLUB_NOT_JOIN_UNION");
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (!autoFlag) {
            if (UnionDefine.UNION_WARN_EXAMINE.AUTO.ordinal() == union.getUnionBO().getExamineStatus()) {
                return SData_Result.make(ErrorCode.UNION_EXAMINE_STATUS_AUTO, "UNION_EXAMINE_STATUS_AUTO");
            }
        }
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
        }
        SData_Result result = this.checkUnionPromotionRights(req.getClubId(), exePid, req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        req.setType(req.getValue() > 0 ? 1 : 0);
        req.setValue(Math.abs(req.getValue()));
        UnionDefine.UNION_SPORTS_POINT sportsPoint = UnionDefine.UNION_SPORTS_POINT.valueOf(req.getType());

        ClubMember opClubMember = find(req.getOpPid(), req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        double opPreValue = Double.valueOf(opClubMember.getSportsPoint());//10
        //找到执行者与被执行者
        ClubMember exeClubMember = find(exePid, req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(opClubMember) || Objects.isNull(exeClubMember)) {
            // 找不到操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", req.getDateType());
        ClubExamineItem examineItem = ContainerMgr.get().getComponent(ExamineFlogService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("toClubMemberId", opClubMember.getId())), ClubExamineItem.class, ClubExamineItem.getItemsName());
        if (Objects.nonNull(examineItem)) {
            // 找不到操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.CLUB_EXAMINE, "CLUB_EXAMINE");
        }
        double pidValue;
        if (UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_ADD.equals(sportsPoint)) {
            pidValue = req.getValue();
            // 比赛分增加
            result = execSportsPointAdd(req, club.getClubListBO().getUnionId(), exePid, club.getClubListBO().getId(), union.getUnionBO().getOutSports(), ItemFlow.CLUB_SPORTS_POINT_EXAMINE);
        } else {
            pidValue = -req.getValue();
            // 比赛分减少
            result = execSportsPointMinus(req, club.getClubListBO().getUnionId(), exePid, club.getClubListBO().getId(), union.getUnionBO().getOutSports(), ItemFlow.CLUB_SPORTS_POINT_EXAMINE);
        }
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 比赛分增加/减少
        UnionDefine.UNION_EXEC_TYPE opItemFlow = UnionDefine.UNION_EXEC_TYPE.getUnionExecTypeExamineOp(req.getType() <= 0);
        // 修改前后值
        UnionSportsPointItem item = (UnionSportsPointItem) result.getData();
        double opCurValue = Double.valueOf(opClubMember.getSportsPoint());
        // 比赛分值审核变动
        UnionDynamicBO.insertSportsPoint(req.getOpPid(), club.getClubListBO().getUnionId(), req.getClubId(), exePid, req.getClubId(), CommTime.nowSecond(), opItemFlow.value(), String.valueOf(req.getValue()), 1, String.valueOf(item.getCurValue()), String.valueOf(item.getPreValue()),
                String.valueOf(item.getPidCurValue()), String.valueOf(item.getPidPreValue()), String.valueOf(pidValue), String.valueOf(item.getExecPidCurValue()), String.valueOf(item.getExecPidPreValue()), String.valueOf(-pidValue));
        ContainerMgr.get().getComponent(ExamineFlogService.class).save(ExamineLogFlow.examineLogFlowInit(CommTime.getYesterDayStringYMD(Club_define.CLUB_PROMOTION_TIME_TYPE.valueOf(req.getDateType()).value()),
                req.getOpPid(), opClubMember.getId(), exeClubMember.getId(), opPreValue, req.getValue(), opCurValue));
        return SData_Result.make(ErrorCode.Success, CUnion_ChangeSportPoint.make(req.getType(), req.getValue()));
    }

    /**
     * 比赛分增加
     */
    @SuppressWarnings("rawtypes")
    private SData_Result execSportsPointAdd(CClub_SportsPointUpdate req, long unionId, long exePid, long clubId, double outSports, ItemFlow itemFlow) {
        ClubMember exeClubMember = find(exePid, req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == exeClubMember) {
            // 找不到操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        // 操作者是否在游戏中
        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(exePid);
        if (Objects.nonNull(sharePlayer) && sharePlayer.getRoomInfo().getRoomId() > 0L && sharePlayer.getRoomInfo().getClubId() == clubId && unionId == sharePlayer.getRoomInfo().getUnionId()) {
            return SData_Result.make(ErrorCode.UNION_PLAYER_IN_GAME_ERROR, "UNION_PLAYER_IN_GAME_ERROR");
        }
        ClubMember opClubMember = find(req.getOpPid(), req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == opClubMember) {
            // 找不到被操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        double pidPreValue = Double.valueOf(opClubMember.getSportsPoint());
        double execPidPreValue = Double.valueOf(exeClubMember.getSportsPoint());
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            // TODO 2021-11-19,傅哥说：先改成只通知本人和管理员(本来是通知亲友圈所有人)
            List<Long> pidList = Arrays.asList(exeClubMember.getClubMemberBO().getPlayerID(), opClubMember.getClubMemberBO().getPlayerID());
            notify2AllMinisterAndPidByClub(clubId, pidList, SUnion_SportsPoint.make(clubId, exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getClubMemberBO().getUnionState()));
            double preValue = opClubMember.getClubMemberBO().getSportsPoint();
            // 执行增加被操作者的比赛分
            opClubMember.getClubMemberBO().execSportsPointUpdate(unionId, req.getValue(), itemFlow, RoomTypeEnum.CLUB, outSports);
            notify2AllMinisterAndPidByClub(clubId, pidList, SUnion_SportsPoint.make(clubId, opClubMember.getClubMemberBO().getPlayerID(), opClubMember.getClubMemberBO().getSportsPoint(), opClubMember.getClubMemberBO().getUnionState()));
            return SData_Result.make(ErrorCode.Success, new UnionSportsPointItem(opClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getSportsPoint(), opClubMember.getClubMemberBO().getSportsPoint(), pidPreValue, exeClubMember.getSportsPoint(), execPidPreValue));
        } else if (UnionDefine.UNION_TYPE.NORMAL.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            // 执行减去操作者的比赛分
            if (exeClubMember.getClubMemberBO().execSportsPointUpdate(unionId, -req.getValue(), itemFlow, RoomTypeEnum.CLUB, outSports)) {
                // TODO 2021-11-19,傅哥说：先改成只通知本人和管理员(本来是通知亲友圈所有人)
                List<Long> pidList = Arrays.asList(exeClubMember.getClubMemberBO().getPlayerID(), opClubMember.getClubMemberBO().getPlayerID());
                notify2AllMinisterAndPidByClub(clubId, pidList, SUnion_SportsPoint.make(clubId, exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getClubMemberBO().getUnionState()));
                double preValue = opClubMember.getClubMemberBO().getSportsPoint();
                // 执行增加被操作者的比赛分
                opClubMember.getClubMemberBO().execSportsPointUpdate(unionId, req.getValue(), itemFlow, RoomTypeEnum.CLUB, outSports);
                notify2AllMinisterAndPidByClub(clubId, pidList, SUnion_SportsPoint.make(clubId, opClubMember.getClubMemberBO().getPlayerID(), opClubMember.getClubMemberBO().getSportsPoint(), opClubMember.getClubMemberBO().getUnionState()));
                return SData_Result.make(ErrorCode.Success, new UnionSportsPointItem(opClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getSportsPoint(), opClubMember.getClubMemberBO().getSportsPoint(), pidPreValue, exeClubMember.getSportsPoint(), execPidPreValue));
            } else {
                // 操作者的比赛分不足
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
        } else {
            // 赛事的类型不存在
            return SData_Result.make(ErrorCode.UNION_TYPE_NOT_EXIST, "UNION_TYPE_NOT_EXIST");
        }

    }


    /**
     * 比赛分减少
     */
    @SuppressWarnings("rawtypes")
    private SData_Result execSportsPointMinus(CClub_SportsPointUpdate req, long unionId, long exePid, long clubId, double outSports, ItemFlow itemFlow) {
        // 获取亲友圈成员信息
        ClubMember exeClubMember = find(req.getOpPid(), req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == exeClubMember) {
            // 找不到被操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        // 被操作者是否在游戏中
        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(req.getOpPid());
//        if (Config.isShare()) {
////            SharePlayerMgr.getInstance().getPlayer(player);
////        }
        if (Objects.nonNull(sharePlayer) && sharePlayer.getRoomInfo().getRoomId() > 0L && sharePlayer.getRoomInfo().getClubId() == clubId && unionId == sharePlayer.getRoomInfo().getUnionId()) {
            return SData_Result.make(ErrorCode.UNION_PLAYER_IN_GAME_ERROR, "UNION_PLAYER_IN_GAME_ERROR");
        }
        ClubMember opClubMember = find(exePid, req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == opClubMember) {
            // 找不到操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }

        double pidPreValue = Double.valueOf(opClubMember.getSportsPoint());
        double execPidPreValue = Double.valueOf(exeClubMember.getSportsPoint());

        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            // 执行减去被操作者的比赛分
//            if (exeClubMember.getClubMemberBO().execSportsPointUpdate(unionId, -req.getValue(), itemFlow, RoomTypeEnum.CLUB, outSports)) {
            exeClubMember.getClubMemberBO().execSportsPointUpdate(unionId, -req.getValue(), itemFlow, RoomTypeEnum.CLUB, outSports);
            // TODO 2021-11-19,傅哥说：先改成只通知本人和管理员(本来是通知亲友圈所有人)
            List<Long> pidList = Arrays.asList(exeClubMember.getClubMemberBO().getPlayerID(), opClubMember.getClubMemberBO().getPlayerID());
            notify2AllMinisterAndPidByClub(clubId, pidList, SUnion_SportsPoint.make(clubId, exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getClubMemberBO().getUnionState()));
            // 执行增加操作者的比赛分
//                opClubMember.getClubMemberBO().execSportsPointUpdate(unionId, req.getValue(), itemFlow, RoomTypeEnum.CLUB, outSports);
            notify2AllMinisterAndPidByClub(clubId, pidList, SUnion_SportsPoint.make(clubId, opClubMember.getClubMemberBO().getPlayerID(), opClubMember.getClubMemberBO().getSportsPoint(), opClubMember.getClubMemberBO().getUnionState()));
            return SData_Result.make(ErrorCode.Success, new UnionSportsPointItem(opClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getSportsPoint(), exeClubMember.getClubMemberBO().getSportsPoint(), execPidPreValue, opClubMember.getSportsPoint(), pidPreValue));
//            } else {
//                // 操作者的比赛分不足
//                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
//            }
        } else if (UnionDefine.UNION_TYPE.NORMAL.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            // 执行减去被操作者的比赛分
            if (exeClubMember.getClubMemberBO().execSportsPointUpdate(unionId, -req.getValue(), itemFlow, RoomTypeEnum.CLUB, outSports)) {
                // TODO 2021-11-19,傅哥说：先改成只通知本人和管理员(本来是通知亲友圈所有人)
                List<Long> pidList = Arrays.asList(exeClubMember.getClubMemberBO().getPlayerID(), opClubMember.getClubMemberBO().getPlayerID());
                notify2AllMinisterAndPidByClub(clubId, pidList, SUnion_SportsPoint.make(clubId, exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getClubMemberBO().getUnionState()));
                // 执行增加操作者的比赛分
                opClubMember.getClubMemberBO().execSportsPointUpdate(unionId, req.getValue(), itemFlow, RoomTypeEnum.CLUB, outSports);
                notify2AllMinisterAndPidByClub(clubId, pidList, SUnion_SportsPoint.make(clubId, opClubMember.getClubMemberBO().getPlayerID(), opClubMember.getClubMemberBO().getSportsPoint(), opClubMember.getClubMemberBO().getUnionState()));
                return SData_Result.make(ErrorCode.Success, new UnionSportsPointItem(opClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getSportsPoint(), exeClubMember.getClubMemberBO().getSportsPoint(), execPidPreValue, opClubMember.getSportsPoint(), pidPreValue));
            } else {
                // 操作者的比赛分不足
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
        } else {
            // 赛事的类型不存在
            return SData_Result.make(ErrorCode.UNION_TYPE_NOT_EXIST, "UNION_TYPE_NOT_EXIST");
        }

    }


    /**
     * 赛事管理员禁止指定玩家完游戏
     *
     * @param unionId 赛事Id
     * @param pid     指定玩家Pid
     * @param isBan   0:解除,1:禁止
     */
    @SuppressWarnings("rawtypes")
    public SData_Result execUnionClubMemberBan(long unionId, long pid, int isBan, long exePid, long clubId) {
        // 获取加入指定赛事的亲友圈Id
        List<Long> clubIdList;
        if (Config.isShare()) {
            clubIdList = ClubMgr.getInstance().getClubListMgr().getClubIdListByUnion(unionId);
        } else {
            clubIdList = ClubMgr.getInstance().getClubListMgr().getClubMap().values().stream().filter(k -> null != k && k.getClubListBO().getUnionId() == unionId).map(k -> k.getClubListBO().getId()).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(clubIdList)) {
            // 没有
            return SData_Result.make(ErrorCode.NotAllow, "clubIdList == null");
        }

        List<Long> clubMemberIdList;
        if (Config.isShare()) {
            clubMemberIdList = ShareClubMemberMgr.getInstance().getAllOnePlayerClubMember(pid).values().stream().filter(k -> {
                if (k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && clubIdList.contains(k.getClubID())) {
                    return k.getClubMemberBO().setUnionBanGame(isBan);
                }
                return false;
            }).map(k -> k.getId()).collect(Collectors.toList());
        } else {
            clubMemberIdList = this.getClubMemberMap().values().stream().filter(k -> {
                if (k.getClubMemberBO().getPlayerID() == pid && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && clubIdList.contains(k.getClubID())) {
                    return k.getClubMemberBO().setUnionBanGame(isBan);
                }
                return false;
            }).map(k -> k.getId()).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(clubMemberIdList)) {
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("unionBanGame", isBan);
            ContainerMgr.get().getDao(ClubMemberBODao.class).update(updateMap, Restrictions.in("id", clubMemberIdList));
            UnionDynamicBO.insertUnionClub(pid, clubId, unionId, exePid, CommTime.nowSecond(), isBan <= 0 ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_CANCEL_BAN_GAME.value() : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_BAN_GAME.value());
            return SData_Result.make(ErrorCode.Success);
        }
        return SData_Result.make(ErrorCode.NotAllow, "clubMemberIdList == null");
    }

    public double sumAllClubSportsPoint(final List<Long> clubIdList) {
        if (Config.isShare()) {
            if (Config.isShareLocal()) {
                return LocalClubMemberMgr.getInstance().getAllClubMemberByClubIds(clubIdList).values().stream().filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).mapToDouble(ClubMember::getTotalSportsPoint).sum();
            } else {
                double sumAllClubSportsPoint = 0;
                for (Long clubId : clubIdList) {
                    sumAllClubSportsPoint += CommMath.FormatDouble(ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value())).mapToDouble(k -> ClubMemberUtils.getArrayValueDouble(k, "sportsPoint") + ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")).sum());
//                sumAllClubSportsPoint += ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).mapToDouble(ClubMember::getTotalSportsPoint).sum();
                }
                return sumAllClubSportsPoint;
            }
        } else {
            return getClubMemberMap().values().stream().filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).mapToDouble(ClubMember::getTotalSportsPoint).sum();
        }
    }


    /**
     * 获取指定亲友圈列表的人数
     * 排除主裁判
     *
     * @param clubIdList  亲友圈id列表
     * @param ownerPid    主裁判Pid
     * @param ownerClubId 亲友圈Id
     * @return
     */
    public int clubIdListToCount(final List<Long> clubIdList, long ownerPid, long ownerClubId) {
        if (Config.isShareLocal()) {
            return (int) LocalClubMemberMgr.getInstance().getAllClubMemberByClubIds(clubIdList)
                    .values()
                    .stream()
                    .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && !(k.getClubMemberBO().getPlayerID() == ownerPid && k.getClubMemberBO().getClubID() == ownerClubId)).count();
        } else {
            int count = 0;
            for (Long clubId : clubIdList) {
                count += ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k))
                        .filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()) && !(ClubMemberUtils.getArrayValueLong(k, "playerID") == ownerPid && ClubMemberUtils.getArrayValueLong(k, "clubID") == ownerClubId)).count();
            }
            return count;
        }
//        Map<Long, ClubMember> clubMemberMap = new HashMap<>();
//        if (Config.isShare()) {
//            for (Long clubId : clubIdList) {
//                clubMemberMap.putAll(ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId));
//            }
//        } else {
//            clubMemberMap = this.getClubMemberMap();
//        }
//        return (int) clubMemberMap
//                .values()
//                .stream()
//                .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && !(k.getClubMemberBO().getPlayerID() == ownerPid && k.getClubMemberBO().getClubID() == ownerClubId)).count();
    }

    /**
     * 赛事排名日志
     *
     * @param clubIdList 亲友圈Id列表
     * @param ownerPid   主裁判
     * @param prizeType  消耗类型
     * @param ranking    排名奖励
     * @param value      奖励值
     */
    public void unionMatchLog(final List<Long> clubIdList, long ownerPid, PrizeType prizeType, int ranking, int value, int roundId, long unionId, long ownerClubId) {
        Map<Long, ClubMember> clubMemberMap = new HashMap<>();
        if (Config.isShare()) {
            for (Long clubId : clubIdList) {
                clubMemberMap.putAll(ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId));
            }
        } else {
            clubMemberMap = this.getClubMemberMap();
        }
        List<ClubMember> rankingMemberList = clubMemberMap
                .values()
                .stream()
                .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && !(k.getClubMemberBO().getPlayerID() == ownerPid && k.getClubMemberBO().getClubID() == ownerClubId))
                .sorted(Comparator.comparing(ClubMember::getSportsPointLong).reversed())
                .limit(50)
                .collect(Collectors.toList());


        for (int i = 0, size = rankingMemberList.size(); i < size; i++) {
            ClubMember member = rankingMemberList.get(i);
            if (Objects.isNull(member)) {
                // 跳过空的成员信息
                continue;
            }
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(member.getClubID());
            if (Objects.isNull(club)) {
                // 跳过空亲友圈信息
                continue;
            }
            Player player = PlayerMgr.getInstance().getPlayer(member.getClubMemberBO().getPlayerID());
            if (Objects.isNull(player)) {
                // 跳过空玩家信息
                continue;
            }
            // 排名
            int rankingId = (i + 1);
            if (rankingId <= ranking) {
                // 可以获得奖励
                player.getFeature(PlayerCurrency.class).gainItemFlow(prizeType, value, ItemFlow.UNION_RANKING_MATCH_REWARD);
                member.getClubMemberBO().saveRankingReward(new Gson().toJson(new UnionRankingItem(rankingId, prizeType.value(), value)));
            } else {
                member.getClubMemberBO().saveRankingReward(new Gson().toJson(new UnionRankingItem(rankingId)));
            }
            FlowLogger.unionMatchLog(rankingId, player.getName(), player.getPid(), club.getClubListBO().getClubsign(), club.getClubListBO().getName(), member.getSportsPoint(), roundId, unionId, member.getClubID());
        }
    }


    /**
     * 赛事排名日志
     *
     * @param clubIdList  亲友圈Id列表
     * @param ownerPid    主裁判
     * @param ownerClubId 主裁判亲友圈id
     * @param pid         玩家pid
     */
    public UnionMatchInfo getRoundCurRankingList(final List<Long> clubIdList, long ownerPid, long ownerClubId, long pid, long clubId) {
        Map<Long, ClubMember> clubMemberMap = new HashMap<>();
        if (Config.isShare()) {
            for (Long clubId1 : clubIdList) {
                clubMemberMap.putAll(ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId1));
            }
        } else {
            clubMemberMap = this.getClubMemberMap();
        }
        List<ClubMember> rankingMemberList = clubMemberMap
                .values()
                .stream()
                .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && !(k.getClubMemberBO().getPlayerID() == ownerPid && k.getClubMemberBO().getClubID() == ownerClubId))
                .sorted(Comparator.comparing(ClubMember::getSportsPointLong).reversed())
                .limit(50)
                .collect(Collectors.toList());
        int curRankingId = 0;
        List<UnionMatchItem> unionMatchItemList = Lists.newArrayList();
        for (int i = 0, size = rankingMemberList.size(); i < size; i++) {
            ClubMember member = rankingMemberList.get(i);
            if (Objects.isNull(member)) {
                // 跳过空的成员信息
                continue;
            }
            // 排名
            int rankingId = (i + 1);
            if (rankingId <= 10) {
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(member.getClubID());
                if (Objects.isNull(club)) {
                    // 跳过空亲友圈信息
                    continue;
                }
                Player player = PlayerMgr.getInstance().getPlayer(member.getClubMemberBO().getPlayerID());
                if (Objects.isNull(player)) {
                    // 跳过空玩家信息
                    continue;
                }
                unionMatchItemList.add(new UnionMatchItem(rankingId, player.getName(), player.getPid(), club.getClubListBO().getClubsign(), club.getClubListBO().getName(), member.getSportsPoint()));
            }
            if (member.getClubMemberBO().getPlayerID() == pid && member.getClubMemberBO().getClubID() == clubId) {
                curRankingId = rankingId;
            }
        }
        return new UnionMatchInfo(unionMatchItemList, curRankingId);
    }


    /**
     * 获取亲友圈成员-亲友圈列表中最大竞技点的亲友圈Id
     *
     * @param clubIdList 亲友圈列表
     * @param pid        玩家pid
     * @return
     */
    public long getMemberMaxSportsPointClubId(final List<Long> clubIdList, long pid) {
//        Map<Long, ClubMember> clubMemberMap = new HashMap<>();
//        if (Config.isShare()) {
//            for (Long clubId1 : clubIdList) {
//                clubMemberMap.putAll(ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId1));
//            }
//        } else {
//            clubMemberMap = this.getClubMemberMap();
//        }
        return clubMemberMap
                .values()
                .stream()
                .filter(k -> clubIdList.contains(k.getClubID()) && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && k.getClubMemberBO().getPlayerID() == pid && !(k.isBanGame() || k.isUnionBanGame()))
                .sorted(Comparator.comparing(ClubMember::getSportsPointLong).reversed())
                .map(k -> k.getClubMemberBO().getClubID())
                .findFirst().orElse(0L);
    }


    /**
     * 下属人数分组
     *
     * @param clubId            亲友圈
     * @param promotionItemList 获取推广员列表
     * @return
     */
    @Deprecated
    public Map<Long, Long> getSubordinateNumberMap(long clubId, List<ClubPromotionItem> promotionItemList) {
        if (Config.isShareLocal()) {
            return ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId)
                    .values()
                    .stream()
                    .filter(k -> k.getClubID() == clubId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && promotionItemList.stream().anyMatch(v -> k.isSubordinate(v.getPid()))).collect(Collectors.groupingBy(p -> p.getClubMemberBO().getPartnerPid(), Collectors.counting()));
        } else {
            return ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId)
                    .values()
                    .stream()
                    .map(k -> ClubMemberUtils.stringSwitchArray(k))
                    .filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()) && promotionItemList.stream().anyMatch(v -> ClubMemberUtils.isSubordinate(v.getPid(), ClubMemberUtils.getArrayValueLong(k, "partnerPid"), ClubMemberUtils.getArrayValueInteger(k, "promotion")))).collect(Collectors.groupingBy(p -> ClubMemberUtils.getArrayValueLong(p, "partnerPid"), Collectors.counting()));
        }

//        Map<Long, ClubMember> clubMemberMap = new HashMap<>();
//        if (Config.isShare()) {
//            clubMemberMap = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId);
//        } else {
//            clubMemberMap = this.getClubMemberMap();
//        }
//        return clubMemberMap
//                .values()
//                .stream()
//                .filter(k -> k.getClubID() == clubId && k.getStatus(Club_Player_Status.PLAYER_JIARU.value()) && promotionItemList.stream().anyMatch(v -> k.isSubordinate(v.getPid()))).collect(Collectors.groupingBy(p -> p.getClubMemberBO().getPartnerPid(), Collectors.counting()));
    }

    /**
     * 获取推广员列表
     *
     * @param promotionList 推广员参数
     * @param pid           玩家Pid
     * @return
     */
    @Deprecated
    public SData_Result getClubPromotionList(CClub_PromotionList promotionList, long pid) {
        if (isNotClubCreate(promotionList.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        List<ClubPromotionItem> promotionItemList = this.getPromotionList(promotionList.getClubId()).stream().map(k -> {
                    Player player = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                    if (Objects.nonNull(player)) {
                        return new ClubPromotionItem(player.getPid(), player.getName(), player.getHeadImageUrl(), k.getClubMemberBO().getCalcActive(), k.getClubMemberBO().getSumActivePoint(), k.getClubMemberBO().getPromotion());
                    }
                    return null;
                })
                .filter(k -> Objects.nonNull(k))
                .skip(Page.getPageNum(promotionList.getPageNum()))
                .limit(Page.PAGE_SIZE)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(promotionItemList)) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        final Map<Long, Long> subordinateNumberMap = getSubordinateNumberMap(promotionList.getClubId(), promotionItemList);
        return SData_Result.make(ErrorCode.Success, promotionItemList.stream().map(k -> {
            Long value = subordinateNumberMap.get(k.getPid());
            if (Objects.nonNull(value)) {
                k.setNumber(value.intValue() + 1);
            } else {
                k.setNumber(1);
            }
            return k;
        }).collect(Collectors.toList()));
    }

    /**
     * 活跃异常
     *
     * @param promotionActive
     * @param pid
     * @return
     */
    @Deprecated
    public SData_Result getClubPromotionActive(CClub_PromotionActive promotionActive, long pid) {
        if (isNotClubCreate(promotionActive.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        if (isNotPromotion(promotionActive.getClubId(), promotionActive.getPid())) {
            // 不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }
        ClubMember clubMember = getClubMember(promotionActive.getClubId(), promotionActive.getPid());
        if (Objects.nonNull(clubMember)) {
            double curValue = clubMember.getClubMemberBO().execPromotionActiveValue(promotionActive.getValue(), promotionActive.getType());
            // 	活跃度扣除：年-月-日 时-分-秒  @玩家名称因系统异常，扣除了@玩家名称 活跃度@值 ，当前活跃度@值；
            // 	活跃度补偿：年-月-日 时-分-秒  @玩家名称因系统异常，补偿了@玩家名称 活跃度@值 ，当前活跃度@值；
            PromotionDynamicBO.insertPromotionDynamicBO(promotionActive.getClubId(), 0L, promotionActive.getPid(), pid, Club_define.Club_PROMOTION_DYNAMIC.getPromotionDynamicActive(promotionActive.getType()).value(), String.valueOf(CommMath.FormatDouble(Math.abs(promotionActive.getValue()))), String.valueOf(curValue), promotionActive.getPid());
        } else {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 活跃计算列表
     *
     * @param req
     * @param pid
     * @return
     */
    @Deprecated
    public SData_Result getClubPromotionCalcActiveList(CClub_SubordinateList req, long pid) {
        return SData_Result.make(ErrorCode.Success, Collections.emptyList());
    }

    /**
     * 活跃计算
     *
     * @param promotionActive
     * @param pid
     * @return
     */
    @Deprecated
    public SData_Result getClubPromotionCalcActive(CClub_PromotionCalcActive promotionActive, long pid) {
        if (isNotClubCreate(promotionActive.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        if (isNotPromotion(promotionActive.getClubId(), promotionActive.getPid())) {
            // 不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }
        if (promotionActive.getValue() < 0D || promotionActive.getValue() > 100D) {
            return SData_Result.make(ErrorCode.InvalidParam, "InvalidParam");
        }
        ClubMember clubMember = getClubMember(promotionActive.getClubId(), promotionActive.getPid());
        if (Objects.nonNull(clubMember)) {
            clubMember.getClubMemberBO().saveCalcActive(promotionActive.getValue());
        } else {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 活跃计算批量
     *
     * @param promotionActive
     * @param pid
     * @return
     */
    @Deprecated
    public SData_Result getClubPromotionCalcActiveBatch(CClub_PromotionCalcActiveBatch promotionActive, long pid) {
        if (isNotClubCreate(promotionActive.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        if (isNotPromotion(promotionActive.getClubId(), promotionActive.getPid())) {
            // 不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }
        if (CollectionUtils.isEmpty(promotionActive.getPromotionCalcActiveItemList())) {
            return SData_Result.make(ErrorCode.InvalidParam, "InvalidParam");
        }
        ClubMember clubMember = getClubMember(promotionActive.getClubId(), promotionActive.getPid());
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        return SData_Result.make(ErrorCode.Success, clubMember);
    }


    /**
     * 添加推广员
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    @Deprecated
    public SData_Result getClubPromotionPidAdd(CClub_FindPIDAdd findPIDAdd, long pid) {
        if (isNotClubCreate(findPIDAdd.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        if (findPIDAdd.getPid() == pid || isPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            // 是推广员
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        Player player = PlayerMgr.getInstance().getPlayer(findPIDAdd.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        // 是否亲友圈成员
        ClubMember member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
        if (Objects.isNull(member)) {
            if (!this.onInsertClubMember(player, findPIDAdd.getClubId(), Club_define.Club_PROMOTION.APPOINT.ordinal(), findPIDAdd.getPid(), Club_Player_Status.PLAYER_JIARU.value(), pid, 0, 0L)) {
                return SData_Result.make(ErrorCode.CLUB_INVITATION_ERROR, "CLUB_INVITATION_ERROR");
            }
        } else if (member.getClubMemberBO().getPartnerPid() <= 0L) {
            // 设置推广员
            member.getClubMemberBO().savePromotion(Club_define.Club_PROMOTION.APPOINT.ordinal(), findPIDAdd.getPid());
            SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(player.getPid());
            sharePlayer.pushProtoMq(SClub_PromotionChange.make(findPIDAdd.getPid(), findPIDAdd.getClubId(), Club_define.Club_PROMOTION.APPOINT.ordinal()));
        } else {
            return SData_Result.make(ErrorCode.CLUB_PARTNER_EXIST, "CLUB_PARTNER_EXIST");
        }
        // 	推广员变动通知：年-月-日 时-分-秒  @玩家名称被@玩家名称设置为推广员；
        PromotionDynamicBO.insertPromotionDynamicBO(findPIDAdd.getClubId(), 0L, findPIDAdd.getPid(), pid, Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_SET.value(), findPIDAdd.getPid());
        UnionDynamicBO.insertClubDynamic(findPIDAdd.pid, findPIDAdd.clubId, pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_PROMOTION_DYNAMIC_SET.value());
        return SData_Result.make(ErrorCode.Success, new ClubPromotionItem(player.getPid(), player.getName(), player.getHeadImageUrl(), 1, 0, 0D, Club_define.Club_PROMOTION.APPOINT.ordinal()));
    }

    /**
     * 查询推广员
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    @Deprecated
    public SData_Result getClubPromotionPidInfo(CClub_FindPIDAdd findPIDAdd, long pid) {
        if (isNotClubCreate(findPIDAdd.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        if (isPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            // 是推广员
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        int type = 0;
        ClubMember member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
        if (Objects.nonNull(member) && member.getClubMemberBO().getPartnerPid() > 0L) {
            type = 2;
        }
        Player player = PlayerMgr.getInstance().getPlayer(findPIDAdd.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionPidInfo(player.getShortPlayer(), type));
    }

    /**
     * 推广员任命、卸任
     *
     * @param req
     * @param pid
     * @return
     */
    @Deprecated
    public SData_Result getClubPromotionAppointOrLeaveOffice(CClub_PromotionCalcActive req, long pid) {
        if (isNotClubCreate(req.getClubId(), pid) || req.getPid() == pid) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        // 获取推广员信息
        ClubMember clubMember = this.getPromotion(req.getClubId(), req.getPid());
        if (Objects.isNull(clubMember)) {
            // 不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }
        if (clubMember.setPromotion(pid)) {
            return SData_Result.make(ErrorCode.Success);
        } else {
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }
    }

    /**
     * 改变亲友圈退出加入需要审核功能
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result changeQuitAndJoinConfig(CClub_ChangeQuitAndJoin req, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        // 检查亲友圈是否存在。
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (isNotMinister(req.getClubId(), pid)) {
            // 不是亲友圈管理员
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "CLUB_NOT_CREATE");
        }
        club.getClubListBO().saveJoin(req.getJoinNeedExamine());
        club.getClubListBO().saveQuit(req.getQuitNeedExamine());
        return SData_Result.make(ErrorCode.Success, true);

    }

    /**
     * 改变亲友圈退出加入需要审核功能
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result changeDiamondsAttention(CClub_ChangeDiamondsAttention req, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        // 检查亲友圈是否存在。
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (isNotClubCreate(req.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        club.getClubListBO().saveDiamondsAttentionMinister(req.getDiamondsAttentionMinister());
        club.getClubListBO().saveDiamondsAttentionAll(req.getDiamondsAttentionAll());
        return SData_Result.make(ErrorCode.Success, req);

    }

    /**
     * 改变亲友圈查看人数限制
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result changeShowOnlinePlayerNum(CClub_ChangeShowOnlinePlayerNum req, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        // 检查亲友圈是否存在。
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (isNotMinister(req.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "CLUB_NOTMINISTER");
        }
        club.getClubListBO().saveShowOnlinePlayerNum(req.getShowOnlinePlayerNum());
        return SData_Result.make(ErrorCode.Success, req);
    }

    /**
     * 亲友圈查看人数限制
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result getShowOnlinePlayerNum(CClub_ChangeShowOnlinePlayerNum req, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        // 检查亲友圈是否存在。
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (isNotMinister(req.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "CLUB_NOTMINISTER");
        }
        return SData_Result.make(ErrorCode.Success, SClub_ShowOnlinePlayerNum.make(req.getClubId(), club.getClubListBO().getShowOnlinePlayerNum()));
    }

    /**
     * 删除推广员
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    @Deprecated
    public SData_Result getClubPromotionDelete(CClub_FindPIDAdd findPIDAdd, long pid) {
        if (isNotClubCreate(findPIDAdd.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        if (findPIDAdd.getPid() == pid || isNotPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            // 是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }

        if (checkExistPromotionSubordinateNumberNotEqualZero(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION_SUBIRDINATE, "CLUB_EXIST_PROMOTION_SUBIRDINATE");
        }
        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer((long) findPIDAdd.getPid());
        if (Objects.isNull(sharePlayer)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        ClubMember member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
        if (Objects.nonNull(member)) {
            if (member.getClubMemberBO().getSportsPoint() != 0D) {
                return SData_Result.make(ErrorCode.UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO, "UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO");
            }
            if (sharePlayer.getRoomInfo().getRoomId() > 0L && sharePlayer.getRoomInfo().getClubId() == findPIDAdd.getClubId()) {
                return SData_Result.make(ErrorCode.CLUB_MEMBER_ROOM_ERROR, "CLUB_MEMBER_ROOM_ERROR");
            }
            member.setStatus(Club_Player_Status.PLAYER_TICHU.value(), pid);
        }

        // 	推广员变动通知：年-月-日 时-分-秒  @玩家名称被@玩家名称删除推广员；
        PromotionDynamicBO.insertPromotionDynamicBO(findPIDAdd.clubId, 0L, findPIDAdd.pid, pid, Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_DELETE.value(), findPIDAdd.getPid());
        UnionDynamicBO.insertClubDynamic(findPIDAdd.pid, findPIDAdd.clubId, pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_PROMOTION_DYNAMIC_DELETE.value());
        return SData_Result.make(ErrorCode.Success, new ClubPromotionItem(sharePlayer.getPlayerBO().getId(), sharePlayer.getPlayerBO().getName(), sharePlayer.getPlayerBO().getHeadImageUrl(), 1, 0, 0D, Club_define.Club_PROMOTION.NOT.ordinal()));
    }


    /**
     * 亲友圈变更推广员
     *
     * @param clubID     亲友圈ID
     * @param toPid      变更的玩家PID
     * @param partnerPid 变更
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Deprecated
    public SData_Result getClubPromotionChange(long clubID, long toPid, long partnerPid, long exePid) {
        ClubMember doMember = getClubMember(clubID, exePid);
        if (Objects.isNull(doMember)) {
            // 不是亲友圈成员
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        if (doMember.isNotClubCreate() && doMember.isNotPromotion()) {
            // 没有权限
            return SData_Result.make(ErrorCode.CLUB_PARTNER_CREATE, "CLUB_PARTNER_CREATE");
        }

        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }

        ClubMember member = getClubMember(clubID, toPid);
        if (Objects.isNull(member)) {
            // 不是亲友圈成员
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        if (member.isPromotion() || member.isClubCreate()) {
            // 推广员并且创建者
            return SData_Result.make(ErrorCode.CLUB_PARTNER_CREATE, "CLUB_PARTNER_CREATE");
        }
        // 旧绑定的推广员
        long oldPartnerPid = member.getClubMemberBO().getPartnerPid();
        if (partnerPid <= 0L) {
            // 默认变更创建者。
            member.getClubMemberBO().savePartnerPid(0L);
        } else {
            if (doMember.isNotClubCreate()) {
                return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
            }
            if (isNotPromotion(clubID, partnerPid)) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
            }
            member.getClubMemberBO().savePartnerPid(partnerPid);
        }
        Player player = PlayerMgr.getInstance().getPlayer(toPid);
        // 玩家不存在。
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError);
        }
        // 	玩家从属发生改变：年-月-日 时-分-秒  @玩家名称 被@玩家名称 设置为@玩家名称 的下属玩家；
        PromotionDynamicBO.insertPromotionDynamicBO(clubID, 0L, toPid, exePid, Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_CHANGE_ALL.value(), String.valueOf(partnerPid <= 0L ? club.getClubListBO().getOwnerID() : partnerPid), 0L);
        if (partnerPid > 0L) {
            PromotionDynamicBO.insertPromotionDynamicBO(clubID, 0L, toPid, exePid, Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_CHANGE.value(), String.valueOf(partnerPid), partnerPid);
        }
        if (oldPartnerPid > 0L) {
            PromotionDynamicBO.insertPromotionDynamicBO(clubID, 0L, toPid, exePid, Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_CHANGE.value(), String.valueOf(partnerPid <= 0L ? club.getClubListBO().getOwnerID() : partnerPid), oldPartnerPid);
        }

        return SData_Result.make(ErrorCode.Success, player.getShortPlayer());
    }


    /**
     * 查询亲友圈指定合伙人列表
     *
     * @param promotionChange 参数
     * @param pid             玩家PID
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Deprecated
    public SData_Result getClubPromotionChangeList(CClub_PromotionChange promotionChange, long pid) {
        ClubMember doMember = getClubMember(promotionChange.getClubId(), pid);
        if (Objects.isNull(doMember)) {
            // 不是亲友圈成员
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        if (doMember.isNotClubCreate() && doMember.isNotPromotion()) {
            // 没有权限
            return SData_Result.make(ErrorCode.CLUB_PARTNER_CREATE, "CLUB_PARTNER_CREATE");
        }
        // 创建者?0:合伙人Pid
        long partnerPid = doMember.isClubCreate() ? 0L : pid;
        ClubMember member = getClubMember(promotionChange.getClubId(), promotionChange.getPid());
        if (Objects.isNull(member)) {
            // 不是亲友圈成员
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        if (member.isPromotion() || member.isClubCreate()) {
            // 推广员并且创建者
            return SData_Result.make(ErrorCode.CLUB_PARTNER_CREATE, "CLUB_PARTNER_CREATE");
        }

        List<ClubPartnerChangeInfo> shortPlayers = new ArrayList<>();
        shortPlayers.add(new ClubPartnerChangeInfo(new ShortPlayer(), member.getClubMemberBO().getPartnerPid() <= 0));
        // 推广员列表
        List<ClubMember> clubMemberList = this.getPromotionList(promotionChange.getClubId(), partnerPid);
        if (CollectionUtils.isEmpty(clubMemberList)) {
            return SData_Result.make(ErrorCode.Success, shortPlayers);
        }
        // 添加第一个默认为：默认创建者。
        Player player = null;
        // 遍历合伙人列表
        for (ClubMember cMember : clubMemberList) {
            // 获取玩家信息
            player = PlayerMgr.getInstance().getPlayer(cMember.getClubMemberBO().getPlayerID());
            if (null == player) {
                // 跳过异常数据
                continue;
            }
            // 增加数据项。
            shortPlayers.add(new ClubPartnerChangeInfo(player.getShortPlayer(), member.getClubMemberBO().getPartnerPid() == cMember.getClubMemberBO().getPlayerID()));
        }
        return SData_Result.make(ErrorCode.Success, shortPlayers);
    }

    /**
     * 下属成员列表
     *
     * @param subordinateList 参数
     * @param pid             玩家Pid
     * @return
     */
    @Deprecated
    public final SData_Result getSubordinateList(CClub_SubordinateList subordinateList, long pid) {
        long partnerPid = 0L;
        if (subordinateList.getPid() <= 0L) {
            // 判断玩家本事是否推广员
            if (isNotPromotion(subordinateList.getClubId(), pid)) {
                // 不是推广员
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
            }
            partnerPid = pid;
        } else {
            if (isNotClubCreate(subordinateList.getClubId(), pid)) {
                // 不是亲友圈创建者
                return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
            }
            // 判断目标玩家是否推广员
            if (isNotPromotion(subordinateList.getClubId(), subordinateList.getPid())) {
                // 不是推广员
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
            }
            partnerPid = subordinateList.getPid();
        }

        final long qPid = TypeUtils.StringTypeLong(subordinateList.getQuery());
        List<ClubSubordinateItem> subordinateItemList = this.getSubordinateList(subordinateList.getClubId(), partnerPid)
                .stream().map(k -> {
                    Player player = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                    if (Objects.nonNull(player)) {
                        // 没有查询信息 或者
                        if (StringUtils.isEmpty(subordinateList.getQuery()) || (qPid == player.getPid())) {
                            return new ClubSubordinateItem(player.getPid(), player.getName(), player.getHeadImageUrl(), k.getClubMemberBO().getActivePoint());
                        }
                        if (player.getName().contains(subordinateList.getQuery())) {
                            return new ClubSubordinateItem(player.getPid(), player.getName(), player.getHeadImageUrl(), k.getClubMemberBO().getActivePoint());
                        }
                    }
                    return null;
                })
                .filter(k -> Objects.nonNull(k))
                .skip(Page.getPageNum(subordinateList.getPageNum()))
                .limit(Page.PAGE_SIZE)
                .collect(Collectors.toList());
        return SData_Result.make(ErrorCode.Success, CollectionUtils.isEmpty(subordinateItemList) ? Collections.emptyList() : subordinateItemList);
    }


    /**
     * 添加下属
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    @Deprecated
    public SData_Result getClubSubordinatePidAdd(CClub_FindPIDAdd findPIDAdd, long pid) {
        if (isNotPromotion(findPIDAdd.getClubId(), pid)) {
            // 不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }
        if (findPIDAdd.getPid() == pid || isPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            // 是推广员
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        Player player = PlayerMgr.getInstance().getPlayer(findPIDAdd.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        // 是否亲友圈成员
        ClubMember member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
        if (Objects.isNull(member)) {
            if (!this.onInsertClubMember(player, findPIDAdd.getClubId(), Club_define.Club_PROMOTION.NOT.ordinal(), pid, Club_Player_Status.PLAYER_YAOQING.value(), pid, 0, 0L)) {
                return SData_Result.make(ErrorCode.CLUB_INVITATION_ERROR, "CLUB_INVITATION_ERROR");
            }
        } else {
            return SData_Result.make(ErrorCode.CLUB_PARTNER_EXIST, "CLUB_PARTNER_EXIST");
        }
        return SData_Result.make(ErrorCode.Success, new ClubSubordinateItem(player.getPid(), player.getName(), player.getHeadImageUrl(), 0));

    }

    /**
     * 查询下属
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    @Deprecated
    public SData_Result getClubSubordinatePidInfo(CClub_FindPIDAdd findPIDAdd, long pid) {
        if (isNotPromotion(findPIDAdd.getClubId(), pid)) {
            // 不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }
        if (findPIDAdd.getPid() == pid || isPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            // 是推广员
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        int type = 0;
        ClubMember member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
        if (Objects.nonNull(member)) {
            // 已加入亲友圈
            type = 1;
            if (member.getClubMemberBO().getPartnerPid() > 0L) {
                // 已经绑定了推广员
                type = 2;
            }
        }
        Player player = PlayerMgr.getInstance().getPlayer(findPIDAdd.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionPidInfo(player.getShortPlayer(), type));
    }


    /**
     * 查询亲友圈推广员动态
     *
     * @param dynamic 参数
     * @param pid     玩家Pid
     * @return
     */
    @Deprecated
    public SData_Result getClubPromotionDynamic(CClub_PromotionDynamic dynamic, long pid) {
        ClubMember member = getClubMember(dynamic.getClubId(), pid);
        if (Objects.isNull(member)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        Club_define.Club_PROMOTION_DYNAMIC_TYPE type = Club_define.Club_PROMOTION_DYNAMIC_TYPE.valueOf(dynamic.getType());
        if (Club_define.Club_PROMOTION_DYNAMIC_TYPE.UNION.equals(type) && dynamic.getUnionId() <= 0L) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
        }
        if (member.isClubCreate()) {
            if (dynamic.getPid() <= 0L) {
                // 创建者查询
                return getClubCreateFindPromotionDynamic(dynamic, type);
            } else {
                // 不是推广员
                if (isNotPromotion(dynamic.getClubId(), dynamic.getPid())) {
                    return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
                }
                return getClubFindPromotionDynamic(dynamic, type, dynamic.getPid());
            }
        } else if (member.isPromotion()) {
            return getClubFindPromotionDynamic(dynamic, type, pid);
        }
        return SData_Result.make(ErrorCode.Success, Collections.emptyList());
    }

    /**
     * 亲友圈创建者查询推广员明细
     *
     * @return
     */
    @Deprecated
    private SData_Result getClubCreateFindPromotionDynamic(CClub_PromotionDynamic dynamic, Club_define.Club_PROMOTION_DYNAMIC_TYPE type) {
        long unionId = Club_define.Club_PROMOTION_DYNAMIC_TYPE.UNION.equals(type) ? dynamic.getUnionId() : 0L;
        boolean isAll = Club_define.Club_PROMOTION_DYNAMIC_TYPE.ALL.equals(type);
        // 推广员动态列表
        List<Integer> dynamicTypeList = Club_define.Club_PROMOTION_DYNAMIC_TYPE.valueOf(type, true);
        Criteria criteria = null;
        // "or" 查询
        Criteria restrictionsOr = Restrictions.or(Restrictions.eq("pid", dynamic.getQuery()), Restrictions.eq("execPid", dynamic.getQuery()), Restrictions.eq("partnerPid", dynamic.getQuery()));
        if (CollectionUtils.isEmpty(dynamicTypeList)) {
            criteria = Restrictions.and(Restrictions.eq("clubId", dynamic.getClubId()), isAll ? null : Restrictions.eq("unionId", unionId), Restrictions.ne("execType", Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_CHANGE.value()), dynamic.getQuery() <= 0L ? null : restrictionsOr);
        } else {
            criteria = Restrictions.and(Restrictions.eq("clubId", dynamic.getClubId()), isAll ? null : Restrictions.eq("unionId", unionId), Restrictions.in("execType", dynamicTypeList), dynamic.getQuery() <= 0L ? null : restrictionsOr);
        }
        criteria.desc("id").setPageNum(Page.getPageNum(dynamic.getPageNum())).setPageSize(Page.PAGE_SIZE);
        return SData_Result.make(ErrorCode.Success, getClubPromotionDynamicItem(ContainerMgr.get().getComponent(PromotionDynamicBOService.class).findAll(criteria)));
    }

    /**
     * 亲友圈查询推广员明细
     *
     * @return
     */
    @Deprecated
    private SData_Result getClubFindPromotionDynamic(CClub_PromotionDynamic dynamic, Club_define.Club_PROMOTION_DYNAMIC_TYPE type, long pid) {
        long unionId = Club_define.Club_PROMOTION_DYNAMIC_TYPE.UNION.equals(type) ? dynamic.getUnionId() : 0L;
        boolean isAll = Club_define.Club_PROMOTION_DYNAMIC_TYPE.ALL.equals(type);

        // 推广员动态列表
        List<Integer> dynamicTypeList = Club_define.Club_PROMOTION_DYNAMIC_TYPE.valueOf(type, false);
        Criteria criteria = null;
        // "or" 查询
        Criteria restrictionsOr = Restrictions.or(Restrictions.eq("pid", dynamic.getQuery()), Restrictions.eq("execPid", dynamic.getQuery()));
        if (CollectionUtils.isEmpty(dynamicTypeList)) {
            criteria = Restrictions.and(Restrictions.eq("clubId", dynamic.getClubId()), isAll ? null : Restrictions.eq("unionId", unionId), Restrictions.eq("partnerPid", pid), dynamic.getQuery() <= 0L ? null : restrictionsOr);
        } else {
            criteria = Restrictions.and(Restrictions.eq("clubId", dynamic.getClubId()), isAll ? null : Restrictions.eq("unionId", unionId), Restrictions.eq("partnerPid", pid), Restrictions.in("execType", dynamicTypeList), dynamic.getQuery() <= 0L ? null : restrictionsOr);
        }
        criteria.desc("id").setPageNum(Page.getPageNum(dynamic.getPageNum())).setPageSize(Page.PAGE_SIZE);
        return SData_Result.make(ErrorCode.Success, getClubPromotionDynamicItem(ContainerMgr.get().getComponent(PromotionDynamicBOService.class).findAll(criteria)));
    }

    /**
     * 亲友圈推广员动态列表
     *
     * @param dynamicBOList
     * @return
     */
    @Deprecated
    private List<ClubPromotionDynamicItem> getClubPromotionDynamicItem(List<PromotionDynamicBO> dynamicBOList) {
        return dynamicBOList.stream().map(k -> {
            Player player = PlayerMgr.getInstance().getPlayer(k.getPid());
            if (Objects.nonNull(player)) {
                k.setName(player.getName());
            } else {
                return null;
            }
            if (k.getExecPid() > 0L) {
                Player execPlayer = PlayerMgr.getInstance().getPlayer(k.getExecPid());
                if (Objects.nonNull(execPlayer)) {
                    k.setExecName(execPlayer.getName());
                } else {
                    return null;
                }
            }
            String value = k.getValue();
            if (Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_CHANGE.value() == k.getExecType() || Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_CHANGE_ALL.value() == k.getExecType()) {
                Player valuePlayer = PlayerMgr.getInstance().getPlayer(Long.parseLong(value));
                if (Objects.nonNull(valuePlayer)) {
                    value = valuePlayer.getName();
                } else {
                    return null;
                }
            }
            return new ClubPromotionDynamicItem(k.getId(), k.getPid(), k.getName(), k.getExecPid(), k.getExecName(), k.getExecTime(), k.getExecType(), value, k.getCurValue(), k.getPreValue(), k.getRoomKey());
        }).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
    }

    /**
     * 获取推广员活跃报表
     */
    @Deprecated
    public SData_Result getPromotionActiveReportForm(CClub_FindPIDAdd findPIDAdd, long pid) {
        ClubMember member = getClubMember(findPIDAdd.getClubId(), pid);
        if (Objects.isNull(member)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        if (member.isNotPromotion() && member.isNotClubCreate()) {
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }
        long partnerPid = pid;
        if (member.isClubCreate()) {
            if (isNotPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION findPIDAdd.getPid() :{}", findPIDAdd.getPid());
            }
            partnerPid = findPIDAdd.getPid();
        }
        List<ClubPromotionActiveReportFormItem> clubPromotionActiveReportFormItemList = ((ClubPromotionActiveReportFormLogFlowService) ContainerMgr.get().getComponent(ClubPromotionActiveReportFormLogFlowService.class.getSimpleName()))
                .findAllE(Restrictions.and(
                        Restrictions.eq("clubId", findPIDAdd.getClubId())
                        , Restrictions.eq("pid", partnerPid)
                        , Restrictions.le("date_time", CommTime.getYesterDayStringYMD(1))).desc("id").setLimit(7), ClubPromotionActiveReportFormItem.class, ClubPromotionActiveReportFormItem.getItemsName());
        return SData_Result.make(ErrorCode.Success, CollectionUtils.isEmpty(clubPromotionActiveReportFormItemList) ? Collections.emptyList() : clubPromotionActiveReportFormItemList);
    }

    /**
     * 执行推广员活跃报表记录
     * 已废弃
     */
    @Deprecated
    public void execPromotionActiveReportForm(String dateTime) {
        this.getClubMemberMap().values().stream().filter(k -> k.isPromotion() && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).forEach(k -> FlowLogger.clubPromotionActiveReportFormLog(k.getClubID(), k.getClubMemberBO().getPlayerID(), k.getClubMemberBO().clearDayActivePoint(), dateTime));
    }


    /**
     * 添加推广员
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    public SData_Result getClubPromotionLevelPidAdd(CClub_FindPIDAdd findPIDAdd, long pid) {
        ClubMember createClubMember = getClubMember(findPIDAdd.getClubId(), pid);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(findPIDAdd.getClubId());
        if (Objects.isNull(createClubMember) || Objects.isNull(club) || createClubMember.isNotClubCreate()) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }

        if (findPIDAdd.getPid() == pid || isLevelPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            // 是推广员
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        Player player = PlayerMgr.getInstance().getPlayer(findPIDAdd.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        // 是否亲友圈成员
        ClubMember member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
        if (Objects.isNull(member)) {
            if (UnionMgr.getInstance().getUnionMemberMgr().checkExistInUnionOtherClub(findPIDAdd.getClubId(), player.getPid())) {
                // 同赛事不同亲友圈不能重复拉人
                return SData_Result.make(ErrorCode.CLUB_PLAYER_EXIT_IN_OTHER_UNION, "onJoinClub CLUB_PLAYER_EXIT_IN_OTHER_UNION");
            }
            SData_Result result = ClubMember.checkExistJoinOrQuitTimeLimit(player.getPid(), findPIDAdd.getClubId(), Club_Player_Status.PLAYER_JIARU.value(), true);
            if (!ErrorCode.Success.equals(result.getCode())) {
                return result;
            }
            if (!this.onInsertClubMember(player, findPIDAdd.getClubId(), Club_define.Club_PROMOTION.NOT.ordinal(), 0L, Club_Player_Status.PLAYER_JIARU.value(), pid, Club_define.Club_PROMOTION_LEVEL.APPOINT.ordinal(), createClubMember.getId())) {
                return SData_Result.make(ErrorCode.CLUB_INVITATION_ERROR, "CLUB_INVITATION_ERROR");
            }
            member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
            UnionDynamicBO.insertClubDynamic(findPIDAdd.pid, findPIDAdd.clubId, pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_PROMOTION_DYNAMIC_SET.value());
        } else {
            if (member.getClubMemberBO().getLevel() <= 0 && member.getClubMemberBO().getRealUpLevelId() <= 0L) {
                // 设置推广员
                member.getClubMemberBO().saveLevelAndUpLevelId(Club_define.Club_PROMOTION_LEVEL.APPOINT.ordinal(), createClubMember.getId());
                UnionDynamicBO.insertClubDynamic(findPIDAdd.pid, findPIDAdd.clubId, pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_PROMOTION_DYNAMIC_SET.value());
                DispatcherComponent.getInstance().publish(new PromotionLevelInsertEvent(member.getId(), createClubMember.getId()));
            } else {
                return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
            }
        }
//        //推广员初始区间分成数据
//        if(createClubMember.isSectionShare()){
//            member.initPromotionSection(club.getClubListBO().getUnionId(),false);
//        }
        //亲友圈身份变动记录
        FlowLogger.clubmemberStatusLog(findPIDAdd.pid, findPIDAdd.clubId, pid, findPIDAdd.clubId, Club_define.CLUB_EXEC_TYPE.CLUB_EXEC_BECOME_PROMOTION.value());
        return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItem(player.getPid(), player.getName(), player.getHeadImageUrl(), 1, member.getSportsPoint(), sumSportsPoint(Arrays.asList(member.getId())), member.getClubMemberBO().getLevel(), member.getClubMemberBO().getPersonalSportsPointWarning(),
                member.isLevelPromotion() ? member.getClubMemberBO().getSportsPointWarning() : -1, member.getClubMemberBO().getShareType(), member.getClubMemberBO().getShareValue(), member.getClubMemberBO().getShareFixedValue(), 0, member.getClubMemberBO().getAlivePoint(), member.getClubMemberBO().getEliminatePoint()));
    }


    /**
     * 查询推广员
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    public SData_Result getClubPromotionLevelPidInfo(CClub_FindPIDAdd findPIDAdd, long pid) {
        if (isNotClubCreate(findPIDAdd.getClubId(), pid)) {
            // 不是亲友圈创建者
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        if (findPIDAdd.getPid() == pid || isLevelPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            // 是推广员
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        int type = 0;
        ClubMember member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
        if (Objects.nonNull(member) && member.getClubMemberBO().getUpLevelId() > 0L) {
            type = 2;
        }
        Player player = PlayerMgr.getInstance().getPlayer(findPIDAdd.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionPidInfo(player.getShortPlayer(), type));
    }

    /**
     * 修改归属用
     * 查询推广员
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    public SData_Result getClubPromotionChangeBelongPidInfo(CClub_FindPIDAdd findPIDAdd, long pid) {
        // 查询pid
        final long qPid = TypeUtils.StringTypeLong(findPIDAdd.getQuery());
        if (StringUtils.isEmpty(findPIDAdd.getQuery())) {
            return SData_Result.make(ErrorCode.NotAllow, "query is empty");
        }
        SData_Result result = checkExistPromotionSubordinateLevel(findPIDAdd.getClubId(), qPid, pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(clubMemberItem.getLevelPower())) {
            // 检查是否有权限
            if (clubMemberItem.getDoClubMember().getClubMemberBO().getModifyValue() != 1) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "getClubPromotionChangeBelongPidInfo CLUB_NOT_PROMOTION");
            }
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().findClubIdAllClubMember(findPIDAdd.getClubId(), Club_Player_Status.PLAYER_JIARU.value()).
                parallelStream().filter(k -> {
                    Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                    if (Objects.isNull(tempPlayer)) {
                        return false;
                    }
                    if (qPid == tempPlayer.getPid() || tempPlayer.getName().contains(findPIDAdd.getQuery())) {
                        return true;
                    }
                    return false;
                }).findFirst().orElse(null);

        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "CLUB_NOT_EXIST_MEMBER_INFO");
        }
//        if (clubMember.isLevelPromotion() || clubMember.isClubCreate()) {
        Player player = PlayerMgr.getInstance().getPlayer(clubMember.getClubMemberBO().getPlayerID());
        return SData_Result.make(ErrorCode.Success, new ClubPromotionPidInfo(player.getShortPlayer(), 0));
//        }
        // 不是推广员
//        return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");

    }

    /**
     * 检查是否推广员下属
     *
     * @param clubId 亲友圈Id
     * @param toPid  被操作玩家Id
     * @param pid    操作玩家Id
     * @return
     */
    public SData_Result checkExistPromotionSubordinateLevel(long clubId, long toPid, long pid) {
        // 1、我是创建者、我是推广员并且是你的上级
        ClubMember levelPromotion = getClubMember(clubId, pid);
        if (Objects.isNull(levelPromotion) || (levelPromotion.isNotLevelPromotion() && levelPromotion.isNotClubCreate())) {
            if (!levelPromotion.isPromotionManage()) {
                // 不是推广员
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
            }
            if (Config.isShare()) {
                levelPromotion = ShareClubMemberMgr.getInstance().getClubMember(levelPromotion.getClubMemberBO().getUpLevelId());
            } else {
                levelPromotion = this.getClubMemberMap().get(levelPromotion.getClubMemberBO().getUpLevelId());
            }
            if (Objects.isNull(levelPromotion)) {
                // 不是下属成员
                return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
            }
        }
        ClubMember toClubMember = getClubMember(clubId, toPid);
        if (Objects.isNull(toClubMember)) {
            // 不是下属成员
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }

        if (levelPromotion.isNotClubCreate()) {
            if (toClubMember.getClubMemberBO().getUpLevelId() <= 0L) {
                // 不是下属成员
                return SData_Result.make(ErrorCode.CLUB_NOT_SUBORDINATE, "CLUB_NOT_SUBORDINATE UpLevelId <= 0L");
            }
            boolean notExistFindOneE = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).notExistFindOneE(Restrictions.and(Restrictions.eq("uid", toClubMember.getId()), Restrictions.eq("puid", levelPromotion.getId())));
            if (notExistFindOneE) {
                UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(levelPromotion.getClubID());
                if (Objects.nonNull(club)) {
                    Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
                    if (Objects.nonNull(union)) {
                        unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
                    }
                }

                if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
                    return SData_Result.make(ErrorCode.CLUB_NOT_SUBORDINATE, "CLUB_NOT_SUBORDINATE");
                } else if (UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType) && toPid != pid) {
                    return SData_Result.make(ErrorCode.CLUB_NOT_SUBORDINATE, "CLUB_NOT_SUBORDINATE");
                }
            }
        }
        return SData_Result.make(ErrorCode.Success, new ClubMemberItem(toClubMember, levelPromotion, levelPromotion.isClubCreate() ? Club_define.Club_PROMOTION_LEVEL_POWER.CREATE : Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL));
    }


    /**
     * 获取推广员列表权限
     *
     * @param clubId 亲友圈Id
     * @param toPid  被操作者pid
     * @param pid    操作者pid
     * @return
     */
    private SData_Result getClubPromotionLevelListPowerItem(long clubId, long toPid, long pid) {
        ClubMember doClubMember = getClubMember(clubId, pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", clubId, pid);
        }
        if (doClubMember.isNotClubCreate() && doClubMember.isNotLevelPromotion()) {
            if (!doClubMember.isPromotionManage()) {
                // 不是创建者也不是推广员
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
            if (Config.isShare()) {
                doClubMember = ShareClubMemberMgr.getInstance().getClubMember(doClubMember.getClubMemberBO().getUpLevelId());
            } else {
                doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(doClubMember.getClubMemberBO().getUpLevelId());
            }
            if (Objects.isNull(doClubMember)) {
                // 成员不存在
                return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
        }
        // 是推广员或创建者
        long uid = doClubMember.getId();
        // 推广员id列表
        if (doClubMember.isClubCreate()) {
            // 亲友圈创建者操作：
            if (toPid == pid) {
                // 操作自己本身（查看自己包括自己下线(不包括推广员以及推广员下属)）
                return SData_Result.make(ErrorCode.Success, new ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER.GENERAL, Collections.emptyList(), 0L));
            } else if (toPid > 0) {
                // 操作指定玩家
                ClubMember toClubMember = getClubMember(clubId, toPid);
                if (Objects.isNull(toClubMember)) {
                    // 不是下属成员
                    return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER toClubMember isNull clubId:{},Pid:{}", clubId, toPid);
                }
                uid = toClubMember.getId();
                return SData_Result.make(ErrorCode.Success, new ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL, Collections.emptyList(), uid));
            } else {
                // 亲友圈创造者操作所有人
                return SData_Result.make(ErrorCode.Success, new ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER.CREATE, Collections.emptyList(), uid));
            }
        } else {
            // 亲友圈推广员操作：
            if (toPid > 0L && toPid != pid) {
                ClubMember toClubMember = getClubMember(clubId, toPid);
                if (Objects.isNull(toClubMember) || toClubMember.getClubMemberBO().getUpLevelId() <= 0L) {
                    // 不是下属成员
                    return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER isLevelPromotion toClubMember isNull clubId:{},Pid:{}", clubId, toPid);
                }
                boolean notExistFindOneE = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).notExistFindOneE(Restrictions.and(Restrictions.eq("uid", toClubMember.getId()), Restrictions.eq("puid", doClubMember.getId())));
                if (notExistFindOneE) {
                    return SData_Result.make(ErrorCode.CLUB_NOT_SUBORDINATE, "CLUB_NOT_SUBORDINATE");
                }
                uid = toClubMember.getId();
            } else if (doClubMember.isLevelPromotion()) {
                uid = doClubMember.getId();
            } else {
                return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER  clubId:{},toPid:{},doPid:{}", clubId, toPid, pid);
            }
            return SData_Result.make(ErrorCode.Success, new ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL, Collections.emptyList(), uid));
        }
    }

    /**
     * 获取推广员列表权限
     *
     * @param clubId 亲友圈Id
     * @param toPid  被操作者pid
     * @param pid    操作者pid
     * @return
     */
    private SData_Result getClubCompetitionRankedPower(long clubId, long toPid, long pid) {
        ClubMember doClubMember = getClubMember(clubId, pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "getClubCompetitionRankedPower doClubMember ClubId:{},Pid:{}", clubId, pid);
        }
        if (doClubMember.isNotClubCreate() && doClubMember.isNotLevelPromotion()) {
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "getClubCompetitionRankedPower CLUB_NOT_CREATE ClubId:{},Pid:{}", clubId, pid);

        }
        // 是推广员或创建者
        long uid = doClubMember.getId();
        return SData_Result.make(ErrorCode.Success, new ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL, Collections.emptyList(), uid));

    }
//    /**
//     * 获取推广员列表
//     *
//     * @param promotionList 推广员参数
//     * @param pid           玩家Pid
//     * @return
//     */
//    public SData_Result getClubPromotionLevelList(CClub_PromotionList promotionList, long pid) {
//        // 获取推广员列表权限
//        Long startTime=System.currentTimeMillis();
//        SData_Result result = this.getClubPromotionLevelListPowerItem(promotionList.getClubId(), promotionList.getPid(), pid);
//        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionList.getClubId());
//        if (!ErrorCode.Success.equals(result.getCode())) {
//            return result;
//        }
//        ClubMember playerClubMeber = ClubMgr.getInstance().getClubMemberMgr().getClubMember(promotionList.getClubId(), pid);//当前执行操作的玩家
//        ClubMember exeClubMeber = ClubMgr.getInstance().getClubMemberMgr().getClubMember(promotionList.getClubId(), promotionList.getPid());//点击下级玩家进来的推广员
//        // 推广员列表权限
//        ClubLevelPromotionPowerItem clubLevelPromotionPowerItem = (ClubLevelPromotionPowerItem) result.getData();
//        final long qPid = TypeUtils.StringTypeLong(promotionList.getQuery());
//        final String query = promotionList.getQuery();
//        List<ClubMember> getPromotionLevelList = (qPid <= 0L && query.isEmpty()) ? this.getPromotionLevelList(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), promotionList.getPageNum()) : getPromotionLevelList(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), qPid, promotionList.getPageNum(), query);
//        List<ClubPromotionLevelItem> promotionItemList = getPromotionLevelList.stream().map(k -> this.getClubPromotionLevelItem(k, clubLevelPromotionPowerItem.getLevelPower(), promotionList.getGetType())).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
//        ClubPromotionLevelItem selfInfo;
//        //只有在第一页的时候才需要加入
//        if (promotionList.getPid() > 0) {
//            selfInfo = this.getClubPromotionLevelItemSelf(exeClubMeber, promotionList.getGetType());
//        } else {
//            if (playerClubMeber.isNotClubCreate() && playerClubMeber.isNotLevelPromotion()) {
//                if (Config.isShare()) {
//                    playerClubMeber = ShareClubMemberMgr.getInstance().getClubMember(playerClubMeber.getClubMemberBO().getUpLevelId());
//                } else {
//                    playerClubMeber = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(playerClubMeber.getClubMemberBO().getUpLevelId());
//                }
//            }
//            selfInfo = this.getClubPromotionLevelItemSelf(playerClubMeber, promotionList.getGetType());
//        }
//        if (Objects.nonNull(selfInfo)) {
//            //推广员 只显示自己今天的收益的特殊标志
//            selfInfo.setSpecialFlag(true);
//            promotionItemList.add(selfInfo);
//        }
//        if (CollectionUtils.isEmpty(promotionItemList)) {
//            return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), Collections.emptyList(), promotionList.getGetType()));
//        }
//            CommLogD.info("ptime={}", System.currentTimeMillis()-startTime);
//        return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), promotionItemList, promotionList.getGetType()));
//    }

    /**
     * 获取推广员列表
     *
     * @param promotionList 推广员参数
     * @param pid           玩家Pid
     * @return
     */
    public SData_Result getClubPromotionLevelList(CClub_PromotionList promotionList, long pid) {
        UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionList.getClubId());
        if (Objects.nonNull(club)) {
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
            if (Objects.nonNull(union)) {
                unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
            }
        }
        boolean isZhongZhi = UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType);
        return isZhongZhi ? getClubPromotionLevelListZhongZhi(promotionList, pid) : getClubPromotionLevelListNormal(promotionList, pid);
    }

    /**
     * 获取推广员列表
     *
     * @param promotionList 推广员参数
     * @param pid           玩家Pid
     * @return
     */
    public SData_Result getTeamListZhongZhi(CClub_PromotionList promotionList, long pid) {
        return getClubTeamListZhongZhi(promotionList, pid);
    }

    /**
     * 获取中至比赛的时间状态
     *
     * @return
     */
    public SData_Result getClubCompetitionTimeZhongZhi() {
        List<ClubTimeZhongZhi> timeZhongZhis = new ArrayList<>();
        for (Club_define.CLUB_PROMOTION_TIME_TYPE flow : Club_define.CLUB_PROMOTION_TIME_TYPE.values()) {
            ClubTimeZhongZhi timeZhongZhi = new ClubTimeZhongZhi();
            timeZhongZhi.setType(flow.value());
            if (Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TODAY.equals(flow)) {
                timeZhongZhi.setStatus(1);
                timeZhongZhi.setBeginTime(CommTime.getCycleNowTime6YMD());
            } else if (Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_YESTERDAY.equals(flow)) {
                timeZhongZhi.setEndTime(CommTime.getCycleNowTime6YMD());
                timeZhongZhi.setBeginTime(Integer.parseInt(CommTime.getYesterDay6ByCount(1)));
            } else if (Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TWO.equals(flow)) {
                timeZhongZhi.setEndTime(Integer.parseInt(CommTime.getYesterDay6ByCount(1)));
                timeZhongZhi.setBeginTime(Integer.parseInt(CommTime.getYesterDay6ByCount(2)));
            } else if (Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_THREE.equals(flow)) {
                timeZhongZhi.setEndTime(Integer.parseInt(CommTime.getYesterDay6ByCount(2)));
                timeZhongZhi.setBeginTime(Integer.parseInt(CommTime.getYesterDay6ByCount(3)));
            } else if (Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_FOUR.equals(flow)) {
                timeZhongZhi.setEndTime(Integer.parseInt(CommTime.getYesterDay6ByCount(3)));
                timeZhongZhi.setBeginTime(Integer.parseInt(CommTime.getYesterDay6ByCount(4)));
            } else if (Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_FIVE.equals(flow)) {
                timeZhongZhi.setEndTime(Integer.parseInt(CommTime.getYesterDay6ByCount(4)));
                timeZhongZhi.setBeginTime(Integer.parseInt(CommTime.getYesterDay6ByCount(5)));
            } else if (Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_SIX.equals(flow)) {
                timeZhongZhi.setEndTime(Integer.parseInt(CommTime.getYesterDay6ByCount(5)));
                timeZhongZhi.setBeginTime(Integer.parseInt(CommTime.getYesterDay6ByCount(6)));
            }
            timeZhongZhis.add(timeZhongZhi);
        }
        return SData_Result.make(ErrorCode.Success, timeZhongZhis);

    }

    /**
     * 获取比赛排行界面
     *
     * @param req 比赛排行界面
     * @param pid 玩家Pid
     * @return
     */
    public SData_Result getClubCompetitionRankedZhongZhi(CClub_PromotionList req, long pid, int pageNum) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        // 获取推广员列表权限
        SData_Result result = this.getClubCompetitionRankedPower(req.getClubId(), req.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        CommLogD.error("1111111111111111111111:" + CommTime.nowMS());
        // 推广员列表权限
        ClubLevelPromotionPowerItem clubLevelPromotionPowerItem = (ClubLevelPromotionPowerItem) result.getData();
        final long qPid = TypeUtils.StringTypeLong(req.getQuery());
        final String query = req.getQuery();
        Long startTime = System.currentTimeMillis();
        List<String[]> getPromotionLevelList = (qPid <= 0L && query.isEmpty()) ? this.getCompetitionRankedShare(req.getClubId(), clubLevelPromotionPowerItem.getUid(), req.getPageNum()) : getPromotionLevelListShare(req.getClubId(), clubLevelPromotionPowerItem.getUid(), qPid, req.getPageNum(), query);
        Long endTime = System.currentTimeMillis();
        CommLogD.error("222222222222222222222222222:" + (startTime - endTime));
        List<ClubPromotionLevelItem> promotionItemList = getPromotionLevelList.stream().map(k -> this.getCompetitionRankedZhongZhi(k, clubLevelPromotionPowerItem.getLevelPower(), req.getType())).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
        CommLogD.error("33333333333333333333333333:" + CommTime.nowMS());
        if (CollectionUtils.isEmpty(promotionItemList)) {
            return SData_Result.make(ErrorCode.Success, new ClubCompetitionRanked(Collections.emptyList(), req.getType(), 0, club.getClubListBO().getTotalPointShowStatus()));
        }
        promotionItemList = promotionItemList.stream().skip(Page.getPageNum(pageNum))
                .limit(Page.PAGE_SIZE_100).collect(Collectors.toList());
        //重新赋值客户端所需要的序数id
        for (int i = 0; i < promotionItemList.size(); i++) {
            promotionItemList.get(i).setId((pageNum - 1) * Page.PAGE_SIZE_100 + 1 + i);
        }
        return SData_Result.make(ErrorCode.Success, new ClubCompetitionRanked(promotionItemList, req.getType(),
                promotionItemList.stream().mapToDouble(k -> k.getScorePoint()).sum()
                , club.getClubListBO().getTotalPointShowStatus()));

    }

    /**
     * 获取比赛排行界面
     *
     * @param req 比赛排行界面
     * @param pid 玩家Pid
     * @return
     */
    public SData_Result getAddCaptainZhongZhi(CClub_PromotionList req, long pid, int pageNum) {
        // 获取推广员列表权限
        SData_Result result = this.getClubCompetitionRankedPower(req.getClubId(), req.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(req.getClubId());
        // 查询pid
        final long qPid = TypeUtils.StringTypeLong(req.getQuery());
        List<ClubNormalPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShareAddCaptionZhongZhi(clubMembers, req.getClubId(),
                        Club_Player_Status.PLAYER_JIARU.value()).parallelStream()
                .map(k -> {
                    Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                    if (Objects.isNull(tempPlayer)) {
                        return null;
                    }
                    // 没有查询信息 或者
                    if (StringUtils.isEmpty(req.getQuery()) || (qPid == tempPlayer.getPid())) {
                        return new ClubNormalPlayerInfo(tempPlayer.getShortPlayer(), k.getStatus(), k.getClubMemberBO().getIsminister(), k.getTime());
                    }
                    if (tempPlayer.getName().contains(req.getQuery())) {
                        return new ClubNormalPlayerInfo(tempPlayer.getShortPlayer(), k.getStatus(), k.getClubMemberBO().getIsminister(), k.getTime());
                    }
                    return null;
                }).filter(k -> Objects.nonNull(k))
                .sorted(Comparator.comparing(ClubNormalPlayerInfo::getTime))
                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_100)).limit(Page.PAGE_SIZE_100).collect(Collectors.toList());
        //重新赋值客户端所需要的序数id
        for (int i = 0; i < clubPlayerInfos.size(); i++) {
            clubPlayerInfos.get(i).setId((pageNum - 1) * Page.PAGE_SIZE_100 + 1 + i);
        }
        return SData_Result.make(ErrorCode.Success, clubPlayerInfos);

    }

    /**
     * 获取比赛排行界面
     *
     * @param req 比赛排行界面
     * @param pid 玩家Pid
     * @return
     */
    public SData_Result getChangeAlivePointListZhongZhi(CClub_PromotionList req, long pid, int pageNum) {
        // 获取推广员列表权限
        SData_Result result = this.getClubCompetitionRankedPower(req.getClubId(), req.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(req.getClubId());
        // 查询pid
        final long qPid = TypeUtils.StringTypeLong(req.getQuery());
        List<ClubNormalPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShareChangeAlivePointZhongZhi(clubMembers, req.getClubId(),
                        Club_Player_Status.PLAYER_JIARU.value()).parallelStream()
                .map(k -> {
                    Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                    if (Objects.isNull(tempPlayer)) {
                        return null;
                    }
                    ClubMember upClubmember = ShareClubMemberMgr.getInstance().getClubMember(k.getClubMemberBO().getUpLevelId());
                    if (Objects.isNull(upClubmember)) {
                        return null;
                    }
                    Player tempUpPlayer = PlayerMgr.getInstance().getPlayer(upClubmember.getClubMemberBO().getPlayerID());
                    if (Objects.isNull(tempUpPlayer)) {
                        return null;
                    }

                    //有日期过滤
                    if (req.getType() > 0) {
                        //今天的战绩
                        ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubGeneral(k.getClubID(), k.getId(), k.getClubMemberBO().getUpLevelId(), k.getLevel());
                        boolean todayHasRecore = clubPromotionLevelItem.getSetCount() > 0;
                        //查看今天没有战绩
                        if (todayHasRecore) {
                            return null;
                        }
                        Criteria zeroClockSZhongZhi = null;
                        if (req.getType() == Club_define.Club_HAS_RECORD.Club_HAS_RECORD_2.value()) {
                            //最近三天无战绩
                            zeroClockSZhongZhi = Restrictions.between("date_time", CommTime.getYesterDay6ByCount(2), String.valueOf(CommTime.getNowTime6YMD()));
                        } else if (req.getType() == Club_define.Club_HAS_RECORD.Club_HAS_RECORD_3.value()) {
                            //最近七天无战绩
                            zeroClockSZhongZhi = Restrictions.between("date_time", CommTime.getYesterDay6ByCount(6), String.valueOf(CommTime.getNowTime6YMD()));
                        }
                        ClubPromotionLevelReportFormItem clubPromotionLevelItemCount = null;
                        if (Objects.nonNull(zeroClockSZhongZhi)) {
                            clubPromotionLevelItemCount = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", k.getClubID()), Restrictions.eq("memberId", k.getId())), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                        }
                        if (Objects.nonNull(clubPromotionLevelItemCount) && clubPromotionLevelItemCount.getSetCount() > 0) {
                            return null;
                        }
                    }
                    // 没有查询信息 或者
                    if (StringUtils.isEmpty(req.getQuery()) || (qPid == tempPlayer.getPid())) {
                        return new ClubNormalPlayerInfo(tempPlayer.getShortPlayer(), k.getStatus(), k.getClubMemberBO().getIsminister(), k.getTime(), tempUpPlayer.getName(), k.getClubMemberBO().getEliminatePoint(), k.getSportsPoint());
                    }
                    if (tempPlayer.getName().contains(req.getQuery())) {
                        return new ClubNormalPlayerInfo(tempPlayer.getShortPlayer(), k.getStatus(), k.getClubMemberBO().getIsminister(), k.getTime(), tempUpPlayer.getName(), k.getClubMemberBO().getEliminatePoint(), k.getSportsPoint());
                    }
                    return null;
                }).filter(k -> Objects.nonNull(k))
                .sorted(Comparator.comparing(ClubNormalPlayerInfo::getTime))
                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_100)).limit(Page.PAGE_SIZE_100).collect(Collectors.toList());
        //重新赋值客户端所需要的序数id
        for (int i = 0; i < clubPlayerInfos.size(); i++) {
            clubPlayerInfos.get(i).setId((pageNum - 1) * Page.PAGE_SIZE_100 + 1 + i);
        }
        return SData_Result.make(ErrorCode.Success, clubPlayerInfos);

    }

    /**
     * 获取比赛排行界面
     *
     * @param req 比赛排行界面
     * @param pid 玩家Pid
     * @return
     */
    public SData_Result getChangePromotionBelongZhongZhi(CClub_PromotionList req, long pid, int pageNum) {
        // 获取推广员列表权限
        SData_Result result = this.getClubCompetitionRankedPower(req.getClubId(), req.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(club.getOwnerPlayer().getPid(), req.getClubId());
        Map<Long, ClubMember> clubMembers = ShareClubMemberMgr.getInstance().getAllOneClubMember(req.getClubId());
        // 查询pid
        final long qPid = TypeUtils.StringTypeLong(req.getQuery());
        List<ClubNormalPlayerInfo> clubPlayerInfos = this.findClubIdAllClubMemberShareChangePromotionZhongZhi(clubMembers, req.getClubId(),
                        Club_Player_Status.PLAYER_JIARU.value(), req.getPid()).parallelStream()
                .map(k -> {
                    Player tempPlayer = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
                    if (Objects.isNull(tempPlayer)) {
                        return null;
                    }
                    ClubMember upClubmember = ShareClubMemberMgr.getInstance().getClubMember(k.getClubMemberBO().getUpLevelId());
                    if (Objects.isNull(upClubmember)) {
                        return null;
                    }
                    Player tempUpPlayer = PlayerMgr.getInstance().getPlayer(upClubmember.getClubMemberBO().getPlayerID());
                    if (Objects.isNull(tempUpPlayer)) {
                        return null;
                    }
                    int type = clubMember.getId() == upClubmember.getId() ? 0 : 1;
                    // 没有查询信息 或者
                    if (StringUtils.isEmpty(req.getQuery()) || (qPid == tempPlayer.getPid())) {
                        return new ClubNormalPlayerInfo(tempPlayer.getShortPlayer(), k.getStatus(), k.getClubMemberBO().getIsminister(), k.getTime(), tempUpPlayer.getName(), type);
                    }
                    if (tempPlayer.getName().contains(req.getQuery())) {
                        return new ClubNormalPlayerInfo(tempPlayer.getShortPlayer(), k.getStatus(), k.getClubMemberBO().getIsminister(), k.getTime(), tempUpPlayer.getName(), type);
                    }
                    return null;
                }).filter(k -> Objects.nonNull(k))
                .sorted(Comparator.comparing(ClubNormalPlayerInfo::getTime))
                .skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_100)).limit(Page.PAGE_SIZE_100).collect(Collectors.toList());
        //重新赋值客户端所需要的序数id
        for (int i = 0; i < clubPlayerInfos.size(); i++) {
            clubPlayerInfos.get(i).setId((pageNum - 1) * Page.PAGE_SIZE_100 + 1 + i);
        }
        return SData_Result.make(ErrorCode.Success, clubPlayerInfos);

    }

    /**
     * 普通获取
     *
     * @param promotionList
     * @param pid
     * @return
     */
    public SData_Result getClubPromotionLevelListNormal(CClub_PromotionList promotionList, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionList.getClubId());
        // 获取推广员列表权限
        SData_Result result = this.getClubPromotionLevelListPowerItem(promotionList.getClubId(), promotionList.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMember playerClubMeber = ClubMgr.getInstance().getClubMemberMgr().getClubMember(promotionList.getClubId(), pid);//当前执行操作的玩家
        ClubMember exeClubMeber = ClubMgr.getInstance().getClubMemberMgr().getClubMember(promotionList.getClubId(), promotionList.getPid());//点击下级玩家进来的推广员
        // 推广员列表权限
        ClubLevelPromotionPowerItem clubLevelPromotionPowerItem = (ClubLevelPromotionPowerItem) result.getData();
        final long qPid = TypeUtils.StringTypeLong(promotionList.getQuery());
        final String query = promotionList.getQuery();
        Long startTime = System.currentTimeMillis();
        List<String[]> getPromotionLevelList = (qPid <= 0L && query.isEmpty()) ? this.getPromotionLevelListShare(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), promotionList.getPageNum()) : getPromotionLevelListShare(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), qPid, promotionList.getPageNum(), query);
        Long endTime = System.currentTimeMillis() - startTime;
        if (endTime > 200) {
            CommLogD.error("getClubPromotionLevelListNormal time1={}", endTime);
        }
        Long startTime2 = System.currentTimeMillis();
        List<ClubPromotionLevelItem> promotionItemList = getPromotionLevelList.stream().map(k -> this.getClubPromotionLevelItemShare(k, clubLevelPromotionPowerItem.getLevelPower(), promotionList.getType(), false)).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
        Long endTime2 = System.currentTimeMillis() - startTime2;
        if (endTime2 > 200) {
            CommLogD.error("getClubPromotionLevelListNormal time2={} size={}", endTime2, getPromotionLevelList.size());
        }
        Long endTime3 = System.currentTimeMillis() - startTime;
        if (endTime3 > 200) {
            CommLogD.error("getClubPromotionLevelListNormal time3={}", endTime3);
        }
        ClubPromotionLevelItem selfInfo = null;
        //只有在第一页的时候才需要加入
        if (promotionList.getPid() > 0) {
            selfInfo = this.getClubPromotionLevelItemSelf(exeClubMeber, promotionList.getType());

        } else {
            if (playerClubMeber.isNotClubCreate() && playerClubMeber.isNotLevelPromotion()) {
                if (Config.isShare()) {
                    playerClubMeber = ShareClubMemberMgr.getInstance().getClubMember(playerClubMeber.getClubMemberBO().getUpLevelId());
                } else {
                    playerClubMeber = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(playerClubMeber.getClubMemberBO().getUpLevelId());
                }
            }
            selfInfo = this.getClubPromotionLevelItemSelf(playerClubMeber, promotionList.getType());

        }
        if (Objects.nonNull(selfInfo)) {
            //推广员 只显示自己今天的收益的特殊标志
            selfInfo.setSpecialFlag(true);
            promotionItemList.add(selfInfo);
        }
        if (CollectionUtils.isEmpty(promotionItemList)) {
            return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), Collections.emptyList(), promotionList.getType()));
        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), promotionItemList, promotionList.getType()));
    }

    /**
     * 中至获取
     *
     * @param promotionList
     * @param pid
     * @return
     */
    public SData_Result getClubPromotionLevelListZhongZhi(CClub_PromotionList promotionList, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionList.getClubId());
        // 获取推广员列表权限
        SData_Result result = this.getClubPromotionLevelListPowerItem(promotionList.getClubId(), promotionList.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMember playerClubMeber = ClubMgr.getInstance().getClubMemberMgr().getClubMember(promotionList.getClubId(), pid);//当前执行操作的玩家
        ClubMember exeClubMeber = ClubMgr.getInstance().getClubMemberMgr().getClubMember(promotionList.getClubId(), promotionList.getPid());//点击下级玩家进来的推广员
        // 推广员列表权限
        ClubLevelPromotionPowerItem clubLevelPromotionPowerItem = (ClubLevelPromotionPowerItem) result.getData();
        final long qPid = TypeUtils.StringTypeLong(promotionList.getQuery());
        final String query = promotionList.getQuery();
        List<String[]> getPromotionLevelList = (qPid <= 0L && query.isEmpty()) ? this.getPromotionLevelListShare(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), promotionList.getPageNum()) : getPromotionLevelListShare(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), qPid, promotionList.getPageNum(), query);
        List<ClubPromotionLevelItem> promotionItemList = getPromotionLevelList.stream().map(k -> this.getClubPromotionLevelItemShareZhongZhi(k, clubLevelPromotionPowerItem.getLevelPower(), promotionList.getType())).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
        ClubPromotionLevelItem selfInfo = null;
        //只有在第一页的时候才需要加入
        if (promotionList.getPid() > 0) {
            selfInfo = this.getClubPromotionLevelItemSelfZhongZhi(exeClubMeber, promotionList.getType());
        } else {
            if (playerClubMeber.isNotClubCreate() && playerClubMeber.isNotLevelPromotion()) {
                if (Config.isShare()) {
                    playerClubMeber = ShareClubMemberMgr.getInstance().getClubMember(playerClubMeber.getClubMemberBO().getUpLevelId());
                } else {
                    playerClubMeber = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(playerClubMeber.getClubMemberBO().getUpLevelId());
                }
            }
            selfInfo = this.getClubPromotionLevelItemSelfZhongZhi(playerClubMeber, promotionList.getType());

        }
        if (Objects.nonNull(selfInfo)) {
            //推广员 只显示自己今天的收益的特殊标志
            selfInfo.setSpecialFlag(true);
            promotionItemList.add(selfInfo);
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        Club unionCreateClub = ClubMgr.getInstance().getClubListMgr().findClub(union.getUnionBO().getClubId());
        if (CollectionUtils.isEmpty(promotionItemList)) {
            return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(unionCreateClub.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), unionCreateClub.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), Collections.emptyList(), promotionList.getType()));
        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(unionCreateClub.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), unionCreateClub.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), promotionItemList, promotionList.getType()));
    }

    /**
     * 中至获取
     *
     * @param promotionList
     * @param pid
     * @return
     */
    public SData_Result getClubTeamListZhongZhi(CClub_PromotionList promotionList, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionList.getClubId());
        // 获取推广员列表权限
        SData_Result result = this.getClubPromotionLevelListPowerItem(promotionList.getClubId(), promotionList.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 推广员列表权限
        ClubLevelPromotionPowerItem clubLevelPromotionPowerItem = (ClubLevelPromotionPowerItem) result.getData();
        final long qPid = Objects.isNull(promotionList.getQuery()) ? 0L : TypeUtils.StringTypeLong(promotionList.getQuery());
        final String query = Objects.isNull(promotionList.getQuery()) ? "" : promotionList.getQuery();
        final List<Integer> queryList = Objects.isNull(promotionList.getQuery()) ? new ArrayList<>() : promotionList.getLevelQuery();
        List<String[]> getPromotionLevelList = (qPid <= 0L && query.isEmpty() && CollectionUtils.isEmpty(promotionList.getLevelQuery())) ? this.geClubTeamListShare(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), promotionList.getPageNum(), queryList, promotionList.getPid()) : getPromotionLevelListShareZhongZhi(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), qPid, promotionList.getPageNum(), query, queryList, promotionList.getPid());
        List<ClubTeamListInfo> promotionItemList = getPromotionLevelList.stream().map(k -> this.getClubTeamListZhongZhi(k, clubLevelPromotionPowerItem.getLevelPower(), promotionList.getType())).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(promotionItemList)) {
            return SData_Result.make(ErrorCode.Success, new ClubTeamListZhongZhi(Collections.emptyList(), promotionList.getType()));
        }
        promotionItemList = promotionItemList.stream().skip(Page.getPageNum(promotionList.getPageNum()))
                .limit(Page.PAGE_SIZE_100).collect(Collectors.toList());
        //重新赋值客户端所需要的序数id
        for (int i = 0; i < promotionItemList.size(); i++) {
            promotionItemList.get(i).setId((promotionList.getPageNum() - 1) * Page.PAGE_SIZE_100 + 1 + i);
        }
        return SData_Result.make(ErrorCode.Success, new ClubTeamListZhongZhi(promotionItemList, promotionList.getType()));
    }

    /**
     * 获取七天之和的列表
     *
     * @param promotionList
     * @param pid
     * @return
     */
    public SData_Result getClubPromotionLevelListCount(CClub_PromotionList promotionList, long pid) {
        SData_Result result = this.getClubPromotionLevelListPowerItem(promotionList.getClubId(), promotionList.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }

        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionList.getClubId());
        List<ClubPromotionLevelItem> firstList = new ArrayList<>();
        List<ClubPromotionLevelItem> allList = new ArrayList<>();
        for (Club_define.CLUB_PROMOTION_TIME_TYPE con : Club_define.CLUB_PROMOTION_TIME_TYPE.values()) {
            promotionList.setType(con.value());
            SData_Result result1 = getClubPromotionLevelList(promotionList, pid);
            ClubPromotionLevelItemList itemList = (ClubPromotionLevelItemList) result1.getData();
            if (Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TODAY.equals(con)) {
                firstList = itemList.getClubPromotionLevelItemList();
            }
            allList.addAll(itemList.getClubPromotionLevelItemList());
        }
        //七天统计

        List<ClubPromotionLevelItem> resultList = allList.stream().collect(Collectors.toMap(ClubPromotionLevelItem::isSamePidAndSpecial, a -> a, (o1, o2) -> {
            o1.setSetCount(o1.getSetCount() + o2.getSetCount());
            o1.setWinner(o1.getWinner() + o2.getWinner());
            o1.setEntryFee(CommMath.addDouble(o1.getEntryFee(), o2.getEntryFee()));
            o1.setConsume(o1.getConsume() + o2.getConsume());
            o1.setSportsPointConsume(CommMath.addDouble(o1.getSportsPointConsume(), o2.getSportsPointConsume()));
            o1.setSportsPoint(CommMath.addDouble(o1.getSportsPoint(), o2.getSportsPoint()));
            o1.setSumSportsPoint(CommMath.addDouble(o1.getSumSportsPoint(), o2.getSumSportsPoint()));
            o1.setScorePoint(CommMath.addDouble(o1.getScorePoint(), o2.getScorePoint()));
            o1.setPromotionShareValue(CommMath.addDouble(o1.getPromotionShareValue(), o2.getPromotionShareValue()));
            o1.setActualEntryFee(CommMath.addDouble(o1.getActualEntryFee(), o2.getActualEntryFee()));
            o1.setTheoryEntryFee(CommMath.addDouble(o1.getTheoryEntryFee(), o2.getTheoryEntryFee()));
            o1.setTotalPoint(CommMath.addDouble(o1.getTotalPoint(), o2.getTotalPoint()));
            o1.setPlayerTotalPoint(CommMath.addDouble(o1.getPlayerTotalPoint(), o2.getPlayerTotalPoint()));
            if (Objects.nonNull(o1.getAlivePoint()) && Objects.nonNull(o2.getAlivePoint())) {
                o1.setAlivePoint(CommMath.addDouble(o1.getAlivePoint(), o2.getAlivePoint()));
            }
            o1.setZhongZhiTotalPoint(CommMath.addDouble(o1.getZhongZhiTotalPoint(), o2.getZhongZhiTotalPoint()));
            return o1;
        })).values().stream().collect(Collectors.toList());
//        for(ClubPromotionLevelItem con:firstList){
//            List<ClubPromotionLevelItem> allResult=allList.stream().filter(k->k.getPid()==con.getPid()&&(k.isSpecialFlag()==con.isSpecialFlag())).collect(Collectors.toList());
//            con.setSetCount(allResult.stream().mapToInt(k->k.getSetCount()).sum());
//            con.setWinner(allResult.stream().mapToInt(k->k.getWinner()).sum());
//            con.setEntryFee(allResult.stream().mapToDouble(k->k.getEntryFee()).sum());
//            con.setConsume(allResult.stream().mapToInt(k->k.getConsume()).sum());
//            con.setSportsPointConsume(allResult.stream().mapToDouble(k->k.getSportsPointConsume()).sum());
//            con.setSportsPoint(allResult.stream().mapToDouble(k->k.getSportsPoint()).sum());
//            con.setSumSportsPoint(allResult.stream().mapToDouble(k->k.getSumSportsPoint()).sum());
//            con.setScorePoint(allResult.stream().mapToDouble(k->k.getScorePoint()).sum());
//            con.setPromotionShareValue(allResult.stream().mapToDouble(k->k.getPromotionShareValue()).sum());
//            con.setActualEntryFee(allResult.stream().mapToDouble(k->k.getActualEntryFee()).sum());
//            con.setTheoryEntryFee(allResult.stream().mapToDouble(k->k.getTheoryEntryFee()).sum());
//            con.setTotalPoint(allResult.stream().mapToDouble(k->k.getTotalPoint()).sum());
//            con.setPlayerTotalPoint(allResult.stream().mapToDouble(k->k.getPlayerTotalPoint()).sum());
//            con.setAlivePoint(allResult.stream().mapToDouble(k->k.getAlivePoint()).sum());
//            con.setZhongZhiTotalPoint(allResult.stream().mapToDouble(k->k.getZhongZhiTotalPoint()).sum());
//        }
        if (CollectionUtils.isEmpty(resultList)) {
            return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), Collections.emptyList(), promotionList.getType()));
        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), resultList, promotionList.getType()));


    }

    /**
     * 获取推广员分成信息
     *
     * @param promotionList 推广员参数
     * @param pid           玩家Pid
     * @return
     */
    public SData_Result getPromotionListShareInfo(CClub_PromotionShareChange promotionList, long pid) {
        SData_Result result = clubPromotionLevelShareChangePower(promotionList.getClubId(), promotionList.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        // 分成类型
        int shareType = UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal();
        // 下级分成类型
        int lowerLevelShareType = UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal();
        // 上级分成类型
        int upLevelShareType = clubMemberItem.getDoClubMember().getClubMemberBO().getShareType();
        double maxValue = 0D;
        double maxFixedValue = 0D;
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", clubMemberItem.getToClubMember().getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        if (Objects.nonNull(queryUidOrPidItemList) && !queryUidOrPidItemList.isEmpty()) {
            for (QueryUidOrPuidItem queryUidOrPuidItem : queryUidOrPidItemList) {
                ClubMember promotionMember;
                if (Config.isShare()) {
                    promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem.getUid());
                } else {
                    promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem.getUid());
                }
                if (Objects.isNull(promotionMember)) {
                    continue;
                }
                lowerLevelShareType = lowerLevelShareType != UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal() ? promotionMember.getClubMemberBO().getShareType() : lowerLevelShareType;
                if (promotionMember.getClubMemberBO().getShareValue() > maxValue) {
                    maxValue = promotionMember.getClubMemberBO().getShareValue();
                }
                if (promotionMember.getClubMemberBO().getShareFixedValue() > maxFixedValue) {
                    maxFixedValue = promotionMember.getClubMemberBO().getShareFixedValue();
                }
            }
        }
        if (clubMemberItem.getDoClubMember().isClubCreate()) {
            // 不是固定值,重新设置值
            shareType = shareType != UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() ? clubMemberItem.getToClubMember().getClubMemberBO().getShareType() : shareType;
        } else {
            // 不是固定值,重新设置值
            shareType = UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() == upLevelShareType ? upLevelShareType : clubMemberItem.getToClubMember().getClubMemberBO().getShareType();
        }
        //推广员分成功能修改后 不限制最小值
        maxValue = 0D;
        return SData_Result.make(ErrorCode.Success, new ClubPromotionItem(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID(),
                clubMemberItem.getToClubMember().getClubMemberBO().getShareValue(), clubMemberItem.getToClubMember().getClubMemberBO().getShareFixedValue(), shareType, clubMemberItem.getDoClubMember().isClubCreate() ? 100 : clubMemberItem.getDoClubMember().getClubMemberBO().getShareValue(), maxValue, clubMemberItem.getDoClubMember().isClubCreate() ? 10000 : clubMemberItem.getDoClubMember().getClubMemberBO().getShareFixedValue(), maxFixedValue, clubMemberItem.getDoClubMember().isClubCreate(), lowerLevelShareType, clubMemberItem.getToClubMember().getId(), upLevelShareType));
    }

    /**
     * 获取推广员分成信息
     *
     * @param promotionList 推广员参数
     * @param
     * @return
     */
    public SData_Result getPromotionListShareInfoSelf(CClub_PromotionShareChange promotionList, Player player) {
        ClubMember doClubMember = getClubMember(promotionList.getClubId(), player.getPid());
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", promotionList.getClubId(), player.getPid());
        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionItem(player.getPid(), player.getName(),
                doClubMember.getClubMemberBO().getShareValue(), doClubMember.getClubMemberBO().getShareFixedValue(), doClubMember.getClubMemberBO().getShareType()));
    }

    /**
     * 修改推广员预警值
     *
     * @param sportsPointWarningChange 推广员参数
     * @param pid                      玩家Pid
     * @return
     */
    public SData_Result changeSportsPointWarning(CClub_SportsPointWarningChange sportsPointWarningChange, long pid) {
        SData_Result result = clubPromotionWarningPower(sportsPointWarningChange.getClubId(), sportsPointWarningChange.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        ClubMember exeClubMember = clubMemberItem.getToClubMember();
        Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        double preValue = exeClubMember.getClubMemberBO().getSportsPointWarning();
        if (sportsPointWarningChange.getWarnStatus() == UnionDefine.UNION_WARN_STATUS.CLOSE.ordinal()) {
            exeClubMember.getClubMemberBO().saveWarnStatus(0);
            UnionDynamicBO.insertSportsPointLog(exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubID(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SPORTS_WARNING_CLOSE.value(),
                    sportsPointWarningChange.getUnionId(), String.valueOf(preValue), String.valueOf(preValue));
        } else {
            exeClubMember.getClubMemberBO().saveWarnStatus(1);
            exeClubMember.getClubMemberBO().saveSportsPointWarning(sportsPointWarningChange.getValue());
            UnionDynamicBO.insertSportsPointLog(exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubID(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SPORTS_WARNING_CHANGE.value(),
                    sportsPointWarningChange.getUnionId(), String.valueOf(preValue), String.valueOf(sportsPointWarningChange.getValue()));
        }
        return SData_Result.make(ErrorCode.Success, new ClubSportsPointWarningItem(player.getPid(), player.getName(),
                clubMemberItem.getToClubMember().getClubMemberBO().getWarnStatus(), clubMemberItem.getToClubMember().getClubMemberBO().getSportsPointWarning()));

    }

    /**
     * 修改个人预警值
     *
     * @param sportsPointWarningChange 推广员参数
     * @param pid                      玩家Pid
     * @return
     */
    public SData_Result changePersonalSportsPointWarning(CClub_SportsPointWarningChange sportsPointWarningChange, long pid) {
        SData_Result result = clubPersonalWarningPower(sportsPointWarningChange.getClubId(), sportsPointWarningChange.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        ClubMember exeClubMember = clubMemberItem.getToClubMember();
        Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        double preValue = exeClubMember.getClubMemberBO().getPersonalSportsPointWarning();
        if (sportsPointWarningChange.getWarnStatus() == UnionDefine.UNION_WARN_STATUS.CLOSE.ordinal()) {
            exeClubMember.getClubMemberBO().savePersonalWarnStatus(0);
            UnionDynamicBO.insertPersonalSportsPointLog(exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubID(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PERSONAL_SPORTS_WARNING_CLOSE.value(),
                    sportsPointWarningChange.getUnionId(), String.valueOf(preValue), String.valueOf(preValue));
        } else {
            exeClubMember.getClubMemberBO().savePersonalWarnStatus(1);
            exeClubMember.getClubMemberBO().savePersonalSportsPointWarning(sportsPointWarningChange.getValue());
            UnionDynamicBO.insertPersonalSportsPointLog(exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubID(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_PERSONAL_SPORTS_WARNING_CHANGE.value(),
                    sportsPointWarningChange.getUnionId(), String.valueOf(preValue), String.valueOf(sportsPointWarningChange.getValue()));
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(sportsPointWarningChange.getClubId());
        UnionInfo unionInfo = club.getUnionInfo(pid);
        notify2AllMinisterAndPidByClub(sportsPointWarningChange.getClubId(), sportsPointWarningChange.getPid(), SClub_PersonalWarnInfoChange.make(sportsPointWarningChange.getClubId(),
                sportsPointWarningChange.getUnionId(), sportsPointWarningChange.getWarnStatus(), unionInfo.getOutSportsPoint(), sportsPointWarningChange.getValue()));
        return SData_Result.make(ErrorCode.Success, new ClubPersonalSportsPointWarningItem(player.getPid(), player.getName(),
                clubMemberItem.getToClubMember().getClubMemberBO().getPersonalWarnStatus(), clubMemberItem.getToClubMember().getClubMemberBO().getPersonalSportsPointWarning()));

    }

    /**
     * 修改个人淘汰分
     *
     * @param sportsPointWarningChange 推广员参数
     * @param pid                      玩家Pid
     * @return
     */
    public SData_Result changeEliminatePointChange(CClub_SportsPointWarningChange sportsPointWarningChange, long pid) {
        SData_Result result = clubPersonalZhongZhiWarningPower(sportsPointWarningChange.getClubId(), sportsPointWarningChange.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(sportsPointWarningChange.getClubId());
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        ClubMember exeClubMember = clubMemberItem.getToClubMember();
        Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        double preValue = exeClubMember.getClubMemberBO().getEliminatePoint();
        exeClubMember.getClubMemberBO().saveEliminatePoint(sportsPointWarningChange.getValue());
        UnionDynamicBO.insertPersonalSportsPointLog(exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubID(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_ELIMINATE_POINT_CHANGE.value(),
                club.getClubListBO().getUnionId(), String.valueOf(preValue), String.valueOf(sportsPointWarningChange.getValue()));
        UnionInfo unionInfo = club.getUnionInfo(pid);
        notify2AllMinisterAndPidByClub(sportsPointWarningChange.getClubId(), sportsPointWarningChange.getPid(), SClub_EliminatePointChange.make(sportsPointWarningChange.getClubId(),
                sportsPointWarningChange.getUnionId(), 0, unionInfo.getOutSportsPoint(), sportsPointWarningChange.getValue()));
        return SData_Result.make(ErrorCode.Success, new ClubPersonalSportsPointWarningItem(player.getPid(), player.getName(),
                0, clubMemberItem.getToClubMember().getClubMemberBO().getEliminatePoint()));

    }

    /**
     * 修改中至战队等级
     *
     * @param alivePointChange 推广员参数
     * @param pid              玩家Pid
     * @return
     */
    public SData_Result changeLevelZhongZhi(CUnion_LevelZhongZhi alivePointChange, long pid) {
        ClubMember doClubMember = getClubMember(alivePointChange.getClubId(), pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", alivePointChange.getClubId(), pid);
        }
        if (doClubMember.isNotClubCreate()) {
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE doClubMember ClubId:{},Pid:{}", alivePointChange.getClubId(), pid);
        }
        ClubMember toClubMember = getClubMember(alivePointChange.getClubId(), alivePointChange.getPid());
        toClubMember.getClubMemberBO().saveLevelZhongZhi(alivePointChange.getValue());
        return SData_Result.make(ErrorCode.Success);

    }

    /**
     * 修改个人生存积分
     *
     * @param alivePointChange 推广员参数
     * @param pid              玩家Pid
     * @return
     */
    public SData_Result changeAlivePointChange(CUnion_AlivePointChange alivePointChange, long pid) {
        SData_Result result = clubPersonalZhongZhiWarningPower(alivePointChange.getClubId(), alivePointChange.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        ClubMember exeClubMember = clubMemberItem.getToClubMember();
        Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        double preValue = exeClubMember.getClubMemberBO().getAlivePoint();
        if (alivePointChange.getAlivePointStatus() == UnionDefine.UNION_WARN_STATUS.CLOSE.ordinal()) {
            exeClubMember.getClubMemberBO().saveAlivePointStatus(0);
            UnionDynamicBO.insertSportsPointLogClub(exeClubMember.getClubMemberBO().getPlayerID(), alivePointChange.getClubId(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_CLOSE.value(),
                    alivePointChange.getUnionId(), String.valueOf(preValue), String.valueOf(alivePointChange.getValue()));
        } else {
            UnionDefine.UNION_EXEC_TYPE type = UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_CHANGE;
            if (exeClubMember.getClubMemberBO().getAlivePointStatus() == UnionDefine.UNION_WARN_STATUS.CLOSE.ordinal()) {
                type = UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_OPEN;
            }
            exeClubMember.getClubMemberBO().saveAlivePointStatus(1);
            exeClubMember.getClubMemberBO().saveAlivePoint(alivePointChange.getValue());
            UnionDynamicBO.insertSportsPointLogClub(exeClubMember.getClubMemberBO().getPlayerID(), alivePointChange.getClubId(), pid, CommTime.nowSecond(), type.value(),
                    alivePointChange.getUnionId(), String.valueOf(preValue), String.valueOf(alivePointChange.getValue()));
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(alivePointChange.getClubId());
        UnionInfo unionInfo = club.getUnionInfo(pid);
        notify2AllMinisterAndPidByClub(alivePointChange.getClubId(), alivePointChange.getOpPid(), SClub_AlivePointChange.make(alivePointChange.getClubId(),
                alivePointChange.getUnionId(), alivePointChange.getAlivePointStatus(), unionInfo.getOutSportsPoint(), alivePointChange.getValue()));
        return SData_Result.make(ErrorCode.Success, new ClubPersonalSportsPointWarningItem(player.getPid(), player.getName(),
                clubMemberItem.getToClubMember().getClubMemberBO().getPersonalWarnStatus(), clubMemberItem.getToClubMember().getClubMemberBO().getPersonalSportsPointWarning()));

    }

    /**
     * 批量修改个人生淘汰分
     *
     * @param eliminatePointChange 推广员参数
     * @param pid                  玩家Pid
     * @return
     */
    public SData_Result eliminatePointChangeMulti(CUnion_EliminatePointChange eliminatePointChange, long pid) {
        SData_Result result = SData_Result.make(ErrorCode.Success);
        ClubMember doClubMember = getClubMember(eliminatePointChange.getClubId(), pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            result = SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", eliminatePointChange.getClubId(), pid);
        }
        if (!doClubMember.isMinister()) {
            result = SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "CLUB_NOTMINISTER doClubMember ClubId:{},Pid:{}", eliminatePointChange.getClubId(), pid);
        }
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        for (Long con : eliminatePointChange.getPidList()) {
            ClubMember exeClubMember = getClubMember(eliminatePointChange.getClubId(), con);
            if (Objects.isNull(doClubMember)) {
                continue;
            }
            double preValue = exeClubMember.getClubMemberBO().getEliminatePoint();
            exeClubMember.getClubMemberBO().saveEliminatePoint(eliminatePointChange.getValue());
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(eliminatePointChange.getClubId());
            UnionDynamicBO.insertSportsPointLogClub(exeClubMember.getClubMemberBO().getPlayerID(), eliminatePointChange.getClubId(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_ELIMINATE_POINT_CHANGE.value(),
                    club.getClubListBO().getUnionId(), String.valueOf(preValue), String.valueOf(eliminatePointChange.getValue()));
        }

        return SData_Result.make(ErrorCode.Success);

    }

    /**
     * 获取玩家预警信息
     *
     * @param sportsPointWarningChange 参数
     * @param pid                      玩家Pid
     * @return
     */
    public SData_Result getSportsPointWaningInfo(CClub_SportsPointWarningChange sportsPointWarningChange, long pid) {
        SData_Result result = clubPromotionWarningPower(sportsPointWarningChange.getClubId(), sportsPointWarningChange.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        return SData_Result.make(ErrorCode.Success, new ClubSportsPointWarningItem(player.getPid(), player.getName(),
                clubMemberItem.getToClubMember().getClubMemberBO().getWarnStatus(), clubMemberItem.getToClubMember().getClubMemberBO().getSportsPointWarning()));
    }

    /**
     * 获取玩家生存积分
     *
     * @param sportsPointWarningChange 参数
     * @param pid                      玩家Pid
     * @return
     */
    public SData_Result getAlivePointWaningInfo(CClub_SportsPointWarningChange sportsPointWarningChange, long pid) {
        SData_Result result = clubPersonalZhongZhiWarningPower(sportsPointWarningChange.getClubId(), sportsPointWarningChange.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        return SData_Result.make(ErrorCode.Success, new ClubSportsPointWarningItem(player.getPid(), player.getName(),
                clubMemberItem.getToClubMember().getClubMemberBO().getAlivePointStatus(), clubMemberItem.getToClubMember().getClubMemberBO().getAlivePoint()));
    }

    /**
     * 获取玩家预警信息
     *
     * @param sportsPointWarningChange 参数
     * @param pid                      玩家Pid
     * @return
     */
    public SData_Result getEliminarePointInfo(CClub_SportsPointWarningChange sportsPointWarningChange, long pid) {
        SData_Result result = clubPersonalZhongZhiWarningPower(sportsPointWarningChange.getClubId(), sportsPointWarningChange.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        return SData_Result.make(ErrorCode.Success, new ClubSportsPointWarningItem(player.getPid(), player.getName(),
                0, clubMemberItem.getToClubMember().getClubMemberBO().getEliminatePoint()));
    }

    /**
     * 获取玩家个人预警信息
     *
     * @param sportsPointWarningChange 参数
     * @param pid                      玩家Pid
     * @return
     */
    public SData_Result getPersonalSportsPointWaningInfo(CClub_SportsPointWarningChange sportsPointWarningChange, long pid) {
        SData_Result result = clubPersonalWarningPower(sportsPointWarningChange.getClubId(), sportsPointWarningChange.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        return SData_Result.make(ErrorCode.Success, new ClubPersonalSportsPointWarningItem(player.getPid(), player.getName(),
                clubMemberItem.getToClubMember().getClubMemberBO().getPersonalWarnStatus(), clubMemberItem.getToClubMember().getClubMemberBO().getPersonalSportsPointWarning()));
    }

    /**
     * 预警值权限查询
     *
     * @param clubId 亲友圈Id
     * @param opPid  被操作者Pid
     * @param pid    操作者Pid
     * @return
     */
    private SData_Result clubPromotionWarningPower(long clubId, long opPid, long pid) {
        ClubMember doClubMember = getClubMember(clubId, pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", clubId, pid);
        }
        if (doClubMember.isNotClubCreate() && doClubMember.isNotLevelPromotion()) {
            if (!doClubMember.isPromotionManage()) {
                // 不是创建者也不是推广员
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
            //如果是推广员管理 重新计算权限
            if (Config.isShare()) {
                doClubMember = ShareClubMemberMgr.getInstance().getClubMember(doClubMember.getClubMemberBO().getUpLevelId());
            } else {
                doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(doClubMember.getClubMemberBO().getUpLevelId());
            }
            if (Objects.isNull(doClubMember)) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
        }

        ClubMember exeClubMember = getClubMember(clubId, opPid);
        if (Objects.isNull(exeClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER exeClubMember ClubId:{},Pid:{}", clubId, opPid);
        }
        if (exeClubMember.isNotLevelPromotion()) {
            // 不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
        }

        if (!doClubMember.isClubCreate() && exeClubMember.getClubMemberBO().getUpLevelId() != doClubMember.getClubMemberBO().getId()) {
            return SData_Result.make(ErrorCode.CLUB_UP_LEVEL_MEMBER_NOT_EXIST, "CLUB_NOTCLUBMEMBER exeClubMember not promotionList ClubId:{},Pid:{}", clubId, opPid);
        }
        return SData_Result.make(ErrorCode.Success, new ClubMemberItem(exeClubMember, doClubMember, Club_define.Club_PROMOTION_LEVEL_POWER.CREATE));
    }

    /**
     * 个人预警值权限查询
     *
     * @param clubId 亲友圈Id
     * @param opPid  被操作者Pid
     * @param pid    操作者Pid
     * @return
     */
    private SData_Result clubPersonalWarningPower(long clubId, long opPid, long pid) {
        ClubMember doClubMember = getClubMember(clubId, pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", clubId, pid);
        }
        if (doClubMember.isNotClubCreate() && doClubMember.isNotLevelPromotion()) {
            if (!doClubMember.isPromotionManage()) {
                // 不是创建者也不是推广员
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
            //如果是推广员管理 重新计算权限
            if (Config.isShare()) {
                doClubMember = ShareClubMemberMgr.getInstance().getClubMember(doClubMember.getClubMemberBO().getUpLevelId());
            } else {
                doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(doClubMember.getClubMemberBO().getUpLevelId());
            }
            if (Objects.isNull(doClubMember)) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
        }

        ClubMember exeClubMember = getClubMember(clubId, opPid);
        if (Objects.isNull(exeClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER exeClubMember ClubId:{},Pid:{}", clubId, opPid);
        }
        if (!doClubMember.isClubCreate() && exeClubMember.getClubMemberBO().getUpLevelId() != doClubMember.getClubMemberBO().getId()) {
            return SData_Result.make(ErrorCode.CLUB_UP_LEVEL_MEMBER_NOT_EXIST, "CLUB_NOTCLUBMEMBER exeClubMember not promotionList ClubId:{},Pid:{}", clubId, opPid);
        }
        return SData_Result.make(ErrorCode.Success, new ClubMemberItem(exeClubMember, doClubMember, Club_define.Club_PROMOTION_LEVEL_POWER.CREATE));
    }

    /**
     * 个人预警值权限查询
     *
     * @param clubId 亲友圈Id
     * @param opPid  被操作者Pid
     * @param pid    操作者Pid
     * @return
     */
    private SData_Result clubPersonalZhongZhiWarningPower(long clubId, long opPid, long pid) {
        ClubMember doClubMember = getClubMember(clubId, pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", clubId, pid);
        }
        if (doClubMember.isNotClubCreate() && doClubMember.isNotLevelPromotion()) {
            if (!doClubMember.isPromotionManage()) {
                // 不是创建者也不是推广员
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
            //如果是推广员管理 重新计算权限
            if (Config.isShare()) {
                doClubMember = ShareClubMemberMgr.getInstance().getClubMember(doClubMember.getClubMemberBO().getUpLevelId());
            } else {
                doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(doClubMember.getClubMemberBO().getUpLevelId());
            }
            if (Objects.isNull(doClubMember)) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
        }

        ClubMember exeClubMember = getClubMember(clubId, opPid);
        if (Objects.isNull(exeClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER exeClubMember ClubId:{},Pid:{}", clubId, opPid);
        }
//        if (!doClubMember.isClubCreate() ) {
//            return SData_Result.make(ErrorCode.CLUB_UP_LEVEL_MEMBER_NOT_EXIST, "CLUB_NOTCLUBMEMBER exeClubMember not promotionList ClubId:{},Pid:{}", clubId, opPid);
//        }
        return SData_Result.make(ErrorCode.Success, new ClubMemberItem(exeClubMember, doClubMember, Club_define.Club_PROMOTION_LEVEL_POWER.CREATE));
    }

    /**
     * 修改推广员百分比分成比例
     *
     * @param promotionList 推广员参数
     * @param pid           玩家Pid
     * @return
     */
    public SData_Result changePromotionListShare(CClub_PromotionShareChange promotionList, long pid) {
        SData_Result result = getPromotionListShareInfo(promotionList, pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubPromotionItem clubPromotionItem = (ClubPromotionItem) result.getData();
        //执行操作的玩家
        ClubMember doClubMember = getClubMember(promotionList.getClubId(), pid);
        //被修改的玩家
        ClubMember exeClubMember = getClubMember(promotionList.getClubId(), promotionList.getPid());
        ClubMember clubCreateMember = this.findCreate(promotionList.getClubId());
//        if (doClubMember.isNotClubCreate()) {
        if (clubCreateMember.getClubMemberBO().getShareType() != promotionList.getType()) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_SHARE_TYPE_DIFFERENT, "CLUB_MEMBER_PROMOTION_SHARE_TYPE_DIFFERENT");
        }

//        }
        // 赛事分成类型
        UnionDefine.UNION_SHARE_TYPE shareType = UnionDefine.UNION_SHARE_TYPE.valueOf(promotionList.getType());
        if (Objects.isNull(shareType)) {
            return SData_Result.make(ErrorCode.NotAllow, "changePromotionListShare null shareType");
        }
//        if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareType) && clubPromotionItem.getLowerLevelShareType() == UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal()) {
//            return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_LEVEL_SHARE_LOWER, "CLUB_MEMBER_PROMOTION_LEVEL_SHARE_LOWER");
//        }
        if (!clubPromotionItem.isUpLevelCreate() && clubPromotionItem.getUpLevelShareType() == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() && !UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareType)) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_LEVEL_SHARE_UP, "CLUB_MEMBER_PROMOTION_LEVEL_SHARE_UP");
        }

        double value = CommMath.FormatDouble(promotionList.getValue());
        if (UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() == shareType.ordinal()) {
            if (clubPromotionItem.getShareType() == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() && (value > clubPromotionItem.getDoShareFixedValue() || value < clubPromotionItem.getMinShareFixedValue())) {
                return SData_Result.make(ErrorCode.NotAllow, "changePromotionListShare DoShareFixedValue");
            }
            // 批量修改所有下级的分成类型（改为 固定值）
            batchUpdateClubMemberShareTypeFixed(clubPromotionItem.getLowerLevelShareType(), clubPromotionItem.getUpLevelId(), promotionList);
        } else if (UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal() == shareType.ordinal()) {
            if (value > clubPromotionItem.getDoShareValue()) {
                return SData_Result.make(ErrorCode.NotAllow, "changePromotionListShare DoShareValue");
            }
            // 批量修改所有下级的分成类型（改为判断百分比是否更小）
            batchUpdateClubMemberShareType(clubPromotionItem.getLowerLevelShareType(), clubPromotionItem.getUpLevelId(), promotionList);
        } else {
            // 批量修改所有下级的区间分成类型
            batchUpdateClubMemberSectionType(exeClubMember, clubPromotionItem.getLowerLevelShareType(), clubPromotionItem.getUpLevelId(), promotionList);
            exeClubMember.getClubMemberBO().saveShareValue(value, promotionList.getType());
            UnionDynamicBO.insertRoomSportsPoint(exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubID(), doClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubID(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SHARE_SECTION.value(), promotionList.getUnionId(), "", "", "");
            return SData_Result.make(ErrorCode.Success, new ClubPromotionItem(exeClubMember.getClubMemberBO().getPlayerID(),
                    exeClubMember.getClubMemberBO().getShareValue(), exeClubMember.getClubMemberBO().getShareFixedValue(), exeClubMember.getClubMemberBO().getShareType(), 0D));
        }
        Integer oldType = Integer.valueOf(exeClubMember.getClubMemberBO().getShareType());
        Double oldValue = exeClubMember.getClubMemberBO().getShareValue();
        Double oldFixedValue = exeClubMember.getClubMemberBO().getShareFixedValue();
        exeClubMember.getClubMemberBO().saveShareValue(value, promotionList.getType());
        UnionDynamicBO.insertRoomSportsPoint(exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubID(), doClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubID(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(), promotionList.getUnionId(), promotionList.getType() == 0 ? String.valueOf((int) value + "%") : String.valueOf(value), oldType == 0 ? String.valueOf(oldValue.intValue() + "%") : String.valueOf(oldFixedValue), "");
        return SData_Result.make(ErrorCode.Success, new ClubPromotionItem(exeClubMember.getClubMemberBO().getPlayerID(),
                exeClubMember.getClubMemberBO().getShareValue(), exeClubMember.getClubMemberBO().getShareFixedValue(), exeClubMember.getClubMemberBO().getShareType(), 0D));
    }

    /***
     * 批量修改所有下级的分成类型（改为 固定值）
     */
    private void batchUpdateClubMemberShareTypeFixed(int lowerLevelShareType, long upLevelId, CClub_PromotionShareChange promotionList) {
        if (lowerLevelShareType == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()) {
            // 所有下级都是固定值了
            return;
        }
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", upLevelId), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            for (QueryUidOrPuidItem queryUidOrPuidItem : queryUidOrPidItemList) {
                ClubMember promotionMember;
                if (Config.isShare()) {
                    promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem.getUid());
                } else {
                    promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem.getUid());
                }
                if (Objects.isNull(promotionMember)) {
                    continue;
                }
                if (promotionMember.getClubMemberBO().getShareType() != UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()) {
                    double oldFixedValue = Double.valueOf(promotionMember.getClubMemberBO().getShareFixedValue());
                    Double oldValue = Double.valueOf(promotionMember.getClubMemberBO().getShareValue());
                    int oldType = promotionMember.getClubMemberBO().getShareType();
                    promotionMember.getClubMemberBO().saveShareValue(0D, UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal());
                    UnionDynamicBO.insertRoomSportsPoint(promotionMember.getClubMemberBO().getPlayerID(), promotionMember.getClubID(), 0, promotionMember.getClubID(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(), promotionList.getUnionId(), promotionList.getType() == 0 ? String.valueOf((int) 0 + "%") : String.valueOf(0), oldType == 0 ? String.valueOf(oldValue.intValue() + "%") : String.valueOf(oldFixedValue), "");

                }
            }
        }
    }

    /***
     * 批量修改所有下级的分成类型（改为 比例）
     */
    private void batchUpdateClubMemberShareType(int lowerLevelShareType, long upLevelId, CClub_PromotionShareChange promotionList) {

        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", upLevelId), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            for (QueryUidOrPuidItem queryUidOrPuidItem : queryUidOrPidItemList) {
                ClubMember promotionMember;
                if (Config.isShare()) {
                    promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem.getUid());
                } else {
                    promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem.getUid());
                }
                if (Objects.isNull(promotionMember)) {
                    continue;
                }
                Double oldValue = promotionMember.getClubMemberBO().getShareValue();
                if (oldValue > promotionList.getValue() || UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() == promotionMember.getClubMemberBO().getShareType()) {
                    int oldType = Integer.valueOf(promotionMember.getClubMemberBO().getShareType());
                    double oldFixedValue = Double.valueOf(promotionMember.getClubMemberBO().getShareFixedValue());
                    Double oldShareValue = Double.valueOf(promotionMember.getClubMemberBO().getShareValue());
                    if (oldValue > promotionList.getValue()) {
                        oldValue = promotionList.getValue();
                    }
                    promotionMember.getClubMemberBO().saveShareValue(oldValue, UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal());
                    UnionDynamicBO.insertRoomSportsPoint(promotionMember.getClubMemberBO().getPlayerID(), promotionMember.getClubID(), 0, promotionMember.getClubID(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(), promotionList.getUnionId(), promotionList.getType() == 0 ? String.valueOf(oldValue.intValue() + "%") : String.valueOf(oldValue), oldType == 0 ? String.valueOf(oldShareValue.intValue() + "%") : String.valueOf(oldFixedValue), "");
                }
            }
        }
    }

    /***
     * 修改自己的区间比例  并且生成默认数据
     */
    private void batchUpdateClubMemberSectionType(ClubMember exeClubMember, int lowerLevelShareType, long upLevelId, CClub_PromotionShareChange promotionList) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionList.getClubId());
        if (Objects.isNull(club)) {
            return;
        }
        List<UnionShareSectionItem> unionShareSectionItems = ((UnionShareSectionBOService) ContainerMgr.get().getComponent(UnionShareSectionBOService.class)).findAllE(Restrictions.eq("unionId", club.getClubListBO().getUnionId()),
                UnionShareSectionItem.class, UnionShareSectionItem.getItemsName());
        if (CollectionUtils.isEmpty(unionShareSectionItems)) {
            return;
        }
        List<PromotionShareSectionItem> promotionShareSectionItems = ((PromotionShareSectionBOService) ContainerMgr.get().getComponent(PromotionShareSectionBOService.class)).findAllE(Restrictions.and(Restrictions.eq("pid", exeClubMember.getClubMemberBO().getPlayerID()),
                Restrictions.eq("clubId", promotionList.getClubId())), PromotionShareSectionItem.class, PromotionShareSectionItem.getItemsName());
        if (CollectionUtils.isEmpty(promotionShareSectionItems)) {
            exeClubMember.initPromotionSection(club.getClubListBO().getUnionId(), exeClubMember.isClubCreate());
        }
    }

    /**
     * 获取推广员列表 模糊搜索 包括全部的 单独出来 以免影响到之前的方法
     *
     * @param promotionList 推广员参数
     * @param pid           玩家Pid
     * @return
     */
    public SData_Result getClubPromotionLevelIncludeAll(CClub_PromotionList promotionList, long pid) {

        ClubMember doClubMember = getClubMember(promotionList.getClubId(), pid);

        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionList.getClubId());
        UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
        if (Objects.nonNull(club)) {
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
            if (Objects.nonNull(union)) {
                unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
            }
        }
        boolean isZhongZhi = UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType);
        final long qPid = TypeUtils.StringTypeLong(promotionList.getQuery());
        final String query = promotionList.getQuery();
        if (doClubMember.isClubCreate()) {
            List<ClubMember> getPromotionLevelList;
            if (Config.isShare()) {
                getPromotionLevelList = ShareClubMemberMgr.getInstance().getAllOneClubMember(promotionList.getClubId()).values().stream().filter(k -> k.getClubID() == promotionList.getClubId() &&
                        (qPid == k.getClubMemberBO().getPlayerID() || (PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()) != null && PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()).getName().contains(query)))
                        && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).collect(Collectors.toList());
            } else {
                getPromotionLevelList = this.getClubMemberMap().values().stream().filter(k -> k.getClubID() == promotionList.getClubId() &&
                        (qPid == k.getClubMemberBO().getPlayerID() || (PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()) != null && PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID()).getName().contains(query)))
                        && k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).collect(Collectors.toList());
            }
            List<ClubPromotionLevelItem> promotionItemList = getPromotionLevelList.stream().map(k -> this.getClubPromotionLevelItem(k, Club_define.Club_PROMOTION_LEVEL_POWER.CREATE, promotionList.getType(), isZhongZhi)).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(promotionItemList)) {
                return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), Collections.emptyList(), promotionList.getType()));
            }
            return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), promotionItemList, promotionList.getType()));
        } else if (doClubMember.getClubMemberBO().getUpLevelId() > 0L || doClubMember.isMinister()) {
            // 获取推广员列表权限
            SData_Result result = this.clubLevelPromotionPowerItemIncludeAll(promotionList.getClubId(), pid, pid);
            if (!ErrorCode.Success.equals(result.getCode())) {
                return result;
            }

            // 推广员列表权限
            ClubLevelPromotionPowerItem clubLevelPromotionPowerItem = (ClubLevelPromotionPowerItem) result.getData();
            List<ClubMember> getPromotionLevelList = (qPid <= 0L && query.isEmpty()) ? this.getPromotionLevelList(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), promotionList.getPageNum()) : getPromotionLevelListIncludeAll(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), qPid, promotionList.getPageNum(), query, clubLevelPromotionPowerItem.getUidList());
            List<ClubPromotionLevelItem> promotionItemList = getPromotionLevelList.stream().map(k -> this.getClubPromotionLevelItem(k, clubLevelPromotionPowerItem.getLevelPower(), promotionList.getType(), isZhongZhi)).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(promotionItemList)) {
                return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), Collections.emptyList(), promotionList.getType()));
            }
            return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), promotionItemList, promotionList.getType()));

        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(club.getClubListBO().getPromotionShowClubConfigJson().getShowConfig(), club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), Collections.emptyList(), promotionList.getType()));
    }

    /**
     * 获取自己当天活跃度信息
     */

    public ClubPromotionLevelItem getClubPromotionLevelItemSelf(ClubMember k, int type) {
        Player player = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
        Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", type);
        if (type == 0) {
            ClubPromotionLevelItem clubPromotionLevelItem = null;
            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", k.getClubID()), Restrictions.eq("memberId", k.getId())), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsName());
            if (Objects.nonNull(clubPromotionLevelItem)) {
                return new ClubPromotionLevelItem(
                        player.getPid(),
                        player.getName(),
                        player.getHeadImageUrl(),
                        1,
                        clubPromotionLevelItem.getSetCount(),
                        clubPromotionLevelItem.getWinner(),
                        clubPromotionLevelItem.getEntryFee(),
                        Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                        clubPromotionLevelItem.getSportsPointConsume(),
                        k.getSportsPoint(),
                        k.getSportsPoint(),
                        k.getClubMemberBO().getLevel(),
                        0,
                        k.getClubMemberBO().getShareType(),
                        k.getClubMemberBO().getShareValue(),
                        k.getClubMemberBO().getShareFixedValue(),
                        clubPromotionLevelItem.getPromotionShareValue(),
                        clubPromotionLevelItem.getActualEntryFee(),
                        clubPromotionLevelItem.getConsume(),
                        k.getSportsPointWarningPersonal(),
                        k.getSportsPointWarning(),
                        this.getExamineStatus(k, zeroClockS, type),
                        k.getAlivePointZhongZhi(),
                        k.getClubMemberBO().getEliminatePoint());
            }
            return null;
        } else {
            ClubPromotionLevelReportFormItem clubPromotionLevelItem = null;
            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", k.getClubID()), Restrictions.eq("memberId", k.getId())), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
            if (Objects.nonNull(clubPromotionLevelItem)) {
                return new ClubPromotionLevelItem(
                        player.getPid(),
                        player.getName(),
                        player.getHeadImageUrl(),
                        1,
                        clubPromotionLevelItem.getSetCount(),
                        clubPromotionLevelItem.getWinner(),
                        clubPromotionLevelItem.getEntryFee(),
                        clubPromotionLevelItem.getConsume(),
                        clubPromotionLevelItem.getSportsPointConsume(),
                        k.getSportsPoint(),
                        k.getSportsPoint(),
                        k.getClubMemberBO().getLevel(),
                        0,
                        k.getClubMemberBO().getShareType(),
                        k.getClubMemberBO().getShareValue(),
                        k.getClubMemberBO().getShareFixedValue(),
                        clubPromotionLevelItem.getPromotionShareValue(),
                        clubPromotionLevelItem.getActualEntryFee(),
                        clubPromotionLevelItem.getConsume(),
                        k.getSportsPointWarningPersonal(),
                        k.getSportsPointWarning(),
                        this.getExamineStatus(k, zeroClockS, type),
                        k.getAlivePointZhongZhi(),
                        k.getClubMemberBO().getEliminatePoint());
            }
            return null;
        }
    }

    /**
     * 获取自己当天活跃度信息
     */

    public ClubPromotionLevelItem getClubPromotionLevelItemSelfZhongZhi(ClubMember k, int type) {
        Player player = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
        Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", type);
        Criteria zeroClockSZhongZhi = TimeConditionUtils.CLUBDayZeroClockSZhongZhi("date_time", type);
        if (type == 0) {
            ClubPromotionLevelItem clubPromotionLevelItem = null;
            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", k.getClubID()), Restrictions.eq("memberId", k.getId())), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsName());
            if (Objects.nonNull(clubPromotionLevelItem)) {
                return new ClubPromotionLevelItem(
                        player.getPid(),
                        player.getName(),
                        player.getHeadImageUrl(),
                        1,
                        clubPromotionLevelItem.getSetCount(),
                        clubPromotionLevelItem.getWinner(),
                        clubPromotionLevelItem.getEntryFee(),
                        Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                        clubPromotionLevelItem.getSportsPointConsume(),
                        k.getSportsPoint(),
                        k.getSportsPoint(),
                        k.getClubMemberBO().getLevel(),
                        0,
                        k.getClubMemberBO().getShareType(),
                        k.getClubMemberBO().getShareValue(),
                        k.getClubMemberBO().getShareFixedValue(),
                        clubPromotionLevelItem.getPromotionShareValue(),
                        clubPromotionLevelItem.getActualEntryFee(),
                        clubPromotionLevelItem.getConsume(),
                        k.getSportsPointWarningPersonal(),
                        k.getSportsPointWarning(),
                        this.getExamineStatus(k, zeroClockS, type),
                        k.getAlivePointZhongZhi(),
                        k.getClubMemberBO().getEliminatePoint());
            }
            return null;
        } else {
            ClubPromotionLevelReportFormItem clubPromotionLevelItem = null;
            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", k.getClubID()), Restrictions.eq("memberId", k.getId())), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
            if (Objects.nonNull(clubPromotionLevelItem)) {
                return new ClubPromotionLevelItem(
                        player.getPid(),
                        player.getName(),
                        player.getHeadImageUrl(),
                        1,
                        clubPromotionLevelItem.getSetCount(),
                        clubPromotionLevelItem.getWinner(),
                        clubPromotionLevelItem.getEntryFee(),
                        clubPromotionLevelItem.getConsume(),
                        clubPromotionLevelItem.getSportsPointConsume(),
                        k.getSportsPoint(),
                        k.getSportsPoint(),
                        k.getClubMemberBO().getLevel(),
                        0,
                        k.getClubMemberBO().getShareType(),
                        k.getClubMemberBO().getShareValue(),
                        k.getClubMemberBO().getShareFixedValue(),
                        clubPromotionLevelItem.getPromotionShareValue(),
                        clubPromotionLevelItem.getActualEntryFee(),
                        clubPromotionLevelItem.getConsume(),
                        k.getSportsPointWarningPersonal(),
                        k.getSportsPointWarning(),
                        this.getExamineStatus(k, zeroClockS, type),
                        k.getAlivePointZhongZhi(),
                        k.getClubMemberBO().getEliminatePoint());
            }
            return null;
        }
    }

    /**
     * 获取推广员审核状态
     *
     * @return
     */
    public int getExamineStatus(ClubMember clubMember, Criteria zeroClockS, int type) {
        if (type == 0) {
            return Club_define.Club_Examine.Not.ordinal();
        }
        ClubExamineItem examineItem = ContainerMgr.get().getComponent(ExamineFlogService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("toClubMemberId", clubMember.getId())), ClubExamineItem.class, ClubExamineItem.getItemsName());
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubMember.getClubID());
        if (Objects.isNull(club)) {
            return Club_define.Club_Examine.Not.ordinal();
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union) || UnionDefine.UNION_WARN_EXAMINE.CLOSE.ordinal() == union.getUnionBO().getExamineStatus()) {
            return Club_define.Club_Examine.Not.ordinal();
        }
        if (UnionDefine.UNION_WARN_EXAMINE.AUTO.ordinal() == union.getUnionBO().getExamineStatus()) {
            if (Objects.nonNull(examineItem)) {
                return Club_define.Club_Examine.Examine_Already.ordinal();
            }
            return Club_define.Club_Examine.Not.ordinal();
        }
        int typeStatus = clubMember.isLevelPromotion() ? Club_define.Club_Examine.Examine_Not.ordinal() : Club_define.Club_Examine.Not.ordinal();
        if (Objects.nonNull(examineItem)) {
            return Club_define.Club_Examine.Examine_Already.ordinal();
        }
        return typeStatus;
    }

    /**
     * 获取推广员审核状态
     *
     * @return
     */
    public int getExamineStatusShare(String[] clubMember, Criteria zeroClockS, int type) {
        if (type == 0) {
            return Club_define.Club_Examine.Not.ordinal();
        }
        ClubExamineItem examineItem = ContainerMgr.get().getComponent(ExamineFlogService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("toClubMemberId", ClubMemberUtils.getArrayValueLong(clubMember, "id"))), ClubExamineItem.class, ClubExamineItem.getItemsName());
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(ClubMemberUtils.getArrayValueLong(clubMember, "clubID"));
        if (Objects.isNull(club)) {
            return Club_define.Club_Examine.Not.ordinal();
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union) || UnionDefine.UNION_WARN_EXAMINE.CLOSE.ordinal() == union.getUnionBO().getExamineStatus()) {
            return Club_define.Club_Examine.Not.ordinal();
        }
        if (UnionDefine.UNION_WARN_EXAMINE.AUTO.ordinal() == union.getUnionBO().getExamineStatus()) {
            if (Objects.nonNull(examineItem)) {
                return Club_define.Club_Examine.Examine_Already.ordinal();
            }
            return Club_define.Club_Examine.Not.ordinal();
        }
        int typeStatus = ClubMemberUtils.getArrayValueInteger(clubMember, "level") > 0 ? Club_define.Club_Examine.Examine_Not.ordinal() : Club_define.Club_Examine.Not.ordinal();
        if (Objects.nonNull(examineItem)) {
            return Club_define.Club_Examine.Examine_Already.ordinal();
        }
        return typeStatus;
    }

    /**
     * 推广员项
     *
     * @param k 亲友圈成员
     * @return
     */
    public ClubPromotionLevelItem getClubPromotionLevelItemShare(String[] k, Club_define.Club_PROMOTION_LEVEL_POWER levelPower, int type, boolean isZhongZhi) {
        // 获取用户信息
        // 时间
        Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", type);
        Player player = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));

        if (Objects.nonNull(player)) {
            if (type == 0) {
                ClubPromotionLevelItem clubPromotionLevelItem = null;
                if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister")) && Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利并且是创建者
                    clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubCreate(ClubMemberUtils.getArrayValueLong(k, "clubID"));
                    if (Objects.nonNull(clubPromotionLevelItem)) {
                        return new ClubPromotionLevelItem(
                                player.getPid(),
                                player.getName(),
                                player.getHeadImageUrl(),
                                clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")),
                                clubPromotionLevelItem.getSetCount(),
                                clubPromotionLevelItem.getWinner(),
                                clubPromotionLevelItem.getEntryFee(),
                                Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                clubPromotionLevelItem.getSportsPointConsume(),
                                ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                sumTotalSportsPoint(ClubMemberUtils.getArrayValueLong(k, "clubID")),
                                ClubMemberUtils.getArrayValueInteger(k, "level"),
                                2,
                                ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                clubPromotionLevelItem.getPromotionShareValue(),
                                clubPromotionLevelItem.getActualEntryFee(),
                                clubPromotionLevelItem.getConsume(),
                                ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                this.getExamineStatusShare(k, zeroClockS, type),
                                ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                    }
                    return new ClubPromotionLevelItem(player.getPid(), 2, player.getName(), player.getHeadImageUrl(), clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")), ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), sumSportsPoint(ClubMemberUtils.getArrayValueLong(k, "clubID")), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")), ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")), ClubMemberUtils.getArrayValueInteger(k, "shareType"), ClubMemberUtils.getArrayValueDouble(k, "shareValue"), ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"), this.getExamineStatusShare(k, zeroClockS, type), ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(levelPower) || Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利或者推广员权利
                    List<Long> uidList = Lists.newArrayList();
                    uidList.add(ClubMemberUtils.getArrayValueLong(k, "id"));
                    List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", ClubMemberUtils.getArrayValueLong(k, "id")), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
                    if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                        // 查询我的所有下线（包括我）：
                        uidList.addAll(queryUidOrPuidItemList.stream().map(v -> v.getUid()).collect(Collectors.toList()));
                    }
                    int useLessNum = 0;
                    for (Long con : uidList) {
                        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                        if (Objects.isNull(clubMember)) {
                            useLessNum++;
                            continue;
                        }
                        if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                            useLessNum++;
                        }
                    }
                    if (CollectionUtils.isNotEmpty(uidList)) {
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubPromotionLevel(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), uidList);
                        ClubMember upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(ClubMemberUtils.getArrayValueLong(k, "upLevelId"));
                        if (isZhongZhi && ClubMemberUtils.getArrayValueInteger(k, "level") <= 0 && Objects.nonNull(upLevelMember)) {
                            ClubPromotionLevelItem clubPromotionLevelItemZhongZhi = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubPromotionLevelPlayGameId(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "playerID"), upLevelMember.getClubMemberBO().getPlayerID(), type);
                            //普通成员
                            if (Objects.nonNull(clubPromotionLevelItem) && Objects.nonNull(clubPromotionLevelItemZhongZhi)) {
                                clubPromotionLevelItem.setPromotionShareValue(clubPromotionLevelItemZhongZhi.getPromotionShareValue());
                            }
                        }
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    uidList.size() - useLessNum,
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    sumTotalSportsPoint(uidList),
                                    ClubMemberUtils.getArrayValueInteger(k, "level"),
                                    0,
                                    ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                    ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                    this.getExamineStatusShare(k, zeroClockS, type),
                                    ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                    ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                        }
                    }
                    return new ClubPromotionLevelItem(player.getPid(), player.getName(), player.getHeadImageUrl(), uidList.size(), ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), sumSportsPoint(uidList), ClubMemberUtils.getArrayValueInteger(k, "level"),
                            ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")), ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")), ClubMemberUtils.getArrayValueInteger(k, "shareType"), ClubMemberUtils.getArrayValueDouble(k, "shareValue"), ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"), this.getExamineStatusShare(k, zeroClockS, type), ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.GENERAL.equals(levelPower)) {
                    // 普通成员权利
                    if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister"))) {
                        // 普通成员权利中的创建者
                        List<Long> uidList = getClubCreateSubordinateLevelIdList(ClubMemberUtils.getArrayValueLong(k, "clubID"));
                        int useLessNum = 0;
                        for (Long con : uidList) {
                            ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                            if (Objects.isNull(clubMember)) {
                                useLessNum++;
                                continue;
                            }
                            if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                                useLessNum++;
                            }
                        }
                        if (CollectionUtils.isNotEmpty(uidList)) {
                            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubGeneral(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), uidList);
                            if (Objects.nonNull(clubPromotionLevelItem)) {
                                return new ClubPromotionLevelItem(
                                        player.getPid(),
                                        player.getName(),
                                        player.getHeadImageUrl(),
                                        uidList.size() - useLessNum,
                                        clubPromotionLevelItem.getSetCount(),
                                        clubPromotionLevelItem.getWinner(),
                                        clubPromotionLevelItem.getEntryFee(),
                                        Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                        clubPromotionLevelItem.getSportsPointConsume(),
                                        ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                        sumTotalSportsPoint(uidList),
                                        ClubMemberUtils.getArrayValueInteger(k, "level"),
                                        0,
                                        ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                        ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                        ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                        clubPromotionLevelItem.getPromotionShareValue(),
                                        clubPromotionLevelItem.getActualEntryFee(),
                                        clubPromotionLevelItem.getConsume(),
                                        ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                        ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                        this.getExamineStatusShare(k, zeroClockS, type),
                                        ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                        ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                            }
                        }
                    } else {
                        // 普通成员权利中的普通玩家
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubGeneral(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"));
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    1,
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    ClubMemberUtils.getArrayValueInteger(k, "level"),
                                    0,
                                    ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                    ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                    this.getExamineStatusShare(k, zeroClockS, type),
                                    ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                    ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                        }
                    }
                }
            } else {
                ClubPromotionLevelReportFormItem clubPromotionLevelItem = null;
                if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister")) && Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利并且是创建者
                    clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID"))), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                    if (Objects.nonNull(clubPromotionLevelItem)) {
                        return new ClubPromotionLevelItem(
                                player.getPid(),
                                player.getName(),
                                player.getHeadImageUrl(),
                                clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")),
                                clubPromotionLevelItem.getSetCount(),
                                clubPromotionLevelItem.getWinner(),
                                clubPromotionLevelItem.getEntryFee(),
                                Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                clubPromotionLevelItem.getSportsPointConsume(),
                                ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                sumTotalSportsPoint(ClubMemberUtils.getArrayValueLong(k, "clubID")),
                                ClubMemberUtils.getArrayValueInteger(k, "level"),
                                2,
                                ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                clubPromotionLevelItem.getPromotionShareValue(),
                                clubPromotionLevelItem.getActualEntryFee(),
                                clubPromotionLevelItem.getConsume(),
                                ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                this.getExamineStatusShare(k, zeroClockS, type),
                                ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                    }
                    return new ClubPromotionLevelItem(player.getPid(), 2, player.getName(), player.getHeadImageUrl(), clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")), ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), sumSportsPoint(ClubMemberUtils.getArrayValueLong(k, "clubID")), ClubMemberUtils.getArrayValueInteger(k, "level"),
                            ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")), ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")), ClubMemberUtils.getArrayValueInteger(k, "shareType"), ClubMemberUtils.getArrayValueDouble(k, "shareValue"), ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"), this.getExamineStatusShare(k, zeroClockS, type), ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(levelPower) || Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利或者推广员权利
                    List<Long> uidList = Lists.newArrayList();
                    uidList.add(ClubMemberUtils.getArrayValueLong(k, "id"));
                    List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", ClubMemberUtils.getArrayValueLong(k, "id")), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
                    if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                        // 查询我的所有下线（包括我）：
                        uidList.addAll(queryUidOrPuidItemList.stream().map(v -> v.getUid()).collect(Collectors.toList()));
                    }
                    int useLessNum = 0;
                    for (Long con : uidList) {
                        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                        if (Objects.isNull(clubMember)) {
                            useLessNum++;
                            continue;
                        }
                        if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                            useLessNum++;
                        }
                    }
                    if (CollectionUtils.isNotEmpty(uidList)) {
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID")), Restrictions.in("memberId", uidList)), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                        ClubMember upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(ClubMemberUtils.getArrayValueLong(k, "upLevelId"));
                        if (isZhongZhi && ClubMemberUtils.getArrayValueInteger(k, "level") <= 0 && Objects.nonNull(upLevelMember)) {
                            ClubPromotionLevelItem clubPromotionLevelItemZhongZhi = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubPromotionLevelPlayGameId(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "playerID"), upLevelMember.getClubMemberBO().getPlayerID(), type);
                            //普通成员
                            if (Objects.nonNull(clubPromotionLevelItem) && Objects.nonNull(clubPromotionLevelItemZhongZhi)) {
                                clubPromotionLevelItem.setPromotionShareValue(clubPromotionLevelItemZhongZhi.getPromotionShareValue());
                            }
                        }
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    uidList.size() - useLessNum,
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    sumTotalSportsPoint(uidList),
                                    ClubMemberUtils.getArrayValueInteger(k, "level"),
                                    0,
                                    ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                    ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                    this.getExamineStatusShare(k, zeroClockS, type),
                                    ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                    ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                        }
                    }
                    return new ClubPromotionLevelItem(player.getPid(), player.getName(), player.getHeadImageUrl(), uidList.size(), ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), sumSportsPoint(uidList), ClubMemberUtils.getArrayValueInteger(k, "level"),
                            ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")), ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")), ClubMemberUtils.getArrayValueInteger(k, "shareType"), ClubMemberUtils.getArrayValueDouble(k, "shareValue"), ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"), this.getExamineStatusShare(k, zeroClockS, type), ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                            ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.GENERAL.equals(levelPower)) {
                    // 普通成员权利
                    if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister"))) {
                        // 普通成员权利中的创建者
                        List<Long> uidList = getClubCreateSubordinateLevelIdList(ClubMemberUtils.getArrayValueLong(k, "clubID"));
                        if (CollectionUtils.isNotEmpty(uidList)) {
                            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID")), Restrictions.in("memberId", uidList)), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                            int useLessNum = 0;
                            for (Long con : uidList) {
                                ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                                if (Objects.isNull(clubMember)) {
                                    useLessNum++;
                                    continue;
                                }
                                if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                                    useLessNum++;
                                }
                            }
                            if (Objects.nonNull(clubPromotionLevelItem)) {
                                return new ClubPromotionLevelItem(
                                        player.getPid(),
                                        player.getName(),
                                        player.getHeadImageUrl(),
                                        uidList.size() - useLessNum,
                                        clubPromotionLevelItem.getSetCount(),
                                        clubPromotionLevelItem.getWinner(),
                                        clubPromotionLevelItem.getEntryFee(),
                                        Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                        clubPromotionLevelItem.getSportsPointConsume(),
                                        ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                        sumTotalSportsPoint(uidList),
                                        ClubMemberUtils.getArrayValueInteger(k, "level"),
                                        0,
                                        ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                        ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                        ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                        clubPromotionLevelItem.getPromotionShareValue(),
                                        clubPromotionLevelItem.getActualEntryFee(),
                                        clubPromotionLevelItem.getConsume(),
                                        ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                        ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                        this.getExamineStatusShare(k, zeroClockS, type),
                                        ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                        ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                            }
                        }
                    } else {
                        // 普通成员权利中的普通玩家
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID")), Restrictions.eq("memberId", ClubMemberUtils.getArrayValueLong(k, "id"))), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    1,
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    ClubMemberUtils.getArrayValueInteger(k, "level"),
                                    0,
                                    ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                    ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                    this.getExamineStatusShare(k, zeroClockS, type),
                                    ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                    ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                        }
                    }
                }

            }


        }
        return null;
    }

    /**
     * 推广员项
     *
     * @param k 亲友圈成员
     * @return
     */
    public ClubTeamListInfo getClubTeamListZhongZhi(String[] k, Club_define.Club_PROMOTION_LEVEL_POWER levelPower, int type) {
        // 获取用户信息
        // 时间
        Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", type);
        Criteria zeroClockSZhongZhi = TimeConditionUtils.CLUBDayZeroClockSZhongZhi("date_time", type);
        Player player = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));
        if (Objects.nonNull(player)) {
            if (type == 0) {
                ClubPromotionLevelItem clubPromotionLevelItem = null;
                if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister")) && Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利并且是创建者
                    clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubCreate(ClubMemberUtils.getArrayValueLong(k, "clubID"));
                    if (Objects.nonNull(clubPromotionLevelItem)) {
                        return new ClubTeamListInfo(
                                player.getPid(),
                                player.getName(),
                                player.getHeadImageUrl(),
                                clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")),
                                Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                clubPromotionLevelItem.getSportsPointConsume(),
                                ClubMemberUtils.getArrayValueInteger(k, "levelZhongZhi"),
                                clubPromotionLevelItem.getPromotionShareValue(),
                                ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"),
                                ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage"))
                        );
                    }
                    return new ClubTeamListInfo(player.getPid(), player.getName(), player.getHeadImageUrl(), clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "eliminatePoint"),
                            ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(levelPower) || Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利或者推广员权利
                    List<Long> uidList = Lists.newArrayList();
                    uidList.add(ClubMemberUtils.getArrayValueLong(k, "id"));
                    List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", ClubMemberUtils.getArrayValueLong(k, "id")), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
                    if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                        // 查询我的所有下线（包括我）：
                        uidList.addAll(queryUidOrPuidItemList.stream().map(v -> v.getUid()).collect(Collectors.toList()));
                    }
                    int useLessNum = 0;
                    for (Long con : uidList) {
                        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                        if (Objects.isNull(clubMember)) {
                            useLessNum++;
                            continue;
                        }
                        if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                            useLessNum++;
                        }
                    }
                    if (CollectionUtils.isNotEmpty(uidList)) {
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubPromotionLevel(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), uidList);
                        ClubMember upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(ClubMemberUtils.getArrayValueLong(k, "upLevelId"));
                        if (ClubMemberUtils.getArrayValueInteger(k, "level") <= 0 && Objects.nonNull(upLevelMember)) {
                            ClubPromotionLevelItem clubPromotionLevelItemZhongZhi = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubPromotionLevelPlayGameId(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "playerID"), upLevelMember.getClubMemberBO().getPlayerID(), type);
                            //普通成员
                            if (Objects.nonNull(clubPromotionLevelItem) && Objects.nonNull(clubPromotionLevelItemZhongZhi)) {
                                clubPromotionLevelItem.setPromotionShareValue(clubPromotionLevelItemZhongZhi.getPromotionShareValue());
                            }
                        }
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubTeamListInfo(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    uidList.size() - useLessNum,
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getArrayValueInteger(k, "levelZhongZhi"),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"),
                                    ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")
                                    ));
                        }
                    }
                    return new ClubTeamListInfo(player.getPid(), player.getName(), player.getHeadImageUrl(), uidList.size(), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "eliminatePoint"),
                            ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.GENERAL.equals(levelPower)) {
                    // 普通成员权利
                    if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister"))) {
                        // 普通成员权利中的创建者
                        List<Long> uidList = getClubCreateSubordinateLevelIdList(ClubMemberUtils.getArrayValueLong(k, "clubID"));
                        int useLessNum = 0;
                        for (Long con : uidList) {
                            ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                            if (Objects.isNull(clubMember)) {
                                useLessNum++;
                                continue;
                            }
                            if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                                useLessNum++;
                            }
                        }
                        if (CollectionUtils.isNotEmpty(uidList)) {
                            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubGeneral(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), uidList);
                            if (Objects.nonNull(clubPromotionLevelItem)) {
                                return new ClubTeamListInfo(
                                        player.getPid(),
                                        player.getName(),
                                        player.getHeadImageUrl(),
                                        uidList.size() - useLessNum,
                                        Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                        clubPromotionLevelItem.getSportsPointConsume(),
                                        ClubMemberUtils.getArrayValueInteger(k, "levelZhongZhi"),
                                        clubPromotionLevelItem.getPromotionShareValue(), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"),
                                        ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")
                                        ));
                            }
                        }
                    } else {
                        // 普通成员权利中的普通玩家
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubGeneral(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"));
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubTeamListInfo(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    1,
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getArrayValueInteger(k, "levelZhongZhi"),
                                    clubPromotionLevelItem.getPromotionShareValue(), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"),
                                    ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")
                                    ));
                        }
                    }
                }
            } else {
                ClubPromotionLevelReportFormItem clubPromotionLevelItem = null;
                if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister")) && Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利并且是创建者
                    clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID"))), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                    if (Objects.nonNull(clubPromotionLevelItem)) {
                        return new ClubTeamListInfo(
                                player.getPid(),
                                player.getName(),
                                player.getHeadImageUrl(),
                                clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")),
                                Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                clubPromotionLevelItem.getSportsPointConsume(),
                                ClubMemberUtils.getArrayValueInteger(k, "level"),
                                clubPromotionLevelItem.getPromotionShareValue(), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"),
                                ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")
                                ));
                    }
                    return new ClubTeamListInfo(player.getPid(), player.getName(), player.getHeadImageUrl(), clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "eliminatePoint"),
                            ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(levelPower) || Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利或者推广员权利
                    List<Long> uidList = Lists.newArrayList();
                    uidList.add(ClubMemberUtils.getArrayValueLong(k, "id"));
                    List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", ClubMemberUtils.getArrayValueLong(k, "id")), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
                    if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                        // 查询我的所有下线（包括我）：
                        uidList.addAll(queryUidOrPuidItemList.stream().map(v -> v.getUid()).collect(Collectors.toList()));
                    }
                    int useLessNum = 0;
                    for (Long con : uidList) {
                        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                        if (Objects.isNull(clubMember)) {
                            useLessNum++;
                            continue;
                        }
                        if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                            useLessNum++;
                        }
                    }
                    if (CollectionUtils.isNotEmpty(uidList)) {
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID")), Restrictions.in("memberId", uidList)), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                        ClubMember upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(ClubMemberUtils.getArrayValueLong(k, "upLevelId"));
                        if (ClubMemberUtils.getArrayValueInteger(k, "level") <= 0 && Objects.nonNull(upLevelMember)) {
                            ClubPromotionLevelItem clubPromotionLevelItemZhongZhi = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubPromotionLevelPlayGameId(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "playerID"), upLevelMember.getClubMemberBO().getPlayerID(), type);
                            //普通成员
                            if (Objects.nonNull(clubPromotionLevelItem) && Objects.nonNull(clubPromotionLevelItemZhongZhi)) {
                                clubPromotionLevelItem.setPromotionShareValue(clubPromotionLevelItemZhongZhi.getPromotionShareValue());
                            }
                        }
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubTeamListInfo(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    uidList.size() - useLessNum,
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getArrayValueInteger(k, "level"),
                                    clubPromotionLevelItem.getPromotionShareValue(), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"),
                                    ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")
                                    ));
                        }
                    }
                    return new ClubTeamListInfo(player.getPid(), player.getName(), player.getHeadImageUrl(), uidList.size(), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "eliminatePoint"),
                            ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.GENERAL.equals(levelPower)) {
                    // 普通成员权利
                    if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister"))) {
                        // 普通成员权利中的创建者
                        List<Long> uidList = getClubCreateSubordinateLevelIdList(ClubMemberUtils.getArrayValueLong(k, "clubID"));
                        if (CollectionUtils.isNotEmpty(uidList)) {
                            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID")), Restrictions.in("memberId", uidList)), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                            int useLessNum = 0;
                            for (Long con : uidList) {
                                ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                                if (Objects.isNull(clubMember)) {
                                    useLessNum++;
                                    continue;
                                }
                                if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                                    useLessNum++;
                                }
                            }
                            if (Objects.nonNull(clubPromotionLevelItem)) {
                                return new ClubTeamListInfo(
                                        player.getPid(),
                                        player.getName(),
                                        player.getHeadImageUrl(),
                                        uidList.size() - useLessNum,
                                        Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                        clubPromotionLevelItem.getSportsPointConsume(),
                                        ClubMemberUtils.getArrayValueInteger(k, "level"),
                                        clubPromotionLevelItem.getPromotionShareValue(), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"),
                                        ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")
                                        ));
                            }
                        }
                    } else {
                        // 普通成员权利中的普通玩家
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID")), Restrictions.eq("memberId", ClubMemberUtils.getArrayValueLong(k, "id"))), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubTeamListInfo(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    1,
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getArrayValueInteger(k, "level"),
                                    clubPromotionLevelItem.getPromotionShareValue(), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"),
                                    ClubMemberUtils.zhongZhiGetPosistion(ClubMemberUtils.getArrayValueInteger(k, "isminister"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "promotionManage")));
                        }
                    }
                }

            }


        }
        return null;
    }

    /**
     * 推广员项
     *
     * @param k 亲友圈成员
     * @return
     */
    public ClubPromotionLevelItem getClubPromotionLevelItemShareZhongZhi(String[] k, Club_define.Club_PROMOTION_LEVEL_POWER levelPower, int type) {
        // 获取用户信息
        // 时间
        Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", type);
        Criteria zeroClockSZhongZhi = TimeConditionUtils.CLUBDayZeroClockSZhongZhi("date_time", type);
        Player player = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));
        if (Objects.nonNull(player)) {
            if (type == 0) {
                ClubPromotionLevelItem clubPromotionLevelItem = null;
                if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister")) && Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利并且是创建者
                    clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubCreate(ClubMemberUtils.getArrayValueLong(k, "clubID"));
                    if (Objects.nonNull(clubPromotionLevelItem)) {
                        return new ClubPromotionLevelItem(
                                player.getPid(),
                                player.getName(),
                                player.getHeadImageUrl(),
                                clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")),
                                clubPromotionLevelItem.getSetCount(),
                                clubPromotionLevelItem.getWinner(),
                                clubPromotionLevelItem.getEntryFee(),
                                Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                clubPromotionLevelItem.getSportsPointConsume(),
                                ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                sumTotalSportsPoint(ClubMemberUtils.getArrayValueLong(k, "clubID")),
                                ClubMemberUtils.getArrayValueInteger(k, "level"),
                                2,
                                ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                clubPromotionLevelItem.getPromotionShareValue(),
                                clubPromotionLevelItem.getActualEntryFee(),
                                clubPromotionLevelItem.getConsume(),
                                ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                this.getExamineStatusShare(k, zeroClockS, type),
                                ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                    }
                    return new ClubPromotionLevelItem(player.getPid(), 2, player.getName(), player.getHeadImageUrl(), clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")), ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), sumSportsPoint(ClubMemberUtils.getArrayValueLong(k, "clubID")), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")), ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")), ClubMemberUtils.getArrayValueInteger(k, "shareType"), ClubMemberUtils.getArrayValueDouble(k, "shareValue"), ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"), this.getExamineStatusShare(k, zeroClockS, type), ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(levelPower) || Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利或者推广员权利
                    List<Long> uidList = Lists.newArrayList();
                    uidList.add(ClubMemberUtils.getArrayValueLong(k, "id"));
                    List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", ClubMemberUtils.getArrayValueLong(k, "id")), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
                    if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                        // 查询我的所有下线（包括我）：
                        uidList.addAll(queryUidOrPuidItemList.stream().map(v -> v.getUid()).collect(Collectors.toList()));
                    }
                    int useLessNum = 0;
                    for (Long con : uidList) {
                        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                        if (Objects.isNull(clubMember)) {
                            useLessNum++;
                            continue;
                        }
                        if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                            useLessNum++;
                        }
                    }
                    if (CollectionUtils.isNotEmpty(uidList)) {
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubPromotionLevel(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), uidList);
                        ClubMember upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(ClubMemberUtils.getArrayValueLong(k, "upLevelId"));
                        if (ClubMemberUtils.getArrayValueInteger(k, "level") <= 0 && Objects.nonNull(upLevelMember)) {
                            ClubPromotionLevelItem clubPromotionLevelItemZhongZhi = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubPromotionLevelPlayGameId(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "playerID"), upLevelMember.getClubMemberBO().getPlayerID(), type);
                            //普通成员
                            if (Objects.nonNull(clubPromotionLevelItem) && Objects.nonNull(clubPromotionLevelItemZhongZhi)) {
                                clubPromotionLevelItem.setPromotionShareValue(clubPromotionLevelItemZhongZhi.getPromotionShareValue());
                            }
                        }
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    uidList.size() - useLessNum,
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    sumTotalSportsPoint(uidList),
                                    ClubMemberUtils.getArrayValueInteger(k, "level"),
                                    0,
                                    ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                    ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                    this.getExamineStatusShare(k, zeroClockS, type),
                                    ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                    ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                        }
                    }
                    return new ClubPromotionLevelItem(player.getPid(), player.getName(), player.getHeadImageUrl(), uidList.size(), ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), sumSportsPoint(uidList), ClubMemberUtils.getArrayValueInteger(k, "level"),
                            ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")), ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")), ClubMemberUtils.getArrayValueInteger(k, "shareType"), ClubMemberUtils.getArrayValueDouble(k, "shareValue"), ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"), this.getExamineStatusShare(k, zeroClockS, type), ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.GENERAL.equals(levelPower)) {
                    // 普通成员权利
                    if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister"))) {
                        // 普通成员权利中的创建者
                        List<Long> uidList = getClubCreateSubordinateLevelIdList(ClubMemberUtils.getArrayValueLong(k, "clubID"));
                        int useLessNum = 0;
                        for (Long con : uidList) {
                            ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                            if (Objects.isNull(clubMember)) {
                                useLessNum++;
                                continue;
                            }
                            if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                                useLessNum++;
                            }
                        }
                        if (CollectionUtils.isNotEmpty(uidList)) {
                            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubGeneral(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), uidList);
                            if (Objects.nonNull(clubPromotionLevelItem)) {
                                return new ClubPromotionLevelItem(
                                        player.getPid(),
                                        player.getName(),
                                        player.getHeadImageUrl(),
                                        uidList.size() - useLessNum,
                                        clubPromotionLevelItem.getSetCount(),
                                        clubPromotionLevelItem.getWinner(),
                                        clubPromotionLevelItem.getEntryFee(),
                                        Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                        clubPromotionLevelItem.getSportsPointConsume(),
                                        ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                        sumTotalSportsPoint(uidList),
                                        ClubMemberUtils.getArrayValueInteger(k, "level"),
                                        0,
                                        ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                        ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                        ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                        clubPromotionLevelItem.getPromotionShareValue(),
                                        clubPromotionLevelItem.getActualEntryFee(),
                                        clubPromotionLevelItem.getConsume(),
                                        ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                        ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                        this.getExamineStatusShare(k, zeroClockS, type),
                                        ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                        ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                            }
                        }
                    } else {
                        // 普通成员权利中的普通玩家
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubGeneral(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"));
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    1,
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    ClubMemberUtils.getArrayValueInteger(k, "level"),
                                    0,
                                    ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                    ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                    this.getExamineStatusShare(k, zeroClockS, type),
                                    ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                    ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                        }
                    }
                }
            } else {
                ClubPromotionLevelReportFormItem clubPromotionLevelItem = null;
                if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister")) && Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利并且是创建者
                    clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID"))), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                    if (Objects.nonNull(clubPromotionLevelItem)) {
                        return new ClubPromotionLevelItem(
                                player.getPid(),
                                player.getName(),
                                player.getHeadImageUrl(),
                                clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")),
                                clubPromotionLevelItem.getSetCount(),
                                clubPromotionLevelItem.getWinner(),
                                clubPromotionLevelItem.getEntryFee(),
                                Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                clubPromotionLevelItem.getSportsPointConsume(),
                                ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                sumTotalSportsPoint(ClubMemberUtils.getArrayValueLong(k, "clubID")),
                                ClubMemberUtils.getArrayValueInteger(k, "level"),
                                2,
                                ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                clubPromotionLevelItem.getPromotionShareValue(),
                                clubPromotionLevelItem.getActualEntryFee(),
                                clubPromotionLevelItem.getConsume(),
                                ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                this.getExamineStatusShare(k, zeroClockS, type),
                                ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                    }
                    return new ClubPromotionLevelItem(player.getPid(), 2, player.getName(), player.getHeadImageUrl(), clubPeopleNum(ClubMemberUtils.getArrayValueLong(k, "clubID")), ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), sumSportsPoint(ClubMemberUtils.getArrayValueLong(k, "clubID")), ClubMemberUtils.getArrayValueInteger(k, "level"),
                            ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")), ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")), ClubMemberUtils.getArrayValueInteger(k, "shareType"), ClubMemberUtils.getArrayValueDouble(k, "shareValue"), ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"), this.getExamineStatusShare(k, zeroClockS, type), ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")), ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(levelPower) || Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利或者推广员权利
                    List<Long> uidList = Lists.newArrayList();
                    uidList.add(ClubMemberUtils.getArrayValueLong(k, "id"));
                    List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", ClubMemberUtils.getArrayValueLong(k, "id")), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
                    if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                        // 查询我的所有下线（包括我）：
                        uidList.addAll(queryUidOrPuidItemList.stream().map(v -> v.getUid()).collect(Collectors.toList()));
                    }
                    int useLessNum = 0;
                    for (Long con : uidList) {
                        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                        if (Objects.isNull(clubMember)) {
                            useLessNum++;
                            continue;
                        }
                        if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                            useLessNum++;
                        }
                    }
                    if (CollectionUtils.isNotEmpty(uidList)) {
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID")), Restrictions.in("memberId", uidList)), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                        ClubMember upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(ClubMemberUtils.getArrayValueLong(k, "upLevelId"));
                        if (ClubMemberUtils.getArrayValueInteger(k, "level") <= 0 && Objects.nonNull(upLevelMember)) {
                            ClubPromotionLevelItem clubPromotionLevelItemZhongZhi = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubPromotionLevelPlayGameId(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "playerID"), upLevelMember.getClubMemberBO().getPlayerID(), type);
                            //普通成员
                            if (Objects.nonNull(clubPromotionLevelItem) && Objects.nonNull(clubPromotionLevelItemZhongZhi)) {
                                clubPromotionLevelItem.setPromotionShareValue(clubPromotionLevelItemZhongZhi.getPromotionShareValue());
                            }
                        }
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    uidList.size() - useLessNum,
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    sumTotalSportsPoint(uidList),
                                    ClubMemberUtils.getArrayValueInteger(k, "level"),
                                    0,
                                    ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                    ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                    this.getExamineStatusShare(k, zeroClockS, type),
                                    ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                    ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                        }
                    }
                    return new ClubPromotionLevelItem(player.getPid(), player.getName(), player.getHeadImageUrl(), uidList.size(), ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), sumSportsPoint(uidList), ClubMemberUtils.getArrayValueInteger(k, "level"),
                            ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")), ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")), ClubMemberUtils.getArrayValueInteger(k, "shareType"), ClubMemberUtils.getArrayValueDouble(k, "shareValue"), ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"), this.getExamineStatusShare(k, zeroClockS, type), ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                            ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.GENERAL.equals(levelPower)) {
                    // 普通成员权利
                    if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister"))) {
                        // 普通成员权利中的创建者
                        List<Long> uidList = getClubCreateSubordinateLevelIdList(ClubMemberUtils.getArrayValueLong(k, "clubID"));
                        if (CollectionUtils.isNotEmpty(uidList)) {
                            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID")), Restrictions.in("memberId", uidList)), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                            int useLessNum = 0;
                            for (Long con : uidList) {
                                ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(con);
                                if (Objects.isNull(clubMember)) {
                                    useLessNum++;
                                    continue;
                                }
                                if (clubMember.getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {
                                    useLessNum++;
                                }
                            }
                            if (Objects.nonNull(clubPromotionLevelItem)) {
                                return new ClubPromotionLevelItem(
                                        player.getPid(),
                                        player.getName(),
                                        player.getHeadImageUrl(),
                                        uidList.size() - useLessNum,
                                        clubPromotionLevelItem.getSetCount(),
                                        clubPromotionLevelItem.getWinner(),
                                        clubPromotionLevelItem.getEntryFee(),
                                        Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                        clubPromotionLevelItem.getSportsPointConsume(),
                                        ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                        sumTotalSportsPoint(uidList),
                                        ClubMemberUtils.getArrayValueInteger(k, "level"),
                                        0,
                                        ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                        ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                        ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                        clubPromotionLevelItem.getPromotionShareValue(),
                                        clubPromotionLevelItem.getActualEntryFee(),
                                        clubPromotionLevelItem.getConsume(),
                                        ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                        ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                        this.getExamineStatusShare(k, zeroClockS, type),
                                        ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                        ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                            }
                        }
                    } else {
                        // 普通成员权利中的普通玩家
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID")), Restrictions.eq("memberId", ClubMemberUtils.getArrayValueLong(k, "id"))), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    1,
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                                    ClubMemberUtils.getArrayValueInteger(k, "level"),
                                    0,
                                    ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                                    ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                                    ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                                    this.getExamineStatusShare(k, zeroClockS, type),
                                    ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                                    ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                        }
                    }
                }

            }


        }
        return null;
    }

    /**
     * 中至比赛排行界面
     *
     * @param k 亲友圈成员
     * @return
     */
    public ClubPromotionLevelItem getCompetitionRankedZhongZhi(String[] k, Club_define.Club_PROMOTION_LEVEL_POWER levelPower, int type) {
        // 获取用户信息
        // 时间
        Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", type);
        Criteria zeroClockSZhongZhi = TimeConditionUtils.CLUBDayZeroClockSZhongZhi("date_time", type);
        Player player = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));
        if (Objects.nonNull(player)) {
            if (type == 0) {
                ClubPromotionLevelItem clubPromotionLevelItem = null;
                // 普通成员权利中的普通玩家
                clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneClubGeneral(ClubMemberUtils.getArrayValueLong(k, "clubID"), ClubMemberUtils.getArrayValueLong(k, "id"), ClubMemberUtils.getArrayValueLong(k, "upLevelId"), ClubMemberUtils.getArrayValueInteger(k, "level"));
                if (Objects.nonNull(clubPromotionLevelItem)) {
                    return new ClubPromotionLevelItem(
                            player.getPid(),
                            player.getName(),
                            player.getHeadImageUrl(),
                            1,
                            clubPromotionLevelItem.getSetCount(),
                            clubPromotionLevelItem.getWinner(),
                            clubPromotionLevelItem.getEntryFee(),
                            Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                            clubPromotionLevelItem.getSportsPointConsume(),
                            ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                            ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                            ClubMemberUtils.getArrayValueInteger(k, "level"),
                            0,
                            ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                            ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                            ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                            clubPromotionLevelItem.getPromotionShareValue(),
                            clubPromotionLevelItem.getActualEntryFee(),
                            clubPromotionLevelItem.getConsume(),
                            ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                            ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                            this.getExamineStatusShare(k, zeroClockS, type),
                            ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                            ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                }


            } else {
                ClubPromotionLevelReportFormItem clubPromotionLevelItem = null;
                // 普通成员权利中的普通玩家
                clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(zeroClockSZhongZhi, Restrictions.eq("clubID", ClubMemberUtils.getArrayValueLong(k, "clubID")), Restrictions.eq("memberId", ClubMemberUtils.getArrayValueLong(k, "id"))), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                if (Objects.nonNull(clubPromotionLevelItem)) {
                    return new ClubPromotionLevelItem(
                            player.getPid(),
                            player.getName(),
                            player.getHeadImageUrl(),
                            1,
                            clubPromotionLevelItem.getSetCount(),
                            clubPromotionLevelItem.getWinner(),
                            clubPromotionLevelItem.getEntryFee(),
                            Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                            clubPromotionLevelItem.getSportsPointConsume(),
                            ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                            ClubMemberUtils.getTotalSportsPoint(ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueDouble(k, "caseSportsPoint")),
                            ClubMemberUtils.getArrayValueInteger(k, "level"),
                            0,
                            ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                            ClubMemberUtils.getArrayValueDouble(k, "shareValue"),
                            ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"),
                            clubPromotionLevelItem.getPromotionShareValue(),
                            clubPromotionLevelItem.getActualEntryFee(),
                            clubPromotionLevelItem.getConsume(),
                            ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                            ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")),
                            this.getExamineStatusShare(k, zeroClockS, type),
                            ClubMemberUtils.getAlivePointZhongZhi(ClubMemberUtils.getArrayValueInteger(k, "alivePointStatus"), ClubMemberUtils.getArrayValueDouble(k, "alivePoint")),
                            ClubMemberUtils.getArrayValueDouble(k, "eliminatePoint"));
                }
            }


        }
        return null;
    }

    /**
     * 推广员项
     *
     * @param k 亲友圈成员
     * @return
     */
    public ClubPromotionLevelItem getClubPromotionLevelItem(ClubMember k, Club_define.Club_PROMOTION_LEVEL_POWER levelPower, int type, boolean isZhongZhi) {
        // 获取用户信息
        // 时间
        Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", type);
        Player player = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());

        if (Objects.nonNull(player)) {
            if (type == 0) {
                ClubPromotionLevelItem clubPromotionLevelItem = null;
                if (k.isClubCreate() && Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利并且是创建者
                    clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubCreate(k.getClubID());
                    if (Objects.nonNull(clubPromotionLevelItem)) {
                        return new ClubPromotionLevelItem(
                                player.getPid(),
                                player.getName(),
                                player.getHeadImageUrl(),
                                clubPeopleNum(k.getClubID()),
                                clubPromotionLevelItem.getSetCount(),
                                clubPromotionLevelItem.getWinner(),
                                clubPromotionLevelItem.getEntryFee(),
                                Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                clubPromotionLevelItem.getSportsPointConsume(),
                                k.getTotalSportsPoint(),
                                sumTotalSportsPoint(k.getClubID()),
                                k.getClubMemberBO().getLevel(),
                                2,
                                k.getClubMemberBO().getShareType(),
                                k.getClubMemberBO().getShareValue(),
                                k.getClubMemberBO().getShareFixedValue(),
                                clubPromotionLevelItem.getPromotionShareValue(),
                                clubPromotionLevelItem.getActualEntryFee(),
                                clubPromotionLevelItem.getConsume(),
                                k.getSportsPointWarningPersonal(),
                                k.getSportsPointWarning(),
                                this.getExamineStatus(k, zeroClockS, type),
                                k.getAlivePointZhongZhi(),
                                k.getClubMemberBO().getEliminatePoint());
                    }
                    return new ClubPromotionLevelItem(player.getPid(), 2, player.getName(), player.getHeadImageUrl(), clubPeopleNum(k.getClubID()), k.getSportsPoint(), sumSportsPoint(k.getClubID()), k.getClubMemberBO().getLevel(), k.getSportsPointWarningPersonal(), k.getSportsPointWarning(), k.getClubMemberBO().getShareType(), k.getClubMemberBO().getShareValue(), k.getClubMemberBO().getShareFixedValue(), this.getExamineStatus(k, zeroClockS, type), k.getAlivePointZhongZhi(), k.getClubMemberBO().getEliminatePoint());
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(levelPower) || Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利或者推广员权利
                    List<Long> uidList = Lists.newArrayList();
                    uidList.add(k.getId());
                    List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", k.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
                    if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                        // 查询我的所有下线（包括我）：
                        uidList.addAll(queryUidOrPuidItemList.stream().map(v -> v.getUid()).collect(Collectors.toList()));
                    }
                    if (CollectionUtils.isNotEmpty(uidList)) {
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubPromotionLevel(k.getClubID(), k.getId(), k.getClubMemberBO().getUpLevelId(), k.getLevel(), uidList);
                        ClubMember upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(k.getClubMemberBO().getUpLevelId());
                        if (isZhongZhi && !k.isLevelPromotion() && Objects.nonNull(upLevelMember)) {
                            ClubPromotionLevelItem clubPromotionLevelItemZhongZhi = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubPromotionLevelPlayGameId(k.getClubID(), k.getId(), k.getClubMemberBO().getUpLevelId(), k.getLevel(), k.getClubMemberBO().getPlayerID(), upLevelMember.getClubMemberBO().getPlayerID(), type);
                            //普通成员
                            if (Objects.nonNull(clubPromotionLevelItem) && Objects.nonNull(clubPromotionLevelItemZhongZhi)) {
                                clubPromotionLevelItem.setPromotionShareValue(clubPromotionLevelItemZhongZhi.getPromotionShareValue());
                            }

                        }
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    uidList.size(),
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    k.getTotalSportsPoint(),
                                    sumTotalSportsPoint(uidList),
                                    k.getClubMemberBO().getLevel(),
                                    0,
                                    k.getClubMemberBO().getShareType(),
                                    k.getClubMemberBO().getShareValue(),
                                    k.getClubMemberBO().getShareFixedValue(),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    k.getSportsPointWarningPersonal(),
                                    k.getSportsPointWarning(),
                                    this.getExamineStatus(k, zeroClockS, type),
                                    k.getAlivePointZhongZhi(),
                                    k.getClubMemberBO().getEliminatePoint());
                        }
                    }
                    return new ClubPromotionLevelItem(player.getPid(), player.getName(), player.getHeadImageUrl(), uidList.size(), k.getSportsPoint(), sumSportsPoint(uidList), k.getClubMemberBO().getLevel(),
                            k.getSportsPointWarningPersonal(), k.getSportsPointWarning(), k.getClubMemberBO().getShareType(), k.getClubMemberBO().getShareValue(), k.getClubMemberBO().getShareFixedValue(), this.getExamineStatus(k, zeroClockS, type), k.getAlivePointZhongZhi(), k.getClubMemberBO().getEliminatePoint());
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.GENERAL.equals(levelPower)) {
                    // 普通成员权利
                    if (k.isClubCreate()) {
                        // 普通成员权利中的创建者
                        List<Long> uidList = getClubCreateSubordinateLevelIdList(k.getClubID());
                        if (CollectionUtils.isNotEmpty(uidList)) {
                            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubGeneral(k.getClubID(), k.getId(), k.getClubMemberBO().getUpLevelId(), k.getLevel(), uidList);
                            if (Objects.nonNull(clubPromotionLevelItem)) {
                                return new ClubPromotionLevelItem(
                                        player.getPid(),
                                        player.getName(),
                                        player.getHeadImageUrl(),
                                        uidList.size(),
                                        clubPromotionLevelItem.getSetCount(),
                                        clubPromotionLevelItem.getWinner(),
                                        clubPromotionLevelItem.getEntryFee(),
                                        Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                        clubPromotionLevelItem.getSportsPointConsume(),
                                        k.getTotalSportsPoint(),
                                        sumTotalSportsPoint(uidList),
                                        k.getClubMemberBO().getLevel(),
                                        0,
                                        k.getClubMemberBO().getShareType(),
                                        k.getClubMemberBO().getShareValue(),
                                        k.getClubMemberBO().getShareFixedValue(),
                                        clubPromotionLevelItem.getPromotionShareValue(),
                                        clubPromotionLevelItem.getActualEntryFee(),
                                        clubPromotionLevelItem.getConsume(),
                                        k.getSportsPointWarningPersonal(),
                                        k.getSportsPointWarning(),
                                        this.getExamineStatus(k, zeroClockS, type),
                                        k.getAlivePointZhongZhi(),
                                        k.getClubMemberBO().getEliminatePoint());
                            }
                        }
                    } else {
                        // 普通成员权利中的普通玩家
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubGeneral(k.getClubID(), k.getId(), k.getClubMemberBO().getUpLevelId(), k.getLevel());
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    1,
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    k.getTotalSportsPoint(),
                                    k.getTotalSportsPoint(),
                                    k.getClubMemberBO().getLevel(),
                                    0,
                                    k.getClubMemberBO().getShareType(),
                                    k.getClubMemberBO().getShareValue(),
                                    k.getClubMemberBO().getShareFixedValue(),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    k.getSportsPointWarningPersonal(),
                                    k.getSportsPointWarning(),
                                    this.getExamineStatus(k, zeroClockS, type),
                                    k.getAlivePointZhongZhi(),
                                    k.getClubMemberBO().getEliminatePoint());
                        }
                    }
                }
            } else {
                ClubPromotionLevelReportFormItem clubPromotionLevelItem = null;
                if (k.isClubCreate() && Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利并且是创建者
                    clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", k.getClubID())), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                    if (Objects.nonNull(clubPromotionLevelItem)) {
                        return new ClubPromotionLevelItem(
                                player.getPid(),
                                player.getName(),
                                player.getHeadImageUrl(),
                                clubPeopleNum(k.getClubID()),
                                clubPromotionLevelItem.getSetCount(),
                                clubPromotionLevelItem.getWinner(),
                                clubPromotionLevelItem.getEntryFee(),
                                Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                clubPromotionLevelItem.getSportsPointConsume(),
                                k.getTotalSportsPoint(),
                                sumTotalSportsPoint(k.getClubID()),
                                k.getClubMemberBO().getLevel(),
                                2,
                                k.getClubMemberBO().getShareType(),
                                k.getClubMemberBO().getShareValue(),
                                k.getClubMemberBO().getShareFixedValue(),
                                clubPromotionLevelItem.getPromotionShareValue(),
                                clubPromotionLevelItem.getActualEntryFee(),
                                clubPromotionLevelItem.getConsume(),
                                k.getSportsPointWarningPersonal(),
                                k.getSportsPointWarning(),
                                this.getExamineStatus(k, zeroClockS, type),
                                k.getAlivePointZhongZhi(),
                                k.getClubMemberBO().getEliminatePoint());
                    }
                    return new ClubPromotionLevelItem(player.getPid(), 2, player.getName(), player.getHeadImageUrl(), clubPeopleNum(k.getClubID()), k.getSportsPoint(), sumSportsPoint(k.getClubID()), k.getClubMemberBO().getLevel(),
                            k.getSportsPointWarningPersonal(), k.getSportsPointWarning(), k.getClubMemberBO().getShareType(), k.getClubMemberBO().getShareValue(), k.getClubMemberBO().getShareFixedValue(), this.getExamineStatus(k, zeroClockS, type), k.getAlivePointZhongZhi(), k.getClubMemberBO().getEliminatePoint());
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(levelPower) || Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                    // 创建者权利或者推广员权利
                    List<Long> uidList = Lists.newArrayList();
                    uidList.add(k.getId());
                    List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", k.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
                    if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                        // 查询我的所有下线（包括我）：
                        uidList.addAll(queryUidOrPuidItemList.stream().map(v -> v.getUid()).collect(Collectors.toList()));
                    }
                    if (CollectionUtils.isNotEmpty(uidList)) {
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", k.getClubID()), Restrictions.in("memberId", uidList)), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                        ClubMember upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(k.getClubMemberBO().getUpLevelId());
                        if (isZhongZhi && !k.isLevelPromotion() && Objects.nonNull(upLevelMember)) {
                            ClubPromotionLevelItem clubPromotionLevelItemZhongZhi = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneClubPromotionLevelPlayGameId(k.getClubID(), k.getId(), k.getClubMemberBO().getUpLevelId(), k.getLevel(), k.getClubMemberBO().getPlayerID(), upLevelMember.getClubMemberBO().getPlayerID(), type);
                            //普通成员
                            if (Objects.nonNull(clubPromotionLevelItem) && Objects.nonNull(clubPromotionLevelItemZhongZhi)) {
                                clubPromotionLevelItem.setPromotionShareValue(clubPromotionLevelItemZhongZhi.getPromotionShareValue());
                            }
                        }
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    uidList.size(),
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    k.getTotalSportsPoint(),
                                    sumTotalSportsPoint(uidList),
                                    k.getClubMemberBO().getLevel(),
                                    0,
                                    k.getClubMemberBO().getShareType(),
                                    k.getClubMemberBO().getShareValue(),
                                    k.getClubMemberBO().getShareFixedValue(),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    k.getSportsPointWarningPersonal(),
                                    k.getSportsPointWarning(),
                                    this.getExamineStatus(k, zeroClockS, type),
                                    k.getAlivePointZhongZhi(),
                                    k.getClubMemberBO().getEliminatePoint());
                        }
                    }
                    return new ClubPromotionLevelItem(player.getPid(), player.getName(), player.getHeadImageUrl(), uidList.size(), k.getSportsPoint(), sumSportsPoint(uidList), k.getClubMemberBO().getLevel(),
                            k.getSportsPointWarningPersonal(), k.getSportsPointWarning(), k.getClubMemberBO().getShareType(), k.getClubMemberBO().getShareValue(), k.getClubMemberBO().getShareFixedValue(), this.getExamineStatus(k, zeroClockS, type), k.getAlivePointZhongZhi(), k.getClubMemberBO().getEliminatePoint());
                } else if (Club_define.Club_PROMOTION_LEVEL_POWER.GENERAL.equals(levelPower)) {
                    // 普通成员权利
                    if (k.isClubCreate()) {
                        // 普通成员权利中的创建者
                        List<Long> uidList = getClubCreateSubordinateLevelIdList(k.getClubID());
                        if (CollectionUtils.isNotEmpty(uidList)) {
                            clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", k.getClubID()), Restrictions.in("memberId", uidList)), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                            if (Objects.nonNull(clubPromotionLevelItem)) {
                                return new ClubPromotionLevelItem(
                                        player.getPid(),
                                        player.getName(),
                                        player.getHeadImageUrl(),
                                        uidList.size(),
                                        clubPromotionLevelItem.getSetCount(),
                                        clubPromotionLevelItem.getWinner(),
                                        clubPromotionLevelItem.getEntryFee(),
                                        Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                        clubPromotionLevelItem.getSportsPointConsume(),
                                        k.getTotalSportsPoint(),
                                        sumTotalSportsPoint(uidList),
                                        k.getClubMemberBO().getLevel(),
                                        0,
                                        k.getClubMemberBO().getShareType(),
                                        k.getClubMemberBO().getShareValue(),
                                        k.getClubMemberBO().getShareFixedValue(),
                                        clubPromotionLevelItem.getPromotionShareValue(),
                                        clubPromotionLevelItem.getActualEntryFee(),
                                        clubPromotionLevelItem.getConsume(),
                                        k.getSportsPointWarningPersonal(),
                                        k.getSportsPointWarning(),
                                        this.getExamineStatus(k, zeroClockS, type),
                                        k.getAlivePointZhongZhi(),
                                        k.getClubMemberBO().getEliminatePoint());
                            }
                        }
                    } else {
                        // 普通成员权利中的普通玩家
                        clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", k.getClubID()), Restrictions.eq("memberId", k.getId())), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                        if (Objects.nonNull(clubPromotionLevelItem)) {
                            return new ClubPromotionLevelItem(
                                    player.getPid(),
                                    player.getName(),
                                    player.getHeadImageUrl(),
                                    1,
                                    clubPromotionLevelItem.getSetCount(),
                                    clubPromotionLevelItem.getWinner(),
                                    clubPromotionLevelItem.getEntryFee(),
                                    Double.valueOf(clubPromotionLevelItem.getConsume()).intValue(),
                                    clubPromotionLevelItem.getSportsPointConsume(),
                                    k.getTotalSportsPoint(),
                                    k.getTotalSportsPoint(),
                                    k.getClubMemberBO().getLevel(),
                                    0,
                                    k.getClubMemberBO().getShareType(),
                                    k.getClubMemberBO().getShareValue(),
                                    k.getClubMemberBO().getShareFixedValue(),
                                    clubPromotionLevelItem.getPromotionShareValue(),
                                    clubPromotionLevelItem.getActualEntryFee(),
                                    clubPromotionLevelItem.getConsume(),
                                    k.getSportsPointWarningPersonal(),
                                    k.getSportsPointWarning(),
                                    this.getExamineStatus(k, zeroClockS, type),
                                    k.getAlivePointZhongZhi(),
                                    k.getClubMemberBO().getEliminatePoint());
                        }
                    }
                }

            }


        }
        return null;
    }


    /**
     * 获取推广员统计
     *
     * @param promotionList 推广员参数
     * @param pid           玩家Pid
     * @return
     */
    public SData_Result getClubPromotionLevelCount(CClub_PromotionList promotionList, long pid) {
        return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelCountItem());
    }

    /**
     * 下属推广员任命变化
     *
     * @param findPIDAdd
     * @param pid
     * @return
     */
    public SData_Result clubSubordinateLevelChange(CClub_FindPIDAdd findPIDAdd, long pid) {
        if (findPIDAdd.getType() == 0) {
            return clubSubordinateLevelAppoint(findPIDAdd, pid);
        } else {
            return clubSubordinateLevelCancel(findPIDAdd, pid);
        }
    }

    /**
     * 下属推广员任命变化
     *
     * @param findPIDAdd
     * @param pid
     * @return
     */
    public SData_Result clubSubordinateLevelChangeZhongZhi(CClub_FindPIDAdd findPIDAdd, long pid) {
        SData_Result result = clubSubordinateLevelAppoint(findPIDAdd, pid);
        if (ErrorCode.Success.equals(result.getCode())) {
            //成功的话 返回最新的页面
            return result;
        } else {
            return result;
        }

    }

    /**
     * 下属推广员任命变化
     *
     * @param findPIDAdd
     * @param pid
     * @return
     */
    public SData_Result clubSubordinateLeveCancleZhongZhi(CClub_FindPIDAdd findPIDAdd, long pid) {
        return clubSubordinateLevelCancel(findPIDAdd, pid);

    }

    /**
     * 下属推广员任命
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    public SData_Result clubSubordinateLevelAppoint(CClub_FindPIDAdd findPIDAdd, long pid) {
        SData_Result result = checkExistPromotionSubordinateLevel(findPIDAdd.getClubId(), findPIDAdd.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(findPIDAdd.getClubId());
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        if (Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(clubMemberItem.getLevelPower())) {
            if (clubMemberItem.getToClubMember().getClubMemberBO().getLevel() <= 0 && clubMemberItem.getToClubMember().getClubMemberBO().getRealUpLevelId() <= 0L) {
                UnionDynamicBO.insertClubDynamic(findPIDAdd.pid, findPIDAdd.clubId, pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_PROMOTION_DYNAMIC_APPOINT.value());
                // 设置推广员
                clubMemberItem.getToClubMember().getClubMemberBO().saveLevelAndUpLevelId(Club_define.Club_PROMOTION_LEVEL.APPOINT.ordinal(), clubMemberItem.getDoClubMember().getId());
                if (club.isZhongZhiClub()) {
                    //如果是中至的话  战队等级设为1
                    clubMemberItem.getToClubMember().getClubMemberBO().saveLevelZhongZhi(1);
                }
                DispatcherComponent.getInstance().publish(new PromotionLevelInsertEvent(clubMemberItem.getToClubMember().getId(), clubMemberItem.getDoClubMember().getId()));
                //亲友圈身份变动记录
                FlowLogger.clubmemberStatusLog(findPIDAdd.pid, findPIDAdd.clubId, pid, findPIDAdd.clubId, Club_define.CLUB_EXEC_TYPE.CLUB_EXEC_BECOME_PROMOTION.value());
                return SData_Result.make(ErrorCode.Success, clubMemberItem.getToClubMember().getClubMemberBO().getLevel());
            }
        }
        ClubMember upLevelMember;
        if (Config.isShare()) {
            upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId());
        } else {
            upLevelMember = getClubMemberMap().get(clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId());
        }
        if (Objects.isNull(upLevelMember)) {
            // 上级代理不存在
            return SData_Result.make(ErrorCode.CLUB_UP_LEVEL_MEMBER_NOT_EXIST, "CLUB_UP_LEVEL_MEMBER_NOT_EXIST");
        }
        //推广员等级
        int maxLevel = Club_define.CLUB_LEVEL_MAX.CLUB_LEVEL_NORMAL.value();
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.nonNull(union) && union.getUnionBO().getUnionType() == UnionDefine.UNION_TYPE.ZhongZhi.value()) {
            maxLevel = Club_define.CLUB_LEVEL_MAX.CLUB_LEVEL_ZHONG.value();
        }
        if (upLevelMember.getClubMemberBO().getLevel() >= maxLevel) {
            // 上限
            return SData_Result.make(ErrorCode.CLUB_PARTNER_UPPER_LIMIT, "CLUB_PARTNER_UPPER_LIMIT");
        }
        if (clubMemberItem.getToClubMember().getClubMemberBO().getLevel() > 0 || findPIDAdd.getPid() == pid) {
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        UnionDynamicBO.insertClubDynamic(findPIDAdd.pid, findPIDAdd.clubId, pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_PROMOTION_DYNAMIC_APPOINT.value());
        clubMemberItem.getToClubMember().getClubMemberBO().saveLevel(upLevelMember.getClubMemberBO().getLevel() + 1);
        if (club.isZhongZhiClub()) {
            //如果是中至的话  战队等级设为1
            clubMemberItem.getToClubMember().getClubMemberBO().saveLevelZhongZhi(1);
        }
        //亲友圈身份变动记录
        FlowLogger.clubmemberStatusLog(findPIDAdd.pid, findPIDAdd.clubId, pid, findPIDAdd.clubId, Club_define.CLUB_EXEC_TYPE.CLUB_EXEC_BECOME_PROMOTION.value());
        return SData_Result.make(ErrorCode.Success, clubMemberItem.getToClubMember().getClubMemberBO().getLevel());
    }


    /**
     * 下属推广员卸任
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    public SData_Result clubSubordinateLevelCancel(CClub_FindPIDAdd findPIDAdd, long pid) {
        SData_Result result = checkExistPromotionSubordinateLevel(findPIDAdd.getClubId(), findPIDAdd.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        //查出自己的所有下线
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", clubMemberItem.getToClubMember().getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            //存在下级不能取消
            return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_EXIST_LOWER, "CLUB_MEMBER_PROMOTION_EXIST_LOWER");
        }
        ClubMember upLevelMember;
        if (Config.isShare()) {
            upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId());
        } else {
            upLevelMember = getClubMemberMap().get(clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId());
        }
        if (Objects.isNull(upLevelMember)) {
            // 上级代理不存在
            return SData_Result.make(ErrorCode.CLUB_UP_LEVEL_MEMBER_NOT_EXIST, "CLUB_UP_LEVEL_MEMBER_NOT_EXIST");
        }
        if (clubMemberItem.getToClubMember().getClubMemberBO().getLevel() <= 0 || findPIDAdd.getPid() == pid) {
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }
        if (Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(clubMemberItem.getLevelPower())) {
            if (clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId() == clubMemberItem.getDoClubMember().getClubMemberBO().getId()) {
                UnionDynamicBO.insertClubDynamic(findPIDAdd.pid, findPIDAdd.clubId, pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_PROMOTION_DYNAMIC_LEAVE_OFFICE.value());
                clubMemberItem.getToClubMember().getClubMemberBO().saveLevelAndUpLevelId(0, 0L);
                // 删除绑定并且变更下属的绑定
                DispatcherComponent.getInstance().publish(new PromotionLevelDeleteEvent(clubMemberItem.getToClubMember().getClubID(), clubMemberItem.getToClubMember().getId(), upLevelMember.getId(), Club_define.Club_PROMOTION_LEVEL_DELETE.CANCEL_TOP_LEVEL, CommTime.getNowTimeStringYMD()));
                //亲友圈身份变动记录
                FlowLogger.clubmemberStatusLog(findPIDAdd.pid, findPIDAdd.clubId, pid, findPIDAdd.clubId, Club_define.CLUB_EXEC_TYPE.CLUB_EXEC_CANCEL_PROMOTION.value());
                return SData_Result.make(ErrorCode.Success, clubMemberItem.getToClubMember().getClubMemberBO().getLevel());
            }
        }
        UnionDynamicBO.insertClubDynamic(findPIDAdd.pid, findPIDAdd.clubId, pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_PROMOTION_DYNAMIC_LEAVE_OFFICE.value());
        clubMemberItem.getToClubMember().getClubMemberBO().saveClearPromotionLevelPowerOp();
        // 删除绑定并且变更下属的绑定
        DispatcherComponent.getInstance().publish(new PromotionLevelDeleteEvent(clubMemberItem.getToClubMember().getClubID(), clubMemberItem.getToClubMember().getId(), upLevelMember.getId(), Club_define.Club_PROMOTION_LEVEL_DELETE.CANCEL, CommTime.getNowTimeStringYMD()));
        SharePlayer player = SharePlayerMgr.getInstance().getSharePlayerByOnline(((Integer) findPIDAdd.getPid()).longValue());
        if (Objects.nonNull(player)) {
            player.pushProtoMq(SClub_PromotionLevelPowerChange.make(findPIDAdd.getPid(), findPIDAdd.getClubId(), clubMemberItem.getToClubMember().getClubMemberBO().getLevel(), clubMemberItem.getToClubMember().getClubMemberBO().getKicking(), clubMemberItem.getToClubMember().getClubMemberBO().getModifyValue(), clubMemberItem.getToClubMember().getClubMemberBO().getShowShare(), clubMemberItem.getToClubMember().getClubMemberBO().getInvite()));
        }
        //亲友圈身份变动记录
        FlowLogger.clubmemberStatusLog(findPIDAdd.pid, findPIDAdd.clubId, pid, findPIDAdd.clubId, Club_define.CLUB_EXEC_TYPE.CLUB_EXEC_CANCEL_PROMOTION.value());
        return SData_Result.make(ErrorCode.Success, clubMemberItem.getToClubMember().getClubMemberBO().getLevel());
    }


    /**
     * 下属推广员删除
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    public SData_Result clubSubordinateLevelDelete(CClub_FindPIDAdd findPIDAdd, long pid) {
        //检查时间  凌晨0点到2点不允许修改从属
        Calendar ca = Calendar.getInstance();
        if (ca.get(Calendar.HOUR_OF_DAY) < 2) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_TIME_ERROR, "CLUB_MEMBER_TIME_ERROR");
        }
        SData_Result result = checkExistPromotionSubordinateLevel(findPIDAdd.getClubId(), findPIDAdd.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        if (!Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(clubMemberItem.getLevelPower())) {
            if (clubMemberItem.getDoClubMember().getClubMemberBO().getKicking() != 1) {
                return SData_Result.make(ErrorCode.CLUB_UP_LEVEL_MEMBER_NOT_EXIST, "CLUB_UP_LEVEL_MEMBER_NOT_EXIST");
            }
            ClubMember upLevelMember;
            if (Config.isShare()) {
                upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId());
            } else {
                upLevelMember = getClubMemberMap().get(clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId());
            }
            if (Objects.isNull(upLevelMember) || findPIDAdd.getPid() == pid) {
                // 上级代理不存在
                return SData_Result.make(ErrorCode.CLUB_UP_LEVEL_MEMBER_NOT_EXIST, "CLUB_UP_LEVEL_MEMBER_NOT_EXIST");
            }
        }
        if (clubMemberItem.getToClubMember().getClubMemberBO().getSportsPoint() != 0D) {
            return SData_Result.make(ErrorCode.UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO, "UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO");
        }

        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        if (Objects.isNull(sharePlayer)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        if (clubMemberItem.getToClubMember().getClubMemberBO().getCaseSportsPoint() != 0D) {
            if (!clubMemberItem.getDoClubMember().isClubCreate()) {
                return SData_Result.make(ErrorCode.UNION_EXIST_CASE_SPORTS_POINT_NOT_EQUAL_ZERO, "UNION_EXIST_CASE_SPORTS_POINT_NOT_EQUAL_ZERO");
            }

            Club club = ClubMgr.getInstance().getClubListMgr().findClub(findPIDAdd.clubId);
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
            //保险箱的分数转移到竞技点上
            clubMemberItem.getToClubMember().getClubMemberBO().saveCaseSportsPoint(sharePlayer, clubMemberItem.getToClubMember().getClubMemberBO().getCaseSportsPoint(), UnionDefine.UNION_EXEC_TYPE.PLAYER_CASE_SPORTS_POINT_SUB, club.getClubListBO().getUnionId());
            final double finalValue = CommMath.FormatDouble(clubMemberItem.getToClubMember().getClubMemberBO().getSportsPoint());
            double preCasePointValue = clubMemberItem.getToClubMember().getClubMemberBO().getCaseSportsPoint();
            double preSportPoint = clubMemberItem.getToClubMember().getSportsPoint();
            //被踢出的人比赛分清零
            clubMemberItem.getToClubMember().getClubMemberBO().execSportsPointClear(club.getClubListBO().getUnionId());
            //圈主比赛分增加
            clubMemberItem.getDoClubMember().getClubMemberBO().execSportsPointUpdate(club.getClubListBO().getUnionId(), finalValue, ItemFlow.CLUB_CASE_SPORTS_POINT_TICHU, RoomTypeEnum.CLUB, union.getUnionBO().getOutSports());
            // 比赛分清0添加竞技动态
            UnionDynamicBO.insertCaseSportsRecord(sharePlayer.getPlayerBO().getId(), pid, findPIDAdd.getClubId(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_CASE_SPORTS_POINT_TICHU.value(), club.getClubListBO().getUnionId(), String.valueOf(finalValue), String.valueOf(clubMemberItem.getDoClubMember().getClubMemberBO().getCaseSportsPoint()), String.valueOf(clubMemberItem.getDoClubMember().getClubMemberBO().getSportsPoint()), ""
                    , String.valueOf(preCasePointValue), String.valueOf(clubMemberItem.getToClubMember().getClubMemberBO().getCaseSportsPoint()), String.valueOf(-preCasePointValue), String.valueOf(finalValue), String.valueOf(preSportPoint), String.valueOf(preCasePointValue));
        }
        if (sharePlayer.getRoomInfo().getRoomId() > 0L && sharePlayer.getRoomInfo().getClubId() == findPIDAdd.getClubId()) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_ROOM_ERROR, "CLUB_MEMBER_ROOM_ERROR");
        }
        clubMemberItem.getToClubMember().setStatus(Club_Player_Status.PLAYER_TICHU.value(), pid);
        return SData_Result.make(ErrorCode.Success, clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
    }


    /**
     * 执行比赛分
     *
     * @param req
     * @param exePid 操作Pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result clubSubordinateLevelSportsPoint(CClub_SportsPointUpdate req, long exePid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (club.getClubListBO().getUnionId() <= 0L) {
            return SData_Result.make(ErrorCode.CLUB_NOT_JOIN_UNION, "CLUB_NOT_JOIN_UNION");
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
        }
        SData_Result result = checkExistPromotionSubordinateLevel(req.getClubId(), req.getOpPid(), exePid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        return SData_Result.make(ErrorCode.Success, new ClubMemberSportsPointItem(clubMemberItem.getToClubMember().getSportsPoint(), clubMemberItem.getDoClubMember().getAllowSportsPoint()));
    }

    /**
     * 执行比赛分
     *
     * @param req
     * @param exePid 操作Pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result clubMemberSportsPointInfo(CClub_SportsPointExamine req, long exePid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (club.getClubListBO().getUnionId() <= 0L) {
            return SData_Result.make(ErrorCode.CLUB_NOT_JOIN_UNION, "CLUB_NOT_JOIN_UNION");
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
        }
        ClubMember toClubMember = getClubMember(req.getClubId(), req.getOpPid());
        ClubMember doClubMember = getClubMember(req.getExeClubId() > 0 ? req.getExeClubId() : req.getClubId(), exePid);
        if (Objects.isNull(toClubMember) || Objects.isNull(doClubMember)) {
            // 不是下属成员
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        return SData_Result.make(ErrorCode.Success, new ClubMemberSportsPointItem(toClubMember.getSportsPoint(), doClubMember.getAllowSportsPoint()));
    }

    /**
     * 执行比赛分
     *
     * @param req
     * @param exePid 操作Pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result clubSubordinateLevelSportsPointUpdate(CClub_SportsPointUpdate req, long exePid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (club.getClubListBO().getUnionId() <= 0L) {
            return SData_Result.make(ErrorCode.CLUB_NOT_JOIN_UNION, "CLUB_NOT_JOIN_UNION");
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
        }
        SData_Result result = checkExistPromotionSubordinateLevel(req.getClubId(), req.getOpPid(), exePid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();

        UnionDefine.UNION_SPORTS_POINT sportsPoint = UnionDefine.UNION_SPORTS_POINT.valueOf(req.getType());
        if (req.getValue() <= 0D || (req.getOpPid() == exePid && UnionDefine.UNION_TYPE.NORMAL.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType())))) {
            if (UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_MINUS.equals(sportsPoint)) {
                req.setValue(Math.abs(req.getValue()));
                sportsPoint = UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_ADD;
            } else {
                return SData_Result.make(ErrorCode.NotAllow, "execSportsPointUpdate value:+" + req.getValue());
            }
        }
        double pidValue;
        if (UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_ADD.equals(sportsPoint)) {
            pidValue = req.getValue();
            // 比赛分增加
            result = execSportsPointAdd(req, club.getClubListBO().getUnionId(), exePid, club.getClubListBO().getId(), union.getUnionBO().getOutSports(), ItemFlow.PROMOTION_LEVEL_SPORTS_POINT_CHANGE);
        } else {
            pidValue = -req.getValue();
            // 比赛分减少
            result = execSportsPointMinus(req, club.getClubListBO().getUnionId(), exePid, club.getClubListBO().getId(), union.getUnionBO().getOutSports(), ItemFlow.PROMOTION_LEVEL_SPORTS_POINT_CHANGE);
        }
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 比赛分增加/减少
        UnionDefine.UNION_EXEC_TYPE itemFlow = UnionDefine.UNION_EXEC_TYPE.getUnionExecTypeGaneral(clubMemberItem.getToClubMember().isLevelPromotion(), req.getType() <= 0);
        // 修改前后值
        UnionSportsPointItem item = (UnionSportsPointItem) result.getData();
        // 比赛分值修改
        UnionDynamicBO.insertSportsPoint(req.getOpPid(), club.getClubListBO().getUnionId(), req.getClubId(), exePid, req.getClubId(), CommTime.nowSecond(),
                itemFlow.value(), String.valueOf(req.getValue()), 1, String.valueOf(item.getCurValue()), String.valueOf(item.getPreValue()), String.valueOf(item.getPidCurValue()),
                String.valueOf(item.getPidPreValue()), String.valueOf(pidValue), String.valueOf(item.getExecPidCurValue()), String.valueOf(item.getExecPidPreValue()), String.valueOf(-pidValue));
        //如果不是直属上级操作  直属上级那边也推送一条消息
        if (clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId() != clubMemberItem.getDoClubMember().getClubMemberBO().getId()) {
            //找到直属上级
            ClubMember upMember = ShareClubMemberMgr.getInstance().getClubMember(clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId());
            if (Objects.nonNull(upMember)) {
                Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getDoClubMember().getClubMemberBO().getPlayerID());
                Player playerTo = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
                if (Objects.nonNull(player) && Objects.nonNull(playerTo)) {
                    //不能加字段  把没用的 曲线救国
                    // pidPreValue 存pid
                    // execPidPreValue存 name
                    UnionDynamicBO.insertSportsPoint(upMember.getClubMemberBO().getPlayerID(), club.getClubListBO().getUnionId(), req.getClubId(), 0, req.getClubId(), CommTime.nowSecond(),
                            UnionDefine.UNION_EXEC_TYPE.getUnionExecTypeGaneralKuaJi(req.getType() <= 0).value(), String.valueOf(req.getValue()), 0, String.valueOf(item.getCurValue()), String.valueOf(item.getPreValue()), String.valueOf(item.getPidCurValue()),
                            String.valueOf(playerTo.getPid()), String.valueOf(pidValue), String.valueOf(playerTo.getName()), String.valueOf(item.getExecPidPreValue()), String.valueOf(-pidValue), player.getName(), String.valueOf(player.getPid()));
                }
            }
        }
        return SData_Result.make(ErrorCode.Success, CUnion_ChangeSportPoint.make(req.getType(), req.getValue(), item.getPidCurValue()));
    }

    /**
     * 添加下属
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    public SData_Result getClubSubordinateLevelPidAdd(CClub_FindPIDAdd findPIDAdd, long pid) {
        ClubMember levelPromotion = getClubMember(findPIDAdd.getClubId(), pid);
        if (Objects.isNull(levelPromotion) || (levelPromotion.isNotLevelPromotion())) {
            if (levelPromotion.isPromotionManage()) {
                return getClubSubordinateLevelPidAddForUpLevel(findPIDAdd, pid);
            }
            // 不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }

        if (levelPromotion.getClubMemberBO().getInvite() == 0) {
            // 没有邀请权限
            return SData_Result.make(ErrorCode.CLUB_NOT_INVITE, "CLUB_NOT_PROMOTION");
        }
        if (findPIDAdd.getPid() == pid || isLevelPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            // 是推广员
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        Player player = PlayerMgr.getInstance().getPlayer(findPIDAdd.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        // 是否亲友圈成员
        ClubMember member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
        if (Objects.isNull(member)) {
            if (UnionMgr.getInstance().getUnionMemberMgr().checkExistInUnionOtherClub(findPIDAdd.getClubId(), player.getPid())) {
                // 同赛事不同亲友圈不能重复拉人
                return SData_Result.make(ErrorCode.CLUB_PLAYER_EXIT_IN_OTHER_UNION, "onJoinClub CLUB_MEMBER_UPPER_LIMIT");
            }
            SData_Result result = ClubMember.checkExistJoinOrQuitTimeLimit(player.getPid(), findPIDAdd.getClubId(), Club_Player_Status.PLAYER_JIARU.value(), true);
            if (!ErrorCode.Success.equals(result.getCode())) {
                return result;
            }
            if (!this.onInsertClubMember(player, findPIDAdd.getClubId(), Club_define.Club_PROMOTION.NOT.ordinal(), pid, Club_Player_Status.PLAYER_JIARU.value(), pid, 0, levelPromotion.getId())) {
                return SData_Result.make(ErrorCode.CLUB_INVITATION_ERROR, "CLUB_INVITATION_ERROR");
            }
        } else {
            return SData_Result.make(ErrorCode.CLUB_PARTNER_EXIST, "CLUB_PARTNER_EXIST");
        }
//        //区间信息分成初始化
//        if(levelPromotion.isSectionShare()){
//            member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
//            Club club=ClubMgr.getInstance().getClubListMgr().findClub(member.getClubID());
//            if(Objects.nonNull(member)&&Objects.nonNull(club)){
//                member.initPromotionSection(club.clubListBO.getUnionId(),false);
//            }
//        }
        return SData_Result.make(ErrorCode.Success);

    }

    /**
     * 添加下属
     * 推广员管理员给上级添加
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    public SData_Result getClubSubordinateLevelPidAddForUpLevel(CClub_FindPIDAdd findPIDAdd, long pid) {
        ClubMember levelPromotion = getClubMember(findPIDAdd.getClubId(), pid);
        if (!levelPromotion.isPromotionManage()) {
            // 不是推广员管理
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_isPromotionManage");
        }
        ClubMember upClubMember;
        if (Config.isShare()) {
            upClubMember = ShareClubMemberMgr.getInstance().getClubMember(levelPromotion.getClubMemberBO().getUpLevelId());
        } else {
            upClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(levelPromotion.getClubMemberBO().getUpLevelId());
        }
        if (upClubMember.getClubMemberBO().getInvite() == 0) {
            // 没有邀请权限
            return SData_Result.make(ErrorCode.CLUB_NOT_INVITE, "CLUB_NOT_PROMOTION");
        }
        if (findPIDAdd.getPid() == pid || isLevelPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            // 是推广员
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        Player player = PlayerMgr.getInstance().getPlayer(findPIDAdd.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        // 是否亲友圈成员
        ClubMember member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
        if (Objects.isNull(member)) {
            if (UnionMgr.getInstance().getUnionMemberMgr().checkExistInUnionOtherClub(findPIDAdd.getClubId(), player.getPid())) {
                // 同赛事不同亲友圈不能重复拉人
                return SData_Result.make(ErrorCode.CLUB_PLAYER_EXIT_IN_OTHER_UNION, "onJoinClub CLUB_MEMBER_UPPER_LIMIT");
            }
            SData_Result result = ClubMember.checkExistJoinOrQuitTimeLimit(player.getPid(), findPIDAdd.getClubId(), Club_Player_Status.PLAYER_JIARU.value(), true);
            if (!ErrorCode.Success.equals(result.getCode())) {
                return result;
            }
            if (!this.onInsertClubMember(player, findPIDAdd.getClubId(), Club_define.Club_PROMOTION.NOT.ordinal(), pid, Club_Player_Status.PLAYER_JIARU.value(), pid, 0, upClubMember.getId())) {
                return SData_Result.make(ErrorCode.CLUB_INVITATION_ERROR, "CLUB_INVITATION_ERROR");
            }
        } else {
            return SData_Result.make(ErrorCode.CLUB_PARTNER_EXIST, "CLUB_PARTNER_EXIST");
        }

        return SData_Result.make(ErrorCode.Success);

    }

    /**
     * 查询下属
     *
     * @param findPIDAdd 参数
     * @param pid        玩家Pid
     * @return
     */
    public SData_Result getClubSubordinateLevelPidInfo(CClub_FindPIDAdd findPIDAdd, long pid) {
        ClubMember doMember = getClubMember(findPIDAdd.getClubId(), pid);
        if (isNotLevelPromotion(findPIDAdd.getClubId(), pid) && !doMember.isPromotionManage()) {
            // 不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
        }
        if (findPIDAdd.getPid() == pid || isLevelPromotion(findPIDAdd.getClubId(), findPIDAdd.getPid())) {
            // 是推广员
            return SData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "CLUB_EXIST_PROMOTION");
        }
        int type = 0;
        ClubMember member = getClubMember(findPIDAdd.getClubId(), findPIDAdd.getPid());
        if (Objects.nonNull(member)) {
            // 已加入亲友圈
            type = 1;
            if (member.getClubMemberBO().getUpLevelId() > 0L) {
                // 已经绑定了推广员
                type = 2;
            }
        }
        Player player = PlayerMgr.getInstance().getPlayer(findPIDAdd.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionPidInfo(player.getShortPlayer(), type));
    }


    /**
     * 更新亲友圈成员上级推广绑定
     *
     * @param uid  成员id
     * @param puid 上级成员Id
     */
    public void updateClubMemberUpLevelId(long clubId, long uid, long puid) {
        Map<Long, ClubMember> clubMemberMap;
        if (Config.isShare()) {
            clubMemberMap = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId);
        } else {
            clubMemberMap = this.getClubMemberMap();
        }
        if (checkUpLevelIdClubCreate(puid)) {
            clubMemberMap.values().stream().filter(k -> k.getClubMemberBO().getUpLevelId() == uid).forEach(k -> {
                if (k.getClubMemberBO().getLevel() <= 0) {
                    k.getClubMemberBO().saveLevelAndUpLevelId(0, 0L);
                    k.getClubMemberBO().savePromotionManage(0);//推广员管理状态重置为0
                    // 删除绑定并且变更下属的绑定
                    DispatcherComponent.getInstance().publish(new PromotionLevelDeleteEvent(k.getClubID(), k.getId(), k.getId(), Club_define.Club_PROMOTION_LEVEL_DELETE.CANCEL_TOP_LEVEL, CommTime.getNowTimeStringYMD()));
                } else {
                    k.getClubMemberBO().saveUpLevelId(puid);
                    k.getClubMemberBO().savePromotionManage(0);//推广员管理状态重置为0
                }
            });
        } else {
            clubMemberMap.values().stream().filter(k -> k.getClubMemberBO().getUpLevelId() == uid).forEach(k -> {
                k.getClubMemberBO().saveUpLevelId(puid);
                k.getClubMemberBO().savePromotionManage(0);//推广员管理状态重置为0
            });
        }
    }


    /**
     * 更新亲友圈成员上级推广绑定
     *
     * @param uid  成员id
     * @param puid 上级成员Id
     */
    public void updateClubMemberUpLevelIdTest(long uid, long puid) {
        this.getClubMemberMap().values().stream().filter(k -> k.getClubMemberBO().getUpLevelId() == uid).forEach(k -> k.getClubMemberBO().saveUpLevelId(puid));
    }

    /**
     * 更新亲友圈成员上级推广绑定
     *
     * @param pid  成员id
     * @param puid 上级成员Id
     */
    public void updateClubMemberUpLevelIdByPid(long pid, long puid) {
        if (Config.isShare()) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(pid);
            clubMember.getClubMemberBO().saveUpLevelId(puid);
        } else {
            this.getClubMemberMap().values().stream().filter(k -> k.getClubMemberBO().getId() == pid).forEach(k -> k.getClubMemberBO().saveUpLevelId(puid));
        }
    }

    /**
     * 执行推广员活跃报表记录
     */
    public void execPromotionLevel(String dateTime) {
        if (Config.isShare()) {
            Long startTime = System.currentTimeMillis();
            this.getClubMemberMap().values().stream().filter(c -> c.getStatus(Club_Player_Status.PLAYER_JIARU.value())).forEach(c -> {
                ClubMember k = ShareClubMemberMgr.getInstance().getClubMember(c.getId());
                if (Objects.nonNull(k)) {
                    double sportsPoint = k.getSportsPoint();
                    Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getClubID());
                    FlowLogger.clubLevelRoomLog(dateTime, k.getClubMemberBO().getPlayerID(), 0, 0, 0L, k.getClubMemberBO().getUpLevelId(), k.getId(), 0, sportsPoint, 0, 0D, 0D, 0D, k.getClubID(), club.getClubListBO().getUnionId(), 0D, 0l);
                    if (k.isClubCreate()) {
                        FlowLogger.unionLevelRoomCountLog(dateTime, k.getClubID(), club.getClubListBO().getUnionId(), clubPeopleNum(k.getClubID()), String.valueOf(UnionDefine.UNION_SHARE_TYPE.FIXED.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(k.getClubMemberBO().getShareType())) ? k.getClubMemberBO().getShareFixedValue() : k.getClubMemberBO().getShareValue() + "%"), k.getClubMemberBO().getScorePoint(), sportsPoint);
                    }
                }
            });
            CommLogD.info("execPromotionLevel time:{}", System.currentTimeMillis() - startTime);
//
//            ShareClubMemberMgr.getInstance().getAllClubMember().values().stream().filter(k -> k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).forEach(k -> {
//                double sportsPoint = k.getSportsPoint();
//                FlowLogger.clubLevelRoomLog(dateTime, k.getClubMemberBO().getPlayerID(), 0, 0, 0L, k.getClubMemberBO().getUpLevelId(), k.getId(), 0, sportsPoint, 0, 0D, 0D, 0D, k.getClubID(), 0D);
//                if (k.isClubCreate()) {
//                    FlowLogger.unionLevelRoomCountLog(dateTime, k.getClubID(), clubPeopleNum(k.getClubID()), String.valueOf(UnionDefine.UNION_SHARE_TYPE.FIXED.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(k.getClubMemberBO().getShareType())) ? k.getClubMemberBO().getShareFixedValue() : k.getClubMemberBO().getShareValue() + "%"), k.getClubMemberBO().getScorePoint(), sportsPoint);
//                }
//            });
        } else {
            this.getClubMemberMap().values().stream().filter(k -> k.getStatus(Club_Player_Status.PLAYER_JIARU.value())).forEach(k -> {
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getClubID());
                double sportsPoint = k.getSportsPoint();
                FlowLogger.clubLevelRoomLog(dateTime, k.getClubMemberBO().getPlayerID(), 0, 0, 0L, k.getClubMemberBO().getUpLevelId(), k.getId(), 0, sportsPoint, 0, 0D, 0D, 0D, k.getClubID(), club.getClubListBO().getUnionId(), 0D, 0l);
                if (k.isClubCreate()) {
                    FlowLogger.unionLevelRoomCountLog(dateTime, k.getClubID(), club.getClubListBO().getUnionId(), clubPeopleNum(k.getClubID()), String.valueOf(UnionDefine.UNION_SHARE_TYPE.FIXED.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(k.getClubMemberBO().getShareType())) ? k.getClubMemberBO().getShareFixedValue() : k.getClubMemberBO().getShareValue() + "%"), k.getClubMemberBO().getScorePoint(), sportsPoint);
                }
            });
        }
    }


    /**
     * 获取推广员活跃报表
     */
    public SData_Result getPromotionLevelReportForm(CClub_FindPIDAdd findPIDAdd, long pid) {
        SData_Result result = this.clubLevelPromotionPowerItem(findPIDAdd.getClubId(), findPIDAdd.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubLevelPromotionPowerItem clubLevelPromotionPowerItem = (ClubLevelPromotionPowerItem) result.getData();
        List<ClubPromotionLevelReportFormItem> clubPromotionLevelReportFormItemList = null;
        if (Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(clubLevelPromotionPowerItem.getLevelPower())) {
            clubPromotionLevelReportFormItemList = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findAllE(Restrictions.and(Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.le("date_time", CommTime.getYesterDayStringYMD(1))).groupBy("dateTime").descFormat("dateTime").setLimit(7), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
        } else {
            if (CollectionUtils.isEmpty(clubLevelPromotionPowerItem.getUidList())) {
                return SData_Result.make(ErrorCode.Success, Collections.emptyList());
            }
            clubPromotionLevelReportFormItemList = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findAllE(Restrictions.and(Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.in("memberId", clubLevelPromotionPowerItem.getUidList()), Restrictions.le("date_time", CommTime.getYesterDayStringYMD(1))).groupBy("dateTime").descFormat("dateTime").setLimit(7), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
        }
        if (CollectionUtils.isNotEmpty(clubPromotionLevelReportFormItemList)) {
            //查看哪个人的报表
            ClubMember exeClubmeber = ClubMgr.getInstance().getClubMemberMgr().find(findPIDAdd.clubId, findPIDAdd.pid);
            for (ClubPromotionLevelReportFormItem formItem : clubPromotionLevelReportFormItemList) {
                int roomSize = 0;
                ClubRoomSizeItem clubRoomSizeItem;
                //自己参与的数据 
                ClubRoomSizeItem clubRoomSizeItemSelf = null;
                if (Objects.isNull(exeClubmeber) || exeClubmeber.isClubCreate()) {
                    //圈主就是整个亲友圈的
                    clubRoomSizeItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", formItem.getDateTime()), Restrictions.eq("clubId", findPIDAdd.getClubId())), ClubRoomSizeItem.class, ClubRoomSizeItem.getItemsName());
                } else if (exeClubmeber.isLevelPromotion()) {
                    //推广员就看他下面的
                    clubRoomSizeItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", formItem.getDateTime()), Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.eq("upLevelId", exeClubmeber.getId())), ClubRoomSizeItem.class, ClubRoomSizeItem.getItemsName());
                    clubRoomSizeItemSelf = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", formItem.getDateTime()), Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.eq("memberId", exeClubmeber.getId())), ClubRoomSizeItem.class, ClubRoomSizeItem.getItemsName());
                } else if (exeClubmeber.isPromotionManage()) {
                    //推广员管理就看他上级下面的
                    clubRoomSizeItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", formItem.getDateTime()), Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.eq("upLevelId", exeClubmeber.getClubMemberBO().getUpLevelId())), ClubRoomSizeItem.class, ClubRoomSizeItem.getItemsName());
                } else {
                    //普通成员就看他自己的
                    clubRoomSizeItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", formItem.getDateTime()), Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.eq("memberId", exeClubmeber.getId())), ClubRoomSizeItem.class, ClubRoomSizeItem.getItemsName());
                }
                if (Objects.nonNull(clubRoomSizeItem)) {
                    roomSize = clubRoomSizeItem.getRoomSize();
                }
                if (Objects.nonNull(clubRoomSizeItemSelf)) {
                    roomSize += clubRoomSizeItemSelf.getRoomSize();
                }
                formItem.setTable(roomSize);
                //成员总积分和(比赛里输赢积分-比赛分消耗)
                formItem.setZhongZhiTotalPoint(CommMath.subDouble(formItem.getSportsPointConsume(), formItem.getActualEntryFee()));
                //最终积分(总积分+活跃积分总和)
                formItem.setZhongZhiFinalTotalPoint(CommMath.addDouble(formItem.getZhongZhiTotalPoint(), formItem.getPromotionShareValue()));
            }
        }
        return SData_Result.make(ErrorCode.Success, CollectionUtils.isEmpty(clubPromotionLevelReportFormItemList) ? Collections.emptyList() : clubPromotionLevelReportFormItemList);
    }

    /**
     * 获取推广员活跃报表
     */
    public SData_Result getPromotionLevelReportFormZhongZhi(CClub_FindPIDAdd findPIDAdd, long pid) {
        SData_Result result = this.clubLevelPromotionPowerItem(findPIDAdd.getClubId(), findPIDAdd.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubLevelPromotionPowerItem clubLevelPromotionPowerItem = (ClubLevelPromotionPowerItem) result.getData();
        List<ClubPromotionLevelReportFormItem> clubPromotionLevelReportFormItemList = null;
        if (Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(clubLevelPromotionPowerItem.getLevelPower())) {
            clubPromotionLevelReportFormItemList = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findAllE(Restrictions.and(Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.le("date_time", CommTime.getYesterDay6ByCount(1))).groupBy("dateTime").descFormat("dateTime").setLimit(7), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
        } else {
            if (CollectionUtils.isEmpty(clubLevelPromotionPowerItem.getUidList())) {
                return SData_Result.make(ErrorCode.Success, Collections.emptyList());
            }
            clubPromotionLevelReportFormItemList = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findAllE(Restrictions.and(Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.in("memberId", clubLevelPromotionPowerItem.getUidList()), Restrictions.le("date_time", CommTime.getYesterDay6ByCount(1))).groupBy("dateTime").descFormat("dateTime").setLimit(7), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
        }
        if (CollectionUtils.isNotEmpty(clubPromotionLevelReportFormItemList)) {
            //查看哪个人的报表
            ClubMember exeClubmeber = ClubMgr.getInstance().getClubMemberMgr().find(findPIDAdd.clubId, findPIDAdd.pid);
            for (ClubPromotionLevelReportFormItem formItem : clubPromotionLevelReportFormItemList) {
                int roomSize = 0;
                ClubRoomSizeItem clubRoomSizeItem;
                //自己参与的数据
                ClubRoomSizeItem clubRoomSizeItemSelf = null;
                if (Objects.isNull(exeClubmeber) || exeClubmeber.isClubCreate()) {
                    //圈主就是整个亲友圈的
                    clubRoomSizeItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", formItem.getDateTime()), Restrictions.eq("clubId", findPIDAdd.getClubId())), ClubRoomSizeItem.class, ClubRoomSizeItem.getItemsName());
                } else if (exeClubmeber.isLevelPromotion()) {
                    //推广员就看他下面的
                    clubRoomSizeItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", formItem.getDateTime()), Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.eq("upLevelId", exeClubmeber.getId())), ClubRoomSizeItem.class, ClubRoomSizeItem.getItemsName());
                    clubRoomSizeItemSelf = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", formItem.getDateTime()), Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.eq("memberId", exeClubmeber.getId())), ClubRoomSizeItem.class, ClubRoomSizeItem.getItemsName());
                } else if (exeClubmeber.isPromotionManage()) {
                    //推广员管理就看他上级下面的
                    clubRoomSizeItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", formItem.getDateTime()), Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.eq("upLevelId", exeClubmeber.getClubMemberBO().getUpLevelId())), ClubRoomSizeItem.class, ClubRoomSizeItem.getItemsName());
                } else {
                    //普通成员就看他自己的
                    clubRoomSizeItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", formItem.getDateTime()), Restrictions.eq("clubId", findPIDAdd.getClubId()), Restrictions.eq("memberId", exeClubmeber.getId())), ClubRoomSizeItem.class, ClubRoomSizeItem.getItemsName());
                }
                if (Objects.nonNull(clubRoomSizeItem)) {
                    roomSize = clubRoomSizeItem.getRoomSize();
                }
                if (Objects.nonNull(clubRoomSizeItemSelf)) {
                    roomSize += clubRoomSizeItemSelf.getRoomSize();
                }
                formItem.setDateTime(CommTime.getSecToYMDStr(Integer.parseInt(formItem.getDateTime())));
                formItem.setTable(roomSize);
                //成员总积分和(比赛里输赢积分-比赛分消耗)
                formItem.setZhongZhiTotalPoint(CommMath.subDouble(formItem.getSportsPointConsume(), formItem.getActualEntryFee()));
                //最终积分(总积分+活跃积分总和)
                formItem.setZhongZhiFinalTotalPoint(CommMath.addDouble(formItem.getZhongZhiTotalPoint(), formItem.getPromotionShareValue()));
            }
        }
        return SData_Result.make(ErrorCode.Success, CollectionUtils.isEmpty(clubPromotionLevelReportFormItemList) ? Collections.emptyList() : clubPromotionLevelReportFormItemList);
    }

    /**
     * 获取单前玩家的所有下属 包括下属的下属
     *
     * @param clubId
     * @param toPid
     * @param pid
     * @return
     */
    private SData_Result clubLevelPromotionPowerItemIncludeAll(long clubId, long toPid, long pid) {
        ClubMember doClubMember = getClubMember(clubId, pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", clubId, pid);
        }

        // 是推广员或创建者
        long uid;
        if (doClubMember.isNotClubCreate() && doClubMember.isNotLevelPromotion()) {
            // 不是创建者也不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
        }
        // 推广员id列表
        List<Long> uidList = Lists.newArrayList();
        // 操作指定玩家
        ClubMember toClubMember = getClubMember(clubId, toPid);
        if (Objects.isNull(toClubMember)) {
            // 不是下属成员
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER toClubMember isNull clubId:{},Pid:{}", clubId, toPid);
        }
        uid = toClubMember.getId();
        uidList.add(uid);
        if (toClubMember.getClubMemberBO().getUpLevelId() > 0L || toClubMember.isMinister()) {
            List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", uid), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
            if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                // 查询我的所有下线（包括我）：
                uidList.addAll(queryUidOrPuidItemList.stream().map(k -> k.getUid()).collect(Collectors.toList()));
            }
        }
        return SData_Result.make(ErrorCode.Success, new ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL, uidList, uid));
    }

    /**
     * 亲友圈等级推广员权限
     *
     * @param clubId 亲友圈Id
     * @param toPid  被操作玩家Pid
     * @param pid    操作玩家Pid
     * @return
     */
    private SData_Result clubLevelPromotionPowerItem(long clubId, long toPid, long pid) {
        ClubMember doClubMember = getClubMember(clubId, pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", clubId, pid);
        }
        if (doClubMember.isNotClubCreate() && doClubMember.isNotLevelPromotion()) {
            if (!doClubMember.isPromotionManage()) {
                // 不是创建者也不是推广员
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
            //如果是推广员管理 重新计算权限
            if (Config.isShare()) {
                doClubMember = ShareClubMemberMgr.getInstance().getClubMember(doClubMember.getClubMemberBO().getUpLevelId());
            } else {
                doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(doClubMember.getClubMemberBO().getUpLevelId());
            }
            if (Objects.isNull(doClubMember)) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
        }
        // 是推广员或创建者
        long uid = doClubMember.getId();
        // 推广员id列表
        List<Long> uidList = Lists.newArrayList();
        if (doClubMember.isClubCreate()) {
            // 亲友圈创建者操作：
            if (toPid == pid) {
                // 操作自己本身（查看自己包括自己下线(不包括推广员以及推广员下属)）
                uidList.addAll(getClubCreateSubordinateLevelIdList(clubId));
                uidList.add(0L);
                return SData_Result.make(ErrorCode.Success, new ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER.CREATE, uidList, uid));
            } else if (toPid > 0) {
                // 操作指定玩家
                ClubMember toClubMember = getClubMember(clubId, toPid);
                if (Objects.isNull(toClubMember)) {
                    // 不是下属成员
                    return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER toClubMember isNull clubId:{},Pid:{}", clubId, toPid);
                }
                uid = toClubMember.getId();
                uidList.add(uid);
                if (toClubMember.getClubMemberBO().getUpLevelId() > 0L) {
                    List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", uid), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
                    if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                        // 查询我的所有下线（包括我）：
                        uidList.addAll(queryUidOrPuidItemList.stream().map(k -> k.getUid()).collect(Collectors.toList()));
                    }
                }
                return SData_Result.make(ErrorCode.Success, new ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL, uidList, uid));
            } else {
                // 亲友圈创造者操作所有人
                return SData_Result.make(ErrorCode.Success, new ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER.CREATE, uidList, uid));

            }
        } else {
            // 亲友圈推广员操作：
            if (toPid > 0L && toPid != pid) {
                ClubMember toClubMember = getClubMember(clubId, toPid);
                if (Objects.isNull(toClubMember) || toClubMember.getClubMemberBO().getUpLevelId() <= 0L) {
                    // 不是下属成员
                    return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER isLevelPromotion toClubMember isNull clubId:{},Pid:{}", clubId, toPid);
                }
                boolean notExistFindOneE = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).notExistFindOneE(Restrictions.and(Restrictions.eq("uid", toClubMember.getId()), Restrictions.eq("puid", doClubMember.getId())));
                if (notExistFindOneE) {
                    return SData_Result.make(ErrorCode.CLUB_NOT_SUBORDINATE, "CLUB_NOT_SUBORDINATE");
                }
                uid = toClubMember.getId();
                uidList.add(uid);
            } else if (doClubMember.isLevelPromotion()) {
                uid = doClubMember.getId();
                uidList.add(uid);
            } else {
                return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER  clubId:{},toPid:{},doPid:{}", clubId, toPid, pid);
            }
            List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", uid), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
            if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                // 查询我的所有下线（包括我）：
                uidList.addAll(queryUidOrPuidItemList.stream().map(k -> k.getUid()).collect(Collectors.toList()));
            }
            return SData_Result.make(ErrorCode.Success, new ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL, uidList, uid));
        }
    }

    /**
     * 获取某个玩家的备注名称
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result getClubPlayerRemarkNameList(CClub_GetRemarkName req, long pid) {

        List<ClubPlayerRemarkName> clubPlayerRemarkNames = ((ClubPlayerRemarkNameBOService) ContainerMgr.get().getComponent(ClubPlayerRemarkNameBOService.class)).findAllE(Restrictions.eq("pid", pid), ClubPlayerRemarkName.class, ClubPlayerRemarkName.getItemsNameId());
        return SData_Result.make(ErrorCode.Success, clubPlayerRemarkNames);

    }

    /**
     * 增加和改变备注名称
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result changeClubPlayerRemarkName(CClub_GetRemarkName req, long pid) {

        List<ClubPlayerRemarkName> clubPlayerRemarkNames = ((ClubPlayerRemarkNameBOService) ContainerMgr.get().getComponent(ClubPlayerRemarkNameBOService.class)).findAllE(Restrictions.and(Restrictions.eq("pid", pid), Restrictions.eq("remarkID", req.getRemarkID())), ClubPlayerRemarkName.class, ClubPlayerRemarkName.getItemsNameId());
        ClubPlayerRemarkNameBO bo = new ClubPlayerRemarkNameBO();
        if (CollectionUtils.isNotEmpty(clubPlayerRemarkNames)) {//存在的话 就保存
            ClubPlayerRemarkName clubPlayerRemarkName = clubPlayerRemarkNames.get(0);
            bo.setId(clubPlayerRemarkName.getId());
            bo.setPid(clubPlayerRemarkName.getPid());
            bo.setRemarkID(clubPlayerRemarkName.getRemarkID());
            bo.setRemarkName(clubPlayerRemarkName.getRemarkName());
            ContainerMgr.get().getComponent(ClubPlayerRemarkNameBOService.class).update(bo, new AsyncInfo(bo.getId()));
        } else {
            bo.setPid(pid);
            bo.setRemarkID(req.getRemarkID());
            bo.setRemarkName(req.getRemarkName());
            ContainerMgr.get().getComponent(ClubPlayerRemarkNameBOService.class).save(bo);
        }
        return SData_Result.make(ErrorCode.Success, bo);
    }

    /**
     * 修改推官员归属
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result changePromotionBelongZhongZhiDiaoRu(CClub_ChangePromotionBelong req, long pid) {
        req.setType(1);
        return this.changePromotionBelong(req, pid);
    }

    /**
     * 修改推官员归属
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result changePromotionBelong(CClub_ChangePromotionBelong req, long pid) {
        //检查时间  凌晨0点到2点不允许修改从属
        Calendar ca = Calendar.getInstance();
        if (ca.get(Calendar.HOUR_OF_DAY) < 2) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_TIME_ERROR, "CLUB_MEMBER_TIME_ERROR");
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        //特殊处理 修改的人是自己并且不是推广员的时候 剑锋 2021-4-16要求添加
        if (req.getPid() == req.getUpLevelId()) {
            ClubMember newClubMemberSelf = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, req.getPid());
            if (!(newClubMemberSelf.isLevelPromotion() || newClubMemberSelf.isClubCreate())) {
                if (newClubMemberSelf.getClubMemberBO().getRealUpLevelId() == 0) {
                    ClubMember clubCreate = getClubMember(req.getClubId(), club.getOwnerPlayerId());
                    newClubMemberSelf.getClubMemberBO().saveLevelAndUpLevelId(Club_define.Club_PROMOTION_LEVEL.APPOINT.ordinal(), clubCreate.getId());
                    DispatcherComponent.getInstance().publish(new PromotionLevelInsertEvent(newClubMemberSelf.getId(), clubCreate.getId()));
                } else {
                    ClubMember upLevelMember;
                    if (Config.isShare()) {
                        upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(newClubMemberSelf.getClubMemberBO().getUpLevelId());
                    } else {
                        upLevelMember = getClubMemberMap().get(newClubMemberSelf.getClubMemberBO().getUpLevelId());
                    }
                    if (Objects.isNull(upLevelMember)) {
                        // 上级代理不存在
                        return SData_Result.make(ErrorCode.CLUB_UP_LEVEL_MEMBER_NOT_EXIST, "CLUB_UP_LEVEL_MEMBER_NOT_EXIST");
                    }
                    DispatcherComponent.getInstance().publish(new PromotionLevelInsertEvent(newClubMemberSelf.getId(), upLevelMember.getId()));
                    newClubMemberSelf.getClubMemberBO().saveLevel(upLevelMember.getClubMemberBO().getLevel() + 1);
                }
                return SData_Result.make(ErrorCode.Success);
            }
        }
        if (club.isMultiChangePromotionFlag()) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_CHANGE_IS_EXIT, "CLUB_MEMBER_PROMOTION_CHANGE_IS_EXIT");
        }
        SData_Result result = checkExistPromotionSubordinateLevel(req.getClubId(), req.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(clubMemberItem.getLevelPower())) {
            // 检查是否有权限
            if (clubMemberItem.getDoClubMember().getClubMemberBO().getModifyValue() != 1) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "getModifyValue CLUB_NOT_PROMOTION");
            }
        }
        ClubMember newClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, req.getUpLevelId());
        if (Objects.isNull(newClubMember)) {
            if (req.getType() == 0) {
                //执行操作的人
                newClubMember = ClubMgr.getInstance().getClubMemberMgr().findCreate(req.clubId);
                if (Objects.isNull(newClubMember)) {
                    return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
                }
            } else {
                return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "CLUB_NOT_EXIST_MEMBER_INFO");
            }
        }
        if (newClubMember.isClubCreate() && newClubMember.getClubMemberBO().getPlayerID() != pid) {
            return SData_Result.make(ErrorCode.CLUB_PROMOTION_CHANGE_NOT_CLUBCREATE, "CLUB_PROMOTION_CHANGE_NOT_CLUBCREATE");
        }
        if (!(newClubMember.isLevelPromotion() || newClubMember.isClubCreate())) {
            if (newClubMember.getClubMemberBO().getRealUpLevelId() == 0) {
                ClubMember clubCreate = getClubMember(req.getClubId(), club.getOwnerPlayerId());
                newClubMember.getClubMemberBO().saveLevelAndUpLevelId(Club_define.Club_PROMOTION_LEVEL.APPOINT.ordinal(), clubCreate.getId());
                DispatcherComponent.getInstance().publish(new PromotionLevelInsertEvent(newClubMember.getId(), clubCreate.getId()));
            } else {
                ClubMember upLevelMember;
                if (Config.isShare()) {
                    upLevelMember = ShareClubMemberMgr.getInstance().getClubMember(newClubMember.getClubMemberBO().getUpLevelId());
                } else {
                    upLevelMember = getClubMemberMap().get(newClubMember.getClubMemberBO().getUpLevelId());
                }
                if (Objects.isNull(upLevelMember)) {
                    // 上级代理不存在
                    return SData_Result.make(ErrorCode.CLUB_UP_LEVEL_MEMBER_NOT_EXIST, "CLUB_UP_LEVEL_MEMBER_NOT_EXIST");
                }
//                if (upLevelMember.getClubMemberBO().getLevel() >= 3) {
//                    // 上限
//                    return SData_Result.make(ErrorCode.CLUB_PARTNER_UPPER_LIMIT, "CLUB_PARTNER_UPPER_LIMIT");
//                }
                newClubMember.getClubMemberBO().saveLevel(upLevelMember.getClubMemberBO().getLevel() + 1);
            }

            if (!(newClubMember.isLevelPromotion() || newClubMember.isClubCreate())) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION");
            }
        }
        //如果都是百分比的话 进行判断
        if (newClubMember.getClubMemberBO().getShareType() == clubMemberItem.getToClubMember().getClubMemberBO().getShareType()) {
            if (newClubMember.getClubMemberBO().getShareType() == UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal()) {
                if (newClubMember.getClubMemberBO().getShareValue() < clubMemberItem.getToClubMember().getClubMemberBO().getShareValue()) {
                    return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_PERCENT_LESS, "CLUB_MEMBER_PROMOTION_PERCENT_LESS");
                }
            } else {
                if (newClubMember.getClubMemberBO().getShareFixedValue() < clubMemberItem.getToClubMember().getClubMemberBO().getShareFixedValue()) {
                    return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_FIXED_LESS, "CLUB_MEMBER_PROMOTION_FIXED_LESS");
                }
            }
        }
        //查出自己的所有下线
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", clubMemberItem.getToClubMember().getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        // 推广员下线列表
        List<Long> uidList = Lists.newArrayList();
        uidList.add(clubMemberItem.getToClubMember().getId());
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            // 查询所有下线：
            uidList.addAll(queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).map(k -> k.getUid()).collect(Collectors.toList()));
        }
        if (uidList.contains(newClubMember.getId()) || (clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId() == newClubMember.getId() || (newClubMember.isClubCreate() && clubMemberItem.getToClubMember().getClubMemberBO().getUpLevelId() == 0))) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_BELONG, "CLUB_MEMBER_PROMOTION_BELONG");
        }
        club.setMultiChangePromotionFlag(true);
        //共享亲友圈更新
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateField(club, "multiChangePromotionFlag");
        }
        DispatcherComponent.getInstance().publish(new PromotionLevelChangeEvent(req.getClubId(), req.getPid(), newClubMember.getClubMemberBO().getPlayerID(), CommTime.getNowTimeStringYMD(), uidList, pid, club.getClubListBO().getUnionId()));
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 获取上级推广员信息
     */
    public SData_Result getUplevelPromotion(CClub_FindPIDAdd req, long pid) {
        SData_Result result = checkExistPromotionSubordinateLevel(req.getClubId(), req.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        if (Club_define.Club_PROMOTION_LEVEL_POWER.PROMOTION_LEVEL.equals(clubMemberItem.getLevelPower())) {
            // 检查是否有权限
            if (clubMemberItem.getDoClubMember().getClubMemberBO().getModifyValue() != 1) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "getModifyValue CLUB_NOT_PROMOTION");
            }
        }

        //执行操作的人
        ClubMember execclubMember = ClubMgr.getInstance().getClubMemberMgr().findCreate(req.clubId);
        if (Objects.isNull(execclubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "CLUB_NOT_EXIST_MEMBER_INFO");
        }
        ClubMember changeClubMember = clubMemberItem.getToClubMember();
        if (Objects.isNull(changeClubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "CLUB_NOT_EXIST_MEMBER_INFO");
        }
        Player player = null;
        ClubMember upClubMember;
        if (Config.isShare()) {
            upClubMember = ShareClubMemberMgr.getInstance().getClubMember(changeClubMember.getClubMemberBO().getUpLevelId());
        } else {
            upClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(changeClubMember.getClubMemberBO().getUpLevelId());
        }
        if (Objects.isNull(upClubMember)) {//如果为空的时候说明是创建者名下
            player = PlayerMgr.getInstance().getPlayer(execclubMember.getClubMemberBO().getPlayerID());
        } else {
            player = PlayerMgr.getInstance().getPlayer(upClubMember.getClubMemberBO().getPlayerID());
        }
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionPidInfo(player.getShortPlayer(), 0));

    }

    /**
     * 检查推广员下的 推广员管理数量是否超出
     *
     * @param clubId
     * @param selfMember
     * @return
     */
    public boolean checkPromotionMinisterUpperLimit(long clubId, ClubMember selfMember) {
        //查出自己的所有下线
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", selfMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        // 推广员下线列表
        List<Long> uidList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            // 查询所有下线：
            uidList.addAll(queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId).reversed()).map(k -> k.getUid()).collect(Collectors.toList()));
        }
        if (Config.isShareLocal()) {
            long promotionManageNum = this.findClubIdAllClubMember(clubId, Club_Player_Status.PLAYER_JIARU.value()).stream().filter(k -> k.getClubMemberBO().getPromotionManage() == 1 && k.getClubMemberBO().getUpLevelId() == selfMember.getId()).count();
            return promotionManageNum >= GameConfig.ClubMinisterUpperLimit();
        } else {
            long promotionManageNum = ShareClubMemberMgr.getInstance().getAllOneClubMemberArray(clubId).values().stream().map(k -> ClubMemberUtils.stringSwitchArray(k)).filter(k -> ClubMemberUtils.getSourceStatus(ClubMemberUtils.getArrayValueInteger(k, "status"), Club_Player_Status.PLAYER_JIARU.value()) && ClubMemberUtils.getArrayValueInteger(k, "promotionManage") == 1 && ClubMemberUtils.getArrayValueLong(k, "upLevelId") == selfMember.getId()).count();
            return promotionManageNum >= GameConfig.ClubMinisterUpperLimit();
        }

    }


    /**
     * 检查成员是否需要强制绑定手机号
     *
     * @return
     */
    public boolean checkClubMemberForcePhone(long pid) {
        if (Config.isShare()) {
            List<ClubMemberBO> list = clubMemberBOService.findAll(Restrictions.eq("playerID", pid));
            return list.parallelStream().anyMatch(k -> Club_define.Club_MINISTER.Club_MINISTER_CREATER.value() == k.getIsminister() || k.getLevel() > 0);
        } else {
            return this.getClubMemberMap().values().parallelStream().anyMatch(k -> k.getClubMemberBO().getPlayerID() == pid && (k.isClubCreate() || k.isLevelPromotion()));
        }
    }


    /**
     * 亲友圈推广员等级分成权限
     *
     * @param clubId 亲友圈Id
     * @param opPid  被操作者Pid
     * @param pid    操作者Pid
     * @return
     */
    private SData_Result clubPromotionLevelShareChangePower(long clubId, long opPid, long pid) {
        ClubMember doClubMember = getClubMember(clubId, pid);
        if (Objects.isNull(doClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER doClubMember ClubId:{},Pid:{}", clubId, pid);
        }
        if (doClubMember.isNotClubCreate() && doClubMember.isNotLevelPromotion()) {
            if (!doClubMember.isPromotionManage()) {
                // 不是创建者也不是推广员
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
            //如果是推广员管理 重新计算权限
            if (Config.isShare()) {
                doClubMember = ShareClubMemberMgr.getInstance().getClubMember(doClubMember.getClubMemberBO().getUpLevelId());
            } else {
                doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(doClubMember.getClubMemberBO().getUpLevelId());
            }
            if (Objects.isNull(doClubMember)) {
                return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
            }
        }

        ClubMember exeClubMember = getClubMember(clubId, opPid);
        if (Objects.isNull(exeClubMember)) {
            // 成员不存在
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER exeClubMember ClubId:{},Pid:{}", clubId, opPid);
        }
        if (exeClubMember.isNotLevelPromotion()) {
            // 不是推广员
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "CLUB_NOT_PROMOTION doClubMember ClubId:{},Pid:{}", clubId, pid);
        }

        if (exeClubMember.getClubMemberBO().getUpLevelId() != doClubMember.getClubMemberBO().getId()) {
            return SData_Result.make(ErrorCode.CLUB_UP_LEVEL_MEMBER_NOT_EXIST, "CLUB_NOTCLUBMEMBER exeClubMember not promotionList ClubId:{},Pid:{}", clubId, opPid);
        }
        return SData_Result.make(ErrorCode.Success, new ClubMemberItem(exeClubMember, doClubMember, Club_define.Club_PROMOTION_LEVEL_POWER.CREATE));
    }

    /**
     * 活跃计算批量
     *
     * @param promotionActive
     * @param pid
     * @return
     */
    public SData_Result CClubPromotionLevelShareChangeBatch(CClub_PromotionCalcActiveBatch promotionActive, long pid) {
        SData_Result result = clubPromotionLevelShareChangePower(promotionActive.getClubId(), promotionActive.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        // 分成类型
        int shareType = UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal();
        // 下级分成类型
        int lowerLevelShareType = UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal();
        // 上级分成类型
        int upLevelShareType = clubMemberItem.getDoClubMember().getClubMemberBO().getShareType();
        double maxValue = 0D;
        double maxFixedValue = 0D;
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", clubMemberItem.getToClubMember().getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        if (Objects.nonNull(queryUidOrPidItemList) && !queryUidOrPidItemList.isEmpty()) {
            for (QueryUidOrPuidItem queryUidOrPuidItem : queryUidOrPidItemList) {
                ClubMember promotionMember;
                if (Config.isShare()) {
                    promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem.getUid());
                } else {
                    promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem.getUid());
                }
                if (Objects.isNull(promotionMember)) {
                    continue;
                }
                lowerLevelShareType = lowerLevelShareType != UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal() ? promotionMember.getClubMemberBO().getShareType() : lowerLevelShareType;

                if (promotionMember.getClubMemberBO().getShareValue() > maxValue) {
                    maxValue = promotionMember.getClubMemberBO().getShareValue();
                }
                if (promotionMember.getClubMemberBO().getShareFixedValue() > maxFixedValue) {
                    maxFixedValue = promotionMember.getClubMemberBO().getShareFixedValue();
                }
            }
        }

        if (clubMemberItem.getDoClubMember().isClubCreate()) {
            // 不是固定值,重新设置值
            shareType = shareType != UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() ? clubMemberItem.getToClubMember().getClubMemberBO().getShareType() : shareType;
        } else {
            // 不是固定值,重新设置值
            shareType = UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() == upLevelShareType ? upLevelShareType : clubMemberItem.getToClubMember().getClubMemberBO().getShareType();

        }
        ClubPromotionItem clubPromotionItem = new ClubPromotionItem(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID(),
                clubMemberItem.getToClubMember().getClubMemberBO().getShareValue(), clubMemberItem.getToClubMember().getClubMemberBO().getShareFixedValue(), shareType, clubMemberItem.getDoClubMember().isClubCreate() ? 100 : clubMemberItem.getDoClubMember().getClubMemberBO().getShareValue(), maxValue, clubMemberItem.getDoClubMember().isClubCreate() ? 10000 : clubMemberItem.getDoClubMember().getClubMemberBO().getShareFixedValue(), maxFixedValue, clubMemberItem.getDoClubMember().isClubCreate(), lowerLevelShareType, clubMemberItem.getToClubMember().getId(), upLevelShareType);

        // 赛事分成类型
        UnionDefine.UNION_SHARE_TYPE shareTypeE = UnionDefine.UNION_SHARE_TYPE.valueOf(promotionActive.getType());
        if (Objects.isNull(shareTypeE)) {
            return SData_Result.make(ErrorCode.NotAllow, "CClubPromotionLevelShareChangeBatch null shareType");
        }
//        if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareTypeE) && clubPromotionItem.getLowerLevelShareType() == UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal()) {
//            return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_LEVEL_SHARE_LOWER, "CLUB_MEMBER_PROMOTION_LEVEL_SHARE_LOWER");
//        }
        if (!clubPromotionItem.isUpLevelCreate() && shareType == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() && !UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareTypeE)) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_LEVEL_SHARE_UP, "CLUB_MEMBER_PROMOTION_LEVEL_SHARE_UP");
        }
        if (CollectionUtils.isEmpty(promotionActive.getPromotionCalcActiveItemList())) {
            return SData_Result.make(ErrorCode.NotAllow, "not execScorePercentBatchUpdate");
        }
        return SData_Result.make(ErrorCode.Success, new ClubPromotionLevetShareChangeBatchItem(clubMemberItem.getToClubMember(), UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() == shareType ? clubPromotionItem.getDoShareFixedValue() : clubPromotionItem.getDoShareValue(), UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() == shareType ? clubPromotionItem.getMinShareFixedValue() : clubPromotionItem.getMinShareValue(), clubMemberItem.getDoClubMember().getClubMemberBO().getPlayerID(), shareTypeE.ordinal(), shareType));
    }

    /**
     * 区间修改
     *
     * @param promotionActive
     * @param pid
     * @return
     */
    public SData_Result CClubPromotionLevelShareSectionChangeBatch(CClub_PromotionSectionCalcActiveBatch promotionActive, long pid) {
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(promotionActive.getOpClubId(), pid);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER);
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionActive.getOpClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST);
        }
        if (club.isSectionChangePromotionFlag()) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_CHANGE_IS_EXIT, "CLUB_MEMBER_PROMOTION_CHANGE_IS_EXIT");
        }
        int updateTime = CommTime.nowSecond();
        boolean changeFlag = false;
        for (ClubPromotionSectionCalcActiveItem con : promotionActive.getPromotionSectionCalcActiveItems()) {
            SharePromotionSection sharePromotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(clubMember.getId());
            if (Objects.isNull(sharePromotionSection)) {
                continue;
            }
            PromotionShareSectionItem promotionShareSectionItems = sharePromotionSection.getPromotionShareSectionItems().stream().filter(k -> k.getUnionSectionId() == con.getUnionSectionId()).findFirst().orElse(null);
            if (Objects.isNull(promotionShareSectionItems)) {
                continue;
            }
            double oldValue = promotionShareSectionItems.getShareToSelfValue();
//            if (con.getShareToSelfValue() != promotionShareSectionItems.getShareToSelfValue()) {
//                con.setChangFlag(true);
//                changeFlag=true;
            con.setEndValue(promotionShareSectionItems.getEndValue());
            con.setBeginValue(promotionShareSectionItems.getBeginValue());
            PromotionShareSectionBO promotionShareSectionBO = new PromotionShareSectionBO();
            promotionShareSectionBO.setShareToSelfValue(con.getShareToSelfValue());
            promotionShareSectionBO.setEndFlag(promotionShareSectionItems.getEndFlag());
            promotionShareSectionBO.setClubId(promotionShareSectionItems.getClubId());
            promotionShareSectionBO.setEndValue(promotionShareSectionItems.getEndValue());
            promotionShareSectionBO.setBeginValue(promotionShareSectionItems.getBeginValue());
            promotionShareSectionBO.setPid(promotionShareSectionItems.getPid());
            promotionShareSectionBO.setUnionSectionId(promotionShareSectionItems.getUnionSectionId());
            promotionShareSectionBO.setAllowShareToValue(promotionShareSectionItems.getAllowShareToValue());
            promotionShareSectionBO.setUpdateTime(updateTime);
            promotionShareSectionBO.getBaseService().saveIgnoreOrUpDate(promotionShareSectionBO);
            UnionDynamicBO.insertSportsPoint(promotionActive.getOpPid(), club.getClubListBO().getUnionId(), promotionActive.getOpClubId(), 0, 0, CommTime.nowSecond(),
                    UnionDefine.UNION_EXEC_TYPE.PROMOTION_EXEC_SHARE_SECTION_CHANGE.value(), "", 2, String.valueOf(con.getEndValue()), String.valueOf(con.getBeginValue()), String.valueOf(con.getShareToSelfValue()),
                    String.valueOf(oldValue), String.valueOf(pid), "", "", "");
//            }
        }
//        //没有值进行修改
//        if(!changeFlag){
//            return SData_Result.make(ErrorCode.Success);
//        }
        clubMember.initRedisSection();
        club.setSectionChangePromotionFlag(true);
        DispatcherComponent.getInstance().publish(new ClubPromotionSectionChangeEvent(clubMember.getClubID(), clubMember.getId(), pid, promotionActive, club.getClubListBO().getUnionId()));
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 亲友圈分成详情比例列表
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result getClubPromotionLevelShareChangeList(CClub_SubordinateList req, long pid) {
        SData_Result result = clubPromotionLevelShareChangePower(req.getClubId(), req.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        ClubMember clubMember = clubMemberItem.getToClubMember();
        ClubMember upClubMember = clubMemberItem.getDoClubMember();
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        // 获取赛事Id
        final long unionId = club.getClubListBO().getUnionId();
        Map<Long, ClubRoomConfigCalcActiveItem> roomConfigMap = null;
        if (unionId > 0L) {
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
            if (Objects.isNull(union)) {
                return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
            }
            roomConfigMap = union.getRoomConfigList(req.getPageNum());
        } else {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        if (MapUtils.isEmpty(roomConfigMap)) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        // 赛事分成类型
        UnionDefine.UNION_SHARE_TYPE shareType = UnionDefine.UNION_SHARE_TYPE.valueOf(req.getType());
        if (Objects.isNull(shareType)) {
            return SData_Result.make(ErrorCode.NotAllow, "getClubPromotionLevelShareChangeList null shareType");
        }
        List<PromotionLevelRoomConfigScorePercentBO> promotionLevelRoomConfigScorePercentBOList = ContainerMgr
                .get()
                .getComponent(PromotionLevelRoomConfigScorePercentBOService.class)
                .findAll(Restrictions.and(Restrictions.eq("pid", req.getPid()), Restrictions.eq("unionId", unionId), Restrictions.eq("clubId", req.getClubId()), Restrictions.eq("type", shareType.ordinal()), Restrictions.in("configId", roomConfigMap.keySet())));
        Map<Long, Double> upLevelroomConfigCalcActiveItemMapFixed = null;
        Map<Long, Double> upLevelroomConfigCalcActiveItemMapPercent = null;
        //如果上级是圈主的话 要去另外一张表里面找数据
        if (upClubMember.isClubCreate()) {
            List<UnionRoomConfigScorePercentBO> unionRoomConfigScorePercentBOList = ContainerMgr.get().getComponent(UnionRoomConfigScorePercentBOService.class).findAll(Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.eq("clubId", req.getClubId())));
            if (CollectionUtils.isEmpty(unionRoomConfigScorePercentBOList)) {
                unionRoomConfigScorePercentBOList = Collections.emptyList();
            }
            upLevelroomConfigCalcActiveItemMapFixed = unionRoomConfigScorePercentBOList.stream().collect(Collectors.toMap(UnionRoomConfigScorePercentBO::getConfigId, scorePercentBO -> scorePercentBO.getScoreDividedInto()));
            upLevelroomConfigCalcActiveItemMapPercent = unionRoomConfigScorePercentBOList.stream().collect(Collectors.toMap(UnionRoomConfigScorePercentBO::getConfigId, scorePercentBO -> (double) scorePercentBO.getScorePercent()));
        } else {
            List<PromotionLevelRoomConfigScorePercentBO> upLevelRoomConfigScorePercentBOList = ContainerMgr
                    .get()
                    .getComponent(PromotionLevelRoomConfigScorePercentBOService.class)
                    .findAll(Restrictions.and(Restrictions.eq("pid", pid), Restrictions.eq("unionId", unionId), Restrictions.eq("clubId", req.getClubId()), Restrictions.eq("type", UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal()), Restrictions.in("configId", roomConfigMap.keySet())));

            if (CollectionUtils.isEmpty(upLevelRoomConfigScorePercentBOList)) {
                upLevelRoomConfigScorePercentBOList = Collections.emptyList();
            }
            List<PromotionLevelRoomConfigScorePercentBO> upLevelRoomConfigScoreFixedBOList = ContainerMgr
                    .get()
                    .getComponent(PromotionLevelRoomConfigScorePercentBOService.class)
                    .findAll(Restrictions.and(Restrictions.eq("pid", pid), Restrictions.eq("unionId", unionId), Restrictions.eq("clubId", req.getClubId()), Restrictions.eq("type", UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()), Restrictions.in("configId", roomConfigMap.keySet())));

            if (CollectionUtils.isEmpty(upLevelRoomConfigScoreFixedBOList)) {
                upLevelRoomConfigScoreFixedBOList = Collections.emptyList();
            }
            upLevelroomConfigCalcActiveItemMapFixed = upLevelRoomConfigScoreFixedBOList.stream().collect(Collectors.toMap(PromotionLevelRoomConfigScorePercentBO::getConfigId, promotionLevelRoomConfigScorePercentBO -> promotionLevelRoomConfigScorePercentBO.getScoreDividedInto()));
            upLevelroomConfigCalcActiveItemMapPercent = upLevelRoomConfigScorePercentBOList.stream()
                    .collect(Collectors.toMap(PromotionLevelRoomConfigScorePercentBO::getConfigId, promotionLevelRoomConfigScorePercentBO -> (double) promotionLevelRoomConfigScorePercentBO.getScorePercent()));
        }
        if (CollectionUtils.isEmpty(promotionLevelRoomConfigScorePercentBOList)) {
            promotionLevelRoomConfigScorePercentBOList = Collections.emptyList();
        }

        Map<Long, Double> roomConfigCalcActiveItemMap = null;
        if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareType)) {
            roomConfigCalcActiveItemMap = promotionLevelRoomConfigScorePercentBOList.stream().collect(Collectors.toMap(PromotionLevelRoomConfigScorePercentBO::getConfigId, promotionLevelRoomConfigScorePercentBO -> promotionLevelRoomConfigScorePercentBO.getScoreDividedInto()));
        } else {
            roomConfigCalcActiveItemMap = promotionLevelRoomConfigScorePercentBOList.stream()
                    .collect(Collectors.toMap(PromotionLevelRoomConfigScorePercentBO::getConfigId, promotionLevelRoomConfigScorePercentBO -> (double) promotionLevelRoomConfigScorePercentBO.getScorePercent()));
        }
        Map<Long, Double> finalRoomConfigCalcActiveItemMap = roomConfigCalcActiveItemMap;
        Map<Long, Double> upFinalRoomConfigCalcActiveItemMapFixed = upLevelroomConfigCalcActiveItemMapFixed;
        Map<Long, Double> upFinalRoomConfigCalcActiveItemMapPercent = upLevelroomConfigCalcActiveItemMapPercent;
        return SData_Result.make(ErrorCode.Success, roomConfigMap.values().stream().map(k -> {
            Double value = null;
            Double upLevelValue = null;
            UnionScoreDividedIntoValueItem item = EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).get(String.format(DataConstants.PROMOTION_LEVEL_ROOM_CONFIG_SCORE_PERCENT, req.getPid(), unionId, req.getClubId(), k.getConfigId()), UnionScoreDividedIntoValueItem.class);
            if (Objects.isNull(item)) {
                value = finalRoomConfigCalcActiveItemMap.get(k.getConfigId());
            } else {
                if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareType)) {
                    value = item.getScoreDividedInto();
                } else {
                    value = item.getScorePercent();
                }
            }
            UnionScoreDividedIntoValueItem upItem = EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).get(String.format(DataConstants.PROMOTION_LEVEL_ROOM_CONFIG_SCORE_PERCENT, pid, unionId, req.getClubId(), k.getConfigId()), UnionScoreDividedIntoValueItem.class);
            if (Objects.isNull(upItem)) {
                if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(upClubMember.getClubMemberBO().getShareType()))) {
                    upLevelValue = upFinalRoomConfigCalcActiveItemMapFixed.get(k.getConfigId());
                } else {
                    upLevelValue = upFinalRoomConfigCalcActiveItemMapPercent.get(k.getConfigId());
                }
            } else {
                if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareType)) {
                    upLevelValue = upItem.getScoreDividedInto();
                } else {
                    upLevelValue = upItem.getScorePercent();
                }
            }
            // 获取配置是否存在
            boolean changeFlag = Objects.isNull(value) || value.intValue() < 0;
            boolean upChangeFlag = Objects.isNull(upLevelValue) || upLevelValue.intValue() < 0;
            //查找有没有对应的配置
            if (!upChangeFlag) {
                k.setAllowValue(upLevelValue);
                k.setType(upClubMember.getClubMemberBO().getShareType());
            } else {
                //没有对应的配置 要去看玩家本身的类型
                //类型相同 则去找玩家身上的配置
                if (shareType.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(upClubMember.getClubMemberBO().getShareType()))) {
                    if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(upClubMember.getClubMemberBO().getShareType()))) {
                        k.setAllowValue(upClubMember.getClubMemberBO().getShareFixedValue());
                    } else {
                        k.setAllowValue(upClubMember.getClubMemberBO().getShareValue());
                    }
                    k.setType(upClubMember.getClubMemberBO().getShareType());
                } else {
//                    //类型不相同的时候 先去找有没有对应的另一边的配置
//                    if(UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareType)){
//                        upLevelValue = upFinalRoomConfigCalcActiveItemMapPercent.get(k.getConfigId());
//                    }else {
//                        upLevelValue = upFinalRoomConfigCalcActiveItemMapFixed.get(k.getConfigId());
//                    }
//                    boolean changeFlagOtherSide = Objects.isNull(upLevelValue) || upLevelValue.intValue() < 0;
//                    if(!changeFlagOtherSide){
//                        //有的话就取这个
//                        k.setAllowValue(upLevelValue);
//                        k.setGetType(upClubMember.getClubMemberBO().getShareType());
//                    }else {
                    //没有的话
                    if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(upClubMember.getClubMemberBO().getShareType()))) {
                        k.setAllowValue(upClubMember.getClubMemberBO().getShareFixedValue());
                    } else {
                        k.setAllowValue(upClubMember.getClubMemberBO().getShareValue());
                    }
                    k.setType(upClubMember.getClubMemberBO().getShareType());
//                    }
                }
            }

            k.setValue(changeFlag ? clubMember.getClubMemberBO().getShareValue() : value);
            k.setChangeFlag(!changeFlag);
            return k;
        }).collect(Collectors.toList()));
    }

    /**
     * 亲友圈区间分成详情比例列表
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result getClubPromotionLevelSectionChangeList(CClub_SectionParam req, long pid) {
        //检查权限
        SData_Result result;
        ClubMember clubMember;
        if (req.getUnionFlag() == 1) {
            result = UnionMgr.getInstance().getUnionMemberMgr().checkUnionRightsShare(req.getUnionId(), req.getClubId(), pid, req.getOpClubId(), req.getOpPid());
        } else {
            result = clubPromotionLevelShareChangePower(req.getClubId(), req.getOpPid(), pid);
        }
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        if (req.getUnionFlag() == 1) {
            clubMember = getClubMember(req.getOpClubId(), req.getOpPid());
            if (Objects.isNull(clubMember)) {
                return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "CLUB_NOT_EXIST_MEMBER_INFO");
            }
        } else {
            ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
            clubMember = clubMemberItem.getToClubMember();
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        SharePromotionSection sharePromotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(clubMember.getId());
        if (Objects.isNull(sharePromotionSection)) {
            clubMember.initPromotionSection(club.getClubListBO().getUnionId(), clubMember.isClubCreate());
            sharePromotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(clubMember.getId());
        }
        //圈主往下进行分成
//        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
//        List<QueryUidOrPuidItem> promotionList = queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).collect(Collectors.toList());
        for (PromotionShareSectionItem con : sharePromotionSection.getPromotionShareSectionItems()) {
            con.setTwoValue(CommMath.div(con.getShareToSelfValue(), 2));
            con.setThreeValue(CommMath.div(con.getShareToSelfValue(), 3));
            con.setFourValue(CommMath.div(con.getShareToSelfValue(), 4));
            con.setTenValue(CommMath.div(con.getShareToSelfValue(), 10));
//            double allowShareToValue=con.getEndFlag()==1?con.getBeginValue():con.getEndValue();
//            for (QueryUidOrPuidItem queryUidOrPuidItem : promotionList) {
//                if(allowShareToValue<=0){
//                    break;
//                }
//                ClubMember promotionMember;
//                if(Config.isShare()){
//                    promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem.getPuid());
//                } else {
//                    promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem.getPuid());
//                }
//                if(Objects.isNull(promotionMember)||(!promotionMember.isLevelPromotion()&&!promotionMember.isClubCreate())){
//                    continue;
//                }
//                SharePromotionSection promotionSection=SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(promotionMember.getId());
//                if(Objects.isNull(promotionSection)){
//                    continue;
//                }
//                PromotionShareSectionItem item=promotionSection.getPromotionShareSectionItems().stream().filter(k->k.getUnionSectionId()==con.getUnionSectionId()).findFirst().orElse(null);
//                if(Objects.isNull(item)){
//                    continue;
//                }
//                allowShareToValue=CommMath.subDouble(allowShareToValue,item.getShareToSelfValue());
//            }
//            con.setAllowShareToValue(allowShareToValue>=0?allowShareToValue:0);
        }
        sharePromotionSection.setMinAllowShareToValue(sharePromotionSection.getPromotionShareSectionItems().stream().min
                (Comparator.comparingDouble(PromotionShareSectionItem::getAllowShareToValue)).get().getAllowShareToValue());
        return SData_Result.make(ErrorCode.Success, sharePromotionSection);
    }

    /**
     * 亲友圈区间分成详情比例列表
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result getClubPromotionLevelSectionChangeListSelf(CClub_SectionParam req, long pid) {
        //检查权限
        SData_Result result;
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.getOpClubId(), pid);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "CLUB_NOT_EXIST_MEMBER_INFO");
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }

        SharePromotionSection sharePromotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(clubMember.getId());
        if (Objects.isNull(sharePromotionSection)) {
            clubMember.initPromotionSection(club.getClubListBO().getUnionId(), clubMember.isClubCreate());
            sharePromotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(clubMember.getId());
        }
//        //圈主往下进行分成
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        List<QueryUidOrPuidItem> promotionList = queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).collect(Collectors.toList());
        for (PromotionShareSectionItem con : sharePromotionSection.getPromotionShareSectionItems()) {
            con.setShareToSelfValue(0);
            con.setTwoValue(CommMath.div(con.getShareToSelfValue(), 2));
            con.setThreeValue(CommMath.div(con.getShareToSelfValue(), 3));
            con.setFourValue(CommMath.div(con.getShareToSelfValue(), 4));
            con.setTenValue(CommMath.div(con.getShareToSelfValue(), 10));
            double allowShareToValue = con.getEndFlag() == 1 ? con.getBeginValue() : con.getEndValue();
            //先减去圈主的那一部分
            ClubMember clubMemberCreate = ClubMgr.getInstance().getClubMemberMgr().find(club.getOwnerPlayerId(), req.getClubId());
            if (Objects.nonNull(clubMemberCreate)) {
                SharePromotionSection promotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(clubMemberCreate.getId());
                if (Objects.isNull(promotionSection)) {
                    continue;
                }
                PromotionShareSectionItem item = promotionSection.getPromotionShareSectionItems().stream().filter(k -> k.getUnionSectionId() == con.getUnionSectionId()).findFirst().orElse(null);
                if (Objects.isNull(item)) {
                    continue;
                }
                allowShareToValue = CommMath.subDouble(allowShareToValue, item.getShareToSelfValue());
            }
            for (QueryUidOrPuidItem queryUidOrPuidItem : promotionList) {
                if (queryUidOrPuidItem.getPuid() == clubMemberCreate.getId()) {
                    continue;
                }
                if (allowShareToValue <= 0) {
                    break;
                }
                ClubMember promotionMember;
                if (Config.isShare()) {
                    promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem.getPuid());
                } else {
                    promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem.getPuid());
                }
                if (Objects.isNull(promotionMember) || (!promotionMember.isLevelPromotion() && !promotionMember.isClubCreate())) {
                    continue;
                }
                SharePromotionSection promotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(promotionMember.getId());
                if (Objects.isNull(promotionSection)) {
                    continue;
                }
                PromotionShareSectionItem item = promotionSection.getPromotionShareSectionItems().stream().filter(k -> k.getUnionSectionId() == con.getUnionSectionId()).findFirst().orElse(null);
                if (Objects.isNull(item)) {
                    continue;
                }
                allowShareToValue = CommMath.subDouble(allowShareToValue, item.getShareToSelfValue());
            }
            //再减去自己的那一部分 并且自己不是圈主
            ClubMember clubMemberSelf = ClubMgr.getInstance().getClubMemberMgr().find(pid, req.getClubId());
            if (Objects.nonNull(clubMemberSelf) && clubMemberSelf.isNotClubCreate()) {
                SharePromotionSection promotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(clubMemberSelf.getId());
                if (Objects.isNull(promotionSection)) {
                    continue;
                }
                PromotionShareSectionItem item = promotionSection.getPromotionShareSectionItems().stream().filter(k -> k.getUnionSectionId() == con.getUnionSectionId()).findFirst().orElse(null);
                if (Objects.isNull(item)) {
                    continue;
                }
                allowShareToValue = CommMath.subDouble(allowShareToValue, item.getShareToSelfValue());
            }
            con.setAllowShareToValue(allowShareToValue >= 0 ? allowShareToValue : 0);
        }
        sharePromotionSection.setShowSelf(true);
        sharePromotionSection.setMinAllowShareToValue(sharePromotionSection.getPromotionShareSectionItems().stream().min
                (Comparator.comparingDouble(PromotionShareSectionItem::getAllowShareToValue)).get().getAllowShareToValue());
        return SData_Result.make(ErrorCode.Success, sharePromotionSection);
    }

    /**
     * 亲友圈分成详情比例列表
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result getClubPromotionLevelShareSelfInfo(CClub_SubordinateList req, long pid) {
        ClubMember doClubMember = getClubMember(req.getClubId(), pid);
        if (Objects.isNull(doClubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "CLUB_NOT_EXIST_MEMBER_INFO");
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        // 获取赛事Id
        final long unionId = club.getClubListBO().getUnionId();
        Map<Long, ClubRoomConfigCalcActiveItem> roomConfigMap = null;
        if (unionId > 0L) {
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
            if (Objects.isNull(union)) {
                return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
            }
            roomConfigMap = union.getRoomConfigList(req.getPageNum());
        } else {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        if (MapUtils.isEmpty(roomConfigMap)) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        // 赛事分成类型
        UnionDefine.UNION_SHARE_TYPE shareType = UnionDefine.UNION_SHARE_TYPE.valueOf(req.getType());
        if (Objects.isNull(shareType)) {
            return SData_Result.make(ErrorCode.NotAllow, "getClubPromotionLevelShareChangeList null shareType");
        }

        Map<Long, Double> roomConfigCalcActiveItemMapFixed = null;
        Map<Long, Double> roomConfigCalcActiveItemMapPercent = null;
        if (doClubMember.isClubCreate()) {
            List<UnionRoomConfigScorePercentBO> unionRoomConfigScorePercentBOList = ContainerMgr.get().getComponent(UnionRoomConfigScorePercentBOService.class).findAll(Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.eq("clubId", req.getClubId())));
            if (null == unionRoomConfigScorePercentBOList) {
                unionRoomConfigScorePercentBOList = new ArrayList<>();
            }
            roomConfigCalcActiveItemMapFixed = unionRoomConfigScorePercentBOList.stream().collect(Collectors.toMap(UnionRoomConfigScorePercentBO::getConfigId, scorePercentBO -> scorePercentBO.getScoreDividedInto()));
            roomConfigCalcActiveItemMapPercent = unionRoomConfigScorePercentBOList.stream().collect(Collectors.toMap(UnionRoomConfigScorePercentBO::getConfigId, scorePercentBO -> (double) scorePercentBO.getScorePercent()));

        } else {
            List<PromotionLevelRoomConfigScorePercentBO> promotionLevelRoomConfigScorePercentBOList = ContainerMgr
                    .get()
                    .getComponent(PromotionLevelRoomConfigScorePercentBOService.class)
                    .findAll(Restrictions.and(Restrictions.eq("pid", req.getPid()), Restrictions.eq("unionId", unionId), Restrictions.eq("clubId", req.getClubId()), Restrictions.eq("type", UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal()), Restrictions.in("configId", roomConfigMap.keySet())));
            if (CollectionUtils.isEmpty(promotionLevelRoomConfigScorePercentBOList)) {
                promotionLevelRoomConfigScorePercentBOList = Collections.emptyList();
            }
            List<PromotionLevelRoomConfigScorePercentBO> promotionLevelRoomConfigScoreFixedBOList = ContainerMgr
                    .get()
                    .getComponent(PromotionLevelRoomConfigScorePercentBOService.class)
                    .findAll(Restrictions.and(Restrictions.eq("pid", req.getPid()), Restrictions.eq("unionId", unionId), Restrictions.eq("clubId", req.getClubId()), Restrictions.eq("type", UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()), Restrictions.in("configId", roomConfigMap.keySet())));
            if (CollectionUtils.isEmpty(promotionLevelRoomConfigScoreFixedBOList)) {
                promotionLevelRoomConfigScoreFixedBOList = Collections.emptyList();
            }
            roomConfigCalcActiveItemMapFixed = promotionLevelRoomConfigScoreFixedBOList.stream().collect(Collectors.toMap(PromotionLevelRoomConfigScorePercentBO::getConfigId, promotionLevelRoomConfigScorePercentBO -> promotionLevelRoomConfigScorePercentBO.getScoreDividedInto()));
            roomConfigCalcActiveItemMapPercent = promotionLevelRoomConfigScorePercentBOList.stream()
                    .collect(Collectors.toMap(PromotionLevelRoomConfigScorePercentBO::getConfigId, promotionLevelRoomConfigScorePercentBO -> (double) promotionLevelRoomConfigScorePercentBO.getScorePercent()));
        }

        Map<Long, Double> finalRoomConfigCalcActiveItemMapFixed = roomConfigCalcActiveItemMapFixed;
        Map<Long, Double> finalRoomConfigCalcActiveItemMapPercent = roomConfigCalcActiveItemMapPercent;
        return SData_Result.make(ErrorCode.Success, roomConfigMap.values().stream().map(k -> {
            Double value = null;
            UnionScoreDividedIntoValueItem item = EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).get(String.format(DataConstants.PROMOTION_LEVEL_ROOM_CONFIG_SCORE_PERCENT, req.getPid(), unionId, req.getClubId(), k.getConfigId()), UnionScoreDividedIntoValueItem.class);
            if (Objects.isNull(item)) {
                if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(doClubMember.getClubMemberBO().getShareType()))) {
                    value = finalRoomConfigCalcActiveItemMapFixed.get(k.getConfigId());
                } else {
                    value = finalRoomConfigCalcActiveItemMapPercent.get(k.getConfigId());
                }
            } else {
                if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareType)) {
                    value = item.getScoreDividedInto();
                } else {
                    value = item.getScorePercent();
                }
            }
            // 获取配置是否存在
            boolean changeFlag = Objects.isNull(value) || value.intValue() < 0;

            k.setChangeFlag(!changeFlag);
            //查找有没有对应的配置
            if (!changeFlag) {
                k.setAllowValue(value);
                k.setType(doClubMember.getClubMemberBO().getShareType());
            } else {
                //没有对应的配置 要去看玩家本身的类型
                //类型相同 则去找玩家身上的配置
                if (shareType.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(doClubMember.getClubMemberBO().getShareType()))) {
                    if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(doClubMember.getClubMemberBO().getShareType()))) {
                        k.setAllowValue(doClubMember.getClubMemberBO().getShareFixedValue());
                    } else {
                        k.setAllowValue(doClubMember.getClubMemberBO().getShareValue());
                    }
                    k.setType(doClubMember.getClubMemberBO().getShareType());
                } else {
//                    //类型不相同的时候 先去找有没有对应的另一边的配置
//                    if(UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareType)){
//                        value = finalRoomConfigCalcActiveItemMapPercent.get(k.getConfigId());
//                    }else {
//                        value = finalRoomConfigCalcActiveItemMapFixed.get(k.getConfigId());
//                    }
//                    boolean changeFlagOtherSide = Objects.isNull(value) || value.intValue() < 0;
//                    if(!changeFlagOtherSide){
//                        //有的话就取这个
//                        k.setAllowValue(value);
//                        k.setGetType(doClubMember.getClubMemberBO().getShareType());
//                    }else {
                    //没有的话
                    if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(doClubMember.getClubMemberBO().getShareType()))) {
                        k.setAllowValue(doClubMember.getClubMemberBO().getShareFixedValue());
                    } else {
                        k.setAllowValue(doClubMember.getClubMemberBO().getShareValue());
                    }
                    k.setType(doClubMember.getClubMemberBO().getShareType());
//                    }
                }
            }


            return k;
        }).collect(Collectors.toList()));


    }

    /**
     * 赛事禁止玩法列表
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getClubBanRoomConfigList(CUnion_BanRoomConfigOp req, long exePid) {
        // 检查当前操作者是否管理员、创建者。
        if (!ClubMgr.getInstance().getClubMemberMgr().isMinister(req.getClubId(), exePid)) {
            return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not club admin ClubID:{%d}", req.getClubId());
        }
        // 检查成员是否在本亲友圈。
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(req.getOpPid(), req.getClubId(), Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "null == clubMember ClubID:{%d}", req.getOpPid());
        }
        // 检查被操作的玩家是创建者
        if (Club_MINISTER.Club_MINISTER_CREATER.value() == clubMember.getClubMemberBO().getIsminister()) {
            return SData_Result.make(ErrorCode.NotAllow, "create club not banPid:{}", req.getOpPid());
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST getUnionBanRoomConfigList");
        }
        List<SUnion_BanRoomConfigItem> unionBanRoomConfigBOList = ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).findAllE(Restrictions.and(Restrictions.eq("unionId", 0L), Restrictions.eq("clubId", req.getClubId()), Restrictions.eq("pid", req.getOpPid())), SUnion_BanRoomConfigItem.class, SUnion_BanRoomConfigItem.getItemsName());
        if (CollectionUtils.isEmpty(unionBanRoomConfigBOList)) {
            unionBanRoomConfigBOList = Collections.emptyList();
        }
        // 转json
        final Gson gson = new Gson();
        Map<Long, SUnion_BanRoomConfigItem> banRoomConfigItemMap = unionBanRoomConfigBOList.stream().collect(Collectors.toMap(k -> k.getConfigId(), k -> k, (k1, k2) -> k1));
        return SData_Result.make(ErrorCode.Success, new UnionBanRoomConfigItem(
                club.getMCreateGamesetMap().entrySet().stream().map(k -> {
                    if (Objects.isNull(k) || !k.getValue().isExistClubRoomConfig()) {
                        return null;
                    }
                    // 是否存在禁止全部
                    SUnion_BanRoomConfigItem banRoomConfigItem = banRoomConfigItemMap.containsKey(0L) ? new SUnion_BanRoomConfigItem() : banRoomConfigItemMap.get(k.getKey());
                    banRoomConfigItem = Objects.isNull(banRoomConfigItem) ? new SUnion_BanRoomConfigItem(0) : banRoomConfigItem;
                    banRoomConfigItem.setConfigId(k.getKey());
                    banRoomConfigItem.setGameId(k.getValue().getGameType().getId());
                    banRoomConfigItem.setDataJsonCfg(gson.toJson(k.getValue().getbRoomConfigure().getBaseCreateRoomT()));
                    return banRoomConfigItem;
                }).filter(k -> Objects.nonNull(k)).collect(Collectors.toList()), banRoomConfigItemMap.containsKey(0L) ? 1 : 0));
    }


    /**
     * 操作赛事禁止玩法列表
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getClubBanRoomConfigOp(CUnion_BanRoomConfigOp req, long exePid) {
        // 检查当前操作者是否管理员、创建者。
        if (!ClubMgr.getInstance().getClubMemberMgr().isMinister(req.getClubId(), exePid)) {
            return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not club admin ClubID:{%d}", req.getClubId());
        }
        // 检查成员是否在本亲友圈。
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(req.getOpPid(), req.getClubId(), Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "null == clubMember ClubID:{%d}", req.getOpPid());
        }
        // 检查被操作的玩家是创建者
        if (Club_MINISTER.Club_MINISTER_CREATER.value() == clubMember.getClubMemberBO().getIsminister()) {
            return SData_Result.make(ErrorCode.NotAllow, "create club not banPid:{}", req.getOpPid());
        }

        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST getUnionBanRoomConfigList");
        }
        if (req.getIsAll() == 1) {
            ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).saveIgnore(new UnionBanRoomConfigBO(req.getUnionId(), req.getClubId(), req.getOpPid(), 0L, CommTime.nowSecond()));
        } else {
            // 找到选择中的玩法id
            List<Long> configIdList = club.getMCreateGamesetMap().entrySet().stream().filter(k -> req.getConfigIdList().contains(k.getKey())).map(k -> k.getKey()).collect(Collectors.toList());
            // 将未选中的玩法删除
            ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).delete(Restrictions.and(Restrictions.eq("unionId", 0L), Restrictions.eq("clubId", req.getClubId()), Restrictions.eq("pid", req.getOpPid()), CollectionUtils.isEmpty(configIdList) ? null : Restrictions.notin("configId", configIdList)));
            if (CollectionUtils.isNotEmpty(configIdList)) {
                // 插入新选中的玩法
                List<UnionBanRoomConfigBO> unionBanRoomConfigBOList = configIdList.stream().map(k -> new UnionBanRoomConfigBO(0L, req.getClubId(), req.getOpPid(), k, CommTime.nowSecond())).collect(Collectors.toList());
                ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).execBatchDb(unionBanRoomConfigBOList);
            }
        }
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 获取推广员列表
     *
     * @param promotionList 推广员参数
     * @param pid           玩家Pid
     * @return
     */
    public SData_Result getClubPromotionLevelPowerInfo(CClub_PromotionList promotionList, long pid) {
        if (this.isNotClubCreate(promotionList.getClubId(), pid)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "getClubPromotionLevelPowerInfo CLUB_NOT_CREATE");
        }
        ClubMember clubMember = this.find(promotionList.getPid(), promotionList.getClubId(), Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(clubMember) || clubMember.isNotLevelPromotion()) {
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "getClubPromotionLevelPowerInfo CLUB_NOT_PROMOTION");
        }

        return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelPowerInfo(clubMember.getClubMemberBO().getKicking(), clubMember.getClubMemberBO().getModifyValue(), clubMember.getClubMemberBO().getShowShare(), clubMember.getClubMemberBO().getInvite()));
    }

    /**
     * 检查竞技点预警值
     *
     * @param clubMember
     * @return
     */
    public SData_Result checkSportsPointWarning(ClubMember clubMember) {
        //如果不是亲友圈的话 不用检测
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.Success);
        }
        Club club = null;
        if (Config.isShare()) {
            club = ShareClubListMgr.getInstance().getClub(clubMember.getClubID());
        } else {
            club = ClubMgr.getInstance().getClubListMgr().getClubMap().get(clubMember.getClubID());
        }
        //找到这个人的上级关系
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        // 上级关系
        List<Long> uidList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            // 整理上级：
            uidList.addAll(queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).map(k -> k.getPuid()).collect(Collectors.toList()));
        }
        if (clubMember.isLevelPromotion()) {
            uidList.add(clubMember.getId());
        }
        for (Long uid : uidList) {
            ClubMember promotionClubMember = null;
            if (Config.isShare()) {
                promotionClubMember = ShareClubMemberMgr.getInstance().getClubMember(uid);
            } else {
                promotionClubMember = this.getClubMemberMap().get(uid);
            }
            if (Objects.isNull(promotionClubMember) || UnionDefine.UNION_WARN_STATUS.CLOSE.ordinal() == promotionClubMember.getClubMemberBO().getWarnStatus() || promotionClubMember.isClubCreate()) {
                continue;
            }
            //获取某个人的全部下级的竞技点总和
            Double promotionAllSportsPoint = this.getPromotionAllSportsPoint(promotionClubMember);
            if (promotionAllSportsPoint < promotionClubMember.getClubMemberBO().getSportsPointWarning()) {
                FlowLogger.sportsPointWarningLog(clubMember.getClubMemberBO().getPlayerID(), clubMember.getClubID(), club.getClubListBO().getUnionId(), uid, promotionClubMember.getClubMemberBO().getSportsPointWarning(), promotionAllSportsPoint, 0);
//                Player player=PlayerMgr.getInstance().getPlayer(promotionClubMember.getClubMemberBO().getPlayerID());
//                if(Objects.isNull(player)){
//
////                    return SData_Result.make(ErrorCode.Error_Code,"玩家"+promotionClubMember.getClubMemberBO().getPlayerID()+"达到预警值。");
//                }else {
//                    return SData_Result.make(ErrorCode.Error_Code,"玩家:"+player.getName()+"(ID:"+player.getPid()+")达到预警值。");
//                }

                return SData_Result.make(ErrorCode.Error_Code, "您所在的推广员队伍或上级队伍比赛分低于预警值，无法加入比赛，请联系管理");
            }
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 检查竞技点预警值
     *
     * @param clubMember
     * @return
     */
    public SData_Result checkAlivePointWarning(ClubMember clubMember) {
        //如果不是亲友圈的话 不用检测
        if (Objects.isNull(clubMember) || clubMember.getClubMemberBO().getAlivePointStatus() == UnionDefine.UNION_WARN_STATUS.CLOSE.ordinal()) {
            return SData_Result.make(ErrorCode.Success);
        }
        Club club = null;
        if (Config.isShare()) {
            club = ShareClubListMgr.getInstance().getClub(clubMember.getClubID());
        } else {
            club = ClubMgr.getInstance().getClubListMgr().getClubMap().get(clubMember.getClubID());
        }
        //找到这个人的上级关系
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        // 上级关系
        List<Long> uidList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            // 整理上级：
            uidList.addAll(queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).map(k -> k.getPuid()).collect(Collectors.toList()));
        }
        if (clubMember.isLevelPromotion()) {
            uidList.add(clubMember.getId());
        }
        for (Long uid : uidList) {
            ClubMember promotionClubMember = null;
            if (Config.isShare()) {
                promotionClubMember = ShareClubMemberMgr.getInstance().getClubMember(uid);
            } else {
                promotionClubMember = this.getClubMemberMap().get(uid);
            }
            if (Objects.isNull(promotionClubMember) || promotionClubMember.isClubCreate()) {
                continue;
            }
            //获取某个人的全部下级的竞技点总和
            Double totalPoint = this.getTotalPoint(promotionClubMember);
            if (totalPoint < promotionClubMember.getClubMemberBO().getAlivePoint()) {
                FlowLogger.sportsPointWarningLog(clubMember.getClubMemberBO().getPlayerID(), clubMember.getClubID(), club.getClubListBO().getUnionId(), uid, promotionClubMember.getClubMemberBO().getSportsPointWarning(), totalPoint, 0);
                return SData_Result.make(ErrorCode.CLUB_PERSONAL_ALIVE_POINT_WARN, "您所在的推广员队伍或上级队伍比赛分低于生存积分，无法加入比赛，请联系管理");
            }
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 检查个人竞技点预警值
     *
     * @param clubMember
     * @return
     */
    public SData_Result checkPersonalSportsPointWarning(ClubMember clubMember) {
        //如果不是亲友圈的话 不用检测
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.Success);
        }
        Club club = null;
        if (Config.isShare()) {
            club = ShareClubListMgr.getInstance().getClub(clubMember.getClubID());
        } else {
            club = ClubMgr.getInstance().getClubListMgr().getClubMap().get(clubMember.getClubID());
        }
        if (UnionDefine.UNION_WARN_STATUS.OPEN.ordinal() == clubMember.getClubMemberBO().getPersonalWarnStatus()) {

            if (clubMember.getSportsPoint() < clubMember.getClubMemberBO().getPersonalSportsPointWarning()) {
                FlowLogger.sportsPointWarningLog(clubMember.getClubMemberBO().getPlayerID(), clubMember.getClubID(), club.getClubListBO().getUnionId(), clubMember.getId(), clubMember.getClubMemberBO().getSportsPointWarning(), clubMember.getSportsPoint(), 0);
                return SData_Result.make(ErrorCode.Error_Code, "您的比赛分低于个人预警值，无法加入房间");
            }

        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 设置权限
     *
     * @param powerOp 推广员参数
     * @param pid     玩家Pid
     * @return
     */
    public SData_Result getClubPromotionLevelPowerOp(CClub_PromotionLevelPowerOp powerOp, long pid) {
        if (this.isNotClubCreate(powerOp.getClubId(), pid)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "getClubPromotionLevelPowerOp CLUB_NOT_CREATE");
        }
        ClubMember clubMember = this.find(powerOp.getPid(), powerOp.getClubId(), Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(clubMember) || clubMember.isNotLevelPromotion()) {
            return SData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "getClubPromotionLevelPowerOp CLUB_NOT_PROMOTION");
        }

        clubMember.getClubMemberBO().savePromotionLevelPowerOp(powerOp.getKicking(), powerOp.getModifyValue(), powerOp.getShowShare(), powerOp.getInvite());
        SharePlayer player = SharePlayerMgr.getInstance().getSharePlayerByOnline(powerOp.getPid());
        if (Objects.nonNull(player)) {
            player.pushProtoMq(SClub_PromotionLevelPowerChange.make(powerOp.getPid(), powerOp.getClubId(), clubMember.getClubMemberBO().getLevel(), clubMember.getClubMemberBO().getKicking(), clubMember.getClubMemberBO().getModifyValue(), clubMember.getClubMemberBO().getShowShare(), clubMember.getClubMemberBO().getInvite()));
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 推送消息到MQ
     *
     * @param topic
     * @param clubId
     * @param pid
     * @param notExistRoom
     * @param signEnumClubID
     * @param baseSendMsg
     */
    private void mqNotifyMessage(String topic, Long clubId, Long pid, Boolean notExistRoom, Long signEnumClubID, BaseSendMsg baseSendMsg) {
        MqClubMemberNotifyBo mqClubMemberNotifyBo = new MqClubMemberNotifyBo();
        mqClubMemberNotifyBo.setClubID(clubId);
        mqClubMemberNotifyBo.setPid(pid);
        mqClubMemberNotifyBo.setNotExistRoom(notExistRoom);
        mqClubMemberNotifyBo.setSignEnumClubID(signEnumClubID);
        mqClubMemberNotifyBo.setBaseSendMsg(baseSendMsg);
        mqClubMemberNotifyBo.setBaseSendMsgClassType(baseSendMsg.getClass().getName());
        MqProducerMgr.get().send(topic, mqClubMemberNotifyBo);
    }

    /**
     * 推送消息到MQ
     *
     * @param topic
     * @param clubId
     * @param pidList
     * @param notExistRoom
     * @param signEnumClubID
     * @param baseSendMsg
     */
    private void mqNotifyMessage(String topic, Long clubId, Long pid, List<Long> pidList, Boolean notExistRoom, Long signEnumClubID, BaseSendMsg baseSendMsg) {
        MqClubMemberNotifyBo mqClubMemberNotifyBo = new MqClubMemberNotifyBo();
        mqClubMemberNotifyBo.setClubID(clubId);
        mqClubMemberNotifyBo.setPidList(pidList);
        mqClubMemberNotifyBo.setNotExistRoom(notExistRoom);
        mqClubMemberNotifyBo.setSignEnumClubID(signEnumClubID);
        mqClubMemberNotifyBo.setBaseSendMsg(baseSendMsg);
        mqClubMemberNotifyBo.setBaseSendMsgClassType(baseSendMsg.getClass().getName());
        MqProducerMgr.get().send(topic, mqClubMemberNotifyBo);
    }

    /**
     * 获取某个玩家所有下级的竞技点之和
     *
     * @param promotionClubMember
     * @return
     */
    private Double getPromotionAllSportsPoint(ClubMember promotionClubMember) {
        UnionSportsPointAllValueItem item = EhCacheFactory.getCacheApi(WarningSportsCacheConfiguration.class).get(String.format(DataConstants.SPORTS_POINT_ALL_WARNING, promotionClubMember.getClubID(), promotionClubMember.getId()), UnionSportsPointAllValueItem.class);
        // 检查缓存是否为空
        if (Objects.isNull(item)) {
            // 检查数据库是否有数据
            Double sportsPointAll = 0D;
            //找到这个人的所有下级
            List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", promotionClubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
            // 上级关系
            List<Long> uidList = Lists.newArrayList();
            uidList.add(promotionClubMember.getId());
            if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
                // 整理上级：
                uidList.addAll(queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).map(k -> k.getUid()).collect(Collectors.toList()));
            }
            for (Long uid : uidList) {
                ClubMember clubMember = null;
                if (Config.isShare()) {
                    clubMember = ShareClubMemberMgr.getInstance().getClubMember(uid);
                } else {
                    clubMember = this.getClubMemberMap().get(uid);
                }
                if (Objects.isNull(clubMember)) {
                    continue;
                }
                sportsPointAll += clubMember.getTotalSportsPoint();
            }
            EhCacheFactory.getCacheApi(WarningSportsCacheConfiguration.class).put(String.format(DataConstants.SPORTS_POINT_ALL_WARNING, promotionClubMember.getClubID(), promotionClubMember.getId()), new UnionSportsPointAllValueItem(sportsPointAll));
            return sportsPointAll;
        }
        return item.getSportsPointAll();

    }

    /**
     * 中至获取某个玩家所有下级的最终纸盒
     *
     * @param promotionClubMember
     * @return
     */
    private Double getTotalPoint(ClubMember promotionClubMember) {
        //找到这个人的所有下级
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", promotionClubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        // 上级关系
        List<Long> uidList = Lists.newArrayList();
        uidList.add(promotionClubMember.getId());
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            // 整理上级：
            uidList.addAll(queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).map(k -> k.getUid()).collect(Collectors.toList()));
        }
        String key = String.format(DataConstants.PLAYER_TODYA_COUNT_ZHONGZHI, promotionClubMember.getClubMemberBO().getClubID(), promotionClubMember.getClubMemberBO().getPlayerID());
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            unionRoomConfigPrizePoolItem = new UnionCountByZhongZhiItem();
            ClubLevelRoomCountLogItem items = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", String.valueOf(CommTime.getCycleNowTime6YMD())),
                    Restrictions.in("memberId", uidList)), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            double allWinLose = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
            }
            unionRoomConfigPrizePoolItem.setAllWinLose(allWinLose);
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(allWinLose - items.getRoomSportsPointConsume());
            Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", 0);
            ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", String.valueOf(CommTime.getCycleNowTime6YMD())), Restrictions.in("memberId", uidList)), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsName());
            double promotionShareValue = 0D;
            if (Objects.nonNull(clubPromotionLevelItem)) {
                promotionShareValue = clubPromotionLevelItem.getPromotionShareValue();
            }
            unionRoomConfigPrizePoolItem.setFinalAllMemberPointTotal(CommMath.addDouble(unionRoomConfigPrizePoolItem.getUnionAllMemberPointTotal(), promotionShareValue));
        }
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return unionRoomConfigPrizePoolItem.getFinalAllMemberPointTotal();

    }

    /**
     * 获取某个玩家的信息
     *
     * @param req
     * @param player
     */
    public SData_Result getMemberCaseSportsInfo(CClub_CaseSports req, Player player) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());

        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "null == union unionId:{%d} pid :{%d}", req.getUnionId(), player.getPid());
        }
        if (union.getUnionBO().getCaseStatus() == UnionDefine.UNION_CASE_STATUS.CLOSE.ordinal()) {
            return SData_Result.make(ErrorCode.UNION_NOT_OPEN_CASE_SPORT, " union case is close");
        }
        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(player.getPid());
        if (sharePlayer.getRoomInfo().getRoomId() > 0) {
            return SData_Result.make(ErrorCode.Exist_OtherRoom, "Exist_OtherRoom");
        }
        // 检查成员是否在本亲友圈。
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.getClubId(), Club_Player_Status.PLAYER_JIARU);

        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "null == clubMember ClubID:{%d} pid :{%d}", req.getClubId(), player.getPid());
        }
        return SData_Result.make(ErrorCode.Success, new ClubCaseSportsItem(player.getPid(), clubMember.getCaseSportsPoint(), clubMember.getSportsPoint()));
    }

    /**
     * 获取某个玩家的信息
     *
     * @param req
     * @param player
     */
    public SData_Result getMemberCaseSportsChange(CClub_CaseSports req, Player player) {
        // 检查成员是否在本亲友圈。
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.getClubId(), Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "null == clubMember ClubID:{%d} pid :{%d}", req.getClubId(), player.getPid());
        }

        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        UnionDefine.UNION_EXEC_TYPE type;
        double preCasePointValue = clubMember.getClubMemberBO().getCaseSportsPoint();
        double preSportPoint = clubMember.getSportsPoint();
        double pidValue;
        if (req.getType() == Club_define.Club_CASE_SPORTS_TYPE.ADD.ordinal()) {
            pidValue = req.getValue();
            type = UnionDefine.UNION_EXEC_TYPE.PLAYER_CASE_SPORTS_POINT_ADD;
            if (req.getValue() > clubMember.getClubMemberBO().getSportsPoint()) {
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "getMemberCaseSportsChange sportsPoint:{%f} value :{%f}", clubMember.getClubMemberBO().getSportsPoint(), req.getValue());
            }
        } else {
            pidValue = -req.getValue();
            if (req.getValue() > clubMember.getClubMemberBO().getCaseSportsPoint()) {
                return SData_Result.make(ErrorCode.NotEnough_CaseSportsPoint, "getMemberCaseSportsChange CaseSportsPoint:{%f} value :{%f}", clubMember.getClubMemberBO().getSportsPoint(), req.getValue());
            }
            type = UnionDefine.UNION_EXEC_TYPE.PLAYER_CASE_SPORTS_POINT_SUB;
        }
        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(player.getPid());
        clubMember.getClubMemberBO().saveCaseSportsPoint(sharePlayer, req.getValue(), type, req.getUnionId());
        UnionDynamicBO.insertCaseSportsRecord(player.getPid(), 0, req.getClubId(), CommTime.nowSecond(), type.value(), club.getClubListBO().getUnionId(), String.valueOf(req.getValue()), String.valueOf(clubMember.getClubMemberBO().getCaseSportsPoint()), String.valueOf(clubMember.getClubMemberBO().getSportsPoint()), ""
                , String.valueOf(clubMember.getClubMemberBO().getCaseSportsPoint()), String.valueOf(preCasePointValue), String.valueOf(pidValue), String.valueOf(clubMember.getSportsPoint()), String.valueOf(preSportPoint), String.valueOf(-pidValue));

        return SData_Result.make(ErrorCode.Success, new ClubCaseSportsItem(player.getPid(), clubMember.getCaseSportsPoint(), clubMember.getSportsPoint()));
    }

    /**
     * 获取推广员列表 常用操作
     *
     * @param promotionList 推广员参数
     * @param pid           玩家Pid
     * @return
     */
    public SData_Result getClubPromotionLevelListCommonOp(CClub_PromotionList promotionList, long pid) {
        // 获取推广员列表权限
        SData_Result result = this.getClubPromotionLevelListPowerItem(promotionList.getClubId(), promotionList.getPid(), pid);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionList.getClubId());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        List<Integer> showConfig = Arrays.asList(3, 7);
        // 推广员列表权限
        ClubLevelPromotionPowerItem clubLevelPromotionPowerItem = (ClubLevelPromotionPowerItem) result.getData();
        final long qPid = TypeUtils.StringTypeLong(promotionList.getQuery());
        final String query = promotionList.getQuery();
        if (Config.isShareLocal()) {
            List<ClubMember> getPromotionLevelList = (qPid <= 0L && query.isEmpty()) ? this.getPromotionLevelList(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), promotionList.getPageNum()) : getPromotionLevelList(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), qPid, promotionList.getPageNum(), query);
            List<ClubPromotionLevelItem> promotionItemList = getPromotionLevelList.stream().map(k -> this.getClubPromotionLevelItemCommonOp(k, clubLevelPromotionPowerItem.getLevelPower(), promotionList.getType())).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(promotionItemList)) {
                return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(showConfig, club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), Collections.emptyList(), promotionList.getType()));
            }
            return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(showConfig, club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), promotionItemList, promotionList.getType()));
        } else {
            List<String[]> getPromotionLevelList = (qPid <= 0L && query.isEmpty()) ? this.getPromotionLevelListShare(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), promotionList.getPageNum()) : getPromotionLevelListShare(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), qPid, promotionList.getPageNum(), query);
            List<ClubPromotionLevelItem> promotionItemList = getPromotionLevelList.stream().map(k -> this.getClubPromotionLevelItemCommonOpShare(k, clubLevelPromotionPowerItem.getLevelPower(), promotionList.getType())).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(promotionItemList)) {
                return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(showConfig, club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), Collections.emptyList(), promotionList.getType()));
            }
            return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(showConfig, club.getClubListBO().getPromotionShowClubConfigJson().getShowConfigSecond(), promotionItemList, promotionList.getType()));
        }
    }

//    /**
//     * 获取推广员列表 常用操作
//     *
//     * @param promotionList 推广员参数
//     * @param pid           玩家Pid
//     * @return
//     */
//    public SData_Result getClubPromotionLevelListCommonOp(CClub_PromotionList promotionList, long pid) {
//        // 获取推广员列表权限
//        SData_Result result = this.getClubPromotionLevelListPowerItem(promotionList.getClubId(), promotionList.getPid(), pid);
//        Club club = ClubMgr.getInstance().getClubListMgr().findClub(promotionList.getClubId());
//        if (!ErrorCode.Success.equals(result.getCode())) {
//            return result;
//        }
//        List<Integer> showConfig = Arrays.asList(3, 7, 9, 10);
//        // 推广员列表权限
//        ClubLevelPromotionPowerItem clubLevelPromotionPowerItem = (ClubLevelPromotionPowerItem) result.getData();
//        final long qPid = TypeUtils.StringTypeLong(promotionList.getQuery());
//        final String query = promotionList.getQuery();
//        List<ClubMember> getPromotionLevelList = (qPid <= 0L && query.isEmpty()) ? this.getPromotionLevelList(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), promotionList.getPageNum()) : getPromotionLevelList(promotionList.getClubId(), clubLevelPromotionPowerItem.getUid(), qPid, promotionList.getPageNum(), query);
//        List<ClubPromotionLevelItem> promotionItemList = getPromotionLevelList.stream().map(k -> this.getClubPromotionLevelItemCommonOp(k, clubLevelPromotionPowerItem.getLevelPower(), promotionList.getGetType())).filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
//        if (CollectionUtils.isEmpty(promotionItemList)) {
//            return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(showConfig, Collections.emptyList(), promotionList.getGetType()));
//        }
//        return SData_Result.make(ErrorCode.Success, new ClubPromotionLevelItemList(showConfig, promotionItemList, promotionList.getGetType()));
//    }

    /**
     * 推广员项 常用操作
     *
     * @param k 亲友圈成员
     * @return
     */
    public ClubPromotionLevelItem getClubPromotionLevelItemCommonOp(ClubMember k, Club_define.Club_PROMOTION_LEVEL_POWER levelPower, int type) {

        Player player = PlayerMgr.getInstance().getPlayer(k.getClubMemberBO().getPlayerID());
        if (Objects.nonNull(player)) {
            int myisminister = 0;
            if (k.isClubCreate() && Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                myisminister = 2;
            }
            return new ClubPromotionLevelItem(player.getPid(), myisminister, player.getName(), player.getHeadImageUrl(), k.getClubMemberBO().getShareType(),
                    k.getClubMemberBO().getShareValue(), k.getClubMemberBO().getShareFixedValue(), k.getSportsPointWarningPersonal(),
                    k.getSportsPointWarning(), k.getSportsPoint(), k.getClubMemberBO().getLevel());
        }
        return null;
    }

    /**
     * 推广员项 常用操作
     *
     * @param k 亲友圈成员
     * @return
     */
    public ClubPromotionLevelItem getClubPromotionLevelItemCommonOpShare(String[] k, Club_define.Club_PROMOTION_LEVEL_POWER levelPower, int type) {

        Player player = PlayerMgr.getInstance().getPlayer(ClubMemberUtils.getArrayValueLong(k, "playerID"));
        if (Objects.nonNull(player)) {
            int myisminister = 0;
            if (ClubMemberUtils.isClubCreate(ClubMemberUtils.getArrayValueInteger(k, "isminister")) && Club_define.Club_PROMOTION_LEVEL_POWER.CREATE.equals(levelPower)) {
                myisminister = 2;
            }
            return new ClubPromotionLevelItem(player.getPid(), myisminister, player.getName(), player.getHeadImageUrl(), ClubMemberUtils.getArrayValueInteger(k, "shareType"),
                    ClubMemberUtils.getArrayValueDouble(k, "shareValue"), ClubMemberUtils.getArrayValueDouble(k, "shareFixedValue"), ClubMemberUtils.getSportsPointWarningPersonal(ClubMemberUtils.getArrayValueInteger(k, "personalWarnStatus"), ClubMemberUtils.getArrayValueDouble(k, "personalSportsPointWarning")),
                    ClubMemberUtils.getSportsPointWarning(ClubMemberUtils.getArrayValueInteger(k, "level"), ClubMemberUtils.getArrayValueInteger(k, "warnStatus"), ClubMemberUtils.getArrayValueDouble(k, "sportsPointWarning")), ClubMemberUtils.getArrayValueDouble(k, "sportsPoint"), ClubMemberUtils.getArrayValueInteger(k, "level"));
        }
        return null;
    }

    /**
     * 获取玩家个人预警信息
     *
     * @param req 参数
     * @param pid 玩家Pid
     * @return
     */
    public SData_Result getReservedValueInfo(CClub_ReservedValueReq req, long pid) {
        SData_Result result = clubPersonalWarningPower(req.getClubId(), req.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberReversedValueBOService) ContainerMgr.get().getComponent(ClubMemberReversedValueBOService.class)).findAllE(Restrictions.and(Restrictions.eq("uid", clubMemberItem.getToClubMember().getId()), Restrictions.eq("puid", clubMemberItem.getDoClubMember().getId())), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameIdReversedValue());
        double value = 0D;
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            value = queryUidOrPidItemList.get(0).getReservedValue();
        }
        Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        return SData_Result.make(ErrorCode.Success, new ClubReservedValueItem(player.getPid(), player.getName(),
                value));
    }

    /**
     * 修改推广员预留值
     *
     * @param req 推广员参数
     * @param pid 玩家Pid
     * @return
     */
    public SData_Result changeReservedValueChange(CClub_PromotionCalcActiveBatch req, long pid) {
        if (UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal() == req.getType()) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_PROMOTION_PERCENT_IS_NOT_ALLOW_MULTI, " CLUB_MEMBER_PROMOTION_PERCENT_IS_NOT_ALLOW_MULTI");
        }
        SData_Result result = clubPromotionWarningPower(req.getClubId(), req.getPid(), pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        ClubMemberItem clubMemberItem = (ClubMemberItem) result.getData();
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberReversedValueBOService) ContainerMgr.get().getComponent(ClubMemberReversedValueBOService.class)).findAllE(Restrictions.and(Restrictions.eq("uid", clubMemberItem.getToClubMember().getId()), Restrictions.eq("puid", clubMemberItem.getDoClubMember().getId())), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameIdReversedValue());
        ClubMember exeClubMember = clubMemberItem.getToClubMember();
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST" + req.getClubId());
        }
        double preValue = 0D;
        ClubMemberReversedValueBo clubMemberReversedValueBo = new ClubMemberReversedValueBo();
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            preValue = queryUidOrPidItemList.get(0).getReservedValue();
            clubMemberReversedValueBo.setId(queryUidOrPidItemList.get(0).getId());
            if (preValue == req.getValue()) {
                return SData_Result.make(ErrorCode.Success);
            }
        }
        clubMemberReversedValueBo.setPuid(clubMemberItem.getDoClubMember().getId());
        clubMemberReversedValueBo.setUid(exeClubMember.getId());
        clubMemberReversedValueBo.setReversedValue(req.getValue());
        ((ClubMemberReversedValueBOService) ContainerMgr.get().getComponent(ClubMemberReversedValueBOService.class)).saveOrUpDate(clubMemberReversedValueBo);
        Player player = PlayerMgr.getInstance().getPlayer(clubMemberItem.getToClubMember().getClubMemberBO().getPlayerID());
        UnionDynamicBO.insertSportsPointLog(exeClubMember.getClubMemberBO().getPlayerID(), exeClubMember.getClubID(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_RESERVED_VALUE_CHANGE.value(),
                club.getClubListBO().getUnionId(), String.valueOf(preValue), String.valueOf(CommMath.FormatDouble(req.getValue())));
        return SData_Result.make(ErrorCode.Success, new ClubReservedValueItem(player.getPid(), player.getName(),
                CommMath.FormatDouble(req.getValue())));

    }

    /**
     * 检查查看禁止房间的权限够不够
     *
     * @return
     */
    public SData_Result checkClubGetMemberPromotionList(long clubID, long pid) {
        //如果是亲友圈 圈主或者管理员
        if (ClubMgr.getInstance().getClubMemberMgr().isMinister(clubID, pid)) {
            return SData_Result.make(ErrorCode.Success);
        }
        //获取当前查询的亲友圈成员信息
        ClubMember doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, pid);
        if (null == doClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "doClubMember CLUB_NOT_EXIST_MEMBER_INFO exePid:{%d},clubId:{%d}", pid, clubID);
        }
        if (doClubMember.isLevelPromotion() || doClubMember.isPromotionManage()) {

            return SData_Result.make(ErrorCode.Success);
        }
        return SData_Result.make(ErrorCode.UNION_POWER_ERROR, "UNION_POWER_ERROR");
    }

    /**
     * 检查查看添加房间的权限够不够
     *
     * @return
     */
    public SData_Result checkClubGetMemberPromotionListAdd(long clubID, long pid, long addPid) {
        //如果是亲友圈 圈主或者管理员
        if (ClubMgr.getInstance().getClubMemberMgr().isMinister(clubID, pid)) {
            return SData_Result.make(ErrorCode.Success);
        }
        //获取当前查询的亲友圈成员信息
        ClubMember doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, pid);
        if (null == doClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "doClubMember CLUB_NOT_EXIST_MEMBER_INFO exePid:{%d},clubId:{%d}", pid, clubID);
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST clubId:{%d}", clubID);
        }
        if (doClubMember.isLevelPromotion() || doClubMember.isPromotionManage()) {
            List<Long> pidList = new ArrayList<>();
            //获取当前查询的亲友圈成员信息
            if (doClubMember.isLevelPromotion()) {
                club.getClubMemberPlayerIDList(doClubMember.getId(), pidList);

            } else if (doClubMember.isPromotionManage()) {
                club.getClubMemberPlayerIDList(doClubMember.getClubMemberBO().getUpLevelId(), pidList);
            } else {
                return SData_Result.make(ErrorCode.UNION_POWER_ERROR, "UNION_POWER_ERROR");
            }
            if (addPid > 0) {
                if (pidList.stream().anyMatch(k -> k.intValue() == addPid)) {
                    return SData_Result.make(ErrorCode.Success);
                }
            } else {
                return SData_Result.make(ErrorCode.Success);
            }
        }
        return SData_Result.make(ErrorCode.UNION_POWER_ERROR, "UNION_POWER_ERROR");
    }

    /**
     * 查看房间分成
     * 在玩家基础上
     *
     * @param req 推广员参数
     *            .asc("execTime") 排序
     * @return
     */
    public SData_Result getRoomPromotionPointList(CClub_RoomPromotionPoint req) {
        //根据type判断要查的是哪个表的数据
        List<ClubRoomPromotionPointInfo> items = new ArrayList<>();
        //getType 0 今天  就实时统计
        if (req.getGetType() == Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TODAY.value()) {
            // 查询并统计昨天的数据
            items = ContainerMgr.get().getComponent(RoomPromotionPointLogFlowService.class).findAllE(Restrictions.eq("pid", req.getPid()).groupBy("roomId`,`pid"), ClubRoomPromotionPointInfo.class, ClubRoomPromotionPointInfo.getItemsNameForListByType(req.getGetType()));
            if (CollectionUtils.isNotEmpty(items)) {
                items.forEach(k -> {
                    //计算这个房间动态变化前后 玩家身上的竞技点 取id最大最小对应的 curRemainder preValue即可
                    RoomPromotionPointLogFlow roomPromotionPointLogFlowMax = ContainerMgr.get().getComponent(RoomPromotionPointLogFlowService.class).findOne(k.getMaxID());
                    RoomPromotionPointLogFlow roomPromotionPointLogFlowMin = ContainerMgr.get().getComponent(RoomPromotionPointLogFlowService.class).findOne(k.getMinID());
                    double curRemainder = Objects.isNull(roomPromotionPointLogFlowMax) ? 0.0 : roomPromotionPointLogFlowMax.getCurRemainder();
                    double preValue = Objects.isNull(roomPromotionPointLogFlowMin) ? 0.0 : roomPromotionPointLogFlowMin.getPreValue();
                    int execTime = Objects.isNull(roomPromotionPointLogFlowMin) ? 0 : roomPromotionPointLogFlowMin.getTimestamp();
                    k.setCurRemainder(curRemainder);
                    k.setPreValue(preValue);
                    k.setExecTime(execTime);
                    k.setGetType(req.getGetType());
                });
            }
        } else if (req.getGetType() == Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_YESTERDAY.value() || req.getGetType() == Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TWO.value()) {
            //getType 1昨天 去统计表中查询
            //getType 2 前台 去统计表中查询
            Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("dateTime", req.getGetType());
            items = ContainerMgr.get().getComponent(RoomPromotionPointCountLogFlowService.class).findAllE(Restrictions.and(zeroClockS, Restrictions.eq("pid", req.getPid())), ClubRoomPromotionPointInfo.class, ClubRoomPromotionPointInfo.getItemsNameForCountList(req.getGetType()));
        }

        return SData_Result.make(ErrorCode.Success, items);
    }

    /**
     * 查看房间分成
     * 在玩家基础上
     *
     * @param req    推广员参数
     * @param player 玩家
     * @return
     */
    public SData_Result getRoomPromotionPointDetailList(CClub_RoomPromotionPoint req, Player player) {
        //根据type判断要查的是哪个表的数据
        List<ClubRoomPromotionPointDetailItem> items = new ArrayList<>();
        //根据type不同 去不同的数据表中查询数据
        if (req.getGetType() == Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TODAY.value()) {
            items = ContainerMgr.get().getComponent(RoomPromotionPointLogFlowService.class).findAllE(Restrictions.and(Restrictions.eq("pid", req.getPid()), Restrictions.eq("roomId", req.getRoomId())), ClubRoomPromotionPointDetailItem.class, ClubRoomPromotionPointDetailItem.getItemsName());
        } else if (req.getGetType() == Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_YESTERDAY.value()) {
            items = ContainerMgr.get().getComponent(RoomPromotionPointLogFlowService.class).getDetailBeforDay(Restrictions.and(Restrictions.eq("pid", req.getPid()), Restrictions.eq("roomId", req.getRoomId())), ClubRoomPromotionPointDetailItem.class, ClubRoomPromotionPointDetailItem.getItemsName(), CommTime.getYesterDayStringYMD(1));
        } else if (req.getGetType() == Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TWO.value()) {
            items = ContainerMgr.get().getComponent(RoomPromotionPointLogFlowService.class).getDetailBeforDay(Restrictions.and(Restrictions.eq("pid", req.getPid()), Restrictions.eq("roomId", req.getRoomId())), ClubRoomPromotionPointDetailItem.class, ClubRoomPromotionPointDetailItem.getItemsName(), CommTime.getYesterDayStringYMD(2));
        }
        if (CollectionUtils.isNotEmpty(items)) {
            items.forEach(k -> {
                //房费的时候这两个都为0
                if (k.getExecPid() <= 0 && k.getReasonPid() <= 0) {
                    k.setType(Club_define.CLUB_Room_Promotion_Point_TYPE.ROOM_CONSUME.value());
                } else {
                    SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(k.getReasonPid());
                    if (Objects.nonNull(sharePlayer)) {
                        k.setReasonPidName(sharePlayer.getPlayerBO().getName());
                    }
                    k.setType(k.getNum() > 0 ? Club_define.CLUB_Room_Promotion_Point_TYPE.UP_LEVEL.value() : Club_define.CLUB_Room_Promotion_Point_TYPE.DAWN_LEVEL.value());
                }
            });
        }
        return SData_Result.make(ErrorCode.Success, items);
    }

    /**
     * 改变亲友圈退出加入需要审核功能
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result changeTotalPointShowStatus(CClub_ChangeTotalPointShowStatus req, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        // 检查亲友圈是否存在。
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (isNotClubCreate(req.getClubId(), pid)) {
            // 不是亲友圈管理员
            return SData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CLUB_NOT_CREATE");
        }
        club.getClubListBO().saveTotalPointShowStatus(req.getType());
        return SData_Result.make(ErrorCode.Success, SClub_TotalPointShowStatus.make(req.getClubId(), req.getType()));

    }


}
