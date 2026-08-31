package business.global.union;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.shareclub.ShareClubListMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.sharegm.ShareInitMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.shareunion.ShareUnionMemberMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqUnionMemberNotifyBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import business.utils.TimeConditionUtils;
import cenum.ItemFlow;
import cenum.Page;
import cenum.RoomTypeEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.GameConfig;
import com.ddm.server.common.ehcache.DataConstants;
import com.ddm.server.common.ehcache.EhCacheFactory;
import com.ddm.server.common.ehcache.configuration.UnionRoomConfigPrizePoolCacheConfiguration;
import com.ddm.server.common.ehcache.configuration.UnionSportsProfitWeekCacheConfiguration;
import com.ddm.server.common.ehcache.configuration.UnionZhongZhiCountInfoCacheConfiguration;
import com.ddm.server.common.ehcache.configuration.WarningSportsCacheConfiguration;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Maps;
import com.ddm.server.common.utils.TypeUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import core.db.entity.clarkGame.*;
import core.db.other.AsyncInfo;
import core.db.other.Criteria;
import core.db.other.MatchMode;
import core.db.other.Restrictions;
import core.db.service.clarkGame.*;
import core.db.service.clarkLog.*;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.union.UnionNotify2AllByClubEvent;
import core.dispatch.event.union.UnionNotify2ClubAllMember;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.QueryIdItem;
import jsproto.c2s.cclass.club.*;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.union.*;
import jsproto.c2s.iclass.union.*;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static jsproto.c2s.cclass.union.UnionDefine.UNION_EXEC_TYPE.UNION_ROOM_QIEPAI_INCOME;

@Data
public class UnionMemberMgr {
    /**
     * 联赛成员map
     */
    private Map<Long, UnionMember> unionMemberMap = Maps.newConcurrentMap();

    public void init() {
        CommLogD.info("[UnionMemberBO.init] load UnionMemberBO begin...]");
        List<UnionMemberBO> unionMemberBOList = ContainerMgr.get().getComponent(UnionMemberBOService.class).findAll(null);
        if (null == unionMemberBOList || unionMemberBOList.size() <= 0) {
            return;
        }
        for (UnionMemberBO unionMemberBO : unionMemberBOList) {
            if (UnionDefine.UNION_PLAYER_STATUS.initMember(unionMemberBO.getStatus())) {
                if (unionMemberBO.getUnionId() <= 0L || unionMemberBO.getClubOwnerId() <= 0L || unionMemberBO.getClubId() <= 0L) {
                    unionMemberBO.getBaseService().delete(unionMemberBO.getId(), new AsyncInfo(unionMemberBO.getId()));
                    if (Config.isShare()) {
                        ShareUnionMemberMgr.getInstance().deleteClubMember(unionMemberBO.getId());
                    }
                } else {
                    getUnionMemberMap().put(unionMemberBO.getId(), new UnionMember(unionMemberBO));
                    if (Config.isShare() && ShareInitMgr.getInstance().getShareDataInit()) {
                        if (!ShareUnionMemberMgr.getInstance().existUnionMember(unionMemberBO.getId())) {
                            ShareUnionMemberMgr.getInstance().addClubMember(getUnionMemberMap().get(unionMemberBO.getId()));
                        }
                    }
                }
            }
        }
        unionMemberBOList = null;
        CommLogD.info("[UnionMemberBO.init] load UnionMemberBO end]");
    }

