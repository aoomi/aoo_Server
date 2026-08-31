package business.global.club;

import business.global.shareclub.ShareClubMemberMgr;
import business.global.shareclub.SharePromotionSectionMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerClub;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.ItemFlow;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import core.db.entity.clarkGame.*;
import core.db.other.Restrictions;
import core.db.service.clarkGame.*;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.promotion.PromotionLevelDeleteEvent;
import core.dispatch.event.promotion.PromotionLevelInsertEvent;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.*;
import jsproto.c2s.cclass.club.Club_define.Club_MINISTER;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.cclass.union.*;
import jsproto.c2s.iclass.club.SClub_BecomePromotionManage;
import jsproto.c2s.iclass.club.SClub_PlayerInfoChange;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static jsproto.c2s.cclass.union.UnionDefine.UNION_EXEC_TYPE.PLAYER_BECOME_PROMOTIONMGR;
import static jsproto.c2s.cclass.union.UnionDefine.UNION_EXEC_TYPE.PLAYER_CANCEL_PROMOTIONMGR;

/**
 * 具体单个亲友圈成员信息
 *
 * @author Administrator
 */
@Data
//@NoArgsConstructor
//@MongoDbs({@MongoDb(doc = @Document(collection = "shareClubMemberKey"), indexes = @CompoundIndexes({@CompoundIndex(name = "clubID_1",def = "{'clubID':1}"),@CompoundIndex(name = "playerID_1",def = "{'playerID':1}")}))})
public class ClubMember implements Serializable {
    private long updateTime;
    /**
     * 亲友圈成员Bo
     */
    protected ClubMemberBO clubMemberBO;

    public ClubMember(ClubMemberBO clubMemberBO) {
        this.onUpdateStatus(clubMemberBO);
    }

    public void onUpdateStatus(ClubMemberBO clubMemberBO) {
        this.clubMemberBO = clubMemberBO;
    }

    /**
     * 不是推广员
     *
     * @return
     */
    public boolean isNotPromotion() {
        return Club_define.Club_PROMOTION.CheckExpectedValue(Club_define.Club_PROMOTION.NOT, getClubMemberBO().getPromotion());
    }


    /**
     * 是推广员
     *
     * @return
     */
    public boolean isPromotion() {
        return !Club_define.Club_PROMOTION.CheckExpectedValue(Club_define.Club_PROMOTION.NOT, getClubMemberBO().getPromotion());
    }

    /**
     * 是推广员
     *
     * @return
     */
    public boolean isLevelPromotion() {
        return getClubMemberBO().getLevel() > 0;
    }

    /**
     * 是推广员管理员
     *
     * @return 林剑峰:
     * 2021-12-06
     * 如果赛事管理 职能是 盟主亲友圈的玩家。。。那么就  赛事管理的权限默认包含  盟主的推广员管理 权限。
     * 是赛事管理  且非推广员或其他推广员的的推广员管理   则默认有盟主的推广员管理权限。。
     */
    public boolean isPromotionManage() {
        return getClubMemberBO().getPromotionManage() == 1 || (this.getClubMemberBO().getIsminister() == Club_MINISTER.Club_MINISTER_UNIONMGR.value() && this.isNotLevelPromotion());
    }

    public int getPromotionManage() {
        return isPromotionManage() ? 1 : 0;
    }


    /**
     * 是推广员
     *
     * @return
     */
    public boolean isNotLevelPromotion() {
        return !isLevelPromotion();
    }

    /**
     * 任命推广员
     *
     * @return
     */
    public boolean isAppointPromotion() {
        return Club_define.Club_PROMOTION.CheckExpectedValue(Club_define.Club_PROMOTION.APPOINT, getClubMemberBO().getPromotion());
    }

    /**
     * 卸任推广员
     *
     * @return
     */
    public boolean isLeaveOfficePromotion() {
        return Club_define.Club_PROMOTION.CheckExpectedValue(Club_define.Club_PROMOTION.LEAVE_OFFICE, getClubMemberBO().getPromotion());
    }

    /**
     * 获取推广员预警值
     *
     * @return
     */
    public Double getSportsPointWarning() {
        if (this.isLevelPromotion() && UnionDefine.UNION_WARN_STATUS.OPEN.ordinal() == getClubMemberBO().getWarnStatus()) {
            return this.getClubMemberBO().getSportsPointWarning();
        }
        return null;
    }

    /**
     * 获取个人预警值
     *
     * @return
     */
    public Double getSportsPointWarningPersonal() {
        if (UnionDefine.UNION_WARN_STATUS.OPEN.ordinal() == getClubMemberBO().getPersonalWarnStatus()) {
            return this.getClubMemberBO().getPersonalSportsPointWarning();
        }
        return null;
    }

    /**
     * 获取个人生存只
     *
     * @return
     */
    public Double getAlivePointZhongZhi() {
        if (UnionDefine.UNION_WARN_STATUS.OPEN.ordinal() == getClubMemberBO().getAlivePointStatus()) {
            return this.getClubMemberBO().getAlivePoint();
        }
        return null;
    }

    public int getIsLevel() {
        return this.isLevelPromotion() || isClubCreate() ? this.getClubMemberBO().getLevel() : Integer.MAX_VALUE;
    }

    public int getLevel() {
        return this.getClubMemberBO().getLevel();
    }

    public boolean setPromotion(long execPid) {
        if (this.isAppointPromotion()) {
            // 任命状态：改为卸任
            this.getClubMemberBO().savePromotion(Club_define.Club_PROMOTION.LEAVE_OFFICE.ordinal(), getClubMemberBO().getPlayerID());
            // 	推广员变动通知：年-月-日 时-分-秒  @玩家名称被@玩家名称删除推广员；
            PromotionDynamicBO.insertPromotionDynamicBO(getClubID(), 0L, getClubMemberBO().getPlayerID(), execPid, Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_LEAVE_OFFICE.value(), getClubMemberBO().getPlayerID());
            UnionDynamicBO.insertClubDynamic(getClubMemberBO().getPlayerID(), getClubID(), execPid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_PROMOTION_DYNAMIC_LEAVE_OFFICE.value());

        } else if (this.isLeaveOfficePromotion()) {
            // 卸任状态：改为任命
            this.getClubMemberBO().savePromotion(Club_define.Club_PROMOTION.APPOINT.ordinal(), getClubMemberBO().getPlayerID());
            // 	推广员变动通知：年-月-日 时-分-秒  @玩家名称被@玩家名称删除推广员；
            PromotionDynamicBO.insertPromotionDynamicBO(getClubID(), 0L, getClubMemberBO().getPlayerID(), execPid, Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_APPOINT.value(), getClubMemberBO().getPlayerID());
            UnionDynamicBO.insertClubDynamic(getClubMemberBO().getPlayerID(), getClubID(), execPid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.CLUB_PROMOTION_DYNAMIC_APPOINT.value());

        } else {
            // 不是推广员
            return false;
        }
        return true;
    }

    /**
     * 是否归属推广员
     *
     * @param partnerPid
     * @return
     */
    public boolean isSubordinate(long partnerPid) {
        return isNotPromotion() && getClubMemberBO().getPartnerPid() == partnerPid;
    }

