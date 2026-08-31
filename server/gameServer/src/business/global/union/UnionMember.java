package business.global.union;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.shareunion.ShareUnionMemberMgr;
import business.player.Player;
import business.player.PlayerMgr;
import cenum.Page;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.ehcache.DataConstants;
import com.ddm.server.common.ehcache.EhCacheFactory;
import com.ddm.server.common.ehcache.configuration.DefaultCacheConfiguration;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import core.db.entity.clarkGame.UnionDynamicBO;
import core.db.entity.clarkGame.UnionMemberBO;
import core.db.entity.clarkGame.UnionRoomConfigScorePercentBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.UnionRoomConfigScorePercentBOService;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.club.ClubPromotionSectionInitEvent;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.union.*;
import jsproto.c2s.iclass.union.CUnion_ScorePercentBatchUpdate;
import jsproto.c2s.iclass.union.CUnion_ScorePercentList;
import jsproto.c2s.iclass.union.SUnion_PostTypeInfoChange;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 赛事成员信息
 */
@Data
public class UnionMember implements Serializable {
    /**
     * 赛事成员
     */
    private UnionMemberBO unionMemberBO;

    public UnionMember(UnionMemberBO unionMemberBO) {
        this.unionMemberBO = unionMemberBO;
    }

    /**
     * 管理员或者创建者
     */
    public boolean isManage() {
        return this.getUnionMemberBO().getType() >= UnionDefine.UNION_POST_TYPE.UNION_MANAGE.value();
    }

    /**
     * 创建者
     *
     * @return
     */
    public boolean isCreate() {
        return this.getUnionMemberBO().getType() == UnionDefine.UNION_POST_TYPE.UNION_CREATE.value();
    }

    /**
     * 获得赛事成员所属的亲友圈id
     *
     * @return
     */
    public long getClubId() {
        return this.getUnionMemberBO().getClubId();
    }

    /**
     * 检查状态是否正确
     *
     * @return status
     */
    public boolean getStatus(int status) {
        return UnionDefine.UNION_PLAYER_STATUS.getStatus(this.getUnionMemberBO().getStatus(), status);
    }

    public void setStatus(int status, long exePid) {
        this.setStatus(null, null, status, exePid);
    }

    /**
     * 设置状态
     *
     * @param player 玩家
     * @param union  赛事
     * @param status 状态
     * @param exePid 执行操作PID
     */
    public void setStatus(Player player, Union union, int status, long exePid) {
        player = player == null ? PlayerMgr.getInstance().getPlayer(this.getUnionMemberBO().getClubOwnerId()) : player;
        if (null == player) {
            CommLogD.error("setStatus null == player ClubOwnerId:{}", this.getUnionMemberBO().getClubOwnerId());
            return;
        }
        union = union == null ? UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionMemberBO().getUnionId()) : union;
        if (null == union) {
            CommLogD.error("setStatus null == club getUnionId:{}", this.getUnionMemberBO().getUnionId());
            return;
        }
        // 赛事动态类型
        UnionDefine.UNION_EXEC_TYPE unionExecType = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_NOT;
        // 旧状态
        int oldStatus = this.getUnionMemberBO().getStatus();
        // 设置成员状态
        this.getUnionMemberBO().setStatus(status);
        if (this.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value())) {
            // 退出、踢出等玩家的时间
            this.getUnionMemberBO().setDeleteTime(CommTime.nowSecond());
        } else if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN.value() == status) {
            // 添加时间
            this.getUnionMemberBO().setCreateTime(CommTime.nowSecond());
        } else {
            // 更新时间
            this.getUnionMemberBO().setUpdateTime(CommTime.nowSecond());
        }
        UnionMemberMgr unionMemberMgr = UnionMgr.getInstance().getUnionMemberMgr();
        if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value() == status) {
            // 加入赛事。
            unionMemberMgr.clubJoinOrQuitUnion(this.getUnionMemberBO().getClubId(), this.getUnionMemberBO().getUnionId(), union);
            // 如果就状态是邀请的话,赛事动态类型记录邀请,否则默认
            unionExecType = oldStatus == UnionDefine.UNION_PLAYER_STATUS.PLAYER_YAOQING.value() ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_YAOQING : unionExecType;
        } else if (this.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value())) {
            // 退出赛事。
            unionMemberMgr.clubJoinOrQuitUnion(this.getUnionMemberBO().getClubId(), 0L, union);
        }
        // 通知玩家本身和所有的管理员