    /**
     * 查询指定玩家
     *
     * @param pid    玩家Pid
     * @param status 状态
     * @return
     */
    public List<UnionMember> findPidAll(long pid, UnionDefine.UNION_PLAYER_STATUS status) {
        if (Config.isShare()) {
            List<UnionMemberBO> unionMemberBOList = ContainerMgr.get().getComponent(UnionMemberBOService.class).findAll(Restrictions.and(Restrictions.eq("clubOwnerId", pid)));
            List<UnionMember> unionMemberList = new ArrayList<>();
            unionMemberBOList.forEach(k -> unionMemberList.add(ShareUnionMemberMgr.getInstance().getUnionMember(k.getId())));
            return unionMemberList.stream().filter(k -> k != null && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(status.value())).collect(Collectors.toList());
        } else {
            return this.getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(status.value())).collect(Collectors.toList());
        }
    }

    /**
     * 赛事人数已满
     *
     * @param unionId 赛事Id
     * @param clubID  赛事Id
     * @return
     */
    public boolean checkUnionMemberUpperLimit(long unionId, long clubID) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubID && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .count() >= GameConfig.ClubMemberUpperLimit();
        } else {
            return this.getUnionMemberMap().values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubID && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .count() >= GameConfig.ClubMemberUpperLimit();
        }
    }

    /**
     * 自己加入的赛事数达到上限
     *
     * @param unionId 赛事Id
     * @param pid     玩家PID
     * @param clubID  赛事Id
     * @return
     */
    public boolean checkPlayerUnionUpperLimit(long unionId, long clubID, long pid) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream().filter(
                            k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubID && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .count() >= GameConfig.ClubPlayerJoinUpperLimit();
        } else {
            return this.getUnionMemberMap().values().stream().filter(
                            k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubID && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .count() >= GameConfig.ClubPlayerJoinUpperLimit();
        }
    }

    /**
     * 检查是否管理员上限
     *
     * @param clubID 赛事ID
     * @return
     */
    public boolean checkUnionMinisterUpperLimit(long unionId, long clubID) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream().filter(
                            k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubID && k.isManage() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .count() >= GameConfig.ClubMinisterUpperLimit();
        } else {
            return this.getUnionMemberMap().values().stream().filter(
                            k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubID && k.isManage() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .count() >= GameConfig.ClubMinisterUpperLimit();
        }
    }

    /**
     * 获取联赛亲友圈统计
     *
     * @param unionId 联赛Id
     * @return
     */
    public int getUnionClubCount(long unionId) {
        if (Config.isShare()) {
            return (int) ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).count();
        } else {
            return (int) this.getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).count();
        }
    }

    /**
     * 更新状态 没有时插入
     **/
    public boolean onJoin(Player player, long clubId, long unionId, long ownerID, long clubMemberId) {
        UnionMember unionMember = this.find(player.getPid(), clubId, unionId);
        if (null == unionMember) {
            // 添加赛事成员(申请加入赛事)
            return this.onInsertUnionMember(player, unionId, clubId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN.value(), 0L, clubMemberId);
        } else {
            int status = 0;
            long exePid = 0L;
            if (unionMember.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_YAOQING.value())) {
                // 如果受到邀请就可以直接进入赛事
                status = UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value();
                // 邀请人
                exePid = unionMember.getUnionMemberBO().getInvitationPid();
            } else if (unionMember.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN.value())) {
                // 未批准
                return false;
            } else if (unionMember.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())) {
                // 已加入赛事了
                return false;
            } else {
                // 否则未批准状态
                status = UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN.value();
            }
            unionMember.setStatus(status, exePid);
        }
        return true;
    }


    /**
     * 获取赛事成员
     *
     * @param pid     玩家PID
     * @param unionId 赛事ID
     * @return
     */
    public UnionMember find(long pid, long clubId, long unionId) {
        return find(pid, clubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_ALL);
    }

    /**
     * 查询创建者亲友圈成员id
     *
     * @param unionId
     * @return
     */
    public long findCreateClubMemberId(long unionId) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getType() == UnionDefine.UNION_POST_TYPE.UNION_CREATE.value() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubMemberId()).findAny().orElse(0L);
        } else {
            return this.getUnionMemberMap().values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getType() == UnionDefine.UNION_POST_TYPE.UNION_CREATE.value() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubMemberId()).findAny().orElse(0L);
        }
    }


    /**
     * 不是管理员、创建者
     *
     * @param pid     玩家PID
     * @param unionId 赛事ID
     * @return
     */
    public boolean isNotManage(long pid, long clubId, long unionId) {
        if (Config.isShare()) {
            return null == ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream()
                    .filter(k -> k.isManage() && k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny()
                    .orElse(null);
        } else {
            return null == this.getUnionMemberMap().values().stream()
                    .filter(k -> k.isManage() && k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny()
                    .orElse(null);
        }
    }

    /**
     * 不是管理员、创建者  加入赛事创建者设置的本身亲友圈 赛事副裁判权限
     *
     * @param pid     玩家PID
     * @param unionId 赛事ID
     * @return
     */
    public boolean isNotUnionManage(long pid, long clubId, long unionId) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
        if (Objects.isNull(club) || club.getClubListBO().getUnionId() != unionId) {
            // 亲友圈找不到或者不是赛事亲友圈
            return true;
        }
        if (club.getClubListBO().getOwnerID() == pid) {
            // 检查是否有权限
            if (Config.isShare()) {
                return !ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream().anyMatch(k -> k.isManage() && k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()));
            } else {
                return !this.getUnionMemberMap().entrySet().stream().anyMatch(k -> k.getValue().isManage() && k.getValue().getUnionMemberBO().getUnionId() == unionId && k.getValue().getUnionMemberBO().getClubId() == clubId && k.getValue().getUnionMemberBO().getClubOwnerId() == pid && k.getValue().getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()));
            }
        } else {
            // 查询玩家Id(者赛事副裁判权限)
            QueryIdItem queryIdItem = ContainerMgr.get().getComponent(ClubMemberBOService.class).findOneE(Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.eq("playerID", pid), Restrictions.eq("isminister", Club_define.Club_MINISTER.Club_MINISTER_UNIONMGR.value())), QueryIdItem.class, QueryIdItem.getItemsName());
            return Objects.isNull(queryIdItem) || queryIdItem.getId() <= 0L;
        }

    }

    /**
     * 不是管理员、创建者
     *
     * @param pid     玩家PID
     * @param unionId 赛事ID
     * @return
     */
    public int getMinister(long pid, long clubId, long unionId) {
        CommLogD.info("getMinister节点{}", Config.nodeName());
        UnionMember unionMember;
        if (Config.isShare()) {
            unionMember = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream().filter(k -> k.isManage() && k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny().orElse(null);
        } else {
            unionMember = this.getUnionMemberMap().values().stream().filter(k -> k.isManage() && k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny().orElse(null);
        }
        //赛事管理员  盟主设置自己的亲友圈队友
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, pid);
        if (Objects.nonNull(clubMember) && clubMember.isUnionMgr()) {
            // 赛事管理员权限
            return 2;
        }
        if (Objects.isNull(unionMember)) {
            return 0;
        }
        int type = unionMember.getUnionMemberBO().getType();
        if (UnionDefine.UNION_POST_TYPE.UNION_CREATE.value() == type) {
            // 创建者权限
            return 2;
        } else if (UnionDefine.UNION_POST_TYPE.UNION_MANAGE.value() == type) {
            // 管理员权限
            return 1;
        }
        // 普通权限
        return 0;
    }

    /**
     * 共享不是管理员、创建者
     *
     * @param pid     玩家PID
     * @param unionId 赛事ID
     * @return
     */
    public int getMinisterShare(long pid, long clubId, long unionId) {
        CommLogD.info("getMinisterShare节点{}", Config.nodeName());
        UnionMember unionMember = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream().filter(k -> k.isManage() && k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny().orElse(null);
        //赛事管理员  盟主设置自己的亲友圈队友
        ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, pid);
        if (Objects.nonNull(clubMember) && clubMember.isUnionMgr()) {
            // 赛事管理员权限
            return 2;
        }
        if (Objects.isNull(unionMember)) {
            return 0;
        }
        int type = unionMember.getUnionMemberBO().getType();
        if (UnionDefine.UNION_POST_TYPE.UNION_CREATE.value() == type) {
            // 创建者权限
            return 2;
        } else if (UnionDefine.UNION_POST_TYPE.UNION_MANAGE.value() == type) {
            // 管理员权限
            return 1;
        }
        // 普通权限s
        return 0;
    }

    /**
     * 获取赛事成员
     *
     * @param pid     玩家PID
     * @param unionId 赛事ID
     * @param status  状态
     * @return
     */
    public UnionMember find(long pid, long clubId, long unionId, UnionDefine.UNION_PLAYER_STATUS status) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(status.value())).findAny()
                    .orElse(null);
        } else {
            return this.getUnionMemberMap().values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(status.value())).findAny()
                    .orElse(null);
        }

    }

    /**
     * 获取赛事成员
     *
     * @param pid     玩家PID
     * @param unionId 赛事ID
     * @param type    权限
     * @return
     */
    public UnionMember findMinister(long pid, long clubId, long unionId, UnionDefine.UNION_POST_TYPE type) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && type.equals(k.getUnionMemberBO().getPostType()) && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny()
                    .orElse(null);
        } else {
            return this.getUnionMemberMap().values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && type.equals(k.getUnionMemberBO().getPostType()) && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny()
                    .orElse(null);
        }
    }

    /**
     * 是否赛事成员
     *
     * @param unionId 赛事Id
     * @param pid     玩家Pid
     * @return
     */
    public boolean anyMatchMinister(long unionId, long pid) {
        return this.getUnionMemberMap()
                .values()
                .stream()
                .anyMatch(k -> k.getUnionMemberBO().getUnionId() == unionId && k.isManage() && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()));
    }

    /**
     * 是否赛事成员
     *
     * @param clubId  亲友圈id
     * @param unionId 赛事Id
     * @param pid     玩家Pid
     * @return
     */
    public boolean anyMatch(long clubId, long unionId, long pid) {
        return this.getUnionMemberMap()
                .values()
                .stream()
                .anyMatch(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()));
    }

    /**
     * 是否赛事成员 或者赛事管理员
     *
     * @param clubId  亲友圈id
     * @param unionId 赛事Id
     * @param pid     玩家Pid
     * @return
     */
    public boolean anyMatchUnionOrUnionMgr(long clubId, long unionId, long pid) {
        boolean isUnionMember = this.getUnionMemberMap()
                .values()
                .stream()
                .anyMatch(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()));
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, pid);
        boolean isUnionMgr = Objects.nonNull(clubMember) && clubMember.isUnionMgr();
        return isUnionMember || isUnionMgr;
    }

    /**
     * 是否赛事成员
     *
     * @param clubId  亲友圈id
     * @param unionId 赛事Id
     * @param pid     玩家Pid
     * @return
     */
    public boolean isNotAnyMatch(long clubId, long unionId, long pid) {
        return !anyMatch(clubId, unionId, pid);
    }

    /**
     * 获取赛事成员列表
     *
     * @param clubId 玩家PID
     * @param status 状态
     * @return
     */
    public List<UnionMember> clubIdFindList(long clubId, UnionDefine.UNION_PLAYER_STATUS status) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneClubUnionMember(clubId).values().stream()
                    .filter(k -> k.getUnionMemberBO().getClubId() == clubId && k.getStatus(status.value())).collect(Collectors.toList());
        } else {
            return this.getUnionMemberMap().values().stream()
                    .filter(k -> k.getUnionMemberBO().getClubId() == clubId && k.getStatus(status.value())).collect(Collectors.toList());
        }
    }

    /**
     * 获取赛事成员*
     *
     * @param unionId 赛事ID
     * @param status  状态
     * @return
     */
    public UnionMember find(long clubId, long unionId, UnionDefine.UNION_PLAYER_STATUS status) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getStatus(status.value())).findAny()
                    .orElse(null);
        } else {
            return this.getUnionMemberMap().values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getUnionMemberBO().getClubId() == clubId && k.getStatus(status.value())).findAny()
                    .orElse(null);
        }

    }


    /**
     * 检查亲友圈是否已经加入赛事
     *
     * @param clubId 赛事ID
     * @return
     */
    public boolean clubHasJoinedUnion(long clubId) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneClubUnionMember(clubId).values().stream().anyMatch(k -> k.getUnionMemberBO().getClubId() == clubId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()));
        } else {
            return this.getUnionMemberMap().values().stream().anyMatch(k -> k.getUnionMemberBO().getClubId() == clubId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()));
        }

    }

    /**
     * 玩家赛事列表
     *
     * @param pid
     * @return
     */
    public List<Long> playerUnionList(long pid) {
        List<Long> clubList = ClubMgr.getInstance().getClubMemberMgr().playerClubList(pid);
        if (Config.isShare()) {
            List<Long> oneUnionList = new ArrayList<>();
            for (Long clubId : clubList) {
                oneUnionList = ShareUnionMemberMgr.getInstance().getAllOneClubUnionMember(clubId).values().stream().filter(k -> k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getUnionId()).collect(Collectors.toList());
                clubList.addAll(oneUnionList);
            }
            return clubList.stream().distinct().collect(Collectors.toList());
        } else {
            return this.getUnionMemberMap().values().stream().filter(k -> clubList.contains(k.getUnionMemberBO().getClubId()) && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getUnionId()).distinct().collect(Collectors.toList());
        }
    }

    /**
     * 获取赛事成员pid列表
     *
     * @param unionId 赛事Id
     * @return
     */
    public List<Long> getUnionMemberPidList(long unionId) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubOwnerId()).collect(Collectors.toList());
        } else {
            return this.getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubOwnerId()).collect(Collectors.toList());
        }
    }

    /**
     * 获取赛事成员clubId列表
     *
     * @param unionId 赛事Id
     * @return
     */
    public List<Long> getUnionMemberClubIdList(long unionId) {
        return this.getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
    }

    /**
     * 操作添加赛事成员
     *
     * @param player  玩家信息
     * @param unionId 赛事Id
     * @param clubId  赛事Id
     * @param status  状态
     * @param exePid  操作者Pid
     * @return
     */
    public boolean onInsertUnionMember(Player player, long unionId, long clubId, int status, long exePid, long clubMemberId) {
        return this.onInsertUnionMember(player, null, unionId, clubId, 0L, UnionDefine.UNION_POST_TYPE.UNION_CLUB.value(), status, exePid, clubMemberId);
    }

    /**
     * 操作添加赛事成员（已加入）
     *
     * @param player 玩家信息
     * @param union  赛事信息
     * @param type   成员身份(0,1,2)
     * @return
     */
    public boolean onInsertUnionMember(Player player, Union union, long clubId, int type, long exePid, long clubMemberId) {
        return this.onInsertUnionMember(player, union, 0L, clubId, 0L, type, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value(), exePid, clubMemberId);
    }

    /**
     * 操作添加赛事成员
     *
     * @param player   玩家信息
     * @param union    赛事信息
     * @param unionId  赛事Id
     * @param clubId   赛事id
     * @param playerId 赛事创建者id
     * @param type     成员职务类型
     * @param status   成员状态
     * @param exePid   操作者Pid
     * @return
     */
    public boolean onInsertUnionMember(Player player, Union union, long unionId, long clubId, long playerId, int type, int status, long exePid, long clubMemberId) {
        // 获取玩家信息
        player = null == player ? PlayerMgr.getInstance().getPlayer(playerId) : player;
        if (null == player) {
            // 找不到玩家信息
            CommLogD.error("null == player Pid:{}", playerId);
            return false;
        }
        // 获取赛事信息
        union = null == union ? UnionMgr.getInstance().getUnionListMgr().findUnion(unionId) : union;
        if (null == union) {
            // 找不到赛事信息
            CommLogD.error("null == union unionId:{}", unionId);
            return false;
        }

        // 检查亲友圈是否已经加入赛事
        if (this.clubHasJoinedUnion(clubId)) {
            // 亲友圈已加入赛事了
            return true;
        }
        // 获取赛事成员
        UnionMember unionMember = this.find(player.getPid(), clubId, union.getUnionBO().getId());
        if (null != unionMember) {
            if (unionMember.getStatus(status)) {
                // 新旧状态设置一样
                return false;
            } else if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_YAOQING.value() == unionMember.getUnionMemberBO().getStatus() || UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN.value() == unionMember.getUnionMemberBO().getStatus()) {
                // 成员本身状态为 已邀请 或者 未批准状态则更新。
                unionMember.setStatus(player, union, status, exePid);
                return true;
            } else if (unionMember.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())) {
                return false;
            }
            return false;
        }
        UnionMemberBO unionMemberBO = new UnionMemberBO();
        // 赛事Id
        unionMemberBO.setUnionId(union.getUnionBO().getId());
        // 赛事创建者Id
        unionMemberBO.setClubOwnerId(player.getPid());
        // 赛事Id
        unionMemberBO.setClubId(clubId);
        // 成员职务类型
        unionMemberBO.setType(type);
        // 0x01未批准,0x02已拒绝加入,0x04为已加入,0x08为已踢出,0x10为已邀请,0x20为拒绝邀请,0x40已退出
        unionMemberBO.setStatus(status);
        unionMemberBO.setClubMemberId(clubMemberId);
        // 创建时间
        unionMemberBO.setCreateTime(CommTime.nowSecond());
        if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value() == status) {
            unionMemberBO.setUpdateTime(CommTime.nowSecond());
        } else if (status == UnionDefine.UNION_PLAYER_STATUS.PLAYER_YAOQING.value()) {
            // 设置邀请人Pid
            unionMemberBO.setInvitationPid(exePid);
        }
        boolean flag = ((UnionMemberBOService) unionMemberBO.getBaseService()).saveIgnoreOrUpDate(unionMemberBO) > 0L;
        if (flag) {
            unionMember = new UnionMember(unionMemberBO);
            //中至修改玩家身上的个人淘汰分
            if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
//                clubMemberInfo.setEliminatePoint(union.getUnionBO().getOutSports());
                double outSports = union.getUnionBO().getOutSports();
                ClubMgr.getInstance().getClubMemberMgr().findClubIdAllClubMember(clubId, Club_define.Club_Player_Status.PLAYER_JIARU.value()).stream().forEach(l -> {
                    l.getClubMemberBO().saveEliminatePoint(outSports);
                });
            }
            this.getUnionMemberMap().put(unionMemberBO.getId(), unionMember);
            if (Config.isShare()) {
                ShareUnionMemberMgr.getInstance().addClubMember(unionMember);
            }
            // 通知玩家本身和所有的管理员
            if (UnionDefine.UNION_POST_TYPE.UNION_CREATE.value() == type) {
                // 赛事创建者
                UnionDynamicBO.insertUnionClub(player.getPid(), unionMemberBO.getClubId(), union.getUnionBO().getId(), 0L, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_CREATER.value(), String.valueOf(union.getUnionBO().getInitSports()));
            } else {
                if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value() == status) {
                    // 只有真正加入赛事的亲友圈才会进入赛事消息
                    // 添加赛事流水
                    unionMember.insertUnionDynamicBO(clubId, unionMemberBO.getUnionId(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_JIARU_NOT, unionMemberBO, 0L);
                }
                if (UnionDefine.UNION_POST_TYPE.UNION_MANAGE.value() == type) {
                    // 加入赛事就设置为管理员
                    unionMember.insertUnionDynamicBO(player.getPid(), unionMemberBO.getUnionId(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_BECOME_MGR, unionMemberBO, exePid);
                }
            }
            if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value() == status) {
                // 亲友圈加入赛事
                this.clubJoinOrQuitUnion(clubId, union.getUnionBO().getId(), union);
            }
            return invitedPlayer(union, player, clubId, status, exePid);
        } else {
            CommLogD.error("saveIgnoreOrUpDate insert UnionMemberBO fail");
            return false;
        }
    }

    /**
     * 亲友圈加入或退出赛事
     *
     * @param clubId  亲友圈id
     * @param unionId 赛事id
     */
    public void clubJoinOrQuitUnion(long clubId, long unionId, Union union) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
        if (null == club) {
            return;
        }
        long oldUnionId = club.getClubListBO().getUnionId();
        club.getClubListBO().saveUnionId(unionId);
        // 清空亲友圈成员赛事信息
        ClubMgr.getInstance().getClubMemberMgr().clearUnionInfo(clubId, union.getUnionBO().getRoundId());
        if (oldUnionId > 0L) {
            ContainerMgr.get().getComponent(UnionRoomConfigScorePercentBOService.class).delete(Restrictions.and(Restrictions.eq("unionId", oldUnionId), Restrictions.eq("clubId", clubId)));
        }
        ContainerMgr.get().getComponent(RoomConfigCalcActiveBOService.class).delete(Restrictions.and(Restrictions.eq("unionId", oldUnionId), Restrictions.eq("clubId", clubId)));
        // 通知：亲友圈所有玩家
        DispatcherComponent.getInstance().publish(new UnionNotify2ClubAllMember(clubId, club.getClubListBO().getOwnerID(), SUnion_ClubChange.make(clubId, unionId, unionId >= 0L ? "" : union.getUnionBO().getName(), union.getUnionBO().getUnionSign() >= 0L ? 0 : union.getUnionBO().getUnionSign())));
        // 更新共享
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().addClub(club);
        }
    }


    /**
     * 邀请指定玩家通知
     *
     * @param union  联赛信息
     * @param player 玩家信息
     * @param status 成员状态
     * @return
     */
    public boolean invitedPlayer(Union union, Player player, long clubId, int status, long execPid) {
        if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_YAOQING.value() == status) {
            pushProto(union, player, clubId, execPid, UnionDefine.UNION_NOTIFY_TYPE.UNION_NOTIFY_YAOQING);
        } else if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value() == status) {
            pushProto(union, player, clubId, UnionDefine.UNION_NOTIFY_TYPE.UNION_NOTIFY_JIARU);
        } else if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value() == status) {
            pushProto(union, player, clubId, UnionDefine.UNION_NOTIFY_TYPE.UNION_NOTIFY_TUICHU);
        } else if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_TICHU.value() == status) {
            pushProto(union, player, clubId, execPid, UnionDefine.UNION_NOTIFY_TYPE.UNION_NOTIFY_TICHU);
        } else if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_JUJIE.value() == status) {
            pushProto(union, player, clubId, execPid, UnionDefine.UNION_NOTIFY_TYPE.UNION_NOTIFY_JUJIE);
        }

        // 赛事审核通知
        this.unionExamineNotify(union.getUnionBO().getId(), status, execPid);
        return true;
    }

    /**
     * 赛事审核通知
     */
    public void unionExamineNotify(long unionId, int status, long execPid) {
        if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN.value() == status ||
                UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU_SHENQING.value() == status) {
            // 未批准
            // 退出申请
            notify2AllByManager(unionId, SUnion_Examine.make(unionId, true));
        }
        if (execPid > 0L && (
                UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value() == status ||
                        UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value() == status ||
                        UnionDefine.UNION_PLAYER_STATUS.PLAYER_JUJIE.value() == status)) {
            // 加入审核
            // 退出审核
            // 拒绝申请
            notify2AllByManager(unionId, SUnion_Examine.make(unionId, sizeUnionMemberExamine(unionId)));
        }
    }

    /**
     * 赛事审核通知
     */
    public void unionMatchApplyExamineNotify(long unionId, long clubId, boolean checkExistApply) {
        if (checkExistApply) {
            ListUtils.union(ClubMgr.getInstance().getClubMemberMgr().getMinisterList(clubId), getUnionMemberPidList(unionId)).stream().distinct().forEach(k -> {
                if (Objects.nonNull(k)) {
                    Player player = PlayerMgr.getInstance().getOnlinePlayerByPid((Long) k);
                    if (null != player) {
                        player.pushProtoMq(SUnion_Examine.make(unionId, checkExistApply));
                    }
                }
            });
        }
    }

    /**
     * 通知玩家或记录
     *
     * @param union           赛事
     * @param player          玩家
     * @param clubId          亲友圈Id
     * @param execPid         执行者Pid
     * @param unionNotifyType 通知类型
     */
    public void pushProto(Union union, Player player, long clubId, long execPid, UnionDefine.UNION_NOTIFY_TYPE unionNotifyType) {
        if (PlayerMgr.getInstance().checkExistOnlinePlayerByPid(player.getPid()) && player.getRoomInfo().getRoomId() <= 0L) {
            // 在线玩家并且没在游戏中，通知
            // 邀请人
            Player execPlayer = PlayerMgr.getInstance().getPlayer(execPid);
            // 被邀请的亲友圈
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
            if (null != execPlayer && null != club) {
                player.pushProtoMq(SUnion_Invited.make(execPlayer.getPid(), execPlayer.getName(), club.getClubListBO().getName(), club.getClubListBO().getClubsign(), union.getUnionBO().getName(), union.getUnionBO().getUnionSign(), unionNotifyType.value(), clubId));
                return;
            }
        }
        new UnionNotifyBO(player.getPid(), clubId, union.getUnionBO().getId(), execPid, unionNotifyType.value()).insert();
    }

    /**
     * 通知玩家或记录
     *
     * @param union           赛事
     * @param player          玩家
     * @param clubId          亲友圈Id
     * @param unionNotifyType 通知类型
     */
    public void pushProto(Union union, Player player, long clubId, UnionDefine.UNION_NOTIFY_TYPE unionNotifyType) {
        if (PlayerMgr.getInstance().checkExistOnlinePlayerByPid(player.getPid()) && player.getRoomInfo().getRoomId() <= 0L) {
            // 在线玩家并且没在游戏中，通知
            // 所在的亲友圈
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
            if (null != club) {
                player.pushProtoMq(SUnion_Invited.make(club.getClubListBO().getName(), club.getClubListBO().getClubsign(), union.getUnionBO().getName(), union.getUnionBO().getUnionSign(), unionNotifyType.value(), clubId));
                return;
            }
        }
        new UnionNotifyBO(player.getPid(), clubId, union.getUnionBO().getId(), 0, unionNotifyType.value()).insert();
    }

    /**
     * 检查加入赛事房间
     *
     * @param player 玩家PId
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result checkJoinUnion(Union union, long clubId, Player player, List<Long> pidList, BaseCreateRoom baseCreateRoom) {
        // 查询
        if (null == union) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, ErrorCode.UNION_NOT_EXIST.name());
        }
        if (union.getUnionBO().getState() == UnionDefine.UNION_STATE.UNION_STATE_STOP.ordinal()) {
            // 赛事停用状态
            return SData_Result.make(ErrorCode.UNION_STATE_STOP, "赛事已停用，无法加入房间，请联系赛事举办方");
        }
        if (clubId <= 0L) {
            return SData_Result.make(ErrorCode.UNION_ENTER_NOT_CLUBMEMBER, ErrorCode.UNION_ENTER_NOT_CLUBMEMBER.name());
        }
        Club club = null;
        //从共享获取数据
        if (Config.isShare()) {
            club = ClubMgr.getInstance().getClubListMgr().findClubShare(clubId);
        } else {
            club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
        }
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, ErrorCode.CLUB_NOT_EXIST.name());
        }
        if (club.getClubListBO().getUnionId() <= 0L) {
            return SData_Result.make(ErrorCode.CLUB_QUIT_UNION, "您所在亲友圈@%s 已退出联赛@%s", club.getClubListBO().getName(), union.getUnionBO().getName());
        }
        if (union.getUnionBO().getId() != club.getClubListBO().getUnionId()) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, ErrorCode.UNION_ID_ERROR.name());
        }
        ClubMember clubMember = null;
        if (Config.isShare()) {
            clubMember = ClubMgr.getInstance().getClubMemberMgr().findShare(player.getPid(), clubId, Club_define.Club_Player_Status.PLAYER_JIARU);
        } else {
            clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), clubId, Club_define.Club_Player_Status.PLAYER_JIARU);
        }
        if (null == clubMember) {
            return SData_Result.make(ErrorCode.UNION_ENTER_NOT_CLUBMEMBER, "= UNION_ENTER_NOT_CLUBMEMBER");
        }
        // ²  注：赛事主裁判无法申请退赛，且赛事主裁判竞技分低于淘汰分时，也不会被淘汰；
        if (union.getUnionBO().getOwnerId() != player.getPid()) {
            // 旧赛事状态
            int oldUnionState = clubMember.getClubMemberBO().getUnionState();
            int unionState = clubMember.getClubMemberBO().getUnionState(union.getUnionBO().getOutSports(), union.getUnionBO().getId());
            if (oldUnionState != unionState) {
                player.pushProto(SUnion_MatchState.make(union.getUnionBO().getId(), clubMember.getClubID(), player.getPid(), unionState));
            }
            if (unionState == UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value()) {
                // 当前状态处于复赛申请中
                return SData_Result.make(ErrorCode.UNION_APPLY_REMATCH_PLAYING, "您的复赛申请等待审批中，请联系赛事举办方");
            }
            if (unionState == UnionDefine.UNION_MATCH_STATE.BACK_OFF.value()) {
                // 当前状态处于退赛申请中
                return SData_Result.make(ErrorCode.UNION_BACK_OFF_PLAYING, "您已申请退赛，当前无法进行比赛，请取消退赛申请或联系赛事举办方");
            }
        }
        if (club.isZhongZhiClub()) {
            //中至的判断0
            if (!zhongZhiCheckRoomSportsThreshold(clubMember.getClubMemberBO(), baseCreateRoom.getRoomSportsThreshold().doubleValue())) {
                return SData_Result.make(ErrorCode.UNION_BELOW_THRESHOLD_VALUE, "比赛分低于加入房间的门槛值, 无法加入房间");
            }
        } else {
            if (clubMember.getClubMemberBO().getSportsPoint() < baseCreateRoom.getRoomSportsThreshold().doubleValue()) {
//            return SData_Result.make(ErrorCode.UNION_BELOW_THRESHOLD_VALUE, "BELOW_THRESHOLD SportsPoint:{%,.2f},SportsThreshold:{%,.2f}", clubMember.getClubMemberBO().getSportsPoint(), baseCreateRoom.getRoomSportsThreshold().doubleValue());
                return SData_Result.make(ErrorCode.UNION_BELOW_THRESHOLD_VALUE, "比赛分低于加入房间的门槛值, 无法加入房间");
            }
        }

        // 检查分组禁令
        String name = union.checkGroupingBan(player.getPid(), pidList);
        if (StringUtils.isNotEmpty(name)) {
            return SData_Result.make(ErrorCode.UNION_GROUPING, "您与@" + name + "处于禁止同桌游戏状态，请联系赛事裁判");
        }
        // 检查分组禁令
        name = club.checkGroupingBan(player.getPid(), pidList);
        if (StringUtils.isNotEmpty(name)) {
            return SData_Result.make(ErrorCode.UNION_GROUPING, "您与@" + name + "处于禁止同桌游戏状态，请联系赛事裁判");
        }
        return SData_Result.make(ErrorCode.Success, clubMember);

    }

    /**
     * 中至检查门槛  false不能进入  true能进入
     * 分3种情况： 淘汰分0 包括在正数里面
     * 玩家个人积分为负数：当淘汰分为负数时，玩家进入房间时需要比较淘汰分与门槛的大小，当门槛为负数时，淘汰分比门槛大则可以进入房间，门槛为正数或0时，玩家不可进入房间；
     * 玩家个人积分为负数：当淘汰分为正数时，玩家状态为重赛申请中，无法进入房间；
     * 玩家个人积分为0：当淘汰分为负数时，且门槛为负数时，则玩家可以直接进入房间，门槛为正数时，玩家个人积分+淘汰分的绝对值大于门槛则可以进入房间；
     * 玩家个人积分为0：当淘汰分为正数时，且个人积分小于淘汰分，则玩家状态为重赛申请中，无法进入房间；
     * 玩家个人积分为正数：当淘汰分为负数时，且门槛为负数时，则玩家可以直接进入房间，门槛为正数时，玩家个人积分+淘汰分的绝对值大于门槛则可以进入房间；
     * 玩家个人积分为正数：当淘汰分为正数时，且个人积分小于淘汰分，则玩家状态为重赛申请中，若个人积分大于淘汰分，则需要判断玩家个人积分+淘汰分的绝对值是否大于门槛，大于则可以进入房间，反之不行；
     *
     * @return
     */
    private boolean zhongZhiCheckRoomSportsThreshold(ClubMemberBO clubMemberBO, double roomSportsThreshold) {
        if (clubMemberBO.getSportsPoint() < 0d) {
            if (clubMemberBO.getEliminatePoint() < 0d) {
                if (roomSportsThreshold < 0d) {
                    return clubMemberBO.getEliminatePoint() >= roomSportsThreshold;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (clubMemberBO.getSportsPoint() > 0d) {
            if (clubMemberBO.getEliminatePoint() < 0d) {
                if (roomSportsThreshold < 0d) {
                    return true;
                } else {
                    return CommMath.addDouble(clubMemberBO.getSportsPoint(), Math.abs(clubMemberBO.getEliminatePoint())) >= roomSportsThreshold;
                }
            } else {
                if (clubMemberBO.getSportsPoint() < clubMemberBO.getEliminatePoint()) {
                    return false;
                } else {
                    return CommMath.addDouble(clubMemberBO.getSportsPoint(), Math.abs(clubMemberBO.getEliminatePoint())) >= roomSportsThreshold;
                }
            }
        } else {
            if (clubMemberBO.getEliminatePoint() < 0d) {
                if (roomSportsThreshold < 0d) {
                    return true;
                } else {
                    return CommMath.addDouble(clubMemberBO.getSportsPoint(), Math.abs(clubMemberBO.getEliminatePoint())) >= roomSportsThreshold;
                }
            } else if (clubMemberBO.getEliminatePoint() > 0d) {
                return false;
            } else {
                return true;
            }
        }
    }


    /**
     * 检查加入赛事房间
     *
     * @param player 玩家PId
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result checkJoinUnion(Union union, long clubId, Player player, BaseCreateRoom baseCreateRoom) {
        // 查询
        if (null == union) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, ErrorCode.UNION_NOT_EXIST.name());
        }
        if (union.getUnionBO().getState() == UnionDefine.UNION_STATE.UNION_STATE_STOP.ordinal()) {
            // 赛事停用状态
            return SData_Result.make(ErrorCode.UNION_STATE_STOP, "赛事已停用，无法加入房间，请联系赛事举办方");
        }
        if (clubId <= 0L) {
            return SData_Result.make(ErrorCode.UNION_ENTER_NOT_CLUBMEMBER, ErrorCode.UNION_ENTER_NOT_CLUBMEMBER.name());
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, ErrorCode.CLUB_NOT_EXIST.name());
        }
        if (club.getClubListBO().getUnionId() <= 0L) {
            return SData_Result.make(ErrorCode.CLUB_QUIT_UNION, "您所在亲友圈@%s 已退出联赛@%s", club.getClubListBO().getName(), union.getUnionBO().getName());
        }
        if (union.getUnionBO().getId() != club.getClubListBO().getUnionId()) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, ErrorCode.UNION_ID_ERROR.name());
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), clubId, Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            return SData_Result.make(ErrorCode.UNION_ENTER_NOT_CLUBMEMBER, "= UNION_ENTER_NOT_CLUBMEMBER");
        }
        // ²  注：赛事主裁判无法申请退赛，且赛事主裁判竞技分低于淘汰分时，也不会被淘汰；
        if (union.getUnionBO().getOwnerId() != player.getPid()) {
            // 旧赛事状态
            int oldUnionState = clubMember.getClubMemberBO().getUnionState();
            int unionState = clubMember.getClubMemberBO().getUnionState(union.getUnionBO().getOutSports(), union.getUnionBO().getId());
            if (unionState == UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value()) {
                if (oldUnionState == UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value()) {
                    player.pushProto(SUnion_MatchState.make(union.getUnionBO().getId(), clubMember.getClubID(), player.getPid(), unionState));
                }
                // 当前状态处于复赛申请中
                return SData_Result.make(ErrorCode.UNION_APPLY_REMATCH_PLAYING, "您的复赛申请等待审批中，请联系赛事举办方");
            }
            if (unionState == UnionDefine.UNION_MATCH_STATE.BACK_OFF.value()) {
                // 当前状态处于退赛申请中
                return SData_Result.make(ErrorCode.UNION_BACK_OFF_PLAYING, "您已申请退赛，当前无法进行比赛，请取消退赛申请或联系赛事举办方");
            }
        }
        if (clubMember.getClubMemberBO().getSportsPoint() < baseCreateRoom.getRoomSportsThreshold().doubleValue()) {
            return SData_Result.make(ErrorCode.UNION_BELOW_THRESHOLD_VALUE, "BELOW_THRESHOLD SportsPoint:{%,.2f},SportsThreshold:{%,.2f}", clubMember.getClubMemberBO().getSportsPoint(), baseCreateRoom.getRoomSportsThreshold().doubleValue());
        }
        if (clubMember.getClubMemberBO().getBanGame() > 0) {
            // 被亲友圈管理员禁止加入房间。
            return SData_Result.make(ErrorCode.CLUB_BAN_GAME, "您已被禁止该游戏，请联系管理");
        }
        if (clubMember.getClubMemberBO().getUnionBanGame() > 0) {
            // 被赛事管理员禁止加入房间。
            return SData_Result.make(ErrorCode.UNION_BAN_GAME, "您已被禁止该游戏，请联系管理");
        }
        return SData_Result.make(ErrorCode.Success, clubMember.getClubMemberBO());

    }


    /**
     * 通知赛事旗下所有亲友圈成员
     *
     * @param unionId
     * @param make
     */
    public void notify2AllByUnion(long unionId, long unionGameCfgId, BaseSendMsg make) {
        if (Config.isShare()) {
            mqNotifyMessage(MqTopic.UNION_ALL_BY_UNION_NOTIFY, unionId, null, unionGameCfgId, make);
        } else {
            DispatcherComponent.getInstance().publish(new UnionNotify2AllByClubEvent(this.getUnionToClubIdList(unionId), unionGameCfgId, make));
        }
    }


    /**
     * 通知赛事旗下所有亲友圈成员
     *
     * @param unionId
     * @param make
     */
    public void notify2AllByUnion(long unionId, BaseSendMsg make) {
        DispatcherComponent.getInstance().publish(new UnionNotify2AllByClubEvent(this.getUnionToClubIdList(unionId), make));
    }

    /**
     * 联赛获取亲友圈Id
     *
     * @param unionId 赛事Id
     * @return
     */
    public List<Long> getUnionToClubIdList(long unionId) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
        } else {
            return this.getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).map(k -> k.getUnionMemberBO().getClubId()).collect(Collectors.toList());
        }
    }


    public SData_Result getUnionMemberExamineSize(CUnion_Base req, long pid) {
        // 检查权限是否足够
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotUnionManage(pid, req.getClubId(), req.getUnionId())) {
            // 3、	联盟的普通亲友圈创建者/管理可看到自己所属亲友圈的玩家退赛、复赛审核信息；
            if (ClubMgr.getInstance().getClubMemberMgr().isNotMinister(req.getClubId(), pid)) {
                // 亲友圈创建者、管理员可见
                return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", req.getClubId(), pid);
            }
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
            if (Objects.isNull(club)) {
                // 亲友圈不存在
                return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
            }
            if (club.getClubListBO().getUnionId() != req.getUnionId()) {
                // 赛事Id错误
                return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
            }
            return SData_Result.make(ErrorCode.Success, SUnion_Examine.make(req.getUnionId(), ClubMgr.getInstance().getClubMemberMgr().checkExistUnionMatchApply(Lists.newArrayList(req.getClubId()))));
        } else {
            return SData_Result.make(ErrorCode.Success, SUnion_Examine.make(req.getUnionId(), sizeUnionMemberExamine(req.getUnionId())));
        }
    }

    /**
     * 获取未审核的人数
     *
     * @return
     */
    public boolean sizeUnionMemberExamine(long unionId) {
        if (this.getUnionMemberMap()
                .values()
                .stream()
                .anyMatch(k -> k.getUnionMemberBO().getUnionId() == unionId && (k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN.value()) || k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU_SHENQING.value())))) {
            return true;
        }
        /**
         * 检查是否存在赛事比赛申请操作
         */
        return checkExistUnionMatchApply(unionId);
    }

    /**
     * 检查是否存在赛事比赛申请操作
     *
     * @param unionId
     * @return
     */
    public boolean checkExistUnionMatchApply(long unionId) {
        return ClubMgr.getInstance().getClubMemberMgr().checkExistUnionMatchApply(this.getUnionToClubIdList(unionId));
    }

    /**
     * 通知俱乐部玩家 只通知管理或者创建者
     **/
    public void notify2AllByManager(long unionId, BaseSendMsg msg) {
        if (Config.isShare()) {
            ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream()
                    // 筛选亲友圈ID和成员状态,筛选管理或者创建者
                    .filter(k -> unionId == k.getUnionMemberBO().getUnionId() && k.isManage() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .map(k -> k.getUnionMemberBO().getClubOwnerId())
                    .distinct()
                    .forEach(k -> {
                        Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
                        if (null != player) {
                            player.pushProtoMq(msg);
                        }
                    });
        } else {
            this.getUnionMemberMap().values().stream()
                    // 筛选亲友圈ID和成员状态,筛选管理或者创建者
                    .filter(k -> unionId == k.getUnionMemberBO().getUnionId() && k.isManage() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .map(k -> k.getUnionMemberBO().getClubOwnerId())
                    .distinct()
                    .forEach(k -> {
                        Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
                        if (null != player && player.notExistRoom()) {
                            player.pushProto(msg);
                        }
                    });
        }

    }


    /**
     * 通知俱乐部玩家 只通知管理或者创建者
     **/
    public void notify2AllByManagerToPid(long unionId, long pid, BaseSendMsg msg) {
        if (Config.isShare()) {
            ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream()
                    // 筛选亲友圈ID和成员状态,筛选管理或者创建者
                    .filter(k -> unionId == k.getUnionMemberBO().getUnionId() && (k.isManage() || k.getUnionMemberBO().getClubOwnerId() == pid) && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .map(k -> k.getUnionMemberBO().getClubOwnerId())
                    .distinct()
                    .forEach(k -> {
                        Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
                        if (null != player) {
                            player.pushProtoMq(msg);
                        }
                    });
        } else {
            this.getUnionMemberMap().values().stream()
                    // 筛选亲友圈ID和成员状态,筛选管理或者创建者
                    .filter(k -> unionId == k.getUnionMemberBO().getUnionId() && (k.isManage() || k.getUnionMemberBO().getClubOwnerId() == pid) && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .map(k -> k.getUnionMemberBO().getClubOwnerId())
                    .distinct()
                    .forEach(k -> {
                        Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
                        if (null != player && player.notExistRoom()) {
                            player.pushProto(msg);
                        }
                    });
        }
    }

    /**
     * 获取赛事成员审核列表
     *
     * @param unionId 赛事id
     * @param clubId  亲友圈Id
     * @param pageNum 页数
     * @param type    0:加入审核,1:退出审核,2:退赛审核,3:重赛审核
     * @return
     */
    public SData_Result getUnionMemberExamineList(long unionId, long clubId, long pid, int pageNum, int type) {
        if (UnionDefine.UNION_EXAMINE_TYPE.checkJoinOrQuit(type)) {
            if (isNotUnionManage(pid, clubId, unionId)) {
                // 	主裁判、副裁判可见；
                return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE pid:{%d},clubId:{%d},unionId:{%d}", pid, clubId, unionId);
            }
            // 0:加入审核,1:退出审核
            return SData_Result.make(ErrorCode.Success, getUnionMemberExamineList(unionId, pageNum, clubId, pid, type, type == UnionDefine.UNION_EXAMINE_TYPE.EXAMINE_JOIN.ordinal() ? UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN : UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU_SHENQING));
        } else if (UnionDefine.UNION_EXAMINE_TYPE.checkMatch(type)) {
            List<Long> clubIdList = Lists.newArrayList();
            // 2、	联盟管理/创建者可看到所有玩家的退赛、复赛申请消息；
            if (isNotUnionManage(pid, clubId, unionId)) {
                // 3、	联盟的普通亲友圈创建者/管理可看到自己所属亲友圈的玩家退赛、复赛审核信息；
                if (ClubMgr.getInstance().getClubMemberMgr().isNotMinister(clubId, pid)) {
                    // 亲友圈创建者、管理员可见
                    return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", clubId, pid);
                }
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
                if (Objects.isNull(club)) {
                    // 亲友圈不存在
                    return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
                }
                if (club.getClubListBO().getUnionId() != unionId) {
                    // 赛事Id错误
                    return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
                }
                // 查询指定亲友圈
                clubIdList.add(clubId);
            } else {
                // 查询赛事内所有亲友圈
                clubIdList.addAll(this.getUnionToClubIdList(unionId));
            }
            // 2:退赛审核,3:重赛审核
            return SData_Result.make(ErrorCode.Success, getUnionMatchApplyExamineList(clubIdList, pageNum, clubId, pid, type, UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH));
        }
        return SData_Result.make(ErrorCode.NotAllow, "NotAllow type:{%d}", type);
    }

    /**
     * 获取赛事成员审核列表
     *
     * @param unionId 赛事id
     * @param clubId  亲友圈Id
     * @param pageNum 页数
     * @param type    0:加入审核,1:退出审核,2:退赛审核,3:重赛审核
     * @return
     */
    public SData_Result getUnionMemberExamineListZhongZhi(long unionId, long clubId, long pid, int pageNum, int type, String query) {
        List<Long> clubIdList = Lists.newArrayList();
        // 2、	联盟管理/创建者可看到所有玩家的退赛、复赛申请消息；
        if (isNotUnionManage(pid, clubId, unionId)) {
            // 3、	联盟的普通亲友圈创建者/管理可看到自己所属亲友圈的玩家退赛、复赛审核信息；
            if (ClubMgr.getInstance().getClubMemberMgr().isNotMinister(clubId, pid)) {
                // 亲友圈创建者、管理员可见
                return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", clubId, pid);
            }
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
            if (Objects.isNull(club)) {
                // 亲友圈不存在
                return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
            }
            if (club.getClubListBO().getUnionId() != unionId) {
                // 赛事Id错误
                return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
            }
            // 查询指定亲友圈
            clubIdList.add(clubId);
        } else {
            // 查询赛事内所有亲友圈
            clubIdList.addAll(this.getUnionToClubIdList(unionId));
        }
        // 2:退赛审核,3:重赛审核
        return SData_Result.make(ErrorCode.Success, getUnionMatchApplyExamineListZhongZhi(clubIdList, pageNum, clubId, pid, type, UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH, query));

    }

    /**
     * 获取加入审核、退出审核列表
     *
     * @param unionId 赛事id
     * @param pageNum 页数
     * @param status  审核类型(0:加入审核,1:退出审核)
     * @return
     */
    public List<UnionMemberExamineItem> getUnionMemberExamineList(long unionId, int pageNum, long clubId, long pid, final int type, UnionDefine.UNION_PLAYER_STATUS status) {
        if (Config.isShare()) {
            return ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId).values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(status.value()) && !(k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid))
                    .map(k -> {
                        Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getUnionMemberBO().getClubId());
                        if (Objects.isNull(club)) {
                            return null;
                        }
                        Player player = club.getOwnerPlayer();
                        if (Objects.isNull(player)) {
                            return null;
                        }
                        return new UnionMemberExamineItem(club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), type, ClubMgr.getInstance().getClubMemberMgr().clubPeopleNum(club.getClubListBO().getId()), club.getClubListBO().getClubsign());
                    }).filter(k -> Objects.nonNull(k)).skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).collect(Collectors.toList());

        } else {
            return this.getUnionMemberMap().values().stream()
                    .filter(k -> k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(status.value()) && !(k.getUnionMemberBO().getClubId() == clubId && k.getUnionMemberBO().getClubOwnerId() == pid))
                    .map(k -> {
                        Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getUnionMemberBO().getClubId());
                        if (Objects.isNull(club)) {
                            return null;
                        }
                        Player player = club.getOwnerPlayer();
                        if (Objects.isNull(player)) {
                            return null;
                        }
                        return new UnionMemberExamineItem(club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), type, ClubMgr.getInstance().getClubMemberMgr().clubPeopleNum(club.getClubListBO().getId()), club.getClubListBO().getClubsign());
                    }).filter(k -> Objects.nonNull(k)).skip(Page.getPageNum(pageNum, Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).collect(Collectors.toList());

        }
    }

    /**
     * 获取退赛审核、重赛审核列表
     *
     * @param clubIdList 亲友圈Id列表
     * @param pageNum    页数
     * @param state      审核类型(2:退赛审核,3:重赛审核)
     * @return
     */
    public List<UnionMemberExamineItem> getUnionMatchApplyExamineList(final List<Long> clubIdList, int pageNum, long clubId, long pid, final int type, UnionDefine.UNION_MATCH_STATE state) {
        return ClubMgr.getInstance().getClubMemberMgr().getUnionMatchApplyExamineList(clubIdList, pageNum, clubId, pid, type, state);
    }

    /**
     * 获取退赛审核、重赛审核列表
     *
     * @param clubIdList 亲友圈Id列表
     * @param pageNum    页数
     * @param state      审核类型(2:退赛审核,3:重赛审核)
     * @return
     */
    public List<UnionMemberExamineItemZhongZhi> getUnionMatchApplyExamineListZhongZhi(final List<Long> clubIdList, int pageNum, long clubId, long pid, final int type, UnionDefine.UNION_MATCH_STATE state, String query) {
        return ClubMgr.getInstance().getClubMemberMgr().getUnionMatchApplyExamineListZhongZhi(clubIdList, pageNum, clubId, pid, type, state, query);
    }

    /**
     * 赛事成员审核操作
     *
     * @param unionId  赛事id
     * @param clubId   亲友圈id
     * @param opClubId 操作对象亲友圈id
     * @param type     类型 0:加入审核,1:退出审核,2:退赛审核,3:重赛审核
     * @param operate  0:同意,1:拒绝
     * @return
     */
    public SData_Result getUnionMemberExamineOperate(long unionId, long clubId, long opPid, long opClubId, long exePid, int type, int operate) {
        if (clubId == opClubId && opPid == exePid) {
            return SData_Result.make(ErrorCode.UNION_NOT_OPERATE_YOURSELF, "UNION_NOT_OPERATE_YOURSELF clubId:{%d},opClubId:{%d},opPid:{%d},exePid:{%d}", clubId, opClubId, opPid, exePid);
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(opClubId);
        if (Objects.isNull(club)) {
            // 返回提示：“该亲友圈不存在”，并删除该申请信息；
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST opClubId:{%d}", opClubId);
        }
        if (UnionDefine.UNION_EXAMINE_TYPE.checkJoinOrQuit(type)) {
            // 审核加入、退出申请操作
            if (isNotUnionManage(exePid, clubId, unionId)) {
                // 	联盟盟主、联盟管理可见；
                return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE pid:{%d},clubId:{%d},unionId:{%d}", exePid, clubId, unionId);
            }
            return getUnionJoinOrQuitExamineOperate(unionId, club, exePid, type, type == UnionDefine.UNION_EXAMINE_TYPE.EXAMINE_JOIN.ordinal() ? UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN : UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU_SHENQING, operate);
        } else if (UnionDefine.UNION_EXAMINE_TYPE.checkMatch(type)) {
            // 2、	联盟管理/创建者可看到所有玩家的退赛、复赛申请消息；
            if (isNotUnionManage(exePid, clubId, unionId)) {
                // 3、	联盟的普通亲友圈创建者/管理可看到自己所属亲友圈的玩家退赛、复赛审核信息；
                if (clubId != opClubId || ClubMgr.getInstance().getClubMemberMgr().isNotMinister(opClubId, exePid)) {
                    // 亲友圈创建者、管理员可见
                    return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},exePid:{%d}", clubId, exePid);
                }
            }
            // 审核退赛、重赛申请操作
            return getUnionMatchApplyExamineOperate(unionId, clubId, opPid, club, exePid, type, type == UnionDefine.UNION_EXAMINE_TYPE.EXAMINE_BACK_OFF.ordinal() ? UnionDefine.UNION_MATCH_STATE.BACK_OFF : UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH, operate);
        }
        return SData_Result.make(ErrorCode.NotAllow, "NotAllow type:{%d}", type);
    }

    /**
     * 审核加入、退出申请操作
     *
     * @param unionId 赛事Id
     * @param club    亲友圈信息
     * @param exePid  操作者Pid
     * @param type    类型 0:加入审核,1:退出审核,2:退赛审核,3:重赛审核
     * @param status  状态: 加入申请、退出申请
     * @param operate 操作值 0同意,1拒绝
     * @return
     */
    private SData_Result getUnionJoinOrQuitExamineOperate(long unionId, Club club, long exePid, int type, UnionDefine.UNION_PLAYER_STATUS status, int operate) {
        UnionMember unionMember = this.find(club.getClubListBO().getOwnerID(), club.getClubListBO().getId(), unionId, status);
        if (Objects.isNull(unionMember)) {
            // 找不到没有申请信息
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER pid:{%d},clubId:{%d},unionId:{%d},status:{%d}", club.getClubListBO().getOwnerID(), club.getClubListBO().getId(), unionId, status.value());
        }
        if (unionMember.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN.value())) {
            // 审核未批准操作
            return examineWeiPiZhunOperate(club, unionMember, exePid, type, operate);
        } else if (unionMember.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU_SHENQING.value())) {
            // 审核退出申请操作
            return examineTuiChuShenQingOperate(club, unionMember, exePid, type, operate);
        }
        return SData_Result.make(ErrorCode.UNION_MEMBER_STATUS_ERROR, "UNION_MEMBER_STATUS_ERROR status:{%d}", unionMember.getUnionMemberBO().getStatus());
    }

    /**
     * 审核未批准操作
     *
     * @param club        被操作者的亲友圈信息
     * @param unionMember 赛事成员
     * @param exePid      操作者Pid
     * @param type        类型 0:加入审核,1:退出审核,2:退赛审核,3:重赛审核
     * @param operate     操作值 0同意,1拒绝
     * @return
     */
    private SData_Result examineWeiPiZhunOperate(Club club, UnionMember unionMember, long exePid, int type, int operate) {
        if (club.getClubListBO().getUnionId() > 0L) {
            // 系统拒绝
            unionMember.setStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JUJIE.value(), 0L);
            // 返回提示：“审核失败，该亲友圈已在其他联盟”
            return SData_Result.make(ErrorCode.UNION_EXIST_ADD_OTHERS_UNION, "UNION_EXIST_ADD_OTHERS_UNION type:{%d},unionId:{%d}", type, club.getClubListBO().getUnionId());
        }
        // 未批准
        if (operate <= 0) {
            // 提示：“审核成功，@亲友圈加入联盟”，并在联盟消息中添加信息；
            // 同意加入
            unionMember.setStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value(), exePid);
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionMember.getUnionMemberBO().getUnionId());
            ClubMgr.getInstance().getClubMemberMgr().findClubIdAllClubMember(unionMember.getUnionMemberBO().getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU.value()).stream().forEach(l -> {
                l.getClubMemberBO().saveEliminatePoint(union.getUnionBO().getOutSports());
            });
        } else {
            // 	“拒绝”：则从列表删除该亲友圈的申请信息，并返回提示：“已拒绝该亲友圈申请”；
            // 拒接加入
            unionMember.setStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JUJIE.value(), exePid);
        }
        return SData_Result.make(ErrorCode.Success, SUnion_MemberExamineOperate.make(unionMember.getUnionMemberBO().getUnionId(), club.getClubListBO().getId(), type, operate));
    }

    /**
     * 审核退出申请操作
     *
     * @param club        被操作者的亲友圈信息
     * @param unionMember 赛事成员
     * @param exePid      操作者Pid
     * @param type        类型 0:加入审核,1:退出审核,2:退赛审核,3:重赛审核
     * @param operate     操作值 0同意,1拒绝
     * @return
     */
    private SData_Result examineTuiChuShenQingOperate(Club club, UnionMember unionMember, long exePid, int type, int operate) {
        // 申请退出
        if (operate <= 0) {
            // 同意申请
            unionMember.setStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value(), exePid);
        } else {
            Player player = PlayerMgr.getInstance().getPlayer(unionMember.getUnionMemberBO().getClubOwnerId());
            if (Objects.isNull(player)) {
                return SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
            }
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionMember.getUnionMemberBO().getUnionId());
            if (Objects.isNull(union)) {
                return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
            }
            // 通知指定玩家通知
            UnionMgr.getInstance().getUnionMemberMgr().invitedPlayer(union, player, unionMember.getUnionMemberBO().getClubId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JUJIE.value(), exePid);
            unionMember.getUnionMemberBO().saveStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value());
            UnionMgr.getInstance().getUnionMemberMgr().notify2AllByManager(unionMember.getUnionMemberBO().getUnionId(), SUnion_Examine.make(unionMember.getUnionMemberBO().getUnionId(), UnionMgr.getInstance().getUnionMemberMgr().sizeUnionMemberExamine(unionMember.getUnionMemberBO().getUnionId())));
        }
        return SData_Result.make(ErrorCode.Success, SUnion_MemberExamineOperate.make(unionMember.getUnionMemberBO().getUnionId(), club.getClubListBO().getId(), type, operate));
    }

    /**
     * 审核退赛、重赛申请操作
     *
     * @param unionId 赛事Id
     * @param club    亲友圈信息
     * @param exePid  操作者Pid
     * @param type    类型 0:加入审核,1:退出审核,2:退赛审核,3:重赛审核
     * @param state   状态: 退赛申请、复赛申请
     * @param operate 操作值 0同意,1拒绝
     * @return
     */
    private SData_Result getUnionMatchApplyExamineOperate(long unionId, long clubId, long opPid, Club club, long exePid, int type, UnionDefine.UNION_MATCH_STATE state, int operate) {
        if (club.getClubListBO().getUnionId() != unionId) {
            // 返回提示：“赛事Id错误”
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR type:{%d},unionId:{%d}", type, club.getClubListBO().getUnionId());
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(opPid, club.getClubListBO().getId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(clubMember)) {
            // 返回提示：“找不到成员”
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER clubMember type:{%d},unionId:{%d}", type, club.getClubListBO().getUnionId());
        }
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().find(exePid, clubId, Club_define.Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(execClubMember)) {
            // 返回提示：“找不到成员”
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER execClubMember type:{%d},unionId:{%d}", type, club.getClubListBO().getUnionId());
        }
        if (UnionDefine.UNION_MATCH_STATE.BACK_OFF.value() == clubMember.getClubMemberBO().getUnionState() && UnionDefine.UNION_MATCH_STATE.BACK_OFF.value() == state.value()) {
            // 审核退赛申请操作
            return examineBackOffOperate(unionId, clubMember, execClubMember, type, operate, union.getUnionBO().getOutSports());
        } else if (UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value() == clubMember.getClubMemberBO().getUnionState() && UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value() == state.value()) {
            // 审核复赛申请操作
            return examineApplyRematchOperate(unionId, clubMember, execClubMember, type, operate, union.getUnionBO().getOutSports());
        }
        if (UnionDefine.UNION_MATCH_STATE.BACK_OFF.value() == clubMember.getClubMemberBO().getUnionState()) {
            return SData_Result.make(ErrorCode.UNION_CUR_BACK_OFF, "UNION_CUR_BACK_OFF status:{%d}", clubMember.getClubMemberBO().getUnionState());
        } else if (UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value() == clubMember.getClubMemberBO().getUnionState()) {
            return SData_Result.make(ErrorCode.UNION_CUR_APPLY_REMATCH, "UNION_CUR_APPLY_REMATCH status:{%d}", clubMember.getClubMemberBO().getUnionState());
        } else {
            return SData_Result.make(ErrorCode.UNION_CUR_MATCH_PLAYING, "UNION_CUR_MATCH_PLAYING status:{%d}", clubMember.getClubMemberBO().getUnionState());
        }
    }


    /**
     * 审核退赛申请操作
     *
     * @return
     */
    private SData_Result examineBackOffOperate(long unionId, ClubMember clubMember, ClubMember execClubMember, int type, int operate, double outSports) {
        if (operate <= 0) {
            // 同意
            // 申请退赛
            // 申请时比赛分为@值；
            UnionApplyOperateItem unionApplyOperateItem = clubMember.getClubMemberBO().execSportsPointBackOff(unionId);
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
            if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
                clubMember.getClubMemberBO().saveUnionState(UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value());
                // 赛事消息
                UnionDynamicBO.insertSportsPoint(clubMember.getClubMemberBO().getPlayerID(), unionId, clubMember.getClubID(), execClubMember.getClubMemberBO().getPlayerID(), execClubMember.getClubID(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_BACK_OFF_AGREE.value(), String.valueOf(unionApplyOperateItem.getPreValue()), 0, "");
            } else if (UnionDefine.UNION_TYPE.NORMAL.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
                if (0D == unionApplyOperateItem.getFinalValue() || execClubMember.getClubMemberBO().execSportsPointUpdate(unionId, -unionApplyOperateItem.getFinalValue(), ItemFlow.UNION_BACK_OFF_OP, RoomTypeEnum.NORMAL, outSports)) {
                    clubMember.getClubMemberBO().saveUnionState(UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value());
                    // 赛事消息
                    UnionDynamicBO.insertSportsPoint(clubMember.getClubMemberBO().getPlayerID(), unionId, clubMember.getClubID(), execClubMember.getClubMemberBO().getPlayerID(), execClubMember.getClubID(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_BACK_OFF_AGREE.value(), String.valueOf(unionApplyOperateItem.getPreValue()), 0, "");
                } else {
                    // 联赛申请退赛-回退
                    clubMember.getClubMemberBO().execRollbackValue(unionId, -unionApplyOperateItem.getFinalValue(), ItemFlow.UNION_BACK_OFF_ROLLBACK);
                    // 回退比赛分
                    return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
                }
            } else {
                return SData_Result.make(ErrorCode.UNION_TYPE_NOT_EXIST, "UNION_TYPE_NOT_EXIST");
            }

        } else {
            // 拒绝
            clubMember.getClubMemberBO().saveUnionState(UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value());
            // 赛事消息
            UnionDynamicBO.insertSportsPoint(clubMember.getClubMemberBO().getPlayerID(), unionId, clubMember.getClubID(), execClubMember.getClubMemberBO().getPlayerID(), execClubMember.getClubID(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_BACK_OFF_REFUSE.value(), "", 0, "");
        }
        // 消息通知
        this.notify2Player(clubMember.getClubMemberBO().getPlayerID(), SUnion_MatchState.make(unionId, clubMember.getClubID(), clubMember.getClubMemberBO().getPlayerID(), clubMember.getClubMemberBO().getUnionState(), type, operate));
        return SData_Result.make(ErrorCode.Success, SUnion_MemberExamineOperate.make(unionId, clubMember.getClubID(), clubMember.getClubMemberBO().getPlayerID(), type, operate));
    }


    /**
     * 审核复赛申请操作
     *
     * @return
     */
    private SData_Result examineApplyRematchOperate(long unionId, ClubMember clubMember, ClubMember execClubMember, int type, int operate, double outSports) {

        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        // 获取预扣值
        UnionApplyOperateItem unionApplyOperateItem = null;
        if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            //中至改到0
            unionApplyOperateItem = clubMember.getClubMemberBO().execSportsPointApplyRematchZhongZhi(unionId, 0D);
        } else if (UnionDefine.UNION_TYPE.NORMAL.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            unionApplyOperateItem = clubMember.getClubMemberBO().execSportsPointApplyRematch(unionId, outSports);
        }
        if (Objects.isNull(unionApplyOperateItem)) {
            return SData_Result.make(ErrorCode.NotAllow, "NotAllow");
        }
        if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            clubMember.getClubMemberBO().saveUnionState(UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value());
        } else if (UnionDefine.UNION_TYPE.NORMAL.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            if (unionApplyOperateItem.getFinalValue() == 0D || execClubMember.getClubMemberBO().execSportsPointUpdate(unionId, -unionApplyOperateItem.getFinalValue(), ItemFlow.UNION_APPLY_REMATCH_OP, RoomTypeEnum.NORMAL, outSports)) {
                clubMember.getClubMemberBO().saveUnionState(UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value());
            } else {
                // 联赛申请退赛-回退
                clubMember.getClubMemberBO().execRollbackValue(unionId, -unionApplyOperateItem.getFinalValue(), ItemFlow.UNION_APPLY_REMATCH_ROLLBACK);
                // 回退比赛分
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
        } else {
            return SData_Result.make(ErrorCode.UNION_TYPE_NOT_EXIST, "UNION_TYPE_NOT_EXIST");
        }
        // 赛事消息
        UnionDynamicBO.insertSportsPoint(clubMember.getClubMemberBO().getPlayerID(), unionId, clubMember.getClubID(), execClubMember.getClubMemberBO().getPlayerID(), execClubMember.getClubID(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_APPLY_REMATCH_AGREE.value(), String.valueOf(unionApplyOperateItem.getPreValue()), 0, "");
        // 消息通知
        this.notify2Player(clubMember.getClubMemberBO().getPlayerID(), SUnion_MatchState.make(unionId, clubMember.getClubID(), clubMember.getClubMemberBO().getPlayerID(), clubMember.getClubMemberBO().getUnionState(), type, operate));
        return SData_Result.make(ErrorCode.Success, SUnion_MemberExamineOperate.make(unionId, clubMember.getClubID(), clubMember.getClubMemberBO().getPlayerID(), type, operate));
    }

    /**
     * 通知到指定玩家
     *
     * @param pid         玩家pid
     * @param baseSendMsg 通知信息
     */
    private void notify2Player(long pid, BaseSendMsg baseSendMsg) {
        Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(pid);
        if (Objects.nonNull(player)) {
            player.pushProtoMq(baseSendMsg);
        }
    }


    /**
     * 赛事成员列表
     *
     * @param unionId 赛事Id
     * @param pageNum 第几页
     * @return
     */
    public SData_Result getUnionMemberList(long unionId, int pageNum, String query) {
        // 查询亲友圈ID
        final int clubSign = TypeUtils.StringTypeInt(query);
        Map<Long, UnionMember> unionMemberMap;
        if (Config.isShare()) {
            unionMemberMap = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId);
        } else {
            unionMemberMap = this.getUnionMemberMap();
        }
        return SData_Result.make(ErrorCode.Success, unionMemberMap.values().stream()
                .filter(k -> null != k && k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                .map(k -> {
                    Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getUnionMemberBO().getClubId());
                    if (null == club) {
                        return null;
                    }
                    Player player = club.getOwnerPlayer();
                    if (null == player) {
                        return null;
                    }
                    ClubMember clubMember;
                    if (Config.isShare()) {
                        clubMember = ShareClubMemberMgr.getInstance().getClubMember(k.getUnionMemberBO().getClubMemberId());
                    } else {
                        clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(k.getUnionMemberBO().getClubMemberId());
                    }
                    if (null == clubMember) {
                        return null;
                    }
                    double weekTotalScorePoint = 0L;

                    double zhongZhiTotalPoint = this.getZhongZhiTotalPoint(unionId, club.getClubListBO().getId());

                    double zhongZhiEliminatePointSum = this.getZhongZhiEliminatePointSum(unionId, club.getClubListBO().getId());
//                            this.getWeekTotalScorePoint(unionId, club.getClubListBO().getId());
                    // 没有查询信息 或者
                    if (StringUtils.isEmpty(query) || (clubSign == club.getClubListBO().getClubsign())) {
                        Map<String, Object> unionMemberStatisticsMap = ClubMgr.getInstance().getClubMemberMgr().unionMemberStatisticsMap(club.getClubListBO().getId());
                        return new UnionMemberItem(club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), club.getClubListBO().getClubsign(), clubMember.getClubMemberBO().getSportsPoint(), clubMember.getClubMemberBO().getScoreDividedInto(),
                                weekTotalScorePoint, (Integer) unionMemberStatisticsMap.get("COUNT"), k.getUnionMemberBO().getType(), (Double) unionMemberStatisticsMap.get("SUM"),
                                clubMember.getClubMemberBO().getShareValue(), clubMember.getClubMemberBO().getShareFixedValue(), clubMember.getClubMemberBO().getShareType(), k.getAlivePointZhongZhi(), zhongZhiTotalPoint, zhongZhiEliminatePointSum);
                    }
                    if (club.getClubListBO().getName().contains(query)) {
                        Map<String, Object> unionMemberStatisticsMap = ClubMgr.getInstance().getClubMemberMgr().unionMemberStatisticsMap(club.getClubListBO().getId());
                        return new UnionMemberItem(club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), club.getClubListBO().getClubsign(), clubMember.getClubMemberBO().getSportsPoint(), clubMember.getClubMemberBO().getScoreDividedInto(),
                                weekTotalScorePoint, (Integer) unionMemberStatisticsMap.get("COUNT"), k.getUnionMemberBO().getType(), (Double) unionMemberStatisticsMap.get("SUM"),
                                clubMember.getClubMemberBO().getShareValue(), clubMember.getClubMemberBO().getShareFixedValue(), clubMember.getClubMemberBO().getShareType(), k.getAlivePointZhongZhi(), zhongZhiTotalPoint, zhongZhiEliminatePointSum);
                    }
                    return null;
                })
                .filter(k -> null != k)
//                .sorted(Comparator.comparing(UnionMemberItem::getScorePercent).reversed()) 排序暂时移除
                .skip(Page.getPageNum(pageNum))
                .limit(Page.PAGE_SIZE)
                .collect(Collectors.toList()));

    }

    /**
     * 赛事成员列表
     *
     * @param unionId 赛事Id
     * @param pageNum 第几页
     * @return
     */
    public SData_Result getUnionMemberRanked(long unionId, int pageNum, String query, int type) {
        // 查询亲友圈ID
        final int clubSign = TypeUtils.StringTypeInt(query);
        Map<Long, UnionMember> unionMemberMap;
        if (Config.isShare()) {
            unionMemberMap = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId);
        } else {
            unionMemberMap = this.getUnionMemberMap();
        }
        List<UnionMemberItem> unionMemberItems = unionMemberMap.values().stream()
                .filter(k -> null != k && k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                .map(k -> {
                    Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getUnionMemberBO().getClubId());
                    if (null == club) {
                        return null;
                    }
                    Player player = club.getOwnerPlayer();
                    if (null == player) {
                        return null;
                    }
                    ClubMember clubMember;
                    if (Config.isShare()) {
                        clubMember = ShareClubMemberMgr.getInstance().getClubMember(k.getUnionMemberBO().getClubMemberId());
                    } else {
                        clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(k.getUnionMemberBO().getClubMemberId());
                    }
                    if (null == clubMember) {
                        return null;
                    }
                    double weekTotalScorePoint = 0L;
                    UnionCountByZhongZhiItem unionCountByZhongZhiItem = this.getZhongZhiUnionAllMemberPointTotalPoint(unionId, club.getClubListBO().getId(), type);
                    double zhongZhiEliminatePointSum = this.getZhongZhiEliminatePointSum(unionId, club.getClubListBO().getId());
//                            this.getWeekTotalScorePoint(unionId, club.getClubListBO().getId());

                    // 没有查询信息 或者
                    if (StringUtils.isEmpty(query) || (clubSign == club.getClubListBO().getClubsign())) {
                        Map<String, Object> unionMemberStatisticsMap = ClubMgr.getInstance().getClubMemberMgr().unionMemberStatisticsMap(club.getClubListBO().getId());
                        return new UnionMemberItem(club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), club.getClubListBO().getClubsign(), clubMember.getClubMemberBO().getSportsPoint(), clubMember.getClubMemberBO().getScoreDividedInto(),
                                weekTotalScorePoint, (Integer) unionMemberStatisticsMap.get("COUNT"), k.getUnionMemberBO().getType(), (Double) unionMemberStatisticsMap.get("SUM"),
                                clubMember.getClubMemberBO().getShareValue(), clubMember.getClubMemberBO().getShareFixedValue(), clubMember.getClubMemberBO().getShareType(), k.getAlivePointZhongZhi(), unionCountByZhongZhiItem.getZhongZhiTotalPoint(), zhongZhiEliminatePointSum, unionCountByZhongZhiItem.getUnionAllMemberPointTotal(), unionCountByZhongZhiItem.getConsumeValue());
                    }
                    if (club.getClubListBO().getName().contains(query)) {
                        Map<String, Object> unionMemberStatisticsMap = ClubMgr.getInstance().getClubMemberMgr().unionMemberStatisticsMap(club.getClubListBO().getId());
                        return new UnionMemberItem(club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), club.getClubListBO().getClubsign(), clubMember.getClubMemberBO().getSportsPoint(), clubMember.getClubMemberBO().getScoreDividedInto(),
                                weekTotalScorePoint, (Integer) unionMemberStatisticsMap.get("COUNT"), k.getUnionMemberBO().getType(), (Double) unionMemberStatisticsMap.get("SUM"),
                                clubMember.getClubMemberBO().getShareValue(), clubMember.getClubMemberBO().getShareFixedValue(), clubMember.getClubMemberBO().getShareType(), k.getAlivePointZhongZhi(), unionCountByZhongZhiItem.getZhongZhiTotalPoint(), zhongZhiEliminatePointSum, unionCountByZhongZhiItem.getUnionAllMemberPointTotal(), unionCountByZhongZhiItem.getConsumeValue());
                    }
                    return null;
                })
                .filter(k -> null != k)