    /**
     * 获取上级的pid
     *
     * @return
     */
    public long getPromotionManagePid() {
        ClubMember upClubMember = null;
        if (Config.isShare()) {
            upClubMember = ShareClubMemberMgr.getInstance().getClubMember(this.getClubMemberBO().getUpLevelId());
        } else {
            upClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(this.getClubMemberBO().getUpLevelId());
        }
        if (Objects.isNull(upClubMember)) {
            return 0L;
        }
        return upClubMember.getClubMemberBO().getPlayerID();
    }

    /**
     * 获取玩家pid
     *
     * @param pid 玩家Pid
     * @return
     */
    public boolean checkPidEqual(long pid) {
        if (pid == this.getClubMemberBO().getPlayerID()) {
            return true;
        }
        return false;
    }

    public int getResetTime() {
        return clubMemberBO.getResetTime();
    }

    /**
     * 是否禁止游戏
     *
     * @return T : 禁止游戏 ,F : 正常游戏
     */
    public boolean isBanGame() {
        return this.getClubMemberBO().getBanGame() > 0;
    }

    /**
     * 是否禁止游戏
     *
     * @return T : 禁止游戏 ,F : 正常游戏
     */
    public boolean isUnionBanGame() {
        return this.getClubMemberBO().getUnionBanGame() > 0;
    }


    public long getId() {
        return this.getClubMemberBO().getId();
    }

    /**
     * @return clubID
     */
    public long getClubID() {
        return this.getClubMemberBO().getClubID();
    }