//        clubMemberMgr.notify2AllClubMemberAndPid(player, club, this);
        // 邀请
        if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_YAOQING.value() == status) {
            this.getUnionMemberBO().updateStatus(exePid);
        } else {
            this.getUnionMemberBO().updateStatus(0);
        }
        // 通知指定玩家通知
        unionMemberMgr.invitedPlayer(union, player, this.getUnionMemberBO().getClubId(), this.getUnionMemberBO().getStatus(), exePid);
        // 添加联赛流水
        this.insertUnionDynamicBO(player.getPid(), union.getUnionBO().getId(), unionExecType, this.getUnionMemberBO(), exePid);
        if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_JUJIE.value() == status || UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value() == status
                || status == UnionDefine.UNION_PLAYER_STATUS.PLAYER_TICHU.value()
                || status == UnionDefine.UNION_PLAYER_STATUS.PLAYER_TICHU_CLOSE.value()
                || UnionDefine.UNION_PLAYER_STATUS.PLAYER_JUJIEYAOQING.value() == status) {
            // 移除亲友圈
            unionMemberMgr.getUnionMemberMap().remove(this.getUnionMemberBO().getId());
            if (Config.isShare()) {
                ShareUnionMemberMgr.getInstance().deleteClubMember(this.getUnionMemberBO().getId());
            }
        }
    }

    /**
     * 添加联赛流水
     *
     * @param pid           玩家PID
     * @param unionId       联赛ID
     * @param itemFlow      产生原因类型
     * @param unionMemberBO 联赛成员信息
     */
    public void insertUnionDynamicBO(long pid, long unionId, UnionDefine.UNION_EXEC_TYPE itemFlow, UnionMemberBO unionMemberBO, long exePid) {
        if (UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_NOT.equals(itemFlow)) {
            // 直接使用产生原因类型
            if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value() == unionMemberBO.getStatus()) {
                if (exePid == 0L) {
                    itemFlow = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_JIARU_NOT;
                } else {
                    itemFlow = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_JIARU;
                }
            } else if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_TICHU.value() == unionMemberBO.getStatus()) {
                itemFlow = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_TICHU;
            } else if (UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value() == unionMemberBO.getStatus()) {
                if (exePid == 0L) {
                    itemFlow = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_TUICHU_NOT;
                } else {
                    itemFlow = UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_TUICHU;
                }
            } else {
                // 没有符合记录的动作
                return;
            }
        }
        UnionDynamicBO.insertUnionClub(pid, unionMemberBO.getClubId(), unionId, exePid, CommTime.nowSecond(), itemFlow.value());
    }


    /**
     * 设置职务类型
     *
     * @param postType 职务类型
     * @param exePid   操作者Pid
     */
    @SuppressWarnings("rawtypes")
    public SData_Result setPostType(long opPid, int postType, long exePid) {
        if (this.getUnionMemberBO().getType() == postType) {
            // 修改的职务类型与现在一致
            return SData_Result.make(ErrorCode.Success);
        }
        // 设置职务类型
        this.getUnionMemberBO().saveType(postType);
        // postType == 亲友圈创建者 则取消管理员，否则成为管理
        UnionDefine.UNION_EXEC_TYPE itemFlow = postType == UnionDefine.UNION_POST_TYPE.UNION_CLUB.value() ? UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_CANCEL_MGR : UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_BECOME_MGR;
        // 设置职务类型动态记录
        UnionDynamicBO.insertUnionClub(opPid, getUnionMemberBO().getClubId(), this.getUnionMemberBO().getUnionId(), exePid, CommTime.nowSecond(), itemFlow.value());
        UnionMgr.getInstance().getUnionMemberMgr().notify2AllByManagerToPid(getUnionMemberBO().getUnionId(), opPid, SUnion_PostTypeInfoChange.make(getUnionMemberBO().getUnionId(), getUnionMemberBO().getClubId(), getUnionMemberBO().getType(), opPid));
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 设置积分比例修改
     *
     * @param opPid    被操作者Pid
     * @param opClubId 被操作者亲友圈Id
     * @param value    积分比例值
     * @param exePid   操作者Pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Deprecated
    public SData_Result setScorePercent(long opPid, long opClubId, int value, long exePid) {
        // 获取亲友圈成员信息
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(opPid, opClubId, Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            // 找不到成员
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        // 当前值
        int curRemainder = value;
        // 前值
        int preValue = clubMember.getClubMemberBO().getScorePercent();
        // 设置积分比例修改
        clubMember.getClubMemberBO().saveScorePercent(this.getUnionMemberBO().getUnionId(), value);
        // 记录修改积分比例动态记录
        UnionDynamicBO.insertUnionScorePercent(opPid, opClubId, this.getUnionMemberBO().getUnionId(), exePid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(), String.valueOf(value), String.valueOf(preValue));
        FlowLogger.scorePercentChargeLog(opPid, this.getUnionMemberBO().getUnionId(), opClubId, curRemainder, preValue, exePid);
        // 修改成功
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 设置积分分成修改
     *
     * @param opPid    被操作者Pid
     * @param opClubId 被操作者亲友圈Id
     * @param value    积分比例值
     * @param exePid   操作者Pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result setScoreDividedInto(long opPid, long opClubId, double value, long exePid, int type) {
        // 获取亲友圈成员信息
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(opPid, opClubId, Club_define.Club_Player_Status.PLAYER_JIARU);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(opClubId);
        if (Objects.isNull(club)) {
            CommLogD.error("CLUB_NOT_EXIST is null" + opClubId);
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (null == clubMember) {
            // 找不到成员
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        if (club.isClubCreateShareFlag()) {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_CLUB_PROMOTION_CHANGE_IS_EXIT, "CLUB_MEMBER_CLUB_PROMOTION_CHANGE_IS_EXIT");
        }
        // 当前值
        double curRemainder = CommMath.FormatDouble(value);
        // 前值
        double preValue = clubMember.getClubMemberBO().getScoreDividedInto();
        int oldType = clubMember.getClubMemberBO().getShareType();
        Double shareValue = clubMember.getClubMemberBO().getShareValue();
        Double shareFixedValue = clubMember.getClubMemberBO().getShareFixedValue();
        // 设置积分比例修改
        clubMember.getClubMemberBO().saveShareValue(this.getUnionMemberBO().getUnionId(), curRemainder, type);
        if (UnionDefine.UNION_SHARE_TYPE.SECTION.ordinal() == type) {
            UnionDynamicBO.insertUnionScorePercentShare(opPid, opClubId, this.getUnionMemberBO().getUnionId(), this.getUnionMemberBO().getClubId(), exePid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SHARE_SECTION.value(),
                    "", "");
        } else {
            UnionDynamicBO.insertUnionScorePercentShare(opPid, opClubId, this.getUnionMemberBO().getUnionId(), this.getUnionMemberBO().getClubId(), exePid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(),
                    type == 0 ? String.valueOf(curRemainder) + "%" : String.valueOf(curRemainder), oldType == 0 ? String.valueOf(shareValue) + "%" : String.valueOf(shareFixedValue));
            // 记录修改积分比例动态记录
            FlowLogger.scoreDividedIntoChargeLog(opPid, this.getUnionMemberBO().getUnionId(), opClubId, curRemainder, preValue, exePid);
        }

        club.setClubCreateShareFlag(true);
        DispatcherComponent.getInstance().publish(new ClubPromotionSectionInitEvent(opClubId, opPid, unionMemberBO.getUnionId(), type));
        // 修改成功
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 获取赛事房间收益百分比配置列表
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result getUnionRoomConfigScorePercentItemList(CUnion_ScorePercentList req) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionMemberBO().getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        ClubMember clubMember;
        if (Config.isShare()) {
            clubMember = ShareClubMemberMgr.getInstance().getClubMember(this.getUnionMemberBO().getClubMemberId());
        } else {
            clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(this.getUnionMemberBO().getClubMemberId());
        }
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "UNION_NOT_EXIST");
        }

        List<UnionRoomConfigScorePercentBO> unionRoomConfigScorePercentBOList = ContainerMgr.get().getComponent(UnionRoomConfigScorePercentBOService.class).findAll(Restrictions.and(Restrictions.eq("unionId", this.getUnionMemberBO().getUnionId()), Restrictions.eq("clubId", this.getUnionMemberBO().getClubId())));
        if (null == unionRoomConfigScorePercentBOList) {
            unionRoomConfigScorePercentBOList = new ArrayList<>();
        }
        // 赛事分成类型
        UnionDefine.UNION_SHARE_TYPE shareType = UnionDefine.UNION_SHARE_TYPE.valueOf(req.getType());
        if (Objects.isNull(shareType)) {
            return SData_Result.make(ErrorCode.NotAllow, "getUnionRoomConfigScorePercentItemList null shareType");
        }
        Map<Long, Double> unionRoomConfigScorePercentBOMap = null;
        if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareType)) {
            unionRoomConfigScorePercentBOMap = unionRoomConfigScorePercentBOList.stream().collect(Collectors.toMap(UnionRoomConfigScorePercentBO::getConfigId, scorePercentBO -> scorePercentBO.getScoreDividedInto()));
        } else {
            unionRoomConfigScorePercentBOMap = unionRoomConfigScorePercentBOList.stream().collect(Collectors.toMap(UnionRoomConfigScorePercentBO::getConfigId, scorePercentBO -> (double) scorePercentBO.getScorePercent()));
        }
        Map<Long, Double> finalUnionRoomConfigScorePercentBOMap = unionRoomConfigScorePercentBOMap;
        return SData_Result.make(ErrorCode.Success, union.getRoomConfigBOMap().values().stream().map(k -> {
            // 获取公共配置
            BaseCreateRoom baseCreateRoom = k.getbRoomConfigure().getBaseCreateRoom();
            if (Objects.isNull(baseCreateRoom)) {
                return null;
            }
            Double scorePercent;
            UnionScoreDividedIntoValueItem scorePercentItem = EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).get(String.format(DataConstants.SCORE_PERCENT_CACHE, this.getUnionMemberBO().getUnionId(), this.getUnionMemberBO().getClubId(), k.getGameIndex()), UnionScoreDividedIntoValueItem.class);
            if (Objects.isNull(scorePercentItem)) {
                scorePercent = finalUnionRoomConfigScorePercentBOMap.get(k.getGameIndex());
            } else {
                if (UnionDefine.UNION_SHARE_TYPE.FIXED.equals(shareType)) {
                    scorePercent = scorePercentItem.getScoreDividedInto();
                } else {
                    scorePercent = scorePercentItem.getScorePercent();
                }
            }
            // 获取配置是否存在
            boolean changeFlag = Objects.isNull(scorePercent) || scorePercent.intValue() < 0;
            // 获取百分比
            scorePercent = changeFlag ? clubMember.getClubMemberBO().getScoreDividedInto() : scorePercent;
            return new UnionRoomConfigScorePercentItem(baseCreateRoom.getGameIndex(), baseCreateRoom.getRoomName(), baseCreateRoom.getPlayerNum(), scorePercent, !changeFlag);
        }).filter(k -> null != k).skip(Page.getPageNum(req.getPageNum(), Page.PAGE_SIZE_10)).limit(Page.PAGE_SIZE_10).collect(Collectors.toList()));
    }

    /**
     * 执行更新赛事房间收益百分比
     *
     * @param req 执行收益更新
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result execUpdateUnionRoomConfigScorePercent(CUnion_ScorePercentBatchUpdate req, long exePid) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionMemberBO().getUnionId());
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        ClubMember clubMember;
        if (Config.isShare()) {
            clubMember = ShareClubMemberMgr.getInstance().getClubMember(this.getUnionMemberBO().getClubMemberId());
        } else {
            clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(this.getUnionMemberBO().getClubMemberId());
        }
        if (Objects.isNull(clubMember)) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "UNION_NOT_EXIST");
        }
        UnionRoomConfigScorePercentBO unionRoomConfigScorePercentBO = null;
        for (CUnionScorePercentItem unionScorePercentItem : req.getUnionScorePercentItemList()) {
            if (unionScorePercentItem.getScorePercent() < 0D) {
                // 小于等于 0 或者 大于 100
                continue;
            }
            if (unionScorePercentItem.getConfigId() <= 0L) {
                // 配置Id  小于等于 0
                continue;
            }
            UnionCreateGameSet unionCreateGameSet = union.getRoomConfigBOMap().get(unionScorePercentItem.getConfigId());
            if (Objects.isNull(unionCreateGameSet)) {
                continue;
            }

            String roomName = unionCreateGameSet.getbRoomConfigure().getBaseCreateRoom().getRoomName();
            unionRoomConfigScorePercentBO = new UnionRoomConfigScorePercentBO();
            unionRoomConfigScorePercentBO.setPid(getUnionMemberBO().getClubOwnerId());
            unionRoomConfigScorePercentBO.setUnionId(this.getUnionMemberBO().getUnionId());
            unionRoomConfigScorePercentBO.setConfigId(unionScorePercentItem.getConfigId());
            unionRoomConfigScorePercentBO.setClubId(this.getUnionMemberBO().getClubId());
            unionRoomConfigScorePercentBO.setExePid(exePid);
            unionRoomConfigScorePercentBO.setType(req.getType());
            unionRoomConfigScorePercentBO.setConfigName(roomName);
            unionRoomConfigScorePercentBO.setTagId(unionCreateGameSet.getbRoomConfigure().getTagId());
            if (req.getType() == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()) {
                unionRoomConfigScorePercentBO.setShareValue(clubMember.getClubMemberBO().getShareFixedValue());
                unionRoomConfigScorePercentBO.setScoreDividedInto(CommMath.FormatDouble(unionScorePercentItem.getScorePercent()));
            } else {
                unionRoomConfigScorePercentBO.setShareValue(clubMember.getClubMemberBO().getShareValue());
                unionRoomConfigScorePercentBO.setScorePercent(CommMath.FormatDoubleOnePoint(unionScorePercentItem.getScorePercent()));
            }
            unionRoomConfigScorePercentBO.saveIgnoreOrUpDate();
        }
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 获取个人生存只
     *
     * @return
     */
    public Double getAlivePointZhongZhi() {
        if (UnionDefine.UNION_WARN_STATUS.OPEN.ordinal() == getUnionMemberBO().getAlivePointStatus()) {
            return this.getUnionMemberBO().getAlivePoint();
        }
        return null;
    }
}