//                .sorted(Comparator.comparing(UnionMemberItem::getScorePercent).reversed()) 排序暂时移除
                .skip(Page.getPageNum(pageNum))
                .limit(Page.PAGE_SIZE_100)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(unionMemberItems)) {
            //重新赋值客户端所需要的序数id
            for (int i = 0; i < unionMemberItems.size(); i++) {
                unionMemberItems.get(i).setId((pageNum - 1) * Page.PAGE_SIZE_100 + 1 + i);
            }
        }
        return SData_Result.make(ErrorCode.Success, new UnionMemberRankedInfo(unionMemberItems));
    }

    /**
     * 赛事成员列表
     *
     * @param unionId 赛事Id
     * @param pageNum 第几页
     * @return
     */
    public SData_Result getUnionMemberRankedSumInfo(long unionId, int pageNum, String query, int type) {
        // 查询亲友圈ID
        final int clubSign = TypeUtils.StringTypeInt(query);
        Map<Long, UnionMember> unionMemberMap;
        if (Config.isShare()) {
            unionMemberMap = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId);
        } else {
            unionMemberMap = this.getUnionMemberMap();
        }
        List<UnionMemberItem> unionMemberItems = unionMemberMap.values().stream()
                .filter(k -> null != k && k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                .map(k -> {
                    Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getUnionMemberBO().getClubId());
                    if (null == club) {
                        return null;
                    }
                    Player player = club.getOwnerPlayer();
                    if (null == player) {
                        return null;
                    }
                    ClubMember clubMember;
                    if (Config.isShare()) {
                        clubMember = ShareClubMemberMgr.getInstance().getClubMember(k.getUnionMemberBO().getClubMemberId());
                    } else {
                        clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(k.getUnionMemberBO().getClubMemberId());
                    }
                    if (null == clubMember) {
                        return null;
                    }
                    double weekTotalScorePoint = 0L;
                    UnionCountByZhongZhiItem unionCountByZhongZhiItem = this.getZhongZhiUnionAllMemberPointTotalPoint(unionId, club.getClubListBO().getId(), type);
                    double zhongZhiEliminatePointSum = this.getZhongZhiEliminatePointSum(unionId, club.getClubListBO().getId());
//                            this.getWeekTotalScorePoint(unionId, club.getClubListBO().getId());
                    // 没有查询信息 或者
                    if (StringUtils.isEmpty(query) || (clubSign == club.getClubListBO().getClubsign())) {
                        Map<String, Object> unionMemberStatisticsMap = ClubMgr.getInstance().getClubMemberMgr().unionMemberStatisticsMap(club.getClubListBO().getId());
                        return new UnionMemberItem(club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), club.getClubListBO().getClubsign(), clubMember.getClubMemberBO().getSportsPoint(), clubMember.getClubMemberBO().getScoreDividedInto(),
                                weekTotalScorePoint, (Integer) unionMemberStatisticsMap.get("COUNT"), k.getUnionMemberBO().getType(), (Double) unionMemberStatisticsMap.get("SUM"),
                                clubMember.getClubMemberBO().getShareValue(), clubMember.getClubMemberBO().getShareFixedValue(), clubMember.getClubMemberBO().getShareType(), k.getAlivePointZhongZhi(), unionCountByZhongZhiItem.getZhongZhiTotalPoint(), zhongZhiEliminatePointSum, unionCountByZhongZhiItem.getUnionAllMemberPointTotal());
                    }
                    if (club.getClubListBO().getName().contains(query)) {
                        Map<String, Object> unionMemberStatisticsMap = ClubMgr.getInstance().getClubMemberMgr().unionMemberStatisticsMap(club.getClubListBO().getId());
                        return new UnionMemberItem(club.getClubListBO().getName(), club.getClubListBO().getId(), player.getName(), player.getPid(), club.getClubListBO().getClubsign(), clubMember.getClubMemberBO().getSportsPoint(), clubMember.getClubMemberBO().getScoreDividedInto(),
                                weekTotalScorePoint, (Integer) unionMemberStatisticsMap.get("COUNT"), k.getUnionMemberBO().getType(), (Double) unionMemberStatisticsMap.get("SUM"),
                                clubMember.getClubMemberBO().getShareValue(), clubMember.getClubMemberBO().getShareFixedValue(), clubMember.getClubMemberBO().getShareType(), k.getAlivePointZhongZhi(), unionCountByZhongZhiItem.getZhongZhiTotalPoint(), zhongZhiEliminatePointSum, unionCountByZhongZhiItem.getUnionAllMemberPointTotal());
                    }
                    return null;
                })
                .filter(k -> null != k)