    /**
     * @return status
     */
    public boolean getStatus(int status) {
        if (status == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {//查找退出未批准数据时 会筛选到之前无用的邀请数据 这边过滤
            return ((this.getClubMemberBO().getStatus() & status) > 0) && this.getClubMemberBO().getStatus() != Club_Player_Status.PLAYER_YAOQING.value();
        }
        return (this.getClubMemberBO().getStatus() & status) > 0;
    }

    /**
     * @return status
     */
    public boolean getStatusIncludeTuiChuWeiPiZhun() {
        return this.getClubMemberBO().getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value() || this.getClubMemberBO().getStatus() == Club_Player_Status.PLAYER_JIARU.value();
    }

    /**
     * 获取亲友圈页面
     * 有未批准的影响不能和上面的共用
     *
     * @return status
     */
    public boolean getStatusGetClubList(int status) {
        if (checkHide()) {
            return false;
        }
        if (status == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {//查找退出未批准数据时 会筛选到之前无用的邀请数据 这边过滤
            return ((this.getClubMemberBO().getStatus() & status) > 0) && this.getClubMemberBO().getStatus() != Club_Player_Status.PLAYER_YAOQING.value();
        }
        if (status == Club_Player_Status.PLAYER_JIARU.value()) {
            return (this.getClubMemberBO().getStatus() & status) > 0 || this.getClubMemberBO().getStatus() == Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value();
        }
        return (this.getClubMemberBO().getStatus() & status) > 0;
    }

    /**
     * 检查联盟是否隐藏
     *
     * @return
     */
    private boolean checkHide() {
        //找不到赛事和亲友圈 不是隐藏状态
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(this.getClubID());
        if (Objects.isNull(club)) {
            return false;
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union)) {
            return false;
        }
        return union.getUnionBO().getHideStatus() == UnionDefine.UNION_WARN_STATUS.OPEN.ordinal();
    }

    public int getStatus() {
        return this.getClubMemberBO().getStatus();
    }

    public void setStatus(int status, long exePid) {
        this.setStatus(null, null, status, exePid, false);
    }

    public int getTopTime() {
        return this.getClubMemberBO().getTopTime();
    }

    /**
     * 设置状态
     *
     * @param player 玩家
     * @param club   亲友圈
     * @param status 状态
     * @param exePid 执行操作PID
     * @param audit  判断玩家是不是在退出页面点击的事件
     */
    public void setStatus(Player player, Club club, int status, long exePid, boolean audit) {
        player = Objects.isNull(player) ? PlayerMgr.getInstance().getPlayer(this.getClubMemberBO().getPlayerID()) : player;
        if (Objects.isNull(player)) {
            CommLogD.error("setStatus null == player PID:{}", this.getClubMemberBO().getPlayerID());
            return;
        }
        club = Objects.isNull(club) ? ClubMgr.getInstance().getClubListMgr().findClub(this.getClubID()) : club;
        if (Objects.isNull(club)) {
            CommLogD.error("setStatus null == club ClubID:{}", this.getClubID());
            return;
        }

        // 设置成员状态
        this.getClubMemberBO().setStatus(status);
        //退出是否需要审核
        Club_define.CLUB_QUIT quit = Club_define.CLUB_QUIT.valueOf(club.getClubListBO().getQuit());
        if (Club_Player_Status.PLAYER_TUICHU.value() == status || status == Club_Player_Status.PLAYER_TICHU.value() || status == Club_Player_Status.PLAYER_TICHU_CLOSE.value()) {
            //如果退出玩家需要审核的话  就把他的状态改为退出未批准
            if (Club_define.CLUB_QUIT.CLUB_QUIT_NEED_AUDIT.equals(quit) && !audit && status != Club_Player_Status.PLAYER_TICHU.value()) {
                this.getClubMemberBO().saveStatus(Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value());
                // 通知玩家本身和所有的管理员
                ClubMgr.getInstance().getClubMemberMgr().notify2AllClubMemberAndPid(player, Club_Player_Status.PLAYER_TUICHU_TICHU.value(), club, this);
                return;
            } else {
                // 退出、踢出等玩家的时间
                this.getClubMemberBO().setDeletetime(CommTime.nowSecond());
            }
        } else if (Club_Player_Status.PLAYER_WEIPIZHUN.value() == status) {
            // 添加时间
            this.getClubMemberBO().setCreattime(CommTime.nowSecond());
        } else {
            if (Club_Player_Status.PLAYER_JUJIE.value() == status && audit) {
                this.getClubMemberBO().setUpdatetime(CommTime.nowSecond());
                this.getClubMemberBO().saveStatus(Club_Player_Status.PLAYER_JIARU.value());
                ClubMemberMgr clubMemberMgr = ClubMgr.getInstance().getClubMemberMgr();
                // 通知玩家本身和所有的管理员
                clubMemberMgr.notify2AllClubMemberAndPid(player, Club_Player_Status.PLAYER_TUICHU_JUJUE.value(), club, this);
                return;
            }
            // 更新时间
            this.getClubMemberBO().setUpdatetime(CommTime.nowSecond());
        }
        // 邀请
        if (Club_Player_Status.PLAYER_YAOQING.value() == status) {
            this.getClubMemberBO().saveInvitationPid(exePid);
        }

        ClubMemberMgr clubMemberMgr = ClubMgr.getInstance().getClubMemberMgr();
        if (Club_Player_Status.PLAYER_JIARU.value() == status) {
            // 首次加入亲友圈赠送圈卡
            clubMemberMgr.onGiveReward(player, club, status);
            if (this.getClubMemberBO().getUpLevelId() > 0L) {
                DispatcherComponent.getInstance().publish(new PromotionLevelInsertEvent(getId(), this.getClubMemberBO().getUpLevelId()));
            }
        }
        this.getClubMemberBO().setClubRoomCard(0);
        this.getClubMemberBO().updateStatus();
        // 通知玩家本身和所有的管理员
        clubMemberMgr.notify2AllClubMemberAndPid(player, club, this);
        // 添加亲友圈流水
        this.insertClubDynamicBO(player.getPid(), club.getClubListBO().getId(), UnionDefine.UNION_EXEC_TYPE.Club_EXEC_NOT, this.getClubMemberBO(), exePid);
        if (Club_Player_Status.PLAYER_JUJIE.value() == status || Club_Player_Status.PLAYER_TUICHU.value() == status
                || status == Club_Player_Status.PLAYER_TICHU.value()
                || status == Club_Player_Status.PLAYER_TICHU_CLOSE.value()
                || Club_Player_Status.PLAYER_JUJIEYAOQING.value() == status) {
            // 亲友圈成员Id
            long memberId = this.getId();
            // 上级推广员id
            long upLevelId = this.getClubMemberBO().getUpLevelId();
            // 删除记录
            FlowLogger.clubMemberRemoveLog(memberId, this.getClubMemberBO().getPlayerID(), this.getClubMemberBO().getClubID(), ItemFlow.CLUB_MEMBER_REMOVE.value(), this.getClubMemberBO().getStatus(), this.getClubMemberBO().getIsminister(), exePid, getClubMemberBO().getLevel(), upLevelId, getClubMemberBO().getCreattime(), getClubMemberBO().getUpdatetime(), getClubMemberBO().getDeletetime());
            // 移除亲友圈成员
            clubMemberMgr.removeClubMenber(memberId);
            // 删除亲友圈成员
            this.getClubMemberBO().getBaseService().delete(Restrictions.eq("id", memberId));
            // 推广员:主动退出、踢出;
            if (Club_Player_Status.PLAYER_TUICHU.value() == status || status == Club_Player_Status.PLAYER_TICHU.value() || status == Club_Player_Status.PLAYER_TICHU_CLOSE.value()) {
                // 删除绑定并且变更下属的绑定
                DispatcherComponent.getInstance().publish(new PromotionLevelDeleteEvent(this.getClubMemberBO().getClubID(), memberId, upLevelId, Club_define.Club_PROMOTION_LEVEL_DELETE.DELETE, CommTime.getNowTimeStringYMD()));
            }
            // 退出亲友圈时间记录
            ClubMember.checkExistJoinOrQuitTimeLimit(player.getPid(), club.getClubListBO().getId(), status, false);
        }
        //更新共享亲友圈玩家
//        if(Config.isShare()) {
//            ShareClubMemberMgr.getInstance().updateClubMemberBo(this.getClubMemberBO());
//        }
    }

    /**
     * 添加亲友圈流水
     *
     * @param pid       玩家PID
     * @param clubID    亲友圈ID
     * @param itemFlow  产生原因类型
     * @param cMemberBO 亲友圈成员信息
     */
    public void insertClubDynamicBO(long pid, long clubID, UnionDefine.UNION_EXEC_TYPE itemFlow, ClubMemberBO cMemberBO, long exePid) {
        if (UnionDefine.UNION_EXEC_TYPE.Club_EXEC_NOT.equals(itemFlow)) {
            // 直接使用产生原因类型
            if (Club_Player_Status.PLAYER_JIARU.value() == cMemberBO.getStatus()) {
                itemFlow = UnionDefine.UNION_EXEC_TYPE.Club_EXEC_JIARU;
            } else if (Club_Player_Status.PLAYER_TICHU.value() == cMemberBO.getStatus()) {
                itemFlow = UnionDefine.UNION_EXEC_TYPE.Club_EXEC_TICHU;
            } else if (Club_Player_Status.PLAYER_TUICHU.value() == cMemberBO.getStatus()) {
                itemFlow = UnionDefine.UNION_EXEC_TYPE.Club_EXEC_TUICHU;
                // 退出亲友圈是自己主动的。
//                exePid = 0L;
            } else {
                // 没有符合记录的动作
                return;
            }
        }
        if (Objects.nonNull(cMemberBO)) {
            ClubMember upMember = ShareClubMemberMgr.getInstance().getClubMember(this.getClubMemberBO().getUpLevelId());
            if (Objects.nonNull(upMember)) {
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(upMember.getClubMemberBO().getClubID());
                SharePlayer player = SharePlayerMgr.getInstance().getSharePlayer(exePid);
                //如果是踢出或者退出的话  直属那边添加一条竞技动态
                if (Club_Player_Status.PLAYER_TICHU.value() == cMemberBO.getStatus() || Club_Player_Status.PLAYER_TUICHU.value() == cMemberBO.getStatus()) {
                    UnionDynamicBO.insertClubDynamic(this.getClubMemberBO().getPlayerID(), this.getClubMemberBO().getClubID(), upMember.getClubMemberBO().getPlayerID(), CommTime.nowSecond(),
                            UnionDefine.UNION_EXEC_TYPE.CLUB_ZHI_SHU_TICHU.value(), club.getClubListBO().getUnionId(), String.valueOf(player.getPlayerBO().getId()), player.getPlayerBO().getName());
                } else if (Club_Player_Status.PLAYER_JIARU.value() == cMemberBO.getStatus()) {
                    UnionDynamicBO.insertClubDynamic(this.getClubMemberBO().getPlayerID(), this.getClubMemberBO().getClubID(), upMember.getClubMemberBO().getPlayerID(), CommTime.nowSecond(),
                            UnionDefine.UNION_EXEC_TYPE.CLUB_ZHI_SHU_JIARU.value(), club.getClubListBO().getUnionId(), String.valueOf(player.getPlayerBO().getId()), player.getPlayerBO().getName());

                }
            }

        }
        //踢出的时候 直属上级添加一条消息
        UnionDynamicBO.insertClubDynamic(pid, clubID, exePid, CommTime.nowSecond(), itemFlow.value());
    }


    /**
     * 关闭亲友圈更新状态
     *
     * @param status 要设置的 status
     */
    public void closeClubStatus(int status) {
        Player player = PlayerMgr.getInstance().getPlayer(this.getClubMemberBO().getPlayerID());
        if (Objects.isNull(player)) {
            CommLogD.error("setStatus null == player PID:{}", this.getClubMemberBO().getPlayerID());
            return;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(this.getClubID());
        if (Objects.isNull(club)) {
            CommLogD.error("setStatus null == club ClubID:{}", this.getClubID());
            return;
        }
        // 设置成员状态
        this.getClubMemberBO().setStatus(status);
        if (Club_Player_Status.PLAYER_TUICHU.value() == status || status == Club_Player_Status.PLAYER_TICHU.value() || status == Club_Player_Status.PLAYER_TICHU_CLOSE.value()) {
            // 退出、踢出等玩家的时间
            this.getClubMemberBO().setDeletetime(CommTime.nowSecond());
        }
        ClubMemberMgr clubMemberMgr = ClubMgr.getInstance().getClubMemberMgr();
        BaseSendMsg msg = SClub_PlayerInfoChange.make(getClubMemberBO().getClubID(), club.getClubListBO().getName(),
                getClubPlayerInfo(player, club.getClubListBO().getAgentsID(), club.getClubListBO().getLevel(),
                        isBanGame(), getClubMemberBO().getPromotion(), this.getSportsPoint(club)));
        player.pushProtoMq(msg);
        // 通知玩家本身和所有的管理员
        this.getClubMemberBO().setClubRoomCard(0);
        this.getClubMemberBO().updateStatus();
        if (Club_Player_Status.PLAYER_JUJIE.value() == status || Club_Player_Status.PLAYER_TUICHU.value() == status
                || status == Club_Player_Status.PLAYER_TICHU.value()
                || status == Club_Player_Status.PLAYER_TICHU_CLOSE.value()
                || Club_Player_Status.PLAYER_JUJIEYAOQING.value() == status) {
            clubMemberMgr.removeClubMenber(this.getId());
        }
        //更新共享亲友圈玩家
//        if(Config.isShare()) {
//            ShareClubMemberMgr.getInstance().updateClubMemberBo(this.getClubMemberBO());
//        }
    }

    /**
     * 获取亲友圈疲劳值
     *
     * @param club 亲友圈信息
     * @return
     */
    public double getSportsPoint(Club club) {
        return club.getClubListBO().getUnionId() > 0L ? this.getClubMemberBO().getSportsPoint() : 0D;
    }

    public double getSportsPoint() {
        return this.getClubMemberBO().getSportsPoint();
    }

    public double getAllowSportsPoint() {
        double sportsPoint = this.getClubMemberBO().getSportsPoint();
        if (this.isLevelPromotion() && UnionDefine.UNION_WARN_STATUS.OPEN.ordinal() == getClubMemberBO().getWarnStatus() && this.getSportsPointWarning() < 0D) {
            sportsPoint += Math.abs(this.getSportsPointWarning());
        }
        return sportsPoint;
    }

    /**
     * 获取玩家保险箱内的竞技点分数
     *
     * @return
     */
    public double getCaseSportsPoint() {
        return this.getClubMemberBO().getCaseSportsPoint();
    }

    /**
     * 获取玩家身上的总竞技点分数 包括保险箱
     *
     * @return
     */
    public double getTotalSportsPoint() {
        return CommMath.FormatDouble(this.getClubMemberBO().getSportsPoint() + this.getClubMemberBO().getCaseSportsPoint());
    }

    public long getSportsPointLong() {
        return CommMath.mulLong(this.getClubMemberBO().getSportsPoint(), 100D);
    }

    /**
     * @param isminister 要设置的 isminister
     */
    public void setIsminister(Player player, Club club, int isminister, long exePid) {
        ClubMemberMgr clubMemberMgr = ClubMgr.getInstance().getClubMemberMgr();
        // 获取玩家设置状态
        Club_Player_Status status = status(isminister, exePid);
        this.getClubMemberBO().saveIsminister(isminister);
        Club_define.CLUB_EXEC_TYPE execType;
        if (isminister == 1) {
            execType = Club_define.CLUB_EXEC_TYPE.CLUB_EXEC_BECOME_MGR;
        } else if (isminister == 3) {
            execType = Club_define.CLUB_EXEC_TYPE.CLUB_EXEC_BECOME_UNION_MGR;
        } else {
            execType = Club_define.CLUB_EXEC_TYPE.CLUB_EXEC_CANCEL_MGR;
        }
        //亲友圈身份变动记录
        FlowLogger.clubmemberStatusLog(player.getPid(), club.getClubListBO().getId(), exePid, club.getClubListBO().getId(), execType.value());
        clubMemberMgr.notify2AllClubMemberAndPid(player, status.value(), club, this);
    }

    /**
     * @param isminister 要设置的 isminister
     */
    public void setPromotionMinister(Player player, Club club, int isminister, long exePid) {
        this.getClubMemberBO().savePromotionManage(isminister);
        // 获取玩家设置状态
        if (isminister == 1) {
            this.insertClubDynamicBO(this.getClubMemberBO().getPlayerID(), this.getClubMemberBO().getClubID(), PLAYER_BECOME_PROMOTIONMGR, null, exePid);
            //亲友圈身份变动记录
            FlowLogger.clubmemberStatusLog(player.getPid(), club.getClubListBO().getId(), exePid, club.getClubListBO().getId(), Club_define.CLUB_EXEC_TYPE.CLUB_EXEC_BECOME_PROMOTION_MGR.value());
        } else {
            this.insertClubDynamicBO(this.getClubMemberBO().getPlayerID(), this.getClubMemberBO().getClubID(), PLAYER_CANCEL_PROMOTIONMGR, null, exePid);
            //亲友圈身份变动记录
            FlowLogger.clubmemberStatusLog(player.getPid(), club.getClubListBO().getId(), exePid, club.getClubListBO().getId(), Club_define.CLUB_EXEC_TYPE.CLUB_EXEC_CANCEL_PROMOTION_MGR.value());
        }
        player.pushProtoMq(SClub_BecomePromotionManage.make(club.getClubListBO().getId(), club.clubListBO.getName(), isminister));
    }

    /**
     * 获取玩家状态
     *
     * @param isminister 设置是否管理员
     * @return
     */
    private Club_Player_Status status(int isminister, long exePid) {
        Club_Player_Status status = Club_Player_Status.valueOf(this.clubMemberBO.getStatus());
        if (Club_MINISTER.Club_MINISTER_MGR.value() == isminister) {
            status = Club_Player_Status.PLAYER_BECOME_MGR;
        } else if (Club_MINISTER.Club_MINISTER_UNIONMGR.value() == isminister) {
            status = Club_Player_Status.PLAYER_BECOME_UNIONMGR;
        } else if (Club_MINISTER.Club_MINISTER_GENERAL.value() == isminister) {
            status = Club_Player_Status.PLAYER_CANCEL_MGR;
        }
        // 记录操作类型
        UnionDefine.UNION_EXEC_TYPE itemFlow = UnionDefine.UNION_EXEC_TYPE.Club_EXEC_CANCEL_MGR;
        switch (status) {
            case PLAYER_CANCEL_MGR:
                if (this.getClubMemberBO().getIsminister() == Club_MINISTER.Club_MINISTER_UNIONMGR.value()) {
                    itemFlow = UnionDefine.UNION_EXEC_TYPE.PLAYER_CANCEL_UNIONMGR;
                } else {
                    itemFlow = UnionDefine.UNION_EXEC_TYPE.Club_EXEC_CANCEL_MGR;
                }
                break;
            case PLAYER_BECOME_UNIONMGR:
                itemFlow = UnionDefine.UNION_EXEC_TYPE.PLAYER_BECOME_UNIONMGR;
                break;
            case PLAYER_BECOME_MGR:
                itemFlow = UnionDefine.UNION_EXEC_TYPE.Club_EXEC_BECOME_MGR;
                break;
        }
        // 记录操作类型
//        UnionDefine.UNION_EXEC_TYPE itemFlow = Club_Player_Status.PLAYER_BECOME_MGR.equals(status) ? UnionDefine.UNION_EXEC_TYPE.Club_EXEC_BECOME_MGR : UnionDefine.UNION_EXEC_TYPE.Club_EXEC_CANCEL_MGR;
        // 添加亲友圈动态
        this.insertClubDynamicBO(this.getClubMemberBO().getPlayerID(), this.getClubMemberBO().getClubID(), itemFlow, null, exePid);
        return status;
    }

    /**
     * @return isminister
     */
    public boolean isMinister() {
        return Club_MINISTER.Club_MINISTER_GENERAL.value() != this.getClubMemberBO().getIsminister();
    }

    public boolean isClubCreate() {
        return Club_define.Club_MINISTER.Club_MINISTER_CREATER.value() == this.getClubMemberBO().getIsminister();
    }

    public boolean isUnionMgr() {
        return Club_define.Club_MINISTER.Club_MINISTER_UNIONMGR.value() == this.getClubMemberBO().getIsminister();
    }

    public boolean isNotClubCreate() {
        return !isClubCreate();
    }

    /**
     * 根据传入的值返回时间
     */
    public int getTime() {
        if (Club_Player_Status.PLAYER_TUICHU.value() == this.getStatus()
                || Club_Player_Status.PLAYER_TICHU.value() == this.getStatus()
                || Club_Player_Status.PLAYER_TICHU_CLOSE.value() == this.getStatus()) {
            return this.getClubMemberBO().getDeletetime();
        } else if (Club_Player_Status.PLAYER_WEIPIZHUN.value() == this.getStatus()) {
            return this.getClubMemberBO().getCreattime();
        } else {
            return this.getClubMemberBO().getUpdatetime();
        }
    }


    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public ClubPlayerInfo getClubPlayerInfo(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, long agentsID, int level, boolean isBanGame, int partner, double sportsPoint) {
        return this.getClubPlayerInfo(player, upShortPlayer, getStatus(), agentsID, level, isBanGame, partner, sportsPoint);
    }

    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public ClubPlayerInfo getClubPlayerInfoZhongZhi(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, long agentsID, int level, boolean isBanGame, int partner, double sportsPoint) {
        return this.getClubPlayerInfoZhongZhi(player, upShortPlayer, getStatus(), agentsID, level, isBanGame, partner, sportsPoint);
    }

    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public ClubPlayerInfoZhongZhi getClubPlayerInfoComPetitionZhongZhi(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, long agentsID, int level, boolean isBanGame, int partner, double sportsPoint) {
        return new ClubPlayerInfoZhongZhi(player.getShortPlayer(), getStatus(), getTime(), sportsPoint, this.getClubMemberBO().getEliminatePoint(), this.getClubMemberBO().getAlivePoint());
    }

    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public ClubPlayerInfo getClubPlayerInfo(Player player, long agentsID, int level, boolean isBanGame, int partner, double sportsPoint) {
        return this.getClubPlayerInfo(player, null, getStatus(), agentsID, level, isBanGame, partner, sportsPoint);
    }


    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public ClubPlayerInfo getClubPlayerInfo(Player player, int status, long agentsID, int level, boolean isBanGame,
                                            int partner, double sportsPoint) {
        return getClubPlayerInfo(player, null, status, agentsID, level, isBanGame, partner, sportsPoint);
    }

    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public ClubPlayerInfo getClubPlayerInfo(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, int status, long agentsID, int level, boolean isBanGame,
                                            int partner, double sportsPoint) {

        return new ClubPlayerInfo(player.getShortPlayer(), upShortPlayer, status, getClubMemberBO().getIsminister(), getTime(),
                player.getFeature(PlayerClub.class).getPlayerClubRoomCard(agentsID, level), isBanGame, partner, sportsPoint, getClubMemberBO().getPromotionManage());
    }


    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public ClubPlayerInfo getClubPlayerInfoZhongZhi(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, int status, long agentsID, int level, boolean isBanGame,
                                                    int partner, double sportsPoint) {
        boolean onlineFlag = PlayerMgr.getInstance().checkExistOnlinePlayerByPid(this.getClubMemberBO().getPlayerID());
        return new ClubPlayerInfo(player.getShortPlayer(), upShortPlayer, status, getClubMemberBO().getIsminister(), getTime(),
                player.getFeature(PlayerClub.class).getPlayerClubRoomCard(agentsID, level), isBanGame, partner, sportsPoint, getClubMemberBO().getPromotionManage(), onlineFlag, CommTime.getSecToYMDStr2(player.getPlayerBO().getLastLogin()));
    }


    /**
     * 赛事管理员查询亲友圈成员
     *
     * @param player         玩家信息
     * @param isUnionBanGame 是否被赛事管理员禁止游戏
     * @param sportsPoint    竞技点
     * @return
     */
    public UnionClubPlayerInfo getUnionClubPlayerInfo(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, boolean isUnionBanGame, double sportsPoint, int minister, double eliminatePoint) {
        return new UnionClubPlayerInfo(player.getShortPlayer(), upShortPlayer, isUnionBanGame, sportsPoint, minister, eliminatePoint);
    }


    /**
     * 玩家申请操作
     *
     * @param outSportsPoint 淘汰分
     * @return
     */
    public SData_Result execApply(long unionId, double outSportsPoint) {
        SData_Result result = null;
        UnionDefine.UNION_MATCH_STATE matchState = UnionDefine.UNION_MATCH_STATE.valueOf(this.clubMemberBO.getUnionState());
        if (UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.equals(matchState)) {
            // 比赛中，只能操作退赛申请
            result = execBackOff(outSportsPoint, matchState);
        } else if (UnionDefine.UNION_MATCH_STATE.BACK_OFF.equals(matchState)) {
            // 退赛申请中，只能操作取消退赛
            result = execCancelBackOff(outSportsPoint, matchState);
        } else {
            // 申请复赛中，只能申请复赛
            result = execApplyRematch(outSportsPoint, matchState);
        }
        UnionMgr.getInstance().getUnionMemberMgr().unionMatchApplyExamineNotify(unionId, getClubID(), checkExistApply());
        return result;
    }

    /**
     * 退赛申请操作
     *
     * @param outSportsPoint 淘汰分
     * @return
     */
    private SData_Result execBackOff(double outSportsPoint, UnionDefine.UNION_MATCH_STATE matchState) {
        // 处于比赛中的状态并且比赛分≥淘汰分，才能申请退赛
//		UnionDefine.UNION_MATCH_STATE matchState = UnionDefine.UNION_MATCH_STATE.valueOf(this.clubMemberBO.getUnionState());
        if (!UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.equals(matchState)) {
            return SData_Result.make(ErrorCode.Success, matchState.value());
        }
        if (this.getClubMemberBO().getSportsPoint() < outSportsPoint) {
            // 比赛分必须大于等于淘汰分
            this.getClubMemberBO().saveUnionState(UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value());
            return SData_Result.make(ErrorCode.Success, UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value());
        }
        this.getClubMemberBO().saveUnionState(UnionDefine.UNION_MATCH_STATE.BACK_OFF.value());
        return SData_Result.make(ErrorCode.Success, UnionDefine.UNION_MATCH_STATE.BACK_OFF.value());
    }

    /**
     * 取消退赛操作
     *
     * @param outSportsPoint 淘汰分
     * @return
     */
    private SData_Result execCancelBackOff(double outSportsPoint, UnionDefine.UNION_MATCH_STATE matchState) {
        // 处于退赛申请状态中并且比赛分≥淘汰分，才能取消退赛
        if (!UnionDefine.UNION_MATCH_STATE.BACK_OFF.equals(matchState)) {
            return SData_Result.make(ErrorCode.Success, matchState.value());
        }
        if (this.getClubMemberBO().getSportsPoint() < outSportsPoint) {
            // 比赛分必须大于等于淘汰分
            this.getClubMemberBO().saveUnionState(UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value());
            return SData_Result.make(ErrorCode.Success, UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value());
        }
        this.getClubMemberBO().saveUnionState(UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value());
        return SData_Result.make(ErrorCode.Success, UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value());
    }


    /**
     * 申请复赛操作
     *
     * @param outSportsPoint 淘汰分
     * @return
     */
    private SData_Result execApplyRematch(double outSportsPoint, UnionDefine.UNION_MATCH_STATE matchState) {
        if (!UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.equals(matchState)) {
            return SData_Result.make(ErrorCode.Success, matchState.value());
        }
        this.getClubMemberBO().saveUnionState(UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.value());
        return SData_Result.make(ErrorCode.Success, UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH);
    }

    /**
     * 检查是否存在有申请操作
     *
     * @return
     */
    public boolean checkExistApply() {
        UnionDefine.UNION_MATCH_STATE matchState = UnionDefine.UNION_MATCH_STATE.valueOf(this.clubMemberBO.getUnionState());
        return UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.equals(matchState) || UnionDefine.UNION_MATCH_STATE.BACK_OFF.equals(matchState);
    }


//    /**
//     * 获取赛事房间收益百分比配置列表
//     * @return
//     */
//    @SuppressWarnings("rawtypes")
//    public SData_Result getUnionRoomConfigScorePercentItemList(int pageNum) {
//        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionMemberBO().getUnionId());
//        if (Objects.isNull(union)) {
//            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
//        }
//        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(this.getUnionMemberBO().getClubMemberId());
//        if (Objects.isNull(clubMember)) {
//            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "UNION_NOT_EXIST");
//        }
//
//        List<UnionRoomConfigScorePercentBO> unionRoomConfigScorePercentBOList = ContainerMgr.get().getComponent(UnionRoomConfigScorePercentBOService.class).findAll(Restrictions.and(Restrictions.eq("unionId", this.getUnionMemberBO().getUnionId()), Restrictions.eq("clubId", this.getUnionMemberBO().getClubId())));
//        if (null == unionRoomConfigScorePercentBOList) {
//            unionRoomConfigScorePercentBOList = new ArrayList<>();
//        }
//        Map<Long, Double> unionRoomConfigScorePercentBOMap = unionRoomConfigScorePercentBOList.stream().collect(Collectors.toMap(UnionRoomConfigScorePercentBO::getConfigId, scorePercentBO -> scorePercentBO.getScoreDividedInto()));
//        return SData_Result.make(ErrorCode.Success, union.getRoomConfigBOMap().values().stream().map(k -> {
//            // 获取公共配置
//            BaseCreateRoom baseCreateRoom = k.getbRoomConfigure().getBaseCreateRoom();
//            if (Objects.isNull(baseCreateRoom)) {
//                return null;
//            }
//            Double scorePercent;
//            UnionScoreDividedIntoValueItem scorePercentItem = EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).get(String.format(DataConstants.SCORE_PERCENT_CACHE, this.getUnionMemberBO().getUnionId(),this.getUnionMemberBO().getClubId(),k.getGameIndex()),UnionScoreDividedIntoValueItem.class);
//            if (Objects.isNull(scorePercentItem)) {
//                scorePercent = unionRoomConfigScorePercentBOMap.get(k.getGameIndex());
//            } else {
//                scorePercent = scorePercentItem.getScoreDividedInto();
//            }
//            // 获取配置是否存在
//            // 获取百分比
//            scorePercent = Objects.isNull(scorePercent) ? clubMember.getClubMemberBO().getScoreDividedInto() : scorePercent;
//            return new UnionRoomConfigScorePercentItem(baseCreateRoom.getGameIndex(),baseCreateRoom.getRoomName(), baseCreateRoom.getPlayerNum(), scorePercent);
//        }).filter(k -> null != k).skip(Page.getPageNum(pageNum,Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).collect(Collectors.toList()));
//    }


    /**
     * 执行更新赛事房间收益百分比
     *
     * @param promotionCalcActiveItemList 房间配置ID列表
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result execClubPromotionCalcActiveBatch(List<ClubPromotionCalcActiveItem> promotionCalcActiveItemList, ClubPromotionLevetShareChangeBatchItem clubPromotionLevetShareChangeBatchItem) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(getClubID());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.NotAllow);
        }
        if (club.getClubListBO().getUnionId() <= 0L) {
            return SData_Result.make(ErrorCode.NotAllow);
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.NotAllow);
        }
        //执行操作的玩家
        ClubMember doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(getClubID(), clubPromotionLevetShareChangeBatchItem.getUpLevelPid());
        if (Objects.isNull(doClubMember)) {
            return SData_Result.make(ErrorCode.NotAllow, "doClubMember is null");
        }
        PromotionLevelRoomConfigScorePercentBO promotionLevelRoomConfigScorePercentBO = null;
        for (ClubPromotionCalcActiveItem calcActiveItem : promotionCalcActiveItemList) {
            if (calcActiveItem.getValue() < 0D) {
                // 小于等于 0 或者 大于 100
                continue;
            }
            if (calcActiveItem.getConfigId() <= 0L) {
                // 配置Id  小于等于 0
                continue;
            }
            // 查询上级同配置分层比例
            double findScorePercen = ContainerMgr.get().getComponent(PromotionLevelRoomConfigScorePercentBOService.class).findScorePercen(clubPromotionLevetShareChangeBatchItem.getUpLevelPid(), club.getClubListBO().getUnionId(), this.getClubID(), calcActiveItem.getConfigId(), clubPromotionLevetShareChangeBatchItem.getDoType(), clubPromotionLevetShareChangeBatchItem.getMaxValue());
            if (clubPromotionLevetShareChangeBatchItem.getDoType() == clubPromotionLevetShareChangeBatchItem.getToType() && (calcActiveItem.getValue() > findScorePercen)) {
                continue;
            }
            UnionCreateGameSet unionCreateGameSet = union.getRoomConfigBOMap().get(calcActiveItem.getConfigId());
            if (Objects.isNull(unionCreateGameSet)) {
                continue;
            }
            String roomName = unionCreateGameSet.getbRoomConfigure().getBaseCreateRoom().getRoomName();
            promotionLevelRoomConfigScorePercentBO = new PromotionLevelRoomConfigScorePercentBO();
            promotionLevelRoomConfigScorePercentBO.setUnionId(club.getClubListBO().getUnionId());
            promotionLevelRoomConfigScorePercentBO.setConfigId(calcActiveItem.getConfigId());
            promotionLevelRoomConfigScorePercentBO.setClubId(this.getClubID());
            promotionLevelRoomConfigScorePercentBO.setPid(this.getClubMemberBO().getPlayerID());
            promotionLevelRoomConfigScorePercentBO.setShareValue(this.getClubMemberBO().getShareValue());
            promotionLevelRoomConfigScorePercentBO.setConfigName(roomName);
            promotionLevelRoomConfigScorePercentBO.setTagId(unionCreateGameSet.getbRoomConfigure().getTagId());
            promotionLevelRoomConfigScorePercentBO.setType(clubPromotionLevetShareChangeBatchItem.getDoType());
            if (clubPromotionLevetShareChangeBatchItem.getDoType() == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()) {
                promotionLevelRoomConfigScorePercentBO.setShareValue(getClubMemberBO().getShareFixedValue());
                promotionLevelRoomConfigScorePercentBO.setScoreDividedInto(CommMath.FormatDoubleOnePoint(calcActiveItem.getValue()));
            } else {
                promotionLevelRoomConfigScorePercentBO.setShareValue(getClubMemberBO().getShareValue());
                promotionLevelRoomConfigScorePercentBO.setScorePercent(CommMath.FormatDoubleOnePoint(calcActiveItem.getValue()));
            }
            promotionLevelRoomConfigScorePercentBO.saveIgnoreOrUpDate(doClubMember.getClubMemberBO().getPlayerID());
            //如果当前是百分比的修改的话 要判断是不是有下级分成高于 有的话要设置为相同
            if (UnionDefine.UNION_SHARE_TYPE.PERCENT.equals(UnionDefine.UNION_SHARE_TYPE.valueOf(clubPromotionLevetShareChangeBatchItem.getDoType()))) {
                List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", this.getClubMemberBO().getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
                if (Objects.nonNull(queryUidOrPidItemList) && !queryUidOrPidItemList.isEmpty()) {
                    for (QueryUidOrPuidItem queryUidOrPuidItem : queryUidOrPidItemList) {
                        //下级成员
                        ClubMember dawnClubmember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem.getUid());

                        if (Objects.isNull(dawnClubmember)) continue;
                        List<PromotionLevelRoomConfigScorePercentBO> promotionLevelRoomConfigScorePercentBOList = ContainerMgr
                                .get()
                                .getComponent(PromotionLevelRoomConfigScorePercentBOService.class)
                                .findAll(Restrictions.and(Restrictions.eq("pid", dawnClubmember.getClubMemberBO().getPlayerID()), Restrictions.eq("unionId", club.getClubListBO().getUnionId()), Restrictions.eq("clubId", dawnClubmember.getClubID()), Restrictions.eq("type", clubPromotionLevetShareChangeBatchItem.getDoType()), Restrictions.eq("configId", calcActiveItem.getConfigId())));
                        if (CollectionUtils.isNotEmpty(promotionLevelRoomConfigScorePercentBOList)) {
                            PromotionLevelRoomConfigScorePercentBO dawnBO = promotionLevelRoomConfigScorePercentBOList.get(0);
                            if (dawnBO.getScorePercent() > CommMath.FormatDoubleOnePoint(calcActiveItem.getValue())) {
                                dawnBO.setShareValue(dawnClubmember.getClubMemberBO().getShareValue());
                                dawnBO.setScorePercent(CommMath.FormatDoubleOnePoint(calcActiveItem.getValue()));
                                dawnBO.setConfigName(roomName);
                                dawnBO.saveIgnoreOrUpDate(doClubMember.getClubMemberBO().getPlayerID());
                            }
                        }
                    }


                }
            }
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 根据预留值执行更新赛事房间收益百分比
     *
     * @param promotionCalcActiveItemList 房间配置ID列表
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result execClubPromotionCalcActiveBatchByReversedValue(List<ClubPromotionCalcActiveItem> promotionCalcActiveItemList, ClubPromotionLevetShareChangeBatchItem clubPromotionLevetShareChangeBatchItem, double reversedValue) {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(getClubID());
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.NotAllow);
        }
        if (club.getClubListBO().getUnionId() <= 0L) {
            return SData_Result.make(ErrorCode.NotAllow);
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.NotAllow);
        }
        PromotionLevelRoomConfigScorePercentBO promotionLevelRoomConfigScorePercentBO = null;
        //执行操作的玩家
        ClubMember doClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(getClubID(), clubPromotionLevetShareChangeBatchItem.getUpLevelPid());
        if (Objects.isNull(doClubMember)) {
            return SData_Result.make(ErrorCode.NotAllow, "doClubMember is null");
        }
        //执行操作玩家的上级
        ClubMember doClubMemberUplevel;
        boolean clubFlag = false;
        if (doClubMember.getClubMemberBO().getUpLevelId() > 0) {
            if (Config.isShare()) {
                doClubMemberUplevel = ShareClubMemberMgr.getInstance().getClubMember(doClubMember.getClubMemberBO().getUpLevelId());
            } else {
                doClubMemberUplevel = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(doClubMember.getClubMemberBO().getUpLevelId());
            }
        } else {
            clubFlag = true;
            doClubMemberUplevel = ClubMgr.getInstance().getClubMemberMgr().getClubMember(getClubID(), club.getOwnerPlayerId());
        }

        //执行玩家上级的通用固定值 如果没有对应房间配置的话 会取通用的值代替
        double upleveFixedValue = doClubMemberUplevel.getClubMemberBO().getShareFixedValue();
        List<ClubRoomConfigCalcActiveItem> clubRoomConfigCalcActiveItems = new ArrayList<>();
        for (ClubPromotionCalcActiveItem calcActiveItem : promotionCalcActiveItemList) {
            if (calcActiveItem.getValue() < 0D) {
                // 小于等于 0 或者 大于 100
                continue;
            }
            if (calcActiveItem.getConfigId() <= 0L) {
                // 配置Id  小于等于 0
                continue;
            }
            // 查询上级同配置分层比例
            double findScorePercen = ContainerMgr.get().getComponent(PromotionLevelRoomConfigScorePercentBOService.class).findScorePercen(clubPromotionLevetShareChangeBatchItem.getUpLevelPid(), club.getClubListBO().getUnionId(), this.getClubID(), calcActiveItem.getConfigId(), clubPromotionLevetShareChangeBatchItem.getDoType(), clubPromotionLevetShareChangeBatchItem.getMaxValue());
            if (clubPromotionLevetShareChangeBatchItem.getDoType() == clubPromotionLevetShareChangeBatchItem.getToType() && (calcActiveItem.getValue() > findScorePercen)) {
                continue;
            }
            UnionCreateGameSet unionCreateGameSet = union.getRoomConfigBOMap().get(calcActiveItem.getConfigId());
            if (Objects.isNull(unionCreateGameSet)) {
                continue;
            }
            String roomName = unionCreateGameSet.getbRoomConfigure().getBaseCreateRoom().getRoomName();
            promotionLevelRoomConfigScorePercentBO = new PromotionLevelRoomConfigScorePercentBO();
            promotionLevelRoomConfigScorePercentBO.setUnionId(club.getClubListBO().getUnionId());
            promotionLevelRoomConfigScorePercentBO.setConfigId(calcActiveItem.getConfigId());
            promotionLevelRoomConfigScorePercentBO.setClubId(this.getClubID());
            promotionLevelRoomConfigScorePercentBO.setPid(this.getClubMemberBO().getPlayerID());
            promotionLevelRoomConfigScorePercentBO.setShareValue(this.getClubMemberBO().getShareValue());
            promotionLevelRoomConfigScorePercentBO.setConfigName(roomName);
            promotionLevelRoomConfigScorePercentBO.setTagId(unionCreateGameSet.getbRoomConfigure().getTagId());
            promotionLevelRoomConfigScorePercentBO.setType(clubPromotionLevetShareChangeBatchItem.getDoType());
            double scoreDividedInto = 0D;
            if (clubPromotionLevetShareChangeBatchItem.getDoType() == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()) {
                promotionLevelRoomConfigScorePercentBO.setShareValue(getClubMemberBO().getShareFixedValue());
                //先去数据库查找
                if (clubFlag) {
                    List<UnionRoomConfigScorePercentBO> unionRoomConfigScorePercentBOList = ContainerMgr.get().getComponent(UnionRoomConfigScorePercentBOService.class).findAll(Restrictions.and(Restrictions.eq("unionId", club.getClubListBO().getUnionId()), Restrictions.eq("clubId", doClubMember.getClubMemberBO().getClubID()), Restrictions.eq("type", clubPromotionLevetShareChangeBatchItem.getDoType()),
                            Restrictions.eq("configId", calcActiveItem.getConfigId())));
                    if (CollectionUtils.isNotEmpty(unionRoomConfigScorePercentBOList)) {
                        scoreDividedInto = unionRoomConfigScorePercentBOList.get(0).getScoreDividedInto() - reversedValue;
                    } else {
                        scoreDividedInto = upleveFixedValue - reversedValue;
                    }
                } else {
                    UnionRoomPromotionShareCfgItem upLevelRoomConfig = ContainerMgr.get().getComponent(PromotionLevelRoomConfigScorePercentBOService.class)
                            .findOneE(Restrictions.and(Restrictions.eq("pid", doClubMemberUplevel.getClubMemberBO().getPlayerID()), Restrictions.eq("unionId", club.getClubListBO().getUnionId()),
                                            Restrictions.eq("clubId", club.getClubListBO().getId()), Restrictions.eq("configId", calcActiveItem.getConfigId()), Restrictions.eq("type", clubPromotionLevetShareChangeBatchItem.getDoType())),
                                    UnionRoomPromotionShareCfgItem.class, UnionRoomPromotionShareCfgItem.getItemsNameCount());
                    if (Objects.nonNull(upLevelRoomConfig)) {
                        scoreDividedInto = upLevelRoomConfig.getScoreDividedInto() - reversedValue;
                    } else {
                        scoreDividedInto = upleveFixedValue - reversedValue;
                    }
                }
                //找到对应玩家的上级 对应玩法的分成 再减去该玩家本身的预留值
                promotionLevelRoomConfigScorePercentBO.setScoreDividedInto(CommMath.FormatDoubleOnePoint(scoreDividedInto >= 0 ? scoreDividedInto : 0));
            }

            ClubRoomConfigCalcActiveItem configCalcActiveItem = new ClubRoomConfigCalcActiveItem();
            configCalcActiveItem.setConfigId(calcActiveItem.getConfigId());
            configCalcActiveItem.setConfigName(roomName);
            configCalcActiveItem.setSize(unionCreateGameSet.getbRoomConfigure().getBaseCreateRoom().getPlayerNum());
            configCalcActiveItem.setAllowValue(doClubMember.getClubMemberBO().getShareFixedValue());
            configCalcActiveItem.setChangeFlag(true);
            configCalcActiveItem.setValue(CommMath.FormatDoubleOnePoint(scoreDividedInto >= 0 ? scoreDividedInto : 0));
            configCalcActiveItem.setType(1);
            clubRoomConfigCalcActiveItems.add(configCalcActiveItem);
        }
        return SData_Result.make(ErrorCode.Success, clubRoomConfigCalcActiveItems);
    }

    /**
     * 判断是不是区间分成
     *
     * @return
     */
    public boolean isSectionShare() {
        return UnionDefine.UNION_SHARE_TYPE.SECTION.ordinal() == clubMemberBO.getShareType();
    }

    /**
     * 初始化推广员区间分成数据
     */
    public SData_Result initPromotionSection(long unionId, boolean isClubCreate) {
        List<UnionShareSectionItem> unionShareSectionItems = ((UnionShareSectionBOService) ContainerMgr.get().getComponent(UnionShareSectionBOService.class)).findAllE(Restrictions.eq("unionId", unionId), UnionShareSectionItem.class, UnionShareSectionItem.getItemsName());
        if (CollectionUtils.isEmpty(unionShareSectionItems)) {
            Union union;
            if (Config.isShare()) {
                union = ShareUnionListMgr.getInstance().getUnion(unionId);
            } else {
                union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
            }
            if (Objects.isNull(union)) {
                CommLogD.error("unionID is null:" + unionId);
                return SData_Result.make(ErrorCode.Error_Code);
            }
            union.initUnionShareSection();
        }
        int createTime = CommTime.nowSecond();
        //圈主往下进行分成
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", this.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        List<QueryUidOrPuidItem> promotionList = queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).collect(Collectors.toList());

        for (UnionShareSectionItem unionShareSectionItem : unionShareSectionItems) {
            double allowShareToValue = unionShareSectionItem.getEndFlag() == 1 ? unionShareSectionItem.getBeginValue() : unionShareSectionItem.getEndValue();
            PromotionShareSectionBO promotionShareSectionBO = new PromotionShareSectionBO();
            promotionShareSectionBO.setClubId(this.getClubID());
            promotionShareSectionBO.setUnionSectionId(unionShareSectionItem.getId());
            promotionShareSectionBO.setPid(this.getClubMemberBO().getPlayerID());
            promotionShareSectionBO.setCreateTime(createTime);
            promotionShareSectionBO.setBeginValue(unionShareSectionItem.getBeginValue());
            promotionShareSectionBO.setEndValue(unionShareSectionItem.getEndValue());
            promotionShareSectionBO.setEndFlag(unionShareSectionItem.getEndFlag());
            for (QueryUidOrPuidItem queryUidOrPuidItem : promotionList) {
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
                PromotionShareSectionItem item = promotionSection.getPromotionShareSectionItems().stream().filter(k -> k.getUnionSectionId() == unionShareSectionItem.getId()).findFirst().orElse(null);
                if (Objects.isNull(item)) {
                    continue;
                }
                allowShareToValue = CommMath.subDouble(allowShareToValue, item.getShareToSelfValue());
            }
            promotionShareSectionBO.setAllowShareToValue(allowShareToValue);
            promotionShareSectionBO.setShareToSelfValue(isClubCreate ? unionShareSectionItem.getEndValue() : allowShareToValue);
            ((PromotionShareSectionBOService) promotionShareSectionBO.getBaseService()).saveIgnoreOrUpDateInit(promotionShareSectionBO);
        }
//        getClubMemberBO().saveShareType(UnionDefine.UNION_SHARE_TYPE.SECTION.ordinal());
        initRedisSection();
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * //数据初始化到redis中
     */
    public void initRedisSection() {
        List<PromotionShareSectionItem> promotionShareSectionItems = ((PromotionShareSectionBOService) ContainerMgr.get().getComponent(PromotionShareSectionBOService.class)).findAllE(Restrictions.and(
                Restrictions.eq("pid", clubMemberBO.getPlayerID()), Restrictions.eq("clubId", clubMemberBO.getClubID())), PromotionShareSectionItem.class, PromotionShareSectionItem.getItemsName());
        if (CollectionUtils.isEmpty(promotionShareSectionItems)) {
            return;
        }
        SharePromotionSectionMgr.getInstance().addClubMemberPromotionSection(this,
                new SharePromotionSection(promotionShareSectionItems));
    }


    /**
     * 加入或者退出时间限制
     *
     * @return
     */
    public static SData_Result checkExistJoinOrQuitTimeLimit(long pid, long clubId, int status, boolean isJoin) {
        String redisKey = String.format("CLUB_JOIN_LIMIT:PID:%d:CLUB:%d", pid, clubId);
        if (isJoin) {
            if (Club_Player_Status.PLAYER_JIARU.value() == status || Club_Player_Status.PLAYER_YAOQING.value() == status || Club_Player_Status.PLAYER_WEIPIZHUN.value() == status) {
                // 加入亲友圈检查是否存在时间限制
                if (StringUtils.isNotEmpty(ContainerMgr.get().getRedis().get(redisKey))) {
                    return SData_Result.make(ErrorCode.CLUB_MEMBER_JOIN_TIME_LIMIT, "join club limit ");
                }
            }
        } else {
            ContainerMgr.get().getRedis().putWithTime(redisKey, 600, "1");
        }
        return SData_Result.make(ErrorCode.Success);
    }


}