//                .sorted(Comparator.comparing(UnionMemberItem::getScorePercent).reversed()) 排序暂时移除
                .collect(Collectors.toList());
        double scorePointSum = unionMemberItems.stream().mapToDouble(k -> k.getScorePoint()).sum();
        double consumeValueSum = unionMemberItems.stream().mapToDouble(k -> k.getConsumeValue()).sum();
        double unionAllMemberPointTotalSum = unionMemberItems.stream().mapToDouble(k -> k.getUnionAllMemberPointTotal()).sum();
        double zhongZhiTotalPointSum = unionMemberItems.stream().mapToDouble(k -> k.getZhongZhiTotalPoint()).sum();
        return SData_Result.make(ErrorCode.Success, new UnionMemberRankedSumInfo(scorePointSum, consumeValueSum, unionAllMemberPointTotalSum, zhongZhiTotalPointSum));
    }

    /**
     * 赛事成员列表
     *
     * @return
     */
    public SData_Result getUnionMemberRankedList(CUnion_MemberList req, long pid) {
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(pid, req.getClubId(), req.getUnionId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (Objects.isNull(unionMember)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER getUnionRoomConfigPrizePoolCountRankedByZhongZhi ");
        }
        if (!unionMember.isCreate()) {
            return SData_Result.make(ErrorCode.UNION_NOT_CREATE, "UNION_NOT_CREATE getUnionRoomConfigPrizePoolCountRankedByZhongZhi ");
        }
        // 查询亲友圈ID
        Map<Long, UnionMember> unionMemberMap;
        if (Config.isShare()) {
            unionMemberMap = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(req.getUnionId());
        } else {
            unionMemberMap = this.getUnionMemberMap();
        }
        return SData_Result.make(ErrorCode.Success, unionMemberMap.values().stream()
                .filter(k -> null != k && k.getUnionMemberBO().getUnionId() == req.getUnionId() && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                .map(k -> {
                    Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getUnionMemberBO().getClubId());
                    if (null == club) {
                        return null;
                    }
                    Player player = club.getOwnerPlayer();
                    if (null == player) {
                        return null;
                    }
                    ClubMember clubMember;
                    if (Config.isShare()) {
                        clubMember = ShareClubMemberMgr.getInstance().getClubMember(k.getUnionMemberBO().getClubMemberId());
                    } else {
                        clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(k.getUnionMemberBO().getClubMemberId());
                    }
                    if (null == clubMember) {
                        return null;
                    }
                    UnionCountByZhongZhiItem unionCountByZhongZhiItem = this.getZhongZhiAllUnionMemberInfo(req.getType(), req.getUnionId(), k.getUnionMemberBO().getClubId());
                    return new UnionMemberRankedItem(club.getClubListBO().getName(), club.getClubListBO().getClubsign(), unionCountByZhongZhiItem.getUnionAllMemberPointTotal(),
                            unionCountByZhongZhiItem.getBigWinner(), unionCountByZhongZhiItem.getConsumeValue(), unionCountByZhongZhiItem.getPromotionShareValue(), unionCountByZhongZhiItem.getZhongZhiTotalPoint());

                })
                .filter(k -> null != k)
//                .sorted(Comparator.comparing(UnionMemberItem::getScorePercent).reversed()) 排序暂时移除
                .skip(Page.getPageNum(req.getPageNum()))
                .limit(Page.PAGE_SIZE)
                .collect(Collectors.toList()));

    }

    /**
     * 获取某个亲友圈全部的淘汰分和
     *
     * @param unionId
     * @param clubId
     * @return
     */
    private double getZhongZhiEliminatePointSum(long unionId, long clubId) {
        List<ClubMember> clubMemberList = ClubMgr.getInstance().getClubMemberMgr().findClubIdAllClubMember(clubId,
                Club_define.Club_Player_Status.PLAYER_JIARU.value());
        double zhongZhiEliminatePointSum = 0D;
        if (CollectionUtils.isNotEmpty(clubMemberList)) {
            zhongZhiEliminatePointSum = clubMemberList.stream().mapToDouble(k -> k.getClubMemberBO().getEliminatePoint()).sum();
        }
        return zhongZhiEliminatePointSum;

    }

    /***
     * 获取全部成员排行
     * @param type
     * @param unionId
     * @param clubId
     * @return
     */
    private UnionCountByZhongZhiItem getZhongZhiAllUnionMemberInfo(int type, long unionId, long clubId) {
        if (type == Club_define.CLUB_COUNT_RECORD.CLUB_COUNT_RECORD_Seven.value()) {
            //七天汇总
            return getZhongZhiAllUnionMemberInfoSevenDayTotal(type, unionId, clubId);
        } else {
            //当天计算
            return getZhongZhiAllUnionMemberInfoOneDay(type, unionId, clubId);
        }
    }

    /**
     * * 获取中至计算数据
     * 最终积分算法
     * 获取七天的总合
     *
     * @param type
     * @param unionId
     * @param clubId
     * @return
     */
    private UnionCountByZhongZhiItem getZhongZhiAllUnionMemberInfoSevenDayTotal(int type, long unionId, long clubId) {
        String key = String.format(DataConstants.CLUB_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_TIME, unionId, clubId, "SevenDay");
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            UnionCountByZhongZhiItem resultToday = getZhongZhiAllUnionMemberInfoOneDay(Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TODAY.value(), unionId, clubId);
            UnionCountByZhongZhiItem resultSix = getZhongZhiAllUnionMemberInfoRecentDay(type, unionId, clubId);
            unionRoomConfigPrizePoolItem = new UnionCountByZhongZhiItem();
            unionRoomConfigPrizePoolItem.setConsumeValue(resultToday.getConsumeValue() + resultSix.getConsumeValue());
            unionRoomConfigPrizePoolItem.setPrizePool(CommMath.addDouble(resultToday.getPrizePool(), resultSix.getPrizePool()));
            unionRoomConfigPrizePoolItem.setFinalAllMemberPointTotal(CommMath.addDouble(resultToday.getFinalAllMemberPointTotal(), resultSix.getFinalAllMemberPointTotal()));
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(CommMath.addDouble(resultToday.getUnionAllMemberPointTotal(), resultSix.getUnionAllMemberPointTotal()));
            unionRoomConfigPrizePoolItem.setAllWinLose(CommMath.addDouble(resultToday.getAllWinLose(), resultSix.getAllWinLose()));
            unionRoomConfigPrizePoolItem.setRoomSportsPointConsume(CommMath.addDouble(resultToday.getRoomSportsPointConsume(), resultSix.getRoomSportsPointConsume()));
            unionRoomConfigPrizePoolItem.setPromotionShareValue(CommMath.addDouble(resultToday.getPromotionShareValue(), resultSix.getPromotionShareValue()));
            unionRoomConfigPrizePoolItem.setZhongZhiTotalPoint(CommMath.addDouble(resultToday.getZhongZhiTotalPoint(), resultSix.getZhongZhiTotalPoint()));
            unionRoomConfigPrizePoolItem.setBigWinner(resultToday.getBigWinner() + resultSix.getBigWinner());
            unionRoomConfigPrizePoolItem.setRoomSize(resultToday.getRoomSize() + resultSix.getRoomSize());
            unionRoomConfigPrizePoolItem.setSetCount(resultToday.getSetCount() + resultSix.getSetCount());
        }
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return unionRoomConfigPrizePoolItem;
    }

    /**
     * 获取中至计算数据
     * 最终积分算法
     *
     * @param unionId
     * @param clubId
     * @return
     */
    private UnionCountByZhongZhiItem getZhongZhiAllUnionMemberInfoRecentDay(int type, long unionId, long clubId) {
        String key = String.format(DataConstants.CLUB_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_RECENT, unionId, clubId, Long.valueOf(type));
        String dateTime = CommTime.getYesterDayStringYMD(type);
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            unionRoomConfigPrizePoolItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.lt("date_time", CommTime.getYesterDayStringYMD(0)),
                    Restrictions.ge("date_time", CommTime.getYesterDayStringYMD(6))), UnionCountByZhongZhiItem.class, UnionCountByZhongZhiItem.getItemsNameCount());
            ClubLevelRoomCountLogItem items = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(Restrictions.lt("date_time", CommTime.getYesterDayStringYMD(0)),
                    Restrictions.ge("date_time", CommTime.getYesterDayStringYMD(6)), Restrictions.eq("clubId", clubId)), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            double allWinLose = 0D;
            double allRoomSportsPointConsume = 0D;
            double promotionShareValue = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
                allRoomSportsPointConsume = items.getRoomSportsPointConsume();
                promotionShareValue = items.getPromotionShareValue();
                unionRoomConfigPrizePoolItem.setBigWinner(items.getWinner());
                unionRoomConfigPrizePoolItem.setConsumeValue(items.getConsume());
            }
            //所有成员的输赢积分
            unionRoomConfigPrizePoolItem.setAllWinLose(allWinLose);
            //所有成员的房费消耗
            unionRoomConfigPrizePoolItem.setRoomSportsPointConsume(allRoomSportsPointConsume);
            //所有成员的活跃积分
            unionRoomConfigPrizePoolItem.setPromotionShareValue(promotionShareValue);
            //成员总积分  输赢积分-比赛分消耗(房费消耗)
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(CommMath.subDouble(allWinLose, unionRoomConfigPrizePoolItem.getRoomSportsPointConsume()));
            //最终积分(总积分+活跃积分总和)
            unionRoomConfigPrizePoolItem.setZhongZhiTotalPoint(CommMath.addDouble(unionRoomConfigPrizePoolItem.getUnionAllMemberPointTotal(), unionRoomConfigPrizePoolItem.getPromotionShareValue()));
        }
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return unionRoomConfigPrizePoolItem;
    }


    /**
     * 获取中至计算数据
     * 最终积分算法
     *
     * @param unionId
     * @param clubId
     * @return
     */
    private UnionCountByZhongZhiItem getZhongZhiAllUnionMemberInfoOneDay(int type, long unionId, long clubId) {
        String dateTime = CommTime.getYesterDayStringYMD(type);
        String key = String.format(DataConstants.CLUB_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_TIME, unionId, clubId, dateTime);
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            unionRoomConfigPrizePoolItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.eq("date_time", dateTime)), UnionCountByZhongZhiItem.class, UnionCountByZhongZhiItem.getItemsNameCount());
            ClubLevelRoomCountLogItem items;
            if (type == Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TODAY.value()) {
                items = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", CommTime.getYesterDayStringYMD(0)), Restrictions.eq("clubId", clubId)), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            } else {
                items = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", CommTime.getYesterDayStringYMD(type)), Restrictions.eq("clubId", clubId)), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            }
            double allWinLose = 0D;
            double allRoomSportsPointConsume = 0D;
            double promotionShareValue = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
                allRoomSportsPointConsume = items.getRoomSportsPointConsume();
                promotionShareValue = items.getPromotionShareValue();
                unionRoomConfigPrizePoolItem.setBigWinner(items.getWinner());
                unionRoomConfigPrizePoolItem.setConsumeValue(items.getConsume());
            }
            //所有成员的输赢积分
            unionRoomConfigPrizePoolItem.setAllWinLose(allWinLose);
            //所有成员的房费消耗
            unionRoomConfigPrizePoolItem.setRoomSportsPointConsume(allRoomSportsPointConsume);
            //所有成员的活跃积分
            unionRoomConfigPrizePoolItem.setPromotionShareValue(promotionShareValue);
            //成员总积分  输赢积分-比赛分消耗(房费消耗)
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(CommMath.subDouble(allWinLose, unionRoomConfigPrizePoolItem.getRoomSportsPointConsume()));
            //最终积分(总积分+活跃积分总和)
            unionRoomConfigPrizePoolItem.setZhongZhiTotalPoint(CommMath.addDouble(unionRoomConfigPrizePoolItem.getUnionAllMemberPointTotal(), unionRoomConfigPrizePoolItem.getPromotionShareValue()));
        }
        CommLogD.error(unionRoomConfigPrizePoolItem.toString());
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return unionRoomConfigPrizePoolItem;
    }

    /**
     * 获取中至计算数据
     * 最终积分算法
     * 6点的那张表
     *
     * @param unionId
     * @param clubId
     * @return
     */
    private double getZhongZhiTotalPointBy6(long unionId, long clubId) {
        String key = String.format(DataConstants.CLUB_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI, unionId, clubId, Long.valueOf(0));
        String dateTime = CommTime.getYesterDayStringYMD(0);
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            unionRoomConfigPrizePoolItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.eq("date_time", dateTime)), UnionCountByZhongZhiItem.class, UnionCountByZhongZhiItem.getItemsNameCount());
            ClubLevelRoomCountLogItem items = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", CommTime.getYesterDayStringYMD(0)), Restrictions.eq("clubId", clubId)), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            double allWinLose = 0D;
            double allRoomSportsPointConsume = 0D;
            double promotionShareValue = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
                allRoomSportsPointConsume = items.getRoomSportsPointConsume();
                promotionShareValue = items.getPromotionShareValue();
            }
            //所有成员的输赢积分
            unionRoomConfigPrizePoolItem.setAllWinLose(allWinLose);
            //所有成员的房费消耗
            unionRoomConfigPrizePoolItem.setRoomSportsPointConsume(allRoomSportsPointConsume);
            //所有成员的活跃积分
            unionRoomConfigPrizePoolItem.setPromotionShareValue(promotionShareValue);
            //成员总积分  输赢积分-比赛分消耗(房费消耗)
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(CommMath.subDouble(allWinLose, unionRoomConfigPrizePoolItem.getRoomSportsPointConsume()));
            //最终积分(总积分+活跃积分总和)
            unionRoomConfigPrizePoolItem.setZhongZhiTotalPoint(CommMath.addDouble(unionRoomConfigPrizePoolItem.getUnionAllMemberPointTotal(), unionRoomConfigPrizePoolItem.getPromotionShareValue()));
        }
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return unionRoomConfigPrizePoolItem.getZhongZhiTotalPoint();
//        return item.getScorePoint();
    }

    /**
     * 获取中至计算数据
     * 最终积分算法
     *
     * @param unionId
     * @param clubId
     * @return
     */
    private double getZhongZhiTotalPoint(long unionId, long clubId) {
        String key = String.format(DataConstants.CLUB_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI, unionId, clubId, Long.valueOf(0));
        String dateTime = String.valueOf(CommTime.getCycleNowTime6YMD());
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            unionRoomConfigPrizePoolItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.eq("date_time", dateTime)), UnionCountByZhongZhiItem.class, UnionCountByZhongZhiItem.getItemsNameCount());
            ClubLevelRoomCountLogItem items = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", dateTime), Restrictions.eq("clubId", clubId)), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            double allWinLose = 0D;
            double allRoomSportsPointConsume = 0D;
            double promotionShareValue = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
                allRoomSportsPointConsume = items.getRoomSportsPointConsume();
                promotionShareValue = items.getPromotionShareValue();
            }
            //所有成员的输赢积分
            unionRoomConfigPrizePoolItem.setAllWinLose(allWinLose);
            //所有成员的房费消耗
            unionRoomConfigPrizePoolItem.setRoomSportsPointConsume(allRoomSportsPointConsume);
            //所有成员的活跃积分
            unionRoomConfigPrizePoolItem.setPromotionShareValue(promotionShareValue);
            //成员总积分  输赢积分-比赛分消耗(房费消耗)
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(CommMath.subDouble(allWinLose, unionRoomConfigPrizePoolItem.getRoomSportsPointConsume()));
            //最终积分(总积分+活跃积分总和)
            unionRoomConfigPrizePoolItem.setZhongZhiTotalPoint(CommMath.addDouble(unionRoomConfigPrizePoolItem.getUnionAllMemberPointTotal(), unionRoomConfigPrizePoolItem.getPromotionShareValue()));
        }
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return unionRoomConfigPrizePoolItem.getZhongZhiTotalPoint();
    }

    /***
     *   * 获取中至计算数据
     * 成员总积分和
     * @param unionId
     * @param clubId
     * @return
     */
    private UnionCountByZhongZhiItem getZhongZhiUnionAllMemberPointTotalPoint(long unionId, long clubId, int type) {
        String key = String.format(DataConstants.CLUB_ROOM_CONFIG_PRIZE_POOL_COUNT_ALL_ZHONGZHI, unionId, clubId, Long.valueOf(0));

        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            String dateTime;
            ClubLevelRoomCountLogItem items;
            if (type == 0) {
                dateTime = String.valueOf(CommTime.getCycleNowTime6YMD());
                unionRoomConfigPrizePoolItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.eq("date_time", dateTime)), UnionCountByZhongZhiItem.class, UnionCountByZhongZhiItem.getItemsNameCount());
                items = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", dateTime), Restrictions.eq("clubId", clubId)), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            } else {
                dateTime = String.valueOf(CommTime.getYesterDayStringYMDSix(type));
                unionRoomConfigPrizePoolItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", unionId), Restrictions.eq("date_time", dateTime)), UnionCountByZhongZhiItem.class, UnionCountByZhongZhiItem.getItemsNameCount());
                items = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", dateTime), Restrictions.eq("clubId", clubId)), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            }
            double allWinLose = 0D;
            double allRoomSportsPointConsume = 0D;
            double promotionShareValue = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
                allRoomSportsPointConsume = items.getRoomSportsPointConsume();
                promotionShareValue = items.getPromotionShareValue();
            }
            //所有成员的输赢积分
            unionRoomConfigPrizePoolItem.setAllWinLose(allWinLose);
            //所有成员的房费消耗
            unionRoomConfigPrizePoolItem.setRoomSportsPointConsume(allRoomSportsPointConsume);
            //所有成员的活跃积分
            unionRoomConfigPrizePoolItem.setPromotionShareValue(promotionShareValue);
            //成员总积分  输赢积分-比赛分消耗(房费消耗)
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(CommMath.subDouble(allWinLose, unionRoomConfigPrizePoolItem.getRoomSportsPointConsume()));
            //最终积分(总积分+活跃积分总和)
            unionRoomConfigPrizePoolItem.setZhongZhiTotalPoint(CommMath.addDouble(unionRoomConfigPrizePoolItem.getUnionAllMemberPointTotal(), unionRoomConfigPrizePoolItem.getPromotionShareValue()));
        }
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return unionRoomConfigPrizePoolItem;
    }

    public static void main(String[] args) {
        System.out.println(String.valueOf(CommTime.getCycleNowTime6YMD()));
    }

    /**
     * 获取某个亲友圈最近一周的收益分数
     *
     * @param unionId
     * @param clubId
     * @return
     */
    private double getWeekTotalScorePoint(long unionId, long clubId) {
        UnionSportsPointLogItem item = EhCacheFactory.getCacheApi(UnionSportsProfitWeekCacheConfiguration.class).get(String.format(DataConstants.SCORE_SPORT_WEEK, unionId, clubId), UnionSportsPointLogItem.class);
        // 检查缓存是否为空
        if (Objects.isNull(item)) {
            item = ContainerMgr.get().getComponent(UnionSportsPointProfitLogFlowService.class).findOneE(Restrictions.and(Restrictions.gt("date_time", CommTime.getYesterDayStringYMD(7)), Restrictions.eq("unionId", unionId), Restrictions.eq("clubId", clubId)), UnionSportsPointLogItem.class, UnionSportsPointLogItem.getItemsName());
            if (Objects.isNull(item)) {
                // 返回本身分成
                return 0;
            } else {
                EhCacheFactory.getCacheApi(UnionSportsProfitWeekCacheConfiguration.class).put(String.format(DataConstants.SCORE_SPORT_WEEK, unionId, clubId), new UnionSportsPointLogItem(item.getScorePoint()));
            }
        }
        return item.getScorePoint();
    }

    /**
     * 检查赛事成员权利
     *
     * @param unionId  赛事id
     * @param clubId   亲友圈id
     * @param exePid   操作者id
     * @param opClubId 被操作者亲友圈id
     * @param opPid    被操作者Pid
     * @return
     */
    private SData_Result checkUnionRights(long unionId, long clubId, long exePid, long opClubId, long opPid) {

        // 获取被操作者的赛事成员信息。
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(opPid, opClubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        //获取当前操作者的亲友圈成员信息
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, exePid);
        if (null == execClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "execClubMember CLUB_NOT_EXIST_MEMBER_INFO exePid:{%d},clubId:{%d},unionId:{%d}", exePid, clubId, unionId);
        }
        //是不是当前盟主设置的赛事管理员
        if (execClubMember.isUnionMgr()) {
            return SData_Result.make(ErrorCode.Success, unionMember);
        }
        // 获取当前操作者的赛事成员信息。
        UnionMember execUnionMember = UnionMgr.getInstance().getUnionMemberMgr().find(exePid, clubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == execUnionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER exePid:{%d},clubId:{%d},unionId:{%d}", exePid, clubId, unionId);
        }
        // 检查当前操作者是否有基本权限。
        if (!execUnionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
        }
        if (null == unionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "unionMember UNION_NOT_EXIST_MEMBER opPid:{%d},opClubId:{%d},unionId:{%d}", opPid, opClubId, unionId);
        }

        // 检查操作者和被操作者权限是否一致。
        if (unionMember.getUnionMemberBO().getPostType().value() >= execUnionMember.getUnionMemberBO().getPostType().value()) {
            // 被操作者的权力 >= 操作者的权力
            return SData_Result.make(ErrorCode.UNION_MEMBER_SAME_POST_TYPE, "UNION_MEMBER_SAME_RIGHTS execPostType:{%d},opPostType:{%d}，", execUnionMember.getUnionMemberBO().getPostType().value(), unionMember.getUnionMemberBO().getPostType().value());
        }
        return SData_Result.make(ErrorCode.Success, unionMember);
    }

    /**
     * 检查赛事成员权利
     *
     * @param unionId  赛事id
     * @param clubId   亲友圈id
     * @param exePid   操作者id
     * @param opClubId 被操作者亲友圈id
     * @param opPid    被操作者Pid
     * @return
     */
    private SData_Result checkUnionRightsShareChange(long unionId, long clubId, long exePid, long opClubId, long opPid) {
        // 获取当前操作者的赛事成员信息。
        UnionMember execUnionMember = UnionMgr.getInstance().getUnionMemberMgr().find(exePid, clubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == execUnionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER exePid:{%d},clubId:{%d},unionId:{%d}", exePid, clubId, unionId);
        }
        // 检查当前操作者是否有基本权限。
        if (!execUnionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
        }
        // 获取被操作者的赛事成员信息。
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(opPid, opClubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == unionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "unionMember UNION_NOT_EXIST_MEMBER opPid:{%d},opClubId:{%d},unionId:{%d}", opPid, opClubId, unionId);
        }

        // 检查操作者和被操作者权限是否一致。
        if (unionMember.getUnionMemberBO().getPostType().value() > execUnionMember.getUnionMemberBO().getPostType().value()) {
            // 被操作者的权力 >= 操作者的权力
            return SData_Result.make(ErrorCode.UNION_MEMBER_SAME_POST_TYPE, "UNION_MEMBER_SAME_RIGHTS execPostType:{%d},opPostType:{%d}，", execUnionMember.getUnionMemberBO().getPostType().value(), unionMember.getUnionMemberBO().getPostType().value());
        }
        return SData_Result.make(ErrorCode.Success, unionMember);
    }

    /**
     * 检查赛事成员权利
     *
     * @param unionId  赛事id
     * @param clubId   亲友圈id
     * @param exePid   操作者id
     * @param opClubId 被操作者亲友圈id
     * @param opPid    被操作者Pid
     * @return
     */
    public SData_Result checkUnionRightsShare(long unionId, long clubId, long exePid, long opClubId, long opPid) {
        //获取当前操作者的亲友圈成员信息
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, exePid);
        if (null == execClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "execClubMember CLUB_NOT_EXIST_MEMBER_INFO exePid:{%d},clubId:{%d},unionId:{%d}", exePid, clubId, unionId);
        }
        // 获取当前操作者的赛事成员信息。
        UnionMember execUnionMember = UnionMgr.getInstance().getUnionMemberMgr().find(exePid, clubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (!execClubMember.isUnionMgr() && null == execUnionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER exePid:{%d},clubId:{%d},unionId:{%d}", exePid, clubId, unionId);
        }
        // 检查当前操作者是否有基本权限。
        if (!execClubMember.isUnionMgr() && !execUnionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
        }
        // 获取被操作者的赛事成员信息。
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(opPid, opClubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == unionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "unionMember UNION_NOT_EXIST_MEMBER opPid:{%d},opClubId:{%d},unionId:{%d}", opPid, opClubId, unionId);
        }
        //如果是盟主操作自己的话 是可以的
        if (unionMember.isCreate() && execUnionMember.isCreate()) {
            return SData_Result.make(ErrorCode.Success, unionMember);
        }
        // 检查操作者和被操作者权限是否一致。
        if (!execClubMember.isUnionMgr() && unionMember.getUnionMemberBO().getPostType().value() >= execUnionMember.getUnionMemberBO().getPostType().value()) {
            // 被操作者的权力 >= 操作者的权力
            return SData_Result.make(ErrorCode.UNION_MEMBER_SAME_POST_TYPE, "UNION_MEMBER_SAME_RIGHTS execPostType:{%d},opPostType:{%d}，", execUnionMember.getUnionMemberBO().getPostType().value(), unionMember.getUnionMemberBO().getPostType().value());
        }
        return SData_Result.make(ErrorCode.Success, unionMember);
    }

    /**
     * 检查赛事成员权利
     *
     * @param unionId 赛事id
     * @param clubId  亲友圈id
     * @param exePid  操作者id
     * @param opPid   被操作者Pid
     * @return
     */
    public SData_Result checkUnionRights(long unionId, long clubId, long exePid, long opPid) {
        if (exePid == opPid) {
            // 自己不能操作自己
            return SData_Result.make(ErrorCode.UNION_NOT_OPERATE_YOURSELF, "UNION_NOT_OPERATE_YOURSELF");
        }
        //判断是不是赛事管理员的  盟主设置的
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, exePid);
        if (Objects.nonNull(clubMember) && clubMember.isUnionMgr()) {
            // 如果是赛事管理员，拥有和创建者一样的权限
            return SData_Result.make(ErrorCode.Success);
        }

        // 获取当前操作者的赛事成员信息。
        UnionMember execUnionMember = UnionMgr.getInstance().getUnionMemberMgr().find(exePid, clubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (Objects.isNull(execUnionMember)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER exePid:{%d},clubId:{%d},unionId:{%d}", exePid, clubId, unionId);
        }
        if (execUnionMember.isCreate()) {
            // 如果我是创建者，最高权限
            return SData_Result.make(ErrorCode.Success);
        }
        // 检查当前操作者是否有基本权限。
        if (!execUnionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
        }
        // 获取被操作者的赛事成员信息。
        boolean anyMatchMinister = UnionMgr.getInstance().getUnionMemberMgr().anyMatchMinister(unionId, opPid);
        if (anyMatchMinister) {
            return SData_Result.make(ErrorCode.UNION_MEMBER_SAME_POST_TYPE, "UNION_MEMBER_SAME_RIGHTS execPostType:{%d}", execUnionMember.getUnionMemberBO().getPostType().value());
        } else {
            return SData_Result.make(ErrorCode.Success);
        }
    }


    /**
     * 赛事退出
     *
     * @return
     */
    public SData_Result execUnionQuit(CUnion_Base req, long exePid) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (null == union) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }

        // 获取当前操作者的赛事成员信息。
        UnionMember execUnionMember = UnionMgr.getInstance().getUnionMemberMgr().find(exePid, req.getClubId(), req.getUnionId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == execUnionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionQuit UNION_NOT_EXIST_MEMBER");
        }
        if (UnionDefine.UNION_POST_TYPE.UNION_CREATE.equals(execUnionMember.getUnionMemberBO().getPostType())) {
            return SData_Result.make(ErrorCode.NotAllow, "UNION_CREATE");
        }

        if (ClubMgr.getInstance().getClubMemberMgr().checkExistSportsPointNotEqualZero(req.getClubId())) {
            return SData_Result.make(ErrorCode.UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO, "UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO");
        }
        UnionDefine.UNION_QUIT quit = UnionDefine.UNION_QUIT.valueOf(union.getUnionBO().getQuit());
        if (UnionDefine.UNION_QUIT.UNION_QUIT_NEED_AUDIT.equals(quit)) {
            execUnionMember.setStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU_SHENQING.value(), 0L);
            return SData_Result.make(ErrorCode.Success, UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU_SHENQING.value());
        } else {
            execUnionMember.setStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value(), 0);
            return SData_Result.make(ErrorCode.Success, UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value());
        }
    }

    /**
     * 移除赛事成员信息
     *
     * @return
     */
    public SData_Result execUnionRemoveMember(CUnion_RemoveMember req, long exePid) {
        SData_Result result = this.checkUnionRights(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        if (ClubMgr.getInstance().getClubMemberMgr().checkExistSportsPointNotEqualZero(req.getOpClubId())) {
            return SData_Result.make(ErrorCode.UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO, "UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO");
        }
        if (ClubMgr.getInstance().getClubMemberMgr().checkExistCaseSportsPointNotEqualZero(req.getOpClubId())) {
            return SData_Result.make(ErrorCode.CLUB_EXIST_CASE_SPORTS_POINT_NOT_EQUAL_ZERO, "UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO");
        }
        ((UnionMember) result.getData()).setStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_TICHU.value(), exePid);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getOpClubId());
        club.getClubListBO().saveSkin(0);
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 执行收益更新
     *
     * @return
     */
    public SData_Result execScorePercentUpdate(CUnion_ScorePercentUpdate req, long exePid) {
        SData_Result result = this.checkUnionRights(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 设置积分比例修改
        return ((UnionMember) result.getData()).setScoreDividedInto(req.getOpPid(), req.getOpClubId(), req.getValue(), exePid, 1);
    }

    /**
     * 执行职务类型更新
     *
     * @return
     */
    public SData_Result execPostTypeUpdate(CUnion_PostTypeUpdate req, long exePid) {
        SData_Result result = this.checkUnionRights(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        return ((UnionMember) result.getData()).setPostType(req.getOpPid(), req.getValue() <= 0 ? UnionDefine.UNION_POST_TYPE.UNION_CLUB.value() : UnionDefine.UNION_POST_TYPE.UNION_MANAGE.value(), exePid);
    }


    /**
     * 执行比赛分
     *
     * @param req
     * @param exePid 操作Pid
     * @return
     */
    public SData_Result execSportsPointUpdate(CUnion_SportsPointUpdate req, long exePid) {
        SData_Result result = this.checkUnionRightsClub(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 是否管理员
        boolean isManage = (boolean) result.getData();
        UnionDefine.UNION_SPORTS_POINT sportsPoint = UnionDefine.UNION_SPORTS_POINT.valueOf(req.getType());
        if (req.getValue() <= 0) {
            if (UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_MINUS.equals(sportsPoint)) {
                req.setValue(Math.abs(req.getValue()));
                sportsPoint = UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_ADD;
            } else {
                return SData_Result.make(ErrorCode.NotAllow, "execSportsPointUpdate value:{%s}", req.getValue());
            }
            return SData_Result.make(ErrorCode.NotAllow, "execSportsPointUpdate value:{%s}", req.getValue());
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
        }

        double pidValue;
        if (UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_ADD.equals(sportsPoint)) {
            pidValue = req.getValue();
            // 比赛分增加
            result = execSportsPointAdd(req, exePid, union.getUnionBO().getOutSports());
        } else {
            pidValue = -req.getValue();
            // 比赛分减少
            result = execSportsPointMinus(req, exePid, union.getUnionBO().getOutSports());
        }
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 比赛分增加/减少
        UnionDefine.UNION_EXEC_TYPE itemFlow = UnionDefine.UNION_EXEC_TYPE.getUnionExecTypeGaneral(isManage, req.getType() <= 0);
        // 修改前后值
        UnionSportsPointItem item = (UnionSportsPointItem) result.getData();
        // 比赛分值修改
        UnionDynamicBO.insertSportsPoint(req.getOpPid(), req.getUnionId(), req.getOpClubId(), exePid, req.getClubId(), CommTime.nowSecond(), itemFlow.value(), String.valueOf(req.getValue()), 0, String.valueOf(item.getCurValue()), String.valueOf(item.getPreValue()), String.valueOf(item.getPidCurValue()),
                String.valueOf(item.getPidPreValue()), String.valueOf(pidValue), String.valueOf(item.getExecPidCurValue()), String.valueOf(item.getExecPidPreValue()), String.valueOf(-pidValue));
        //新增日志
        UnionDefine.UNION_EXEC_TYPE itemFlowExe = UnionDefine.UNION_EXEC_TYPE.getUnionExecTypeGaneralSelf(isManage, req.getType() <= 0);
        //执行者的记录
        UnionDynamicBO.insertSportsPoint(exePid, req.getUnionId(), req.getClubId(), req.getOpPid(), req.getOpClubId(), CommTime.nowSecond(), itemFlowExe.value(), String.valueOf(req.getValue()), 1, String.valueOf(item.getCurValue()), String.valueOf(item.getPreValue()), String.valueOf(item.getPidCurValue()),
                String.valueOf(item.getPidPreValue()), String.valueOf(pidValue), String.valueOf(item.getExecPidCurValue()), String.valueOf(item.getExecPidPreValue()), String.valueOf(-pidValue));
        //被执行者的记录
//        UnionDynamicBO.insertSportsPoint(req.getOpPid(), req.getUnionId(),req.getOpClubId() ,exePid,req.getClubId(), CommTime.nowSecond(), itemFlow.value(), String.valueOf(req.getValue()), 1, String.valueOf(item.getCurValue()),String.valueOf(item.getPreValue()));

        ClubMember exeClubMember = ClubMgr.getInstance().getClubMemberMgr().find(exePid, req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        ClubMember opClubMember = ClubMgr.getInstance().getClubMemberMgr().find(req.getOpPid(), req.getOpClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (opClubMember.getClubMemberBO().getUpLevelId() != exeClubMember.getClubMemberBO().getId()) {
            Player player = PlayerMgr.getInstance().getPlayer(exePid);
            Player playerTo = PlayerMgr.getInstance().getPlayer(req.getOpPid());
            ClubMember upMember = ShareClubMemberMgr.getInstance().getClubMember(opClubMember.getClubMemberBO().getUpLevelId());
            if (Objects.nonNull(upMember) && Objects.nonNull(player) && Objects.nonNull(playerTo)) {
                //不能加字段  把没用的 曲线救国
                // pidPreValue 存pid
                // execPidPreValue存 name
                UnionDynamicBO.insertSportsPoint(upMember.getClubMemberBO().getPlayerID(), union.getUnionBO().getId(), upMember.getClubID(), 0, exeClubMember.getClubID(), CommTime.nowSecond(),
                        UnionDefine.UNION_EXEC_TYPE.getUnionExecTypeGaneralKuaJi(req.getType() <= 0).value(), String.valueOf(req.getValue()), 0, String.valueOf(item.getCurValue()), String.valueOf(item.getPreValue()), String.valueOf(item.getPidCurValue()),
                        String.valueOf(playerTo.getPid()), String.valueOf(pidValue), String.valueOf(playerTo.getName()), String.valueOf(item.getExecPidPreValue()), String.valueOf(-pidValue), player.getName(), String.valueOf(player.getPid()));
            }
        }
        return SData_Result.make(ErrorCode.Success, CUnion_ChangeSportPoint.make(req.getType(), req.getValue(), item.getPidCurValue()));
    }

    /**
     * 比赛分增加
     */
    private SData_Result execSportsPointAdd(CUnion_SportsPointUpdate req, long exePid, double outSports) {
        ClubMember exeClubMember = ClubMgr.getInstance().getClubMemberMgr().find(exePid, req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == exeClubMember) {
            // 找不到操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        // 被操作者是否在游戏中
        Player player = PlayerMgr.getInstance().getPlayer(exePid);
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().getPlayer(player);
        }
        if (Objects.nonNull(player) && player.getRoomInfo().getRoomId() > 0L && player.getRoomInfo().getClubId() == req.getOpClubId() && req.getUnionId() == player.getRoomInfo().getUnionId()) {
            return SData_Result.make(ErrorCode.UNION_PLAYER_IN_GAME_ERROR, "UNION_PLAYER_IN_GAME_ERROR");
        }
        ClubMember opClubMember = ClubMgr.getInstance().getClubMemberMgr().find(req.getOpPid(), req.getOpClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == opClubMember) {
            // 找不到被操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }

        double pidPreValue = new Double(opClubMember.getSportsPoint());
        double execPidPreValue = new Double(exeClubMember.getSportsPoint());

        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            // 执行增加被操作者的比赛分
            opClubMember.getClubMemberBO().execSportsPointUpdate(req.getUnionId(), req.getValue(), ItemFlow.UNION_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, outSports);
            return SData_Result.make(ErrorCode.Success, new UnionSportsPointItem(opClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getSportsPoint(), opClubMember.getClubMemberBO().getSportsPoint(), pidPreValue, exeClubMember.getSportsPoint(), execPidPreValue));

        } else if (UnionDefine.UNION_TYPE.NORMAL.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            // 执行减去操作者的比赛分
            if (exeClubMember.getClubMemberBO().execSportsPointUpdate(req.getUnionId(), -req.getValue(), ItemFlow.UNION_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, outSports)) {
                // 执行增加被操作者的比赛分
                opClubMember.getClubMemberBO().execSportsPointUpdate(req.getUnionId(), req.getValue(), ItemFlow.UNION_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, outSports);
                return SData_Result.make(ErrorCode.Success, new UnionSportsPointItem(opClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getSportsPoint(), opClubMember.getClubMemberBO().getSportsPoint(), pidPreValue, exeClubMember.getSportsPoint(), execPidPreValue));
            } else {
                // 操作者的比赛分不足
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
        } else {
            // 赛事类型不存在
            return SData_Result.make(ErrorCode.NotAllow, "UNION_TYPE not exist");
        }

    }

    /**
     * 比赛分切牌
     */
    public SData_Result execSportsPointQiePai(Double qiePaiConsume, Union union, ClubMemberBO exeClubMemberBO, AbsBaseRoom room) {
        if (Objects.isNull(union)) {
            // 找不到联赛信息
            return SData_Result.make(ErrorCode.NotEnough_RoomCardByXiPai, "UNION_NOT_EXIST");
        }
        ClubMember clubMemberUnionCreate = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(UnionMgr.getInstance().getUnionMemberMgr().findCreateClubMemberId(union.getUnionBO().getId()));
        if (null == clubMemberUnionCreate) {
            // 找不到操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.NotEnough_RoomCardByXiPai, "clubMemberUnionCreate");
        }
        if (null == exeClubMemberBO) {
            // 找不到操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.NotEnough_RoomCardByXiPai, "CLUB_NOTCLUBMEMBER");
        }
//        if(exeClubMemberBO.getId()==clubMemberUnionCreate.getId()){
//            return SData_Result.make(ErrorCode.NotAllow,"exeClubMemberBO is clubMemberUnionCreate");
//        }
        // 执行减去操作者的比赛分
        boolean execFlag = exeClubMemberBO.saveRoomSportsPointConsumeQiePai(exeClubMemberBO, exeClubMemberBO.getPlayerID(), union.getUnionBO().getId(), -qiePaiConsume, room.getBaseRoomConfigure().getGameType().getId(), -1, room.getRoomID(), room.getRoomKey(), union.getUnionBO().getOutSports());
        if (execFlag) {
            // 执行增加被操作者的比赛分
            clubMemberUnionCreate.getClubMemberBO().saveRoomSportsPointConsumeQiePai(exeClubMemberBO, clubMemberUnionCreate.getClubMemberBO().getPlayerID(), union.getUnionBO().getId(), qiePaiConsume, room.getBaseRoomConfigure().getGameType().getId(), -1, room.getRoomID(), room.getRoomKey(), union.getUnionBO().getOutSports());
            return SData_Result.make(ErrorCode.Success);
        }
        return SData_Result.make(ErrorCode.NotAllow, "execFlag is false");

    }

    /**
     * 比赛分减少
     */
    private SData_Result execSportsPointMinus(CUnion_SportsPointUpdate req, long exePid, double outSports) {
        // 获取亲友圈成员信息
        ClubMember exeClubMember = ClubMgr.getInstance().getClubMemberMgr().find(req.getOpPid(), req.getOpClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == exeClubMember) {
            // 找不到被操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        // 被操作者是否在游戏中
        Player player = PlayerMgr.getInstance().getPlayer(req.getOpPid());
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().getPlayer(player);
        }
        if (Objects.nonNull(player) && player.getRoomInfo().getRoomId() > 0L && player.getRoomInfo().getClubId() == req.getOpClubId() && req.getUnionId() == player.getRoomInfo().getUnionId()) {
            return SData_Result.make(ErrorCode.UNION_PLAYER_IN_GAME_ERROR, "UNION_PLAYER_IN_GAME_ERROR");
        }
        ClubMember opClubMember = ClubMgr.getInstance().getClubMemberMgr().find(exePid, req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == opClubMember) {
            // 找不到操作者亲友圈成员信息
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }

        double pidPreValue = new Double(opClubMember.getSportsPoint());
        double execPidPreValue = new Double(exeClubMember.getSportsPoint());
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            // 执行增加被操作者的比赛分
            exeClubMember.getClubMemberBO().execSportsPointUpdate(req.getUnionId(), -req.getValue(), ItemFlow.UNION_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, outSports);
            return SData_Result.make(ErrorCode.Success, new UnionSportsPointItem(opClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getSportsPoint(), exeClubMember.getClubMemberBO().getSportsPoint(), execPidPreValue, opClubMember.getSportsPoint(), pidPreValue));

        } else if (UnionDefine.UNION_TYPE.NORMAL.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            // 执行减去被操作者的比赛分
            if (exeClubMember.getClubMemberBO().execSportsPointUpdate(req.getUnionId(), -req.getValue(), ItemFlow.UNION_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, outSports)) {
                // 执行增加操作者的比赛分
                opClubMember.getClubMemberBO().execSportsPointUpdate(req.getUnionId(), req.getValue(), ItemFlow.UNION_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, outSports);
                return SData_Result.make(ErrorCode.Success, new UnionSportsPointItem(opClubMember.getClubMemberBO().getSportsPoint(), exeClubMember.getSportsPoint(), exeClubMember.getClubMemberBO().getSportsPoint(), execPidPreValue, opClubMember.getSportsPoint(), pidPreValue));
            } else {
                // 操作者的比赛分不足
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
        } else {
            // 赛事类型不存在
            return SData_Result.make(ErrorCode.NotAllow, "UNION_TYPE not exist");
        }

    }


    /**
     * 后台比赛分更新
     *
     * @return
     */
    public SData_Result execBackstageSportsPointUpdate(long unionId, long clubId, double value, int type) {
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(clubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == unionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE unionId:{%d},clubId:{%d}", unionId, clubId);
        }
        if (!UnionDefine.UNION_POST_TYPE.UNION_CREATE.equals(unionMember.getUnionMemberBO().getPostType())) {
            return SData_Result.make(ErrorCode.UNION_NOT_CREATE, "UNION_NOT_CREATE {%s}", unionMember.getUnionMemberBO().getPostType());
        }
        ClubMember exeClubMember;
        if (Config.isShare()) {
            exeClubMember = ShareClubMemberMgr.getInstance().getClubMember(unionMember.getUnionMemberBO().getClubMemberId());
        } else {
            exeClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(unionMember.getUnionMemberBO().getClubMemberId());
        }
        if (null == exeClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER ClubMemberId:{%d}", unionMember.getUnionMemberBO().getClubMemberId());
        }
        UnionDefine.UNION_SPORTS_POINT sportsPoint = UnionDefine.UNION_SPORTS_POINT.valueOf(type);
        if (UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_ADD.equals(sportsPoint)) {
            if (!exeClubMember.getClubMemberBO().execSportsPointUpdate(unionId, Math.abs(value), ItemFlow.UNION_BACKSTAGE_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, -Integer.MAX_VALUE)) {
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
        } else {
            if (!exeClubMember.getClubMemberBO().execSportsPointUpdate(unionId, -Math.abs(value), ItemFlow.UNION_BACKSTAGE_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, -Integer.MAX_VALUE)) {
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
        }
        // 比赛分增加/减少
//        UnionDefine.UNION_EXEC_TYPE itemFlow = type <= 0 ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_ADD : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_MINUS;
        // 比赛分值修改
//        UnionDynamicBO.insertUnionBackstageSportsPoint(unionMember.getUnionMemberBO().getClubOwnerId(), clubId, unionId, 0, CommTime.nowSecond(), itemFlow.value(), String.valueOf(Math.abs(value)), String.valueOf(exeClubMember.getClubMemberBO().getSportsPoint()));
        return SData_Result.make(ErrorCode.Success, exeClubMember.getClubMemberBO().getSportsPoint());
    }

    /**
     * 后台比赛分更新  盟主身上的分 执行的人分 之间移动
     * 操作亲友圈
     *
     * @return
     */
    public SData_Result execBackstageSportsPointUpdateClub(long unionId, long clubId, double value, int type, long opPid, long exePid, long opClubId) {
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(clubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == unionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE unionId:{%d},clubId:{%d}", unionId, clubId);
        }
        if (!UnionDefine.UNION_POST_TYPE.UNION_CREATE.equals(unionMember.getUnionMemberBO().getPostType())) {
            return SData_Result.make(ErrorCode.UNION_NOT_CREATE, "UNION_NOT_CREATE {%s}", unionMember.getUnionMemberBO().getPostType());
        }
        UnionMember exeUnionMember = UnionMgr.getInstance().getUnionMemberMgr().find(opClubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == exeUnionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER unionId:{%d},clubId:{%d}", unionId, clubId);
        }

        ClubMember doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, exePid);
        if (null == doClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER ClubMemberId:{%d}", unionMember.getUnionMemberBO().getClubMemberId());
        }
        ClubMember exeClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(opClubId, opPid);
        if (null == exeClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER ClubMemberId:{%d}", unionMember.getUnionMemberBO().getClubMemberId());
        }
        UnionDefine.UNION_SPORTS_POINT sportsPoint = UnionDefine.UNION_SPORTS_POINT.valueOf(type);
        if (UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_ADD.equals(sportsPoint)) {
            if (!doClubMember.getClubMemberBO().execSportsPointUpdate(unionId, -Math.abs(value), ItemFlow.UNION_BACKSTAGE_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, -Integer.MAX_VALUE)) {
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
            if (!exeClubMember.getClubMemberBO().execSportsPointUpdate(unionId, Math.abs(value), ItemFlow.UNION_BACKSTAGE_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, -Integer.MAX_VALUE)) {
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
        } else {
            if (!exeClubMember.getClubMemberBO().execSportsPointUpdate(unionId, -Math.abs(value), ItemFlow.UNION_BACKSTAGE_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, -Integer.MAX_VALUE)) {
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
            if (!doClubMember.getClubMemberBO().execSportsPointUpdate(unionId, Math.abs(value), ItemFlow.UNION_BACKSTAGE_SPORTS_POINT_CHANGE, RoomTypeEnum.UNION, -Integer.MAX_VALUE)) {
                return SData_Result.make(ErrorCode.NotEnough_SportsPoint, "NotEnough_SportsPoint");
            }
        }
        // 比赛分增加/减少
//        UnionDefine.UNION_EXEC_TYPE itemFlow = type <= 0 ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_ADD : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_MINUS;
        // 比赛分值修改
//        UnionDynamicBO.insertUnionBackstageSportsPoint(unionMember.getUnionMemberBO().getClubOwnerId(), clubId, unionId, 0, CommTime.nowSecond(), itemFlow.value(), String.valueOf(Math.abs(value)), String.valueOf(exeClubMember.getClubMemberBO().getSportsPoint()));
        return SData_Result.make(ErrorCode.Success, exeClubMember.getClubMemberBO().getSportsPoint());
    }

    /**
     * 检查赛事成员权利
     *
     * @param unionId  赛事id
     * @param clubId   亲友圈id
     * @param exePid   操作者id
     * @param opClubId 被操作者亲友圈id
     * @param opPid    被操作者Pid
     * @return
     */
    private SData_Result checkUnionRightsClub(long unionId, long clubId, long exePid, long opClubId, long opPid) {
        //获取当前操作者的亲友圈成员信息
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, exePid);
        if (null == execClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "execClubMember CLUB_NOT_EXIST_MEMBER_INFO");
        }
        // 获取当前操作者的赛事成员信息。
        UnionMember execUnionMember = UnionMgr.getInstance().getUnionMemberMgr().find(exePid, clubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (!execClubMember.isUnionMgr() && null == execUnionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER");
        }
        // 检查当前操作者是否有基本权限。
        if (!execClubMember.isUnionMgr() && !execUnionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
        }
        // 获取被操作者的赛事成员信息。
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(opPid, opClubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == unionMember) {
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(opClubId);
            if (null == club) {
                return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "checkUnionRightsClub CLUB_NOT_EXIST opClubId:{%d}", opClubId);
            }
            if (club.getClubListBO().getUnionId() <= 0L) {
                return SData_Result.make(ErrorCode.CLUB_NOT_JOIN_UNION, "CLUB_NOT_JOIN_UNION");
            } else {
                return SData_Result.make(ErrorCode.Success, ClubMgr.getInstance().getClubMemberMgr().isMinister(opClubId, opPid));
            }

        }
        UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (Objects.nonNull(union)) {
            unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
        }

        if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
            // 检查操作者和被操作者权限是否一致。
            if (!execClubMember.isUnionMgr() && unionMember.getUnionMemberBO().getPostType().value() >= execUnionMember.getUnionMemberBO().getPostType().value()) {
                // 被操作者的权力 >= 操作者的权力
                return SData_Result.make(ErrorCode.UNION_MEMBER_SAME_POST_TYPE, "UNION_MEMBER_SAME_RIGHTS");
            }
        } else if (UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType)) {
            // 检查操作者和被操作者权限是否一致。
            if (!execClubMember.isUnionMgr() && unionMember.getUnionMemberBO().getPostType().value() > execUnionMember.getUnionMemberBO().getPostType().value()) {
                // 被操作者的权力 >= 操作者的权力
                return SData_Result.make(ErrorCode.UNION_MEMBER_SAME_POST_TYPE, "UNION_MEMBER_SAME_RIGHTS");
            }
        }

        return SData_Result.make(ErrorCode.Success, true);
    }


    /**
     * 执行禁止指定玩家游戏
     *
     * @return
     */
    public SData_Result execUnionBanGame(CUnion_BanGameClubMember req, long exePid) {
        SData_Result result = this.checkUnionRightsClub(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        return ClubMgr.getInstance().getClubMemberMgr().execUnionClubMemberBan(req.getUnionId(), req.getOpPid(), req.getValue(), exePid, req.getOpClubId());
    }


    /**
     * 执行获取指定收益比例列表
     *
     * @return
     */
    public SData_Result execScorePercentList(CUnion_ScorePercentList req, long exePid) {
        SData_Result result = this.checkUnionRightsShareChange(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        UnionDefine.UNION_SHARE_TYPE shareType = UnionDefine.UNION_SHARE_TYPE.valueOf(req.getType());
        if (Objects.isNull(shareType)) {
            return SData_Result.make(ErrorCode.NotAllow, "execScorePercentList type:{%d}", req.getType());
        }
        // 设置积分比例修改
        return ((UnionMember) result.getData()).getUnionRoomConfigScorePercentItemList(req);
    }

    /**
     * 执行收益比例批量更新
     *
     * @return
     */
    public SData_Result execScorePercentBatchUpdate(CUnion_ScorePercentBatchUpdate req, long exePid) {
        SData_Result result = this.checkUnionRightsShareChange(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        UnionDefine.UNION_SHARE_TYPE shareType = UnionDefine.UNION_SHARE_TYPE.valueOf(req.getType());
        if (Objects.isNull(shareType)) {
            return SData_Result.make(ErrorCode.NotAllow, "execScorePercentBatchUpdate type:{%d}", req.getType());
        }
        if (CollectionUtils.isEmpty(req.getUnionScorePercentItemList())) {
            return SData_Result.make(ErrorCode.NotAllow, "not execScorePercentBatchUpdate");
        }
        return result;
    }

    /**
     * 邀请好友列表，只显示在线并未在游戏
     *
     * @param unionId 赛事Id
     * @param number  查询数
     * @param query   查询
     * @return
     */
    public SData_Result<?> getUnionMemberRoomInvitationItemList(long unionId, int number, int pageNum, String query) {
        if (unionId <= 0L || number <= 0) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        // 赛事亲友圈Id列表
        List<Long> clubIdList = this.getUnionToClubIdList(unionId);
        if (CollectionUtils.isEmpty(clubIdList)) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        // 查询pid
        final long qPid = TypeUtils.StringTypeLong(query);
        //TODO 如果玩家在线人多在考虑缓存本地
        Map<Long, SharePlayer> sharePlayers = SharePlayerMgr.getInstance().onlineSharePlayers();
        List<jsproto.c2s.cclass.Player.ShortPlayer> shortPlayerList = ClubMgr.getInstance().getClubMemberMgr().findClubIdAllClubMemberOnline(clubIdList).stream().map(k -> {
            //共享玩家
            if (Config.isShare()) {
                SharePlayer onlinePlayer = sharePlayers.get(k);
//                CommLogD.info("Invitation player [{}]",new Gson().toJson(onlinePlayer));
                if (Objects.nonNull(onlinePlayer) && onlinePlayer.getRoomInfo().getRoomId() <= 0L) {
                    if (!clubIdList.contains(onlinePlayer.getSignEnumClubID())) {
                        return null;
                    }
                    jsproto.c2s.cclass.Player.ShortPlayer ret = new jsproto.c2s.cclass.Player.ShortPlayer();
                    ret.setName(onlinePlayer.getPlayerBO().getName());
                    ret.setPid(onlinePlayer.getPlayerBO().getId());
                    ret.setIconUrl(onlinePlayer.getPlayerBO().getHeadImageUrl());
                    ret.setAccountID(onlinePlayer.getPlayerBO().getAccountID());
                    // 没有查询信息 或者
                    if (StringUtils.isEmpty(query) || (qPid == onlinePlayer.getPlayerBO().getId())) {
                        return ret;
                    }
                    if (onlinePlayer.getPlayerBO().getName().contains(query)) {
                        return ret;
                    }
                }
            } else {
                Player onlinePlayer = PlayerMgr.getInstance().getOnlinePlayerByPid(k);
                if (Objects.nonNull(onlinePlayer) && onlinePlayer.getRoomInfo().getRoomId() <= 0L) {
                    if (!clubIdList.contains(onlinePlayer.getSignEnumClubID())) {
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
            }
            return null;
        }).filter(k -> Objects.nonNull(k)).skip(Page.getPageNum(pageNum, number)).limit(number).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(shortPlayerList)) {
            return SData_Result.make(ErrorCode.Success, Collections.emptyList());
        }
        return SData_Result.make(ErrorCode.Success, shortPlayerList);

    }

    /**
     * 联盟设置亲友圈圈主的分成
     *
     * @param req
     * @param exePid
     * @return
     */

    public SData_Result promotionShareInfo(CUnion_ScorePercentUpdate req, long exePid) {
        SData_Result result = this.checkUnionRightsShare(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        //如果是盟主操作自己的话 是可以的
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 设置积分比例修改
        return ((UnionMember) result.getData()).setScoreDividedInto(req.getOpPid(), req.getOpClubId(), req.getValue(), exePid, req.getShareType());
    }


    /**
     * 加入判断
     * 同赛事不同亲友圈不能重复拉人，
     *
     * @param pid
     * @return
     */
    public boolean checkExistInUnionOtherClub(long clubID, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        if (Objects.isNull(club)) {
            return false;
        }
        if (club.getClubListBO().getUnionId() <= 0L) {
            //  说明亲友圈还没有加入赛事 这时候不存在重复拉人
            return false;
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union)) {
            // 说明赛事不存在
            return false;
        }
        if (UnionDefine.UNION_JOIN_CLUB_SAME_UNION.UNION_JOIN_NEED_AUDIT.equals(UnionDefine.UNION_JOIN_CLUB_SAME_UNION.valueOf(union.getUnionBO().getJoinClubSameUnion()))) {
            // 没有限制
            return false;
        }
        UnionMember unionMember;
        if (Config.isShare()) {
            unionMember = ShareUnionMemberMgr.getInstance().getAllOneClubUnionMember(clubID).values().stream().filter(k -> k.getUnionMemberBO().getClubId() == clubID && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny().orElse(null);
        } else {
            unionMember = this.getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getClubId() == clubID && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny().orElse(null);
        }
        //如果找不到赛事成员 说明亲友圈还没有加入赛事 这时候不存在重复拉人
        if (Objects.isNull(unionMember)) {
            return false;
        }
        //如果有赛事成员的话  那么就去赛事找他的所有成员 并判断成员亲友圈中是否存在这个玩家
        List<Long> clubIDList;
        if (Config.isShare()) {
            clubIDList = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionMember.getUnionMemberBO().getUnionId()).values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == unionMember.getUnionMemberBO().getUnionId()).map(m -> m.getClubId()).collect(Collectors.toList());
        } else {
            clubIDList = this.getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getUnionId() == unionMember.getUnionMemberBO().getUnionId()).map(m -> m.getClubId()).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(clubIDList)) {
            return false;
        }
        //如果联赛下存在一个亲友圈中有这个人
        if (clubIDList.stream().anyMatch(k -> Objects.nonNull(ClubMgr.getInstance().getClubMemberMgr().getClubMember(k.longValue(), pid)))) {
            return true;
        }
        return false;

    }

    /**
     * 修改联盟钻石提醒
     *
     * @param pid
     * @return
     */
    public SData_Result changeDiamondsAttention(CUnion_ChangeDiamondsAttention req, long pid) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, pid);
        if (null == execClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "execClubMember CLUB_NOT_EXIST_MEMBER_INFO");
        }
        // 获取被操作者的赛事成员信息。
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(pid, req.clubId, req.unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (!execClubMember.isUnionMgr() && Objects.isNull(unionMember)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER");
        }
        // 检查当前操作者是否有基本权限。
        if (!execClubMember.isUnionMgr() && !unionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
        }
        union.getUnionBO().saveUnionDiamondsAttentionAll(req.getUnionDiamondsAttentionAll());
        union.getUnionBO().saveUnionDiamondsAttentionMinister(req.getUnionDiamondsAttentionMinister());
        return SData_Result.make(ErrorCode.Success, req);
    }

    /**
     * 赛事经营统计
     *
     * @param req 赛事经营项
     * @return
     */
    /**
     * 赛事经营统计
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getUnionRoomConfigPrizePoolCount(CUnion_RoomConfigPrizePoolItem req, long pid) {
        if (isNotUnionManage(pid, req.getClubId(), req.getUnionId())) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE getUnionRoomConfigPrizePoolCount ");
        }
        String dateTime = CommTime.getYesterDayStringYMD(req.getType());
        String dateTimeSix = CommTime.getYesterDayStringYMDSix(req.getType());
        String key = String.format(DataConstants.UNION_ROOM_CONFIG_PRIZE_POOL_COUNT, req.getUnionId(), dateTime, req.getQuery());
        UnionRoomConfigPrizePoolItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionRoomConfigPrizePoolCacheConfiguration.class).get(key, UnionRoomConfigPrizePoolItem.class);
//        if (Objects.nonNull(unionRoomConfigPrizePoolItem)) {
//            // 获取缓存数据
//            return SData_Result.make(ErrorCode.Success, unionRoomConfigPrizePoolItem);
//        }
        unionRoomConfigPrizePoolItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), Restrictions.eq("date_time", dateTime), StringUtils.isEmpty(req.getQuery()) ? null : Restrictions.like("roomName", req.getQuery(), MatchMode.END)), UnionRoomConfigPrizePoolItem.class, UnionRoomConfigPrizePoolItem.getItemsNameCount());
        //记录当天的盟主洗牌收入和房费收入
        Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", req.getType());
        UnionSportPointInfo unionSportPointInfoIncome = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("unionId", req.getUnionId())), UnionSportPointInfo.class, UnionSportPointInfo.getItemsNameBySportsPoint());
        unionRoomConfigPrizePoolItem.setSportsPointIncome(unionSportPointInfoIncome.getSportPointIncome());
        //洗牌费计算
        double xiPaiIncome;
        if (checkTime(req.getType())) {
            UnionRoomConfigPrizePoolItem xiPaiSum = ContainerMgr.get().getComponent(XiPaiLogFlowService.class).findOneE(Restrictions.and(
                            Restrictions.eq("unionId", req.getUnionId()),
                            TimeConditionUtils.CLUBDayZeroClockS("dateTime", req.getType())),
                    UnionRoomConfigPrizePoolItem.class, UnionRoomConfigPrizePoolItem.getXiPaiCount());
            xiPaiIncome = xiPaiSum.getXiPaiIncome();
        } else {
            List<UnionDynamicBO> dynamicBOs = ContainerMgr.get().getComponent(UnionDynamicBOService.class)
                    .findAll(
                            Restrictions.and(
                                    Restrictions.eq("unionId", req.getUnionId()),
                                    Restrictions.eq("execType", UNION_ROOM_QIEPAI_INCOME.value()),
                                    TimeConditionUtils.CLUBDayZeroClockS("dateTime", req.getType()))
                    );
            xiPaiIncome = dynamicBOs.stream().mapToDouble(k -> Double.parseDouble(k.getValue())).sum();
        }
        unionRoomConfigPrizePoolItem.setXiPaiIncome(CommMath.FormatDouble(xiPaiIncome));
        //中至的数据计算
        String zhongZhikey = String.format(DataConstants.UNION_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI, req.getUnionId(), dateTime, req.getQuery());
        UnionCountByZhongZhiItem byZhongZhiItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(byZhongZhiItem)) {
            byZhongZhiItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), Restrictions.eq("date_time", dateTime)), UnionCountByZhongZhiItem.class, UnionCountByZhongZhiItem.getItemsNameCount());
            //今天的话
            ClubLevelRoomCountLogItem items = null;
            if (req.getType() == Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TODAY.value()) {
                items = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", dateTime), Restrictions.eq("unionId", req.getUnionId())), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());

            } else {
                items = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", dateTimeSix), Restrictions.eq("unionId", req.getUnionId())), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            }
            double allWinLose = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
            }
            byZhongZhiItem.setAllWinLose(allWinLose);
            byZhongZhiItem.setUnionAllMemberPointTotal(allWinLose - byZhongZhiItem.getRoomSportsPointConsume());
            Criteria zhongZhizeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", 0);
            ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneE(Restrictions.and(zhongZhizeroClockS, Restrictions.eq("unionId", req.getUnionId())), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsName());
            double promotionShareValue = 0D;
            if (Objects.nonNull(clubPromotionLevelItem)) {
                promotionShareValue = clubPromotionLevelItem.getPromotionShareValue();
            }
            byZhongZhiItem.setFinalAllMemberPointTotal(CommMath.addDouble(byZhongZhiItem.getUnionAllMemberPointTotal(), promotionShareValue));
            EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(zhongZhikey, unionRoomConfigPrizePoolItem);
        }
        unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(byZhongZhiItem.getUnionAllMemberPointTotal());
        unionRoomConfigPrizePoolItem.setFinalAllMemberPointTotal(byZhongZhiItem.getFinalAllMemberPointTotal());
        EhCacheFactory.getCacheApi(UnionRoomConfigPrizePoolCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return SData_Result.make(ErrorCode.Success, unionRoomConfigPrizePoolItem);
    }

    /**
     * 赛事经营统计
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getUnionRoomConfigPrizePoolCountByZhongZhi(CUnion_RoomConfigPrizePoolItem req, long pid) {
        if (isNotUnionManage(pid, req.getClubId(), req.getUnionId())) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE getUnionRoomConfigPrizePoolCount ");
        }
        String dateTime = CommTime.getYesterDayStringYMD(req.getType());
        String dateTimeSix = CommTime.getYesterDayStringYMDSix(req.getType());
        String key = String.format(DataConstants.UNION_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI, req.getUnionId(), dateTime, req.getQuery());
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            unionRoomConfigPrizePoolItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), Restrictions.eq("date_time", dateTimeSix)), UnionCountByZhongZhiItem.class, UnionCountByZhongZhiItem.getItemsNameCount());
            ClubLevelRoomCountLogItem items = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", dateTime), Restrictions.eq("unionId", req.getUnionId())), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            double allWinLose = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
                unionRoomConfigPrizePoolItem.setBigWinner(items.getWinner());
            }
            unionRoomConfigPrizePoolItem.setAllWinLose(allWinLose);
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(allWinLose - unionRoomConfigPrizePoolItem.getRoomSportsPointConsume());
            Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", 0);
            ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("unionId", req.getUnionId())), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsName());
            double promotionShareValue = 0D;
            if (Objects.nonNull(clubPromotionLevelItem)) {
                promotionShareValue = clubPromotionLevelItem.getPromotionShareValue();

            }
            unionRoomConfigPrizePoolItem.setFinalAllMemberPointTotal(CommMath.addDouble(unionRoomConfigPrizePoolItem.getUnionAllMemberPointTotal(), promotionShareValue));
        }
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return SData_Result.make(ErrorCode.Success, unionRoomConfigPrizePoolItem);
    }

    /**
     * 赛事经营统计
     * 赛事成员排行列表
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getUnionRoomConfigPrizePoolCountRankedByZhongZhi(CUnion_RoomConfigPrizePoolItem req, long pid) {
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(pid, req.getClubId(), req.getUnionId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (Objects.isNull(unionMember)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER getUnionRoomConfigPrizePoolCountRankedByZhongZhi ");
        }
        if (!unionMember.isCreate()) {
            return SData_Result.make(ErrorCode.UNION_NOT_CREATE, "UNION_NOT_CREATE getUnionRoomConfigPrizePoolCountRankedByZhongZhi ");
        }
        if (req.getType() == Club_define.CLUB_COUNT_RECORD.CLUB_COUNT_RECORD_Seven.value()) {
            //七天汇总
            return getUnionRoomConfigPrizePoolCountRankedByZhongZhiSevenDay(req);
        } else {
            //某天计算
            return getUnionRoomConfigPrizePoolCountRankedByZhongZhiOneDay(req);
        }
    }

    /**
     * 赛事经营统计
     * 最近七天计算
     * 赛事成员排行列表
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getUnionRoomConfigPrizePoolCountRankedByZhongZhiSevenDay(CUnion_RoomConfigPrizePoolItem req) {

        String key = String.format(DataConstants.UNION_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_TYPE_SEVENDAY, req.getUnionId(), req.getQuery(), Long.valueOf(req.getType()));
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            req.setType(0);
            UnionCountByZhongZhiItem resultToday = (UnionCountByZhongZhiItem) getUnionRoomConfigPrizePoolCountRankedByZhongZhiOneDay(req).getData();
            UnionCountByZhongZhiItem resultSix = (UnionCountByZhongZhiItem) getUnionRoomConfigPrizePoolCountRankedByZhongZhiRecentDay(req).getData();
            unionRoomConfigPrizePoolItem = new UnionCountByZhongZhiItem();
            unionRoomConfigPrizePoolItem.setPrizePool(CommMath.addDouble(resultToday.getPrizePool(), resultSix.getPrizePool()));
            unionRoomConfigPrizePoolItem.setRoomSize(resultToday.getRoomSize() + resultSix.getRoomSize());
            unionRoomConfigPrizePoolItem.setSetCount(resultToday.getSetCount() + resultSix.getSetCount());
            unionRoomConfigPrizePoolItem.setConsumeValue(resultToday.getConsumeValue() + resultSix.getConsumeValue());
            unionRoomConfigPrizePoolItem.setBigWinner(resultToday.getBigWinner() + resultSix.getBigWinner());
            unionRoomConfigPrizePoolItem.setFinalAllMemberPointTotal(CommMath.addDouble(resultToday.getFinalAllMemberPointTotal(), resultSix.getFinalAllMemberPointTotal()));
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(CommMath.addDouble(resultToday.getUnionAllMemberPointTotal(), resultSix.getUnionAllMemberPointTotal()));
            unionRoomConfigPrizePoolItem.setAllWinLose(CommMath.addDouble(resultToday.getAllWinLose(), resultSix.getAllWinLose()));
            unionRoomConfigPrizePoolItem.setRoomSportsPointConsume(CommMath.addDouble(resultToday.getRoomSportsPointConsume(), resultSix.getRoomSportsPointConsume()));
            unionRoomConfigPrizePoolItem.setPromotionShareValue(CommMath.addDouble(resultToday.getPromotionShareValue(), resultSix.getPromotionShareValue()));
            unionRoomConfigPrizePoolItem.setZhongZhiTotalPoint(CommMath.addDouble(resultToday.getZhongZhiTotalPoint(), resultSix.getZhongZhiTotalPoint()));
        }
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return SData_Result.make(ErrorCode.Success, unionRoomConfigPrizePoolItem);
    }

    /**
     * 赛事经营统计
     * 某一天计算
     * 赛事成员排行列表
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getUnionRoomConfigPrizePoolCountRankedByZhongZhiRecentDay(CUnion_RoomConfigPrizePoolItem req) {
        String key = String.format(DataConstants.UNION_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_TYPE_RECENT, req.getUnionId(), req.getQuery(), Long.valueOf(req.getType()));
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            unionRoomConfigPrizePoolItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), Restrictions.lt("date_time", CommTime.getYesterDayStringYMD(0)),
                    Restrictions.ge("date_time", CommTime.getYesterDayStringYMD(6))), UnionCountByZhongZhiItem.class, UnionCountByZhongZhiItem.getItemsNameCount());
            ClubLevelRoomCountLogItem items = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).
                    findOneE(Restrictions.and(Restrictions.lt("date_time", CommTime.getYesterDayStringYMD(0)),
                            Restrictions.ge("date_time", CommTime.getYesterDayStringYMD(6)), Restrictions.eq("unionId", req.getUnionId())), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).
                    findOneE(Restrictions.and(Restrictions.lt("date_time", CommTime.getYesterDayStringYMD(0)),
                            Restrictions.ge("date_time", CommTime.getYesterDayStringYMD(6)), Restrictions.eq("unionId", req.getUnionId())), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsName());
            double allWinLose = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
                unionRoomConfigPrizePoolItem.setBigWinner(items.getWinner());
            }
            unionRoomConfigPrizePoolItem.setAllWinLose(allWinLose);
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(allWinLose - unionRoomConfigPrizePoolItem.getRoomSportsPointConsume());
            double promotionShareValue = 0D;
            if (Objects.nonNull(clubPromotionLevelItem)) {
                promotionShareValue = clubPromotionLevelItem.getPromotionShareValue();
            }
            unionRoomConfigPrizePoolItem.setFinalAllMemberPointTotal(CommMath.addDouble(unionRoomConfigPrizePoolItem.getUnionAllMemberPointTotal(), promotionShareValue));
        }
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return SData_Result.make(ErrorCode.Success, unionRoomConfigPrizePoolItem);
    }

    /**
     * 赛事经营统计
     * 某一天计算
     * 赛事成员排行列表
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getUnionRoomConfigPrizePoolCountRankedByZhongZhiOneDay(CUnion_RoomConfigPrizePoolItem req) {
        String dateTime = CommTime.getYesterDayStringYMD(req.getType());
        String key = String.format(DataConstants.UNION_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_TYPE, req.getUnionId(), dateTime, req.getQuery(), Long.valueOf(req.getType()));
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            unionRoomConfigPrizePoolItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), Restrictions.eq("date_time", dateTime)), UnionCountByZhongZhiItem.class, UnionCountByZhongZhiItem.getItemsNameCount());
            ClubLevelRoomCountLogItem items;
            ClubPromotionLevelItem clubPromotionLevelItem;
            Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", req.getType());
            if (Club_define.CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TODAY.value() == req.getType()) {
                items = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", CommTime.getYesterDayStringYMD(req.getType())), Restrictions.eq("unionId", req.getUnionId())), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
                clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("unionId", req.getUnionId())), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsName());
            } else {
                items = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", CommTime.getYesterDayStringYMD(req.getType())), Restrictions.eq("unionId", req.getUnionId())), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
                clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(Restrictions.and(zeroClockS, Restrictions.eq("unionId", req.getUnionId())), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsName());
            }
            double allWinLose = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
                unionRoomConfigPrizePoolItem.setBigWinner(items.getWinner());
            }
            unionRoomConfigPrizePoolItem.setAllWinLose(allWinLose);
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(allWinLose - unionRoomConfigPrizePoolItem.getRoomSportsPointConsume());
            double promotionShareValue = 0D;
            if (Objects.nonNull(clubPromotionLevelItem)) {
                promotionShareValue = clubPromotionLevelItem.getPromotionShareValue();
            }
            unionRoomConfigPrizePoolItem.setFinalAllMemberPointTotal(CommMath.addDouble(unionRoomConfigPrizePoolItem.getUnionAllMemberPointTotal(), promotionShareValue));
        }
        CommLogD.error(unionRoomConfigPrizePoolItem.toString());
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return SData_Result.make(ErrorCode.Success, unionRoomConfigPrizePoolItem);
    }

    /**
     * 检查时间是否在
     *
     * @param type
     * @return
     */
    private boolean checkTime(int type) {
        //开始执行的毫秒数
        long beginSecond = 1610726400;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long nowZeroSecond = calendar.getTimeInMillis() / 1000;
        switch (Club_define.CLUB_PROMOTION_TIME_TYPE.valueOf(type)) {
            case RECORD_GET_TYPE_TODAY:
                // 今天
                return nowZeroSecond >= beginSecond;
            case RECORD_GET_TYPE_YESTERDAY:
                // 昨天
                return nowZeroSecond - 86400 >= beginSecond;
            case RECORD_GET_TYPE_TWO:
                return nowZeroSecond - 86400 * 2 >= beginSecond;
            case RECORD_GET_TYPE_THREE:
                // 昨天
                return nowZeroSecond - 86400 * 2 >= beginSecond;
            case RECORD_GET_TYPE_FOUR:
                return nowZeroSecond - 86400 * 2 >= beginSecond;
            case RECORD_GET_TYPE_FIVE:
                return nowZeroSecond - 86400 * 2 >= beginSecond;
            case RECORD_GET_TYPE_SIX:
                return nowZeroSecond - 86400 * 2 >= beginSecond;
            default:
                return false;
        }
    }

    /**
     * 赛事经营列表
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getUnionRoomConfigPrizePoolList(CUnion_RoomConfigPrizePoolItem req, long pid) {
        if (isNotUnionManage(pid, req.getClubId(), req.getUnionId())) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE getUnionRoomConfigPrizePoolList ");
        }
        List<UnionRoomConfigPrizePoolItem> unionRoomConfigPrizePoolItemList = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogFlowService.class).findAllE(Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), Restrictions.eq("date_time", CommTime.getYesterDayStringYMD(req.getType())), StringUtils.isEmpty(req.getQuery()) ? null : Restrictions.like("roomName", req.getQuery(), MatchMode.END)).groupBy("configId").desc("roomId").setPageNum(Page.getPageNum(req.getPageNum(), Page.PAGE_SIZE_15)).setPageSize(Page.PAGE_SIZE_15), UnionRoomConfigPrizePoolItem.class, UnionRoomConfigPrizePoolItem.getItemsName());
        if (CollectionUtils.isNotEmpty(unionRoomConfigPrizePoolItemList)) {
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
            if (Objects.nonNull(union)) {
                unionRoomConfigPrizePoolItemList = unionRoomConfigPrizePoolItemList.stream().map(k -> {
                    UnionCreateGameSet createGameSet = union.getRoomConfigBOMap().get(k.getConfigId());
                    if (Objects.nonNull(createGameSet) && createGameSet.isExistUnionRoomConfig()) {
                        k.setTagId(createGameSet.getbRoomConfigure().getTagId());
                    }
                    return k;
                }).collect(Collectors.toList());
            }
        }
        return SData_Result.make(ErrorCode.Success, CollectionUtils.isEmpty(unionRoomConfigPrizePoolItemList) ? Collections.emptyList() : unionRoomConfigPrizePoolItemList);
    }


    /**
     * 检查赛事成员权利
     *
     * @param unionId  赛事id
     * @param clubId   亲友圈id
     * @param exePid   操作者id
     * @param opClubId 被操作者亲友圈id
     * @param opPid    被操作者Pid
     * @return
     */
    private SData_Result checkUnionRightsClubMember(long unionId, long clubId, long exePid, long opClubId, long opPid) {
        //获取当前操作者的亲友圈成员信息
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, exePid);
        if (null == execClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "execClubMember CLUB_NOT_EXIST_MEMBER_INFO");
        }

        if (execClubMember.isMinister() && clubId == opClubId) {
            ClubMember opClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(opClubId, opPid);
            if (null == opClubMember) {
                return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "opClubMember CLUB_NOT_EXIST_MEMBER_INFO");
            }
            if (opClubMember.isClubCreate() || (!execClubMember.isClubCreate() && opClubMember.isMinister())) {
                return SData_Result.make(ErrorCode.UNION_MEMBER_SAME_POST_TYPE, "UNION_MEMBER_SAME_RIGHTS");
            }
            return SData_Result.make(ErrorCode.Success, true);
        }


        // 获取当前操作者的赛事成员信息。
        UnionMember execUnionMember = UnionMgr.getInstance().getUnionMemberMgr().find(exePid, clubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (!execClubMember.isUnionMgr() && null == execUnionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER");
        }
        // 检查当前操作者是否有基本权限。
        if (!execClubMember.isUnionMgr() && !execUnionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
        }
        // 获取被操作者的赛事成员信息。
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(opPid, opClubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == unionMember) {
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(opClubId);
            if (null == club) {
                return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "checkUnionRightsClub CLUB_NOT_EXIST opClubId:{%d}", opClubId);
            }
            if (club.getClubListBO().getUnionId() <= 0L) {
                return SData_Result.make(ErrorCode.CLUB_NOT_JOIN_UNION, "CLUB_NOT_JOIN_UNION");
            } else {
                return SData_Result.make(ErrorCode.Success, ClubMgr.getInstance().getClubMemberMgr().isMinister(opClubId, opPid));
            }

        }
        // 检查操作者和被操作者权限是否一致。
        if (!execClubMember.isUnionMgr() && unionMember.getUnionMemberBO().getPostType().value() >= execUnionMember.getUnionMemberBO().getPostType().value()) {
            // 被操作者的权力 >= 操作者的权力
            return SData_Result.make(ErrorCode.UNION_MEMBER_SAME_POST_TYPE, "UNION_MEMBER_SAME_RIGHTS");
        }
        return SData_Result.make(ErrorCode.Success, true);
    }


    /**
     * 赛事禁止玩法列表
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getUnionBanRoomConfigList(CUnion_BanRoomConfigOp req, long exePid) {
        SData_Result result = this.checkUnionRightsClubMember(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST getUnionBanRoomConfigList");
        }
        List<SUnion_BanRoomConfigItem> unionBanRoomConfigBOList = ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).findAllE(Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), Restrictions.eq("clubId", req.getOpClubId()), Restrictions.eq("pid", req.getOpPid())), SUnion_BanRoomConfigItem.class, SUnion_BanRoomConfigItem.getItemsName());
        if (CollectionUtils.isEmpty(unionBanRoomConfigBOList)) {
            unionBanRoomConfigBOList = Collections.emptyList();
        }
        // 转json
        final Gson gson = new Gson();
        Map<Long, SUnion_BanRoomConfigItem> banRoomConfigItemMap = unionBanRoomConfigBOList.stream().collect(Collectors.toMap(k -> k.getConfigId(), k -> k, (k1, k2) -> k1));
        return SData_Result.make(ErrorCode.Success, new UnionBanRoomConfigItem(
                union.getRoomConfigBOMap().entrySet().stream().map(k -> {
                    if (Objects.isNull(k) || !k.getValue().isExistUnionRoomConfig()) {
                        return null;
                    }
                    // 是否存在禁止全部
                    SUnion_BanRoomConfigItem banRoomConfigItem = banRoomConfigItemMap.containsKey(0L) ? new SUnion_BanRoomConfigItem() : banRoomConfigItemMap.get(k.getKey());
                    banRoomConfigItem = Objects.isNull(banRoomConfigItem) ? new SUnion_BanRoomConfigItem(0) : banRoomConfigItem;
                    banRoomConfigItem.setConfigId(k.getKey());
                    banRoomConfigItem.setGameId(k.getValue().getGameType().getId());
                    banRoomConfigItem.setDataJsonCfg(gson.toJson(k.getValue().getbRoomConfigure().getBaseCreateRoomT()));
                    banRoomConfigItem.setRoomName(k.getValue().getbRoomConfigure().getBaseCreateRoom().getRoomName());
                    return banRoomConfigItem;
                }).filter(k -> Objects.nonNull(k)).collect(Collectors.toList()), banRoomConfigItemMap.containsKey(0L) ? 1 : 0));
    }

    /**
     * 检查竞技点预警值
     *
     * @return
     */

    public SData_Result checkSportsPointWarning(Union union, long clubId) {
        UnionMember unionMember = null;
        if (Config.isShare()) {
            unionMember = ShareUnionMemberMgr.getInstance().find(clubId, union.getUnionBO().getId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        } else {
            unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(clubId, union.getUnionBO().getId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        }
        if (Objects.isNull(unionMember)) {
            //找不到的话 就不进行检验
            return SData_Result.make(ErrorCode.Success);
        }
        Double totalSportPoint = getPromotionAllSportsPoint(union.getUnionBO().getId(), clubId);
        if (unionMember.getUnionMemberBO().getWarnStatus() == 1 && totalSportPoint < unionMember.getUnionMemberBO().getSportsPointWarning()) {
            return SData_Result.make(ErrorCode.CLUB_SPORT_POINT_WARN, "您所在的亲友圈比赛分低于预警值，无法加入比赛，请联系管理");
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 检查联盟生存积分满足
     *
     * @return
     */

    public SData_Result checkAlivePoint(Union union, long clubId) {
        UnionMember unionMember = null;
        if (Config.isShare()) {
            unionMember = ShareUnionMemberMgr.getInstance().find(clubId, union.getUnionBO().getId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        } else {
            unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(clubId, union.getUnionBO().getId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        }
        if (Objects.isNull(unionMember)) {
            //找不到的话 就不进行检验
            return SData_Result.make(ErrorCode.Success);
        }
        Double totalSportPoint = zhongZhiFinalTotalPoint(union.getUnionBO().getId(), clubId);
        if (unionMember.getUnionMemberBO().getAlivePointStatus() == 1 && totalSportPoint < unionMember.getUnionMemberBO().getAlivePoint()) {
            return SData_Result.make(ErrorCode.CLUB_ALIVE_POINT_WARN, " 您所在的亲友圈生存积分过低，无法加入比赛，请联系管理");
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 获取某个亲友圈的竞技点之和
     *
     * @return
     */
    private Double getPromotionAllSportsPoint(long unionId, long clubId) {
        UnionSportsPointAllValueItem item = EhCacheFactory.getCacheApi(WarningSportsCacheConfiguration.class).get(String.format(DataConstants.SPORTS_POINT_CLUB_ALL_WARNING, unionId, clubId), UnionSportsPointAllValueItem.class);
        // 检查缓存是否为空
        if (Objects.isNull(item)) {
            Map<String, Object> unionMemberStatisticsMap = ClubMgr.getInstance().getClubMemberMgr().unionMemberStatisticsMap(clubId);
            Double totalSportPoint = (Double) unionMemberStatisticsMap.get("SUM");
            EhCacheFactory.getCacheApi(WarningSportsCacheConfiguration.class).put(String.format(DataConstants.SPORTS_POINT_CLUB_ALL_WARNING, unionId, clubId), new UnionSportsPointAllValueItem(totalSportPoint));
            return totalSportPoint;
        }
        return item.getSportsPointAll();

    }

    /***
     * 获取某个亲友圈的最终积分
     * @param unionId
     * @param clubId
     * @return
     */
    private Double zhongZhiFinalTotalPoint(long unionId, long clubId) {
        String key = String.format(DataConstants.CLUB_TODYA_COUNT_ZHONGZHI, clubId);
        UnionCountByZhongZhiItem unionRoomConfigPrizePoolItem = EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).get(key, UnionCountByZhongZhiItem.class);
        if (Objects.isNull(unionRoomConfigPrizePoolItem)) {
            unionRoomConfigPrizePoolItem = new UnionCountByZhongZhiItem();
            ClubLevelRoomCountLogItem items = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", String.valueOf(CommTime.getCycleNowTime6YMD())), Restrictions.eq("clubId", clubId)), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsNameByAllUnionWinLose());
            double allWinLose = 0D;
            if (Objects.nonNull(items)) {
                allWinLose = items.getSportsPointConsume();
            }
            unionRoomConfigPrizePoolItem.setAllWinLose(allWinLose);
            unionRoomConfigPrizePoolItem.setUnionAllMemberPointTotal(allWinLose - items.getRoomSportsPointConsume());
            Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", 0);
            ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("date_time", String.valueOf(CommTime.getCycleNowTime6YMD())), Restrictions.eq("clubId", clubId)), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsName());
            unionRoomConfigPrizePoolItem.setFinalAllMemberPointTotal(CommMath.addDouble(unionRoomConfigPrizePoolItem.getUnionAllMemberPointTotal(), clubPromotionLevelItem.getPromotionShareValue()));
        }
        EhCacheFactory.getCacheApi(UnionZhongZhiCountInfoCacheConfiguration.class).put(key, unionRoomConfigPrizePoolItem);
        return unionRoomConfigPrizePoolItem.getFinalAllMemberPointTotal();

    }

    /**
     * 操作赛事禁止玩法列表
     *
     * @param req 赛事经营项
     * @return
     */
    public SData_Result getUnionBanRoomConfigOp(CUnion_BanRoomConfigOp req, long exePid) {
        SData_Result result = this.checkUnionRightsClubMember(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST getUnionBanRoomConfigList");
        }
        if (req.getIsAll() == 1) {
            ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).saveIgnore(new UnionBanRoomConfigBO(req.getUnionId(), req.getOpClubId(), req.getOpPid(), 0L, CommTime.nowSecond()));
        } else {
            // 找到选择中的玩法id
            List<Long> configIdList = union.getRoomConfigBOMap().entrySet().stream().filter(k -> req.getConfigIdList().contains(k.getKey())).map(k -> k.getKey()).collect(Collectors.toList());
            // 将未选中的玩法删除
            ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).delete(Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), Restrictions.eq("clubId", req.getOpClubId()), Restrictions.eq("pid", req.getOpPid()), CollectionUtils.isEmpty(configIdList) ? null : Restrictions.notin("configId", configIdList)));
            if (CollectionUtils.isNotEmpty(configIdList)) {
                // 插入新选中的玩法
                List<UnionBanRoomConfigBO> unionBanRoomConfigBOList = configIdList.stream().map(k -> new UnionBanRoomConfigBO(req.getUnionId(), req.getOpClubId(), req.getOpPid(), k, CommTime.nowSecond())).collect(Collectors.toList());
                ContainerMgr.get().getComponent(UnionBanRoomConfigBOService.class).execBatchDb(unionBanRoomConfigBOList);
            }
        }
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 执行获取指定收益比例列表
     *
     * @return
     */
    public SData_Result execUnionClubReportForm(CUnion_ScorePercentList req, long exePid) {
        SData_Result result = this.checkUnionRightsShareChange(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        //之前的八天总和
        List<UnionClubReportFormItem> unionClubReportFormItemList = new ArrayList<>();
        //今天的数据
        UnionClubReportFormItem unionClubReportFormItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneE(Restrictions.and(Restrictions.eq("clubId", req.getOpClubId()), Restrictions.eq("date_time", CommTime.getYesterDayStringYMD(0))), UnionClubReportFormItem.class, UnionClubReportFormItem.getItemsNameClubLevelRoomLog());
        //查询的结果肯定有  判断日期是不是为空来进行判断有没有数据
        if (Objects.nonNull(unionClubReportFormItem.getDateTime())) {
            List<ClubRoomSizeItem> roomSizeList = ((ClubLevelRoomLogBeforeDayFlowService) ContainerMgr.get().getComponent(ClubLevelRoomLogBeforeDayFlowService.class)).getRoomSizeList(Restrictions.eq("clubId", req.getOpClubId()), CommTime.getYesterDayStringYMD(0), ClubRoomSizeItem.class, null);
            //房间的计算
            if (CollectionUtils.isNotEmpty(roomSizeList)) {
                ClubRoomSizeItem roomSizeItem = roomSizeList.stream().filter(k -> k.getClubId() == req.getOpClubId()).findFirst().orElse(null);
                if (Objects.nonNull(roomSizeItem)) {
                    unionClubReportFormItem.setRoomSize(roomSizeItem.getRoomSize());
                }
            }
            ClubMember clubCreateMember = ClubMgr.getInstance().getClubMemberMgr().findCreate(req.getOpClubId());
            unionClubReportFormItem.setScorePoint(unionClubReportFormItem.getActualEntryFee());
            unionClubReportFormItem.setPersonalSportsPoint(clubCreateMember.getTotalSportsPoint());
            unionClubReportFormItem.setSumSportsPoint(ClubMgr.getInstance().getClubMemberMgr().sumTotalSportsPoint(req.getOpClubId()));
            unionClubReportFormItem.setZhongZhiTotalPoint(CommMath.subDouble(unionClubReportFormItem.getSportsPointConsume(), unionClubReportFormItem.getActualEntryFee()));
            unionClubReportFormItemList.add(unionClubReportFormItem);
        }
        List<UnionClubReportFormItem> unionClubReportFormItemListToDay = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findAllE(Restrictions.and(Restrictions.eq("clubId", req.getOpClubId()), Restrictions.le("date_time", CommTime.getYesterDayStringYMD(1))).groupBy("dateTime").descFormat("dateTime").setLimit(8), UnionClubReportFormItem.class, UnionClubReportFormItem.getItemsName());
        //计算当天的总和
        //todo  特殊处理 活跃度由之前的总和改为当天的总和
        for (int i = 0; i < unionClubReportFormItemListToDay.size(); i++) {
            UnionClubReportFormItem con = unionClubReportFormItemListToDay.get(i).deepClone();
            con.setZhongZhiTotalPoint(CommMath.subDouble(con.getSportsPointConsume(), con.getActualEntryFee()));
            int j = i + 1;
            if (j >= unionClubReportFormItemListToDay.size()) {
                if (unionClubReportFormItemListToDay.size() <= 8) {
                    unionClubReportFormItemList.add(con);
                }
                break;
            }
            UnionClubReportFormItem next = unionClubReportFormItemListToDay.get(j);
            con.setScorePoint(CommMath.FormatDouble(CommMath.subDouble(con.getScorePoint(), next.getScorePoint())));
            unionClubReportFormItemList.add(con);
            if (unionClubReportFormItemList.size() >= 7) {
                break;
            }
        }
        /**
         * 中至数据计算
         */
        for (UnionClubReportFormItem con : unionClubReportFormItemList) {
            con.setZhongZhiTotalPoint(CommMath.subDouble(con.getSportsPointConsume(), con.getActualEntryFee()));
            //最终积分(总积分+活跃积分总和)
            con.setZhongZhiFinalTotalPoint(CommMath.addDouble(con.getZhongZhiTotalPoint(), con.getPromotionShareValue()));
        }
        return SData_Result.make(ErrorCode.Success, CollectionUtils.isEmpty(unionClubReportFormItemList) ? Collections.emptyList() : unionClubReportFormItemList);
    }

    /**
     * 执行获取指定收益比例列表
     *
     * @return
     */
    public SData_Result execUnionClubReportFormZhongZhi(CUnion_ScorePercentList req, long exePid) {
        SData_Result result = this.checkUnionRightsShareChange(req.getUnionId(), req.getClubId(), exePid, req.getOpClubId(), req.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        //之前的八天总和
        List<UnionClubReportFormItem> unionClubReportFormItemList = new ArrayList<>();
        //今天的数据
        UnionClubReportFormItem unionClubReportFormItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("clubId", req.getOpClubId()), Restrictions.eq("date_time", String.valueOf(CommTime.getCycleNowTime6YMD()))), UnionClubReportFormItem.class, UnionClubReportFormItem.getItemsNameClubLevelRoomLog());
        //查询的结果肯定有  判断日期是不是为空来进行判断有没有数据
        if (Objects.nonNull(unionClubReportFormItem.getDateTime())) {
            List<ClubRoomSizeItem> roomSizeList = ((ClubLevelRoomLogZhongZhiBeforeDayFlowService) ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiBeforeDayFlowService.class)).getRoomSizeList(Restrictions.eq("clubId", req.getOpClubId()), CommTime.getYesterDay6ByCount(0), ClubRoomSizeItem.class, null);
            //房间的计算
            if (CollectionUtils.isNotEmpty(roomSizeList)) {
                ClubRoomSizeItem roomSizeItem = roomSizeList.stream().filter(k -> k.getClubId() == req.getOpClubId()).findFirst().orElse(null);
                if (Objects.nonNull(roomSizeItem)) {
                    unionClubReportFormItem.setRoomSize(roomSizeItem.getRoomSize());
                }
            }
            ClubMember clubCreateMember = ClubMgr.getInstance().getClubMemberMgr().findCreate(req.getOpClubId());
            unionClubReportFormItem.setScorePoint(unionClubReportFormItem.getActualEntryFee());
            unionClubReportFormItem.setPersonalSportsPoint(clubCreateMember.getTotalSportsPoint());
            unionClubReportFormItem.setSumSportsPoint(ClubMgr.getInstance().getClubMemberMgr().sumTotalSportsPoint(req.getOpClubId()));
            unionClubReportFormItem.setZhongZhiTotalPoint(CommMath.subDouble(unionClubReportFormItem.getSportsPointConsume(), unionClubReportFormItem.getActualEntryFee()));
            unionClubReportFormItemList.add(unionClubReportFormItem);
        }
        List<UnionClubReportFormItem> unionClubReportFormItemListToDay = ContainerMgr.get().getComponent(ClubLevelRoomCountLogZhongZhiFlowService.class).findAllE(Restrictions.and(Restrictions.eq("clubId", req.getOpClubId()), Restrictions.le("date_time", CommTime.getYesterDay6ByCount(1))).groupBy("dateTime").descFormat("dateTime").setLimit(8), UnionClubReportFormItem.class, UnionClubReportFormItem.getItemsName());
        //计算当天的总和
        //todo  特殊处理 活跃度由之前的总和改为当天的总和
        for (int i = 0; i < unionClubReportFormItemListToDay.size(); i++) {
            UnionClubReportFormItem con = unionClubReportFormItemListToDay.get(i).deepClone();
            con.setZhongZhiTotalPoint(CommMath.subDouble(con.getSportsPointConsume(), con.getActualEntryFee()));
            int j = i + 1;
            if (j >= unionClubReportFormItemListToDay.size()) {
                if (unionClubReportFormItemListToDay.size() <= 8) {
                    unionClubReportFormItemList.add(con);
                }
                break;
            }
            UnionClubReportFormItem next = unionClubReportFormItemListToDay.get(j);
            con.setScorePoint(CommMath.FormatDouble(CommMath.subDouble(con.getScorePoint(), next.getScorePoint())));
            unionClubReportFormItemList.add(con);
            if (unionClubReportFormItemList.size() >= 7) {
                break;
            }
        }
        /**
         * 中至数据计算
         */
        for (UnionClubReportFormItem con : unionClubReportFormItemList) {
            con.setDateTime(CommTime.getSecToYMDStr(Integer.parseInt(con.getDateTime())));
            con.setZhongZhiTotalPoint(CommMath.subDouble(con.getSportsPointConsume(), con.getActualEntryFee()));
            //最终积分(总积分+活跃积分总和)
            con.setZhongZhiFinalTotalPoint(CommMath.addDouble(con.getZhongZhiTotalPoint(), con.getPromotionShareValue()));
        }
        return SData_Result.make(ErrorCode.Success, CollectionUtils.isEmpty(unionClubReportFormItemList) ? Collections.emptyList() : unionClubReportFormItemList);
    }

    /**
     * 推送消息到MQ
     *
     * @param topic
     * @param unionId
     * @param pid
     * @param unionGameCfgId
     * @param baseSendMsg
     */
    private void mqNotifyMessage(String topic, Long unionId, Long pid, Long unionGameCfgId, BaseSendMsg baseSendMsg) {
        MqUnionMemberNotifyBo mqUnionMemberNotifyBo = new MqUnionMemberNotifyBo();
        mqUnionMemberNotifyBo.setUnionId(unionId);
        mqUnionMemberNotifyBo.setPid(pid);
        mqUnionMemberNotifyBo.setUnionGameCfgId(unionGameCfgId);
        mqUnionMemberNotifyBo.setBaseSendMsg(baseSendMsg);
        mqUnionMemberNotifyBo.setBaseSendMsgClassType(baseSendMsg.getClass().getName());
        MqProducerMgr.get().send(topic, mqUnionMemberNotifyBo);
    }

    /**
     * 修改推广员预警值
     *
     * @param sportsPointWarningChange 推广员参数
     * @param pid                      玩家Pid
     * @return
     */
    public SData_Result changeSportsPointWarning(CUnion_SportsPointWarningChange sportsPointWarningChange, long pid) {
        SData_Result result = checkUnionRightsSprotWarn(sportsPointWarningChange.getUnionId(), sportsPointWarningChange.getClubId(), pid, sportsPointWarningChange.getOpClubId(), sportsPointWarningChange.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        UnionMember unionMember = (UnionMember) result.getData();
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(unionMember.getClubId());
        double preValue = unionMember.getUnionMemberBO().getSportsPointWarning();
        if (sportsPointWarningChange.getWarnStatus() == UnionDefine.UNION_WARN_STATUS.CLOSE.ordinal()) {
            unionMember.getUnionMemberBO().saveWarnStatus(0);
            UnionDynamicBO.insertSportsPointLogClub(0, unionMember.getClubId(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_CLUB_EXEC_SPORTS_WARNING_CLOSE.value(),
                    sportsPointWarningChange.getUnionId(), String.valueOf(preValue), String.valueOf(sportsPointWarningChange.getValue()));
        } else {
            unionMember.getUnionMemberBO().saveWarnStatus(1);
            unionMember.getUnionMemberBO().saveSportsPointWarning(sportsPointWarningChange.getValue());
            UnionDynamicBO.insertSportsPointLogClub(0, unionMember.getClubId(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_CLUB_EXEC_SPORTS_WARNING_CHANGE.value(),
                    sportsPointWarningChange.getUnionId(), String.valueOf(preValue), String.valueOf(sportsPointWarningChange.getValue()));
        }
        return SData_Result.make(ErrorCode.Success, new UnionSportsPointWarningItem(sportsPointWarningChange.getClubId(), club.getClubListBO().getClubsign(), club.getClubListBO().getName(),
                unionMember.getUnionMemberBO().getWarnStatus(), unionMember.getUnionMemberBO().getSportsPointWarning()));
    }

    /**
     * 修改联盟玩家生存积分状态
     *
     * @param alivePointChange 推广员参数
     * @param pid              玩家Pid
     * @return
     */
    public SData_Result changeAlivePointStatus(CUnion_AlivePointChange alivePointChange, long pid) {
        SData_Result result = checkUnionRightsSprotWarn(alivePointChange.getUnionId(), alivePointChange.getClubId(), pid, alivePointChange.getOpClubId(), alivePointChange.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        UnionMember unionMember = (UnionMember) result.getData();
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(unionMember.getClubId());
        double preValue = unionMember.getUnionMemberBO().getAlivePoint();
        if (alivePointChange.getAlivePointStatus() == UnionDefine.UNION_WARN_STATUS.CLOSE.ordinal()) {

            unionMember.getUnionMemberBO().saveAlivePointStatus(0);
            UnionDynamicBO.insertSportsPointLogClub(0, unionMember.getClubId(), pid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_CLOSE.value(),
                    alivePointChange.getUnionId(), String.valueOf(preValue), String.valueOf(alivePointChange.getValue()));
        } else {
            UnionDefine.UNION_EXEC_TYPE type = UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_CHANGE;
            if (unionMember.getUnionMemberBO().getAlivePointStatus() == UnionDefine.UNION_WARN_STATUS.CLOSE.ordinal()) {
                type = UnionDefine.UNION_EXEC_TYPE.UNION_ALIVE_SPORTS_OPEN;
            }
            unionMember.getUnionMemberBO().saveAlivePointStatus(1);
            unionMember.getUnionMemberBO().saveAlivePoint(alivePointChange.getValue());
            UnionDynamicBO.insertSportsPointLogClub(0, unionMember.getClubId(), pid, CommTime.nowSecond(), type.value(),
                    alivePointChange.getUnionId(), String.valueOf(preValue), String.valueOf(alivePointChange.getValue()));
        }
        return SData_Result.make(ErrorCode.Success, new UnionAlivePointItem(alivePointChange.getClubId(), club.getClubListBO().getClubsign(), club.getClubListBO().getName(),
                unionMember.getUnionMemberBO().getWarnStatus(), unionMember.getUnionMemberBO().getAlivePoint()));
    }

    /**
     * 获取親友圈预警信息
     *
     * @param sportsPointWarningChange 参数
     * @param pid                      玩家Pid
     * @return
     */
    public SData_Result getSportsPointWaningInfo(CUnion_SportsPointWarningChange sportsPointWarningChange, long pid) {
        SData_Result result = checkUnionRightsSprotWarn(sportsPointWarningChange.getUnionId(), sportsPointWarningChange.getClubId(), pid, sportsPointWarningChange.getOpClubId(), sportsPointWarningChange.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        UnionMember unionMember = (UnionMember) result.getData();
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(unionMember.getClubId());
        return SData_Result.make(ErrorCode.Success, new UnionSportsPointWarningItem(club.getClubListBO().getId(), club.getClubListBO().getClubsign(), club.getClubListBO().getName(),
                unionMember.getUnionMemberBO().getWarnStatus(), unionMember.getUnionMemberBO().getSportsPointWarning()));
    }

    /**
     * 获取親友圈预警信息
     *
     * @param alivePointChange 参数
     * @param pid              玩家Pid
     * @return
     */
    public SData_Result getAlivePointInfo(CUnion_AlivePointChange alivePointChange, long pid) {
        SData_Result result = checkUnionRightsSprotWarn(alivePointChange.getUnionId(), alivePointChange.getClubId(), pid, alivePointChange.getOpClubId(), alivePointChange.getOpPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        UnionMember unionMember = (UnionMember) result.getData();
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(unionMember.getClubId());
        return SData_Result.make(ErrorCode.Success, new UnionAlivePointItem(club.getClubListBO().getId(), club.getClubListBO().getClubsign(), club.getClubListBO().getName(),
                unionMember.getUnionMemberBO().getAlivePointStatus(), unionMember.getUnionMemberBO().getAlivePoint()));
    }

    /**
     * 检查赛事成员权利
     *
     * @param unionId  赛事id
     * @param clubId   亲友圈id
     * @param exePid   操作者id
     * @param opClubId 被操作者亲友圈id
     * @param opPid    被操作者Pid
     * @return
     */
    private SData_Result checkUnionRightsSprotWarn(long unionId, long clubId, long exePid, long opClubId, long opPid) {
        // 获取被操作者的赛事成员信息。
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(opPid, opClubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        //获取当前操作者的亲友圈成员信息
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, exePid);
        if (null == execClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "execClubMember CLUB_NOT_EXIST_MEMBER_INFO exePid:{%d},clubId:{%d},unionId:{%d}", exePid, clubId, unionId);
        }
        if (null == unionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "unionMember UNION_NOT_EXIST_MEMBER opPid:{%d},opClubId:{%d},unionId:{%d}", opPid, opClubId, unionId);
        }
        //是不是当前盟主
        if (unionMember.isManage()) {
            return SData_Result.make(ErrorCode.Success, unionMember);
        }
        // 获取当前操作者的赛事成员信息。
        UnionMember execUnionMember = UnionMgr.getInstance().getUnionMemberMgr().find(exePid, clubId, unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == execUnionMember) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER exePid:{%d},clubId:{%d},unionId:{%d}", exePid, clubId, unionId);
        }
        // 检查当前操作者是否有基本权限。
        if (!execUnionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
        }
        // 检查操作者和被操作者权限是否一致。
        if (unionMember.getUnionMemberBO().getPostType().value() >= execUnionMember.getUnionMemberBO().getPostType().value()) {
            // 被操作者的权力 >= 操作者的权力
            return SData_Result.make(ErrorCode.UNION_MEMBER_SAME_POST_TYPE, "UNION_MEMBER_SAME_RIGHTS execPostType:{%d},opPostType:{%d}，", execUnionMember.getUnionMemberBO().getPostType().value(), unionMember.getUnionMemberBO().getPostType().value());
        }
        return SData_Result.make(ErrorCode.Success, unionMember);
    }

    /**
     * 获取联赛禁止游戏的玩家列表
     *
     * @param req
     * @return
     */
    public SData_Result getUnionBanGamePlayerList(CUnion_BanGamePlayer req, long exePid) {
        SData_Result dataResult = checkBanGamePermission(req, exePid);
        if (!ErrorCode.Success.equals(dataResult.getCode())) {
            return dataResult;
        }
        List<UnionBanGamePlayerItem> unionBanGamePlayerItemList = ContainerMgr.get().getComponent(UnionBanGamePlayerBOService.class)
                .findAllE(Restrictions.eq("unionId", req.getUnionId()), UnionBanGamePlayerItem.class, UnionBanGamePlayerItem.getItemsName());
        //为空直接返回
        if (CollectionUtils.isEmpty(unionBanGamePlayerItemList)) {
            return SData_Result.make(ErrorCode.Success, new ArrayList<>());
        }
        // 考虑数据量大小 放到这边处理 不在查询语句处理
        // 被查询的是不是pid
        final long qPid = TypeUtils.StringTypeLong(req.getQuery());
        if (qPid <= 0L && req.getQuery().isEmpty()) {
            return SData_Result.make(ErrorCode.Success, unionBanGamePlayerItemList.stream().sorted(Comparator.comparing(UnionBanGamePlayerItem::getCreateTime).reversed()).skip(Page.getPageNum(req.getPageNum(), Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).collect(Collectors.toList()));
        }
        //查询过滤
        unionBanGamePlayerItemList = unionBanGamePlayerItemList.stream().filter(k -> k.getPid() == qPid || k.getName().contains(req.getQuery())).collect(Collectors.toList());
        return SData_Result.make(ErrorCode.Success, unionBanGamePlayerItemList.stream().sorted(Comparator.comparing(UnionBanGamePlayerItem::getCreateTime).reversed()).skip(Page.getPageNum(req.getPageNum(), Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).collect(Collectors.toList()));
    }

    /**
     * 添加禁止游戏
     *
     * @param req
     * @param exePid
     * @return
     */
    public SData_Result getUnionBanGamePlayerAdd(CUnion_BanGamePlayer req, long exePid) {
        SData_Result dataResult = checkBanGamePermission(req, exePid);
        if (!ErrorCode.Success.equals(dataResult.getCode())) {
            return dataResult;
        }
        Player player = PlayerMgr.getInstance().getPlayer(req.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.NotFind_Player, " getUnionBanGamePlayerAdd player is null");
        }
        UnionBanGamePlayerBO unionBanGamePlayerBO = new UnionBanGamePlayerBO();
        unionBanGamePlayerBO.setHeadImageUrl(player.getHeadImageUrl());
        unionBanGamePlayerBO.setName(player.getName());
        unionBanGamePlayerBO.setPid(player.getPid());
        unionBanGamePlayerBO.setUnionId(req.getUnionId());
        unionBanGamePlayerBO.setCreateTime(CommTime.nowSecond());
        unionBanGamePlayerBO.insert();
        UnionDynamicBO.insertSportsPointLogClub(player.getPid(), 0, exePid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_CLUB_BAN_GAME_PLAYER_ADD.value(),
                req.getUnionId(), "", "");
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 删除禁止游戏
     *
     * @param req
     * @param exePid
     * @return
     */
    public SData_Result getUnionBanGamePlayerDelete(CUnion_BanGamePlayer req, long exePid) {
        SData_Result dataResult = checkBanGamePermission(req, exePid);
        if (!ErrorCode.Success.equals(dataResult.getCode())) {
            return dataResult;
        }
        Player player = PlayerMgr.getInstance().getPlayer(req.getPid());
        if (Objects.isNull(player)) {
            return SData_Result.make(ErrorCode.NotFind_Player, " getUnionBanGamePlayerAdd player is null");
        }
        int deleteFlag = ContainerMgr.get().getComponent(UnionBanGamePlayerBOService.class).delete(Restrictions.and(Restrictions.eq("unionId", req.getUnionId()), Restrictions.eq("pid", req.getPid())));
        if (deleteFlag > 0) {
            UnionDynamicBO.insertSportsPointLogClub(player.getPid(), 0, exePid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_CLUB_BAN_GAME_PLAYER_DELETE.value(),
                    req.getUnionId(), "", "");
            return SData_Result.make(ErrorCode.Success);
        }
        return SData_Result.make(ErrorCode.NotFind_Player, "delete is false");
    }

    /**
     * 检查权限
     *
     * @param req
     * @param exePid
     * @return
     */
    public SData_Result checkBanGamePermission(CUnion_BanGamePlayer req, long exePid) {
        // 获取当前操作者的赛事成员信息。
        UnionMember execUnionMember = UnionMgr.getInstance().getUnionMemberMgr().find(exePid, req.getClubId(), req.getUnionId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == execUnionMember) {
            ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(exePid, req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
            if (Objects.isNull(clubMember) || !clubMember.isUnionMgr()) {
                return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER exePid:{%d},clubId:{%d},unionId:{%d}", exePid, req.getClubId(), req.getUnionId());
            }
        } else {
            // 检查当前操作者是否有基本权限。
            if (!execUnionMember.isManage()) {
                return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
            }
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 获取联盟皮肤信息
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result getUnionSkinInfo(CUnion_SkinInfo req, long pid) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            // 赛事不存在
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union)) {
            // 赛事不存在
            return SData_Result.make(ErrorCode.Success, SUnion_SkinInfo.make(0, club.getClubListBO().getSkinType(), 0, 0));
        }
        return SData_Result.make(ErrorCode.Success, SUnion_SkinInfo.make(req.getUnionId(), union.getUnionBO().getSkinType(), union.getUnionBO().getShowUplevelId(), union.getUnionBO().getShowClubSign()));
    }

    /**
     * 获取联盟皮肤信息
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result changeUnionSkinInfo(CUnion_SkinInfo req, long pid) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            // 赛事不存在
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        if (union.isChangeSkinStatus()) {
            return SData_Result.make(ErrorCode.UNION_SKIN_IS_CHANGED, "UNION_SKIN_IS_CHANGED");
        }
        if (req.getSkinType() >= 0) {
            //不是中至模式 皮肤设置不能设置为中至模式
            if (req.getSkinType() == UnionDefine.UNION_SKIN_TYPE.ZhongZhi.value() && !union.isZhongZhiUnion()) {
                return SData_Result.make(ErrorCode.NotAllow, "UNION_SKIN_IS_NOT_ALLOW");
            }
            union.getUnionBO().saveSkin(req.getSkinType());
            List<Long> clubIdList = getUnionMemberClubIdList(req.getUnionId());
            clubIdList.stream().forEach(k -> {
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(k);
                if (Objects.nonNull(club)) {
                    club.getClubListBO().saveSkin(req.getSkinType());
                }
            });
            union.setChangeSkinStatus(true);
            if (Config.isShare()) {
                ShareUnionListMgr.getInstance().addUnion(union);
            }
            notify2AllByUnion(req.getUnionId(), SUnion_SkinInfo.make(req.getUnionId(), req.getSkinType()));
        }
        if (req.getSkinTable() >= 0) {
            union.getUnionBO().saveSkinTable(req.getSkinTable());
        }
        if (req.getSkinBackColor() >= 0) {
            union.getUnionBO().saveSkinBackColor(req.getSkinBackColor());
        }
        return SData_Result.make(ErrorCode.Success, SUnion_SkinInfo.make(req.getUnionId(), union.getUnionBO().getSkinType(), union.getUnionBO().getShowUplevelId(), union.getUnionBO().getShowClubSign(), union.getUnionBO().getSkinType(), union.getUnionBO().getSkinBackColor()));
    }

    /**
     * 获取联盟皮肤信息
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result changeUnionSkinShowInfo(CUnion_SkinInfo req, long pid) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            // 赛事不存在
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        union.getUnionBO().saveShowUplevelId(req.getShowUplevelId());
        union.getUnionBO().saveShowClubSign(req.getShowClubSign());
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().addUnion(union);
        }
        notify2AllByUnion(req.getUnionId(), SUnion_SkinInfo.make(req.getUnionId(), req.getSkinType(), req.getShowUplevelId(), req.getShowClubSign()));
        return SData_Result.make(ErrorCode.Success, SUnion_SkinInfo.make(req.getUnionId(), union.getUnionBO().getSkinType(), union.getUnionBO().getShowUplevelId(), union.getUnionBO().getShowClubSign()));
    }

    /**
     * 获取联盟皮肤信息
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result changeZhongZhiShowStatus(CUnion_MemberList req, long pid) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            // 赛事不存在
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        union.getUnionBO().saveZhongZhiShowStatus(req.getType());
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().addUnion(union);
        }
        return SData_Result.make(ErrorCode.Success, SUnion_ZhongZhiShowStatus.make(req.getUnionId(), union.getUnionBO().getZhongZhiShowStatus()));
    }

    /**
     * 获取联盟皮肤信息
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result showZhongZhiShowStatus(CUnion_MemberList req, long pid) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            // 赛事不存在
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
//        union.getUnionBO().saveZhongZhiShowStatus(req.getType());
//        if (Config.isShare()) {
//            ShareUnionListMgr.getInstance().addUnion(union);
//        }
        return SData_Result.make(ErrorCode.Success, SUnion_ZhongZhiShowStatus.make(req.getUnionId(), union.getUnionBO().getZhongZhiShowStatus()));
    }

    /**
     * 检查查看房间的权限是否足够
     *
     * @param req
     * @param pid
     * @return
     */
    public SData_Result checkUnionGetMemberPromotionList(CUnion_GetMemberManage req, long pid) {
        // 获取操作者的赛事成员信息。
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(pid, req.clubId, req.unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (Objects.nonNull(unionMember)) {
            //是不是当前盟主 或者赛事管理员
            if (unionMember.isManage()) {
                return SData_Result.make(ErrorCode.Success, unionMember);
            }
        }
        //获取当前查询的亲友圈成员信息
        ClubMember queryClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.queryClubId, req.queryPid);
        if (null == queryClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "queryClubMember CLUB_NOT_EXIST_MEMBER_INFO exePid:{%d},clubId:{%d},unionId:{%d}", req.queryPid, req.clubId, req.unionId);
        }

        if (req.getClubId() != req.getQueryClubId()) {
            return SData_Result.make(ErrorCode.UNION_CLUB_NOT_SAME, "UNION_CLUB_NOT_SAME");
        }
        ClubMember doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, pid);
        if (null == doClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "doClubMember CLUB_NOT_EXIST_MEMBER_INFO exePid:{%d},clubId:{%d},unionId:{%d}", pid, req.clubId, req.unionId);
        }
        //亲友圈的话是圈主和管理员可以直接查询
        if (doClubMember.isMinister()) {
            return SData_Result.make(ErrorCode.Success, unionMember);
        }
        //判断是不是上下级
        //查出自己的所有上线
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).
                findAllE(Restrictions.eq("uid", queryClubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());


        if (queryUidOrPidItemList.stream().anyMatch(k -> k.getPuid() == doClubMember.getId())) {
            //是直属下级
            return SData_Result.make(ErrorCode.Success, unionMember);
        } else {
            //不是直属下级 判断是不是上级的推广员管理员
            if (doClubMember.isPromotionManage() && queryUidOrPidItemList.stream().anyMatch(k -> k.getPuid() == doClubMember.getClubMemberBO().getUpLevelId())) {
                return SData_Result.make(ErrorCode.Success, unionMember);
            }
        }
        return SData_Result.make(ErrorCode.UNION_CLUB_NOT_MY_PROMOTION, "UNION_CLUB_NOT_MY_PROMOTION");
    }

    /**
     * 修改中至排行榜显示
     *
     * @param pid
     * @return
     */
    public SData_Result changeZhongZhiRankedInfo(CUnion_ChangeRankedInfo req, long pid) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        if (!union.isZhongZhiUnion()) {
            return SData_Result.make(ErrorCode.UNION_TYPE_NOT_EXIST, "changeZhongZhiRankedInfo isZhongZhiUnion");
        }
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, pid);
        if (null == execClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "execClubMember CLUB_NOT_EXIST_MEMBER_INFO");
        }
        // 获取被操作者的赛事成员信息。
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(pid, req.clubId, req.unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (!execClubMember.isUnionMgr() && Objects.isNull(unionMember)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER");
        }
        // 检查当前操作者是否有基本权限。
        if (!execClubMember.isUnionMgr() && !unionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
        }
        union.getUnionBO().saveRankedOpenZhongZhi(req.isRankedOpenZhongZhi() ? 1 : 0);
        UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(req.getUnionId(), SUnion_ChangeRankedInfo.make(req.getClubId(), union.getUnionBO().getRankedOpenZhongZhi(), union.getUnionBO().getRankedOpenEntryZhongZhi(), req.getUnionId()));
        return SData_Result.make(ErrorCode.Success, req);
    }

    /**
     * 修改中至排行榜显示
     *
     * @param pid
     * @return
     */
    public SData_Result changeZhongZhiRankedInfoOpenEntry(CUnion_ChangeRankedInfo req, long pid) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        if (!union.isZhongZhiUnion()) {
            return SData_Result.make(ErrorCode.UNION_TYPE_NOT_EXIST, "changeZhongZhiRankedInfo isZhongZhiUnion");
        }
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, pid);
        if (null == execClubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "execClubMember CLUB_NOT_EXIST_MEMBER_INFO");
        }
        // 获取被操作者的赛事成员信息。
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(pid, req.clubId, req.unionId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (!execClubMember.isUnionMgr() && Objects.isNull(unionMember)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "execUnionMember UNION_NOT_EXIST_MEMBER");
        }
        // 检查当前操作者是否有基本权限。
        if (!execClubMember.isUnionMgr() && !unionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE");
        }
        union.getUnionBO().saveRankedOpenEntryZhongZhi(req.isRankedOpenEntryZhongZhi() ? 1 : 0);
        return SData_Result.make(ErrorCode.Success, req);
    }
}
