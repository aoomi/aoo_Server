package business.global.union;

import BaseCommon.CommLog;
import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.config.CurrencyKeyMgr;
import business.global.sharegm.ShareInitMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.shareunion.ShareUnionMemberMgr;
import business.player.Player;
import business.player.feature.PlayerCurrency;
import cenum.ConstEnum;
import cenum.ItemFlow;
import cenum.PrizeType;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.mgr.sensitive.SensitiveWordMgr;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Maps;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import core.db.entity.clarkGame.UnionBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.UnionBOService;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.union.UnionCloseCase;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.PrizeTypeItem;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.cclass.union.UnionRankingReward;
import jsproto.c2s.iclass.union.*;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 联赛管理
 */
@Data
public class UnionListMgr {

    /**
     * 联赛map
     */
    Map<Long, Union> unionMap = Maps.newConcurrentMap();

    public void init() {
        CommLogD.info("[UnionBO.init] load UnionBO begin...]");
        if (ShareInitMgr.getInstance().getShareDataInit()) {
            List<UnionBO> unionBOList = ContainerMgr.get().getComponent(UnionBOService.class).findAll(null);
            if (CollectionUtils.isEmpty(unionBOList)) {
                return;
            }
            for (UnionBO unionBO : unionBOList) {
                CurrencyKeyMgr.getInstance().clearKey(unionBO.getUnionSign());
                if (unionBO.getDistime() <= 0) {
                    getUnionMap().put(unionBO.getId(), new Union(unionBO));
                    //共享赛事
                    if (Config.isShare() && ShareInitMgr.getInstance().getShareDataInit()) {
                        ShareUnionListMgr.getInstance().addUnion(getUnionMap().get((unionBO.getId())));
                    }
                }
            }
            unionBOList = null;
        } else {
            setUnionMap(ShareUnionListMgr.getInstance().getAllUnion());
        }
        CommLogD.info("[UnionBO.init] load UnionBO end]");
    }


    /**
     * 查找
     *
     * @param unionId 赛事Id
     * @return
     */
    public Union findUnion(long unionId) {
        if (Config.isShare()) {
            return ShareUnionListMgr.getInstance().getUnion(unionId);
        } else {
            return this.getUnionMap().get(unionId);
        }
    }

    /**
     * 查找共享赛事
     *
     * @param unionId 赛事Id
     * @return
     */
    public Union findUnionShare(long unionId) {
        return ShareUnionListMgr.getInstance().getUnion(unionId);
    }


    /**
     * 查找
     *
     * @param unionSign 赛事标识
     * @return
     */
    public Union findUnion(int unionSign) {
        if (Config.isShare()) {
            UnionBO unionBO = ContainerMgr.get().getComponent(UnionBOService.class).findOne(Restrictions.eq("unionSign", unionSign), null);
            if (unionBO != null) {
                return ShareUnionListMgr.getInstance().getUnion(unionBO.getId());
            } else {
                return null;
            }
        } else {
            return this.getUnionMap().values().stream().filter(k -> k.getUnionBO().getUnionSign() == unionSign).findAny().orElse(null);
        }
    }

    /**
     * 更新赛事房间配置
     *
     * @param unionId        赛事id
     * @param clubId         亲友圈id
     * @param pid            玩家id
     * @param unionRoomCfgId 玩法配置Id
     * @return
     */
    public SData_Result updateUnionRoomCfg(long unionId, long clubId, long pid, long unionRoomCfgId, int status) {
        Union union = this.findUnion(unionId);
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotManage(pid, clubId, unionId)) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE pid:{%d},clubId:{%d},unionId:{%d}", pid, clubId, unionId);
        }
        return union.createGameSetChange(unionRoomCfgId, status);
    }

    /**
     * 更新赛事房间配置 停用启用功能
     *
     * @param unionId        赛事id
     * @param clubId         亲友圈id
     * @param pid            玩家id
     * @param unionRoomCfgId 玩法配置Id
     * @return
     */
    public SData_Result updateUnionRoomCfgStopAndUse(long unionId, long clubId, long pid, long unionRoomCfgId, int status) {
        Union union = this.findUnion(unionId);
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotUnionManage(pid, clubId, unionId)) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE pid:{%d},clubId:{%d},unionId:{%d}", pid, clubId, unionId);
        }
        return union.createGameSetChangeStopAndUse(unionRoomCfgId, status);
    }

    /**
     * 保存勾选的玩法列表
     *
     * @param unionId       赛事id
     * @param clubId        亲友圈id
     * @param pid           玩家id
     * @param unionGameList 勾选玩法id列表
     * @return
     */
    public SData_Result saveUnionRoomCfgList(long unionId, long clubId, long pid, List<Long> unionGameList) {
        Union union = this.findUnion(unionId);
        if (null == union) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (club.getClubListBO().getUnionId() != unionId) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR club UnionId:{%d},unionId:{%d}", club.getClubListBO().getUnionId(), unionId);
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(pid, clubId, Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        clubMember.getClubMemberBO().saveUnionNotGameList(union.getRoomConfigBOMap().keySet().stream().filter(k -> !unionGameList.contains(k)).collect(Collectors.toList()));
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 获取联盟房间玩法列表
     *
     * @param unionId   赛事Id
     * @param classType 类型
     * @param pageNum   第几页
     * @return
     */
    public SData_Result getUnionRoomCfgList(long unionId, long clubId, long pid, int classType, int pageNum) {
        Union union = this.findUnion(unionId);
        if (null == union) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
        }
        if (club.getClubListBO().getUnionId() != unionId) {
            return SData_Result.make(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR club UnionId:{%d},unionId:{%d}", club.getClubListBO().getUnionId(), unionId);
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(pid, clubId, Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        return union.getUnionRoomCfgList(clubMember.getClubMemberBO().getUnionNotGameList(), classType, pageNum);
    }

    /**
     * 获取联盟房间玩法统计
     *
     * @param unionId   赛事Id
     * @param classType 类型
     * @return
     */
    public SData_Result getUnionRoomCfgCount(long unionId, int classType) {
        Union union = this.findUnion(unionId);
        if (null == union) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        return union.getUnionRoomCfgCount(classType);
    }

    /**
     * 获取指定房间配置信息
     *
     * @param unionId        赛事Id
     * @param unionRoomCfgId 房间配置Id
     * @return
     */
    public SData_Result getUnionRoomCfgInfo(long unionId, long unionRoomCfgId) {
        Union union = this.findUnion(unionId);
        if (null == union) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
        }
        return union.getUnionRoomCfgInfo(unionRoomCfgId);
    }


    /**
     * 设置赛事配置
     * 赛事管理员也可以进行赛事权限
     *
     * @param setConfig 设置赛事配置
     * @return
     */
    public SData_Result onUnionSetConfig(CUnion_SetConfig setConfig, Player player) {
        Union union = this.findUnion(setConfig.getUnionId());
        if (Objects.isNull(union)) {
            // 找不到赛事id
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST unionId:{%d}", setConfig.getUnionId());
        }
        //获取当前操作者的亲友圈成员信息
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(setConfig.getClubId(), player.getPid());
        //获取当前操作者的赛事圈成员信息
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(player.getPid(), setConfig.getClubId(), union.getUnionBO().getId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (Objects.isNull(unionMember)) {
            if (Objects.nonNull(execClubMember) && !execClubMember.isUnionMgr()) {
                // 	“联盟设置”页签仅盟主、联盟管理、联盟圈主可见；
                return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER pid:{%d},clubId:{%d},unionId:{%d}", player.getPid(), setConfig.getClubId(), setConfig.getUnionId());
            }
        }
        if (Objects.nonNull(execClubMember) && !execClubMember.isUnionMgr() && !unionMember.isManage()) {
            return SData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE pid:{%d},clubId:{%d},unionId:{%d}", player.getPid(), setConfig.getClubId(), setConfig.getUnionId());
        }
        if (!BooleanUtils.and(new Boolean[]{
                UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.JOIN_LIMIT, setConfig.getJoin()),
                UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.QUIT_LIMIT, setConfig.getQuit())
        })) {
            return SData_Result.make(ErrorCode.InvalidParam, "InvalidParam");
        }
        if (execClubMember.isUnionMgr() || UnionDefine.UNION_POST_TYPE.UNION_CREATE.value() == unionMember.getUnionMemberBO().getType()) {
            if (!UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.RANKING_LIMIT, setConfig.getRanking())) {
                // 排名错误
                return SData_Result.make(ErrorCode.UNION_RANKING_ERROR, "UNION_RANKING_ERROR");
            }
            if (!UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.INIT_SPORTS_LIMIT, (int) setConfig.getInitSports())) {
                // 裁判力度错误
                return SData_Result.make(ErrorCode.UNION_INIT_SPORTS_ERROR, "UNION_INIT_SPORTS_ERROR");
            }
            if (!UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.PRIZE_TYPE_LIMIT, setConfig.getPrizeType())) {
                // 消耗类型
                return SData_Result.make(ErrorCode.UNION_PRIZE_TYPE_ERROR, "PRIZE_TYPE_LIMIT");
            }
            if (!UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.VALUE_LIMIT, setConfig.getValue())) {
                // 数量错误
                return SData_Result.make(ErrorCode.UNION_VALUE_ERROR, "VALUE_LIMIT");
            }
            if (StringUtils.isEmpty(setConfig.getName())) {
                // 赛事名称错误
                return SData_Result.make(ErrorCode.UNION_NAME_ERROR, "UNION_NAME_ERROR");
            }
            if (!UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.NAME_LIMIT, setConfig.getName().length())) {
                // 赛事名称错误
                return SData_Result.make(ErrorCode.UNION_NAME_ERROR, "NAME_LIMIT");
            }
            if (setConfig.getOutSports() > setConfig.getInitSports() && UnionDefine.UNION_TYPE.NORMAL.value() == union.getUnionBO().getUnionType()) {
                // 淘汰分不能大于 裁判力度
                return SData_Result.make(ErrorCode.UNION_OUT_SPORTS_ERROR, "UNION_OUT_SPORTS_ERROR");
            }
            if (!UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.JOIN_CLUB_SAME_UNION, setConfig.getJoinClubSameUnion())) {
                // 允许亲友圈添加同赛事玩家
                return SData_Result.make(ErrorCode.UNION_JOIN_CLUB_SAME_UNION, "UNION_JOIN_CLUB_SAME_UNION");
            }
            String unionName = setConfig.getName();
            if (!union.getUnionBO().getName().equals(setConfig.getName())) {
                // 检查名称是否存在
                unionName = SensitiveWordMgr.getInstance().replaceSensitiveWordMax(setConfig.getName());
                if (this.checkNameExist(unionName)) {
                    return SData_Result.make(ErrorCode.UNION_NAME_EXIST, "UNION_NAME_EXIST");
                }
            }
            if (!BooleanUtils.and(new Boolean[]{
                    UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.STATE_LIMIT, setConfig.getState()),
                    UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.MATCH_RATE_LIMIT, setConfig.getMatchRate())})) {
                return SData_Result.make(ErrorCode.InvalidParam, "InvalidParam");
            }
            if (UnionDefine.UNION_STATE.isEnable(union.getUnionBO().getState())) {
                // 赛事排名奖励
                SData_Result result = checkUnionRankingReward(new UnionRankingReward(union.getUnionBO().getPrizeType(), union.getUnionBO().getRanking(), union.getUnionBO().getValue()), setConfig, player);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    return result;
                }
                //奖励修改后进行一次判断钻石消耗是否不足
//                union.checkDiamondsAttention();
            }
            // 旧裁判力度
            double oldInitSports = union.getUnionBO().getInitSports();
            if (oldInitSports != setConfig.getInitSports()) {
                // 更新修改裁判力度
                this.updateInitSports(player.getPid(), setConfig.getUnionId(), setConfig.getClubId(), oldInitSports - setConfig.getInitSports());
            }
            int oldUnionState = union.getUnionBO().getStateValue();
            if (oldUnionState != setConfig.getState()) {
                SData_Result result = this.execUnionState(union, setConfig, UnionDefine.UNION_STATE.isEnable(setConfig.getState()));
                if (!ErrorCode.Success.equals(result.getCode())) {
                    return result;
                }
            }

            // 淘汰分
            double outSports = CommMath.FormatDouble(setConfig.getOutSports());
            // 旧淘汰分
            double oldOutSports = union.getUnionBO().getOutSports();
            union.setUnionSetConfigInfo(unionName, setConfig.getJoin(), setConfig.getQuit(), setConfig.getTableNum(), setConfig.getExpression(), setConfig.getState(), setConfig.getSports(), setConfig.getInitSports(), setConfig.getMatchRate(), outSports, setConfig.getPrizeType(), setConfig.getRanking(), setConfig.getValue(), player.getPid(), setConfig.getJoinClubSameUnion());
            if (oldOutSports != outSports) {
                // 淘汰分改变通知
                UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(setConfig.getUnionId(), SUnion_OutSportsPoint.make(setConfig.getUnionId(), outSports));
            }
        } else {
            union.setUnionSetConfigInfo(null, setConfig.getJoin(), setConfig.getQuit(), setConfig.getTableNum(), setConfig.getExpression(), -1, setConfig.getSports(), union.getUnionBO().getInitSports(), union.getUnionBO().getMatchRateValue(), union.getUnionBO().getOutSports(), union.getUnionBO().getPrizeType(), union.getUnionBO().getRanking(), union.getUnionBO().getValue(), player.getPid(), setConfig.getJoinClubSameUnion());
        }
        //修改共享赛事
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().addUnion(union);
        }
        return SData_Result.make(ErrorCode.Success, union.getUnionSetConfigInfo());
    }

    /**
     * 赛事排名奖励
     *
     * @param data      旧奖励配置
     * @param setConfig 新奖励配置
     * @param player    玩家
     * @return
     */
    private SData_Result checkUnionRankingReward(UnionRankingReward data, CUnion_SetConfig setConfig, Player player) {
        if (setConfig.getPrizeType() == data.getPrizeType().value() && data.getValue() == setConfig.getValue() && data.getRanking() == setConfig.getRanking()) {
            return SData_Result.make(ErrorCode.Success);
        }
        if (data.getPrizeType().value() != setConfig.getPrizeType()) {
            // 检查比赛奖励消耗
            SData_Result result = this.unionReward(player, new UnionRankingReward(setConfig.getPrizeType(), setConfig.getRanking(), setConfig.getValue()));
            if (!ErrorCode.Success.equals(result.getCode())) {
                return result;
            }
            player.getFeature(PlayerCurrency.class).gainItemFlow(data.getPrizeType(), (data.getValue() * data.getRanking()), ItemFlow.UNION_MATCH_REWARD, ConstEnum.ResOpType.Fallback);
            return result;
        }
        // 旧奖励值
        int oldValue = data.getValue() * data.getRanking();
        // 新奖励值
        int newValue = setConfig.getValue() * setConfig.getRanking();
        if (newValue == oldValue) {
            // 奖励值相等
            return SData_Result.make(ErrorCode.Success);
        }
        // 新旧奖励值差
        int value = oldValue - newValue;
        if (oldValue > newValue) {
            // 旧的大于新的，回退
            player.getFeature(PlayerCurrency.class).gainItemFlow(data.getPrizeType(), value, ItemFlow.UNION_MATCH_REWARD, ConstEnum.ResOpType.Fallback);
        } else {
            // 新的大于旧的，消耗
            if (!player.getFeature(PlayerCurrency.class).checkAndConsumeItemFlow(data.getPrizeType(), Math.abs(value), ItemFlow.UNION_MATCH_REWARD)) {
                return SData_Result.make(ErrorCode.NotEnough_Currency, "checkAndConsumeItemFlow NotEnough_Currency");
            }
        }

        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 更新修改裁判力度
     */
    private void updateInitSports(long pid, long unionId, long clubId, double value) {
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(pid, clubId, Club_define.Club_Player_Status.PLAYER_JIARU);
        if (Objects.nonNull(clubMember)) {
            if (value > 0D) {
                clubMember.getClubMemberBO().execSportsPointInit(unionId, -value, ItemFlow.UNION_INIT_SPORTS);
            } else {
                clubMember.getClubMemberBO().execSportsPointInit(unionId, Math.abs(value), ItemFlow.UNION_INIT_SPORTS);
            }
        } else {
            CommLog.error("updateInitSports Pid:{},unionId:{},clubId:{},value:{}", pid, unionId, clubId, value);
        }
    }


    private SData_Result execUnionState(Union union, CUnion_SetConfig setConfig, boolean isOpen) {
        if (isOpen) {
            // 启动赛事
            return execUnionStateEnable(union, setConfig);
        } else {
            // 停止赛事
            return execUnionStateStop(union, setConfig);
        }
    }

    /**
     * 执行赛事状态启用
     *
     * @param union     赛事
     * @param setConfig 设置配置
     * @return
     */
    private SData_Result execUnionStateEnable(Union union, CUnion_SetConfig setConfig) {
        // 赛事排名奖励
        SData_Result result = this.unionReward(union.getOwnerPlayer(), new UnionRankingReward(setConfig.getPrizeType(), setConfig.getRanking(), setConfig.getValue()));
        if (ErrorCode.Success.equals(result.getCode())) {
            // 更新本轮的开始、结束时间
            union.getUnionBO().saveRoundTime();
            UnionMgr.getInstance().getUnionMemberMgr().notify2AllByUnion(union.getUnionBO().getId(), SUnion_StateChange.make(union.getUnionBO().getId(), UnionDefine.UNION_STATE.UNION_STATE_ENABLE.ordinal(), union.getUnionBO().getEndRoundTime()));
        }
        return result;
    }

    /**
     * 执行赛事状态停止
     *
     * @param union     赛事
     * @param setConfig 设置配置
     * @return
     */
    private SData_Result execUnionStateStop(Union union, CUnion_SetConfig setConfig) {
        return SData_Result.make(ErrorCode.Success, union.execUnionStateStop(setConfig));
    }

    /**
     * 获取赛事配置
     *
     * @param getConfig 获取赛事配置
     * @return
     */
    public SData_Result onUnionGetConfig(CUnion_GetConfig getConfig, long pid) {
        Union union = this.findUnion(getConfig.getUnionId());
        if (null == union) {
            // 找不到赛事id
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST unionId:{%d}", getConfig.getUnionId());
        }
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(pid, getConfig.getClubId(), union.getUnionBO().getId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == unionMember) {
            ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(getConfig.getClubId(), pid);
            if (Objects.isNull(clubMember)) {
                return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "clubMember CLUB_NOTCLUBMEMBER");
            }
            //如果是赛事管理员
            if (clubMember.getClubMemberBO().getIsminister() == Club_define.Club_MINISTER.Club_MINISTER_UNIONMGR.value()) {
                return SData_Result.make(ErrorCode.Success, union.getUnionSetConfigInfo());
            }
            // 	“联盟设置”页签仅盟主、联盟管理、联盟圈主可见；
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER pid:{%d},clubId:{%d},unionId:{%d}", pid, getConfig.getClubId(), getConfig.getUnionId());
        }
        return SData_Result.make(ErrorCode.Success, union.getUnionSetConfigInfo());
    }


    /**
     * 创建赛事
     */
    @SuppressWarnings("rawtypes")
    public SData_Result onUnionCreateTest(CUnion_Create data, Club club, Player player) {
        if (club.getClubListBO().getUnionId() > 0) {
            return SData_Result.make(ErrorCode.UNION_EXIST_ADD_OTHERS_UNION, "UNION_EXIST_ADD_OTHERS_UNION");
        }
        if (data.getInitSports() < 0D) {
            return SData_Result.make(ErrorCode.UNION_INIT_SPORTS_ERROR, "UNION_CREATE_INIT_SPORTS_ERROR");
        }
        if (data.getOutSports() > data.getInitSports()) {
            return SData_Result.make(ErrorCode.UNION_OUT_SPORTS_ERROR, "UNION_CREATE_OUT_SPORTS_ERROR");
        }
        if (!UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.RANKING_LIMIT, data.getRanking())) {
            // 排名错误
            return SData_Result.make(ErrorCode.UNION_RANKING_ERROR, "UNION_RANKING_ERROR");
        }
        if (!UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.INIT_SPORTS_LIMIT, (int) data.getInitSports())) {
            // 裁判力度错误
            return SData_Result.make(ErrorCode.UNION_INIT_SPORTS_ERROR, "UNION_INIT_SPORTS_ERROR");
        }
        if (!UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.PRIZE_TYPE_LIMIT, data.getPrizeType())) {
            // 消耗类型
            return SData_Result.make(ErrorCode.UNION_PRIZE_TYPE_ERROR, "PRIZE_TYPE_LIMIT");
        }
        if (!UnionDefine.UNION_CREATE_LIMIT.checkLimit(UnionDefine.UNION_CREATE_LIMIT.VALUE_LIMIT, data.getValue())) {
            // 数量错误
            return SData_Result.make(ErrorCode.UNION_VALUE_ERROR, "VALUE_LIMIT");
        }
        // 空字符
        if (StringUtils.isEmpty(data.getUnionName())) {
            return SData_Result.make(ErrorCode.UNION_NAME_ERROR, "UNION_NAME_ERROR");
        }
        // 获取字符长度 中文算两个字符 英文一个
        int length = StringUtil.String_length(data.getUnionName());
        if (length > 16) {
            return SData_Result.make(ErrorCode.UNION_NAME_ERROR, "UNION_NAME_ERROR 16");
        }
        String unionName = SensitiveWordMgr.getInstance().replaceSensitiveWordMax(data.getUnionName());
        // 检查名称是否存在
        if (this.checkNameExist(unionName)) {
            return SData_Result.make(ErrorCode.UNION_NAME_EXIST, "UNION_NAME_EXIST");
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), club.getClubListBO().getId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        // 检查比赛奖励消耗
        SData_Result result = this.unionReward(player, new UnionRankingReward(data.getPrizeType(), data.getRanking(), data.getValue()));
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        UnionBO unionBO = new UnionBO();
        // 标识
        unionBO.setUnionSign(CurrencyKeyMgr.getInstance().getNewKey());
        // 名称
        unionBO.setUnionName(unionName);
        // 盟主
        unionBO.setOwnerId(player.getPid());
        // 亲友圈id
        unionBO.setClubId(club.getClubListBO().getId());
        // 创建时间
        unionBO.setCreateTime(CommTime.nowSecond());
        // 赛事频率
        unionBO.setMatchRate(data.getMatchRate());
        // 退出申请(0需要审核、1不需要审核)
        unionBO.setQuit(data.getQuit());
        // 加入申请(0需要审核、1不需要审核)
        unionBO.setJoin(data.getJoin());
        // 裁判力度
        unionBO.setInitSports(data.getInitSports());
        ////钻石提醒 全员默认100
        unionBO.setUnionDiamondsAttentionAll(100);
        //钻石提醒 管理默认500
        unionBO.setUnionDiamondsAttentionMinister(500);
        // 赛事淘汰
        unionBO.setOutSports(data.getOutSports());
        // 排名前50名
        unionBO.setRanking(data.getRanking());
        // 消耗类型(1-金币,2-房卡)
        unionBO.setPrizeType(data.getPrizeType());
        // 数量
        unionBO.setValue(data.getValue());
        // 新赛事
        unionBO.setNewUnionTime(CommTime.nowSecond());
        unionBO.setRoundTime();
        unionBO.setCaseStatus(1);
        unionBO.setShareStatus(1);
        long unionId = unionBO.getBaseService().saveIgnoreOrUpDate(unionBO);
        if (unionId <= 0L) {
            PrizeTypeItem item = (PrizeTypeItem) result.getData();
            player.getFeature(PlayerCurrency.class).gainItemFlow(item.getPrizeType(), item.getValue(), ItemFlow.UNION_MATCH_REWARD, ConstEnum.ResOpType.Fallback);
            return SData_Result.make(ErrorCode.ErrorSysMsg, "创建赛事失败");
        }
        Union union = new Union(unionBO);
        this.getUnionMap().put(unionBO.getId(), union);
        //共享赛事
        if (Config.isShare()) {
            ShareUnionListMgr.getInstance().addUnion(union);
        }
        clubMember.getClubMemberBO().execSportsPointInit(unionBO.getId(), data.getInitSports(), ItemFlow.UNION_INIT_SPORTS);
        clubMember.getClubMemberBO().saveShareTypeInit();//联盟盟主设置分成固定
        UnionMgr.getInstance().getUnionMemberMgr().onInsertUnionMember(player, union, club.getClubListBO().getId(), UnionDefine.UNION_POST_TYPE.UNION_CREATE.value(), player.getPid(), clubMember.getId());
        //初始化联盟区间分成数据
        union.initUnionShareSection();
        return SData_Result.make(ErrorCode.Success);

    }

    /**
     * 比赛奖励
     *
     * @param player 玩家信息
     * @param data
     * @return
     */
    private SData_Result unionReward(Player player, UnionRankingReward data) {
        // 排名限制
        PrizeType prizeType = data.getPrizeType();
        if (data.getValue() < 0) {
            // 数量不符合
            return SData_Result.make(ErrorCode.UNION_VALUE_ERROR, "UNION_CREATE_VALUE_ERROR");
        }
        if (data.getRanking() < 0) {
            // 排名错误
            return SData_Result.make(ErrorCode.UNION_RANKING_ERROR, "UNION_CREATE_RANKING_ERROR");
        }
        if (PrizeType.None.equals(prizeType)) {
            // 没有消耗类型
            return SData_Result.make(ErrorCode.UNION_PRIZE_TYPE_ERROR, "UNION_CREATE_PRIZE_TYPE_ERROR");
        }
        int ranking = data.getRanking() >= UnionDefine.UNION_CREATE_LIMIT.RANKING_LIMIT.getMax() ? UnionDefine.UNION_CREATE_LIMIT.RANKING_LIMIT.getMax() : data.getRanking();
        int sum = data.getValue() * ranking;
        if (player.getFeature(PlayerCurrency.class).checkAndConsumeItemFlow(prizeType, sum, ItemFlow.UNION_MATCH_REWARD)) {
            return SData_Result.make(ErrorCode.Success, new PrizeTypeItem(prizeType, sum));
        } else {
            return SData_Result.make(ErrorCode.NotEnough_Currency, "NotEnough_Currency");
        }
    }


    /**
     * 加入赛事
     *
     * @param player 玩家信息
     * @param data   加入key
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result onJoinUnion(Player player, CUnion_Join data) {
        // 查找指定的赛事信息
        Union union = this.findUnion(data.getUnionSign());
        if (null == union) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST UnionSign:{%d}", data.getUnionSign());
        }
        // 已经加入赛事
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(data.getClubId());
        if (null == club) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST clubId:{%d}", data.getClubId());
        }
        if (club.getClubListBO().getUnionId() > 0L) {
            // 已加入赛事，不能再加入
            return SData_Result.make(ErrorCode.UNION_EXIST_ADD_OTHERS_UNION, "UNION_EXIST_ADD_OTHERS_UNION unionId():{%d}", club.getClubListBO().getUnionId());
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), club.getClubListBO().getId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            return SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
        }
        UnionMember member = UnionMgr.getInstance().getUnionMemberMgr().find(player.getPid(), data.getClubId(), union.getUnionBO().getId());
        if (null != member) {
            if (member.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())) {
                // 玩家已加入本赛事
                return SData_Result.make(ErrorCode.UNION_PLAYER_JIARU, "UNION_PLAYER_JIARU");
            } else if (member.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN.value())) {
                // 玩家已申请加入赛事,等待管理员批准中
                return SData_Result.make(ErrorCode.UNION_PLAYER_WEIPIZHUN, "UNION_PLAYER_WEIPIZHUN");
            }
        } else {
            if (UnionMgr.getInstance().getUnionMemberMgr().checkUnionMemberUpperLimit(union.getUnionBO().getId(), data.getClubId())) {
                // 俱乐部人数已满
                return SData_Result.make(ErrorCode.UNION_MEMBER_UPPER_LIMIT, "UNION_MEMBER_UPPER_LIMIT");
            } else if (UnionMgr.getInstance().getUnionMemberMgr().checkPlayerUnionUpperLimit(union.getUnionBO().getId(), data.getClubId(), player.getPid())) {
                // 自己加入的俱乐部数达到上限
                return SData_Result.make(ErrorCode.UNION_PLAYER_UPPER_LIMIT, "UNION_PLAYER_UPPER_LIMIT");
            }
        }
        UnionDefine.UNION_JOIN join = UnionDefine.UNION_JOIN.valueOf(union.getUnionBO().getJoin());
        if (UnionDefine.UNION_JOIN.UNION_JOIN_NEED_AUDIT.equals(join)) {
            if (UnionMgr.getInstance().getUnionMemberMgr().onJoin(player, data.getClubId(), union.getUnionBO().getId(), union.getUnionBO().getOwnerId(), clubMember.getId())) {
                // 未受到批准
                return SData_Result.make(ErrorCode.Success, UnionDefine.UNION_PLAYER_STATUS.PLAYER_WEIPIZHUN.value());
            }
        } else {
            if (UnionMgr.getInstance().getUnionMemberMgr().onInsertUnionMember(player, union, data.getClubId(), UnionDefine.UNION_POST_TYPE.UNION_CLUB.value(), 0L, clubMember.getId())) {
                // 已加入赛事
                return SData_Result.make(ErrorCode.Success, UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value());
            }
        }
        return SData_Result.make(ErrorCode.UNION_JOIN_FAIL, "UNION_JOIN_FAIL");
    }


    /**
     * 拒绝赛事邀请
     *
     * @param player 玩家信息
     * @param data   加入key
     * @return
     */
    @SuppressWarnings("rawtypes")
    public void onUnionRefuseInvitation(Player player, CUnion_Join data) {
        // 查找指定的赛事信息
        Union union = this.findUnion(data.getUnionSign());
        if (null == union) {
            return;
        }
        // 已经加入赛事
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(data.getClubId());
        if (null == club) {
            return;
        }
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(player.getPid(), club.getClubListBO().getId(), union.getUnionBO().getId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_YAOQING);
        if (null != unionMember) {
            unionMember.setStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JUJIEYAOQING.value(), 0L);
        }
    }


    /**
     * 检查名称是否存在
     *
     * @param clubName 赛事名称
     * @return T:存在重复，F:不存在
     */
    public boolean checkNameExist(String clubName) {
        if (Config.isShare()) {
            UnionBO unionBO = ContainerMgr.get().getComponent(UnionBOService.class).findOne(Restrictions.eq("name", clubName), null);
            if (unionBO != null) {
                return true;
            } else {
                return false;
            }

        } else {
            // 检查是否有赛事数据。
            if (null == getUnionMap() || getUnionMap().size() <= 0) {
                return false;
            }
            return getUnionMap().values().stream().filter(k -> null != k && clubName.equals(k.getUnionBO().getName())).findAny().isPresent();
        }

    }


    /**
     * 赛事房间配置
     *
     * @param dateTime
     */
    public void unionRoomConfigPrizePoolLog(String dateTime) {
        Map<Long, Union> unionMap;
        if (Config.isShare()) {
            unionMap = ShareUnionListMgr.getInstance().getAllUnion();
        } else {
            unionMap = this.getUnionMap();
        }
        for (Map.Entry<Long, Union> entry : unionMap.entrySet()) {
            if (Objects.isNull(entry.getValue())) {
                continue;
            }
            entry.getValue().getRoomConfigBOMap().entrySet().forEach(k -> {
                if (Objects.nonNull(k.getValue()) && k.getValue().isExistUnionRoomConfig()) {
                    FlowLogger.roomConfigPrizePoolLog(dateTime, k.getKey(), 0, 0, 0, 0, entry.getKey(), k.getValue().getbRoomConfigure().getBaseCreateRoom().getRoomName(), new Gson().toJson(k.getValue().getbRoomConfigure().getBaseCreateRoomT()), 0, k.getValue().getbRoomConfigure().getGameType().getId(), 0, 0);
                }
            });
        }
    }

    /**
     * 切换变更盟主状态
     *
     * @param unionId
     * @param type
     * @return
     */
    public SData_Result changeAllyLeader(long unionId, int type) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "null == union unionId:{%d} ", unionId);
        }
        if (type == UnionDefine.UNION_CASE_STATUS.OPEN.ordinal()) {
            union.getUnionBO().saveChangeAllyLeader(1);
        } else if (type == UnionDefine.UNION_CASE_STATUS.CLOSE.ordinal()) {
            union.getUnionBO().saveChangeAllyLeader(0);
        }
        ShareUnionListMgr.getInstance().addUnion(union);
        return SData_Result.make(ErrorCode.Success);

    }

    /**
     * 修改联赛保险箱功能
     *
     * @param unionId
     * @param exePid
     * @return
     */
    public SData_Result changeCaseStatus(long unionId, long exePid, int type) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "null == union unionId:{%d} pid :{%d}", unionId, exePid);
        }
        if (union.isCaseStatusChange()) {
            return SData_Result.make(ErrorCode.UNION_IS_CLOSE_CASE, "union.isCaseStatusChange() unionId:{%d} pid :{%d}", unionId, exePid);
        }
        if (type == UnionDefine.UNION_CASE_STATUS.OPENSHARE.ordinal()) {
            union.getUnionBO().saveCaseStatus(1);
            union.getUnionBO().saveShareStatus(1);
        } else if (type == UnionDefine.UNION_CASE_STATUS.OPEN.ordinal()) {
            union.getUnionBO().saveCaseStatus(1);
        } else if (type == UnionDefine.UNION_CASE_STATUS.CLOSE.ordinal()) {
            union.getUnionBO().saveShareStatus(0);
            union.getUnionBO().saveCaseStatus(0);
            //todo 关闭的时候保险箱内的分数要移动出来
            union.setCaseStatusChange(true);
            DispatcherComponent.getInstance().publish(new UnionCloseCase(unionId));
        }
        ShareUnionListMgr.getInstance().addUnion(union);
        return SData_Result.make(ErrorCode.Success);

    }

    /**
     * 修改联赛类型
     *
     * @param unionId
     * @return
     */
    public SData_Result changeUnionType(long unionId, int type) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "null == union unionId:{%d} ", unionId);
        }
        if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()))) {
            return SData_Result.make(ErrorCode.NotAllow, "UNION_TYPE isZhongZhi ");
        }
        if (UnionDefine.UNION_TYPE.ZhongZhi.equals(UnionDefine.UNION_TYPE.valueOf(type))) {
            union.getUnionBO().saveUnionType(type);
            // 旧裁判力度
            double oldInitSports = union.getUnionBO().getInitSports();
            if (oldInitSports != 0) {
                // 更新修改裁判力度
                this.updateInitSports(union.getUnionBO().getOwnerId(), unionId, union.getUnionBO().getClubId(), oldInitSports - 0);
            }
            //如果是中至 改变整个联盟的所有的个人淘汰分
            Map<Long, UnionMember> unionMemberMap;
            if (Config.isShare()) {
                unionMemberMap = ShareUnionMemberMgr.getInstance().getAllOneUnionMember(unionId);
            } else {
                unionMemberMap = UnionMgr.getInstance().getUnionMemberMgr().getUnionMemberMap();
            }
            unionMemberMap.values().stream()
                    .filter(k -> null != k && k.getUnionMemberBO().getUnionId() == unionId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value()))
                    .forEach(k -> {
                        Club club = ClubMgr.getInstance().getClubListMgr().findClub(k.getUnionMemberBO().getClubId());
                        if (Objects.nonNull(club)) {
                            ClubMgr.getInstance().getClubMemberMgr().findClubIdAllClubMember(club.getClubListBO().getId(), Club_define.Club_Player_Status.PLAYER_JIARU.value()).stream().forEach(l -> {
                                l.getClubMemberBO().saveEliminatePoint(union.getUnionBO().getOutSports());
                            });
                        }
                    });
            ShareUnionListMgr.getInstance().addUnion(union);
        } else {
            //变换其他类型 暂时预留
        }
        return SData_Result.make(ErrorCode.Success);

    }

    /**
     * 修改联赛审核功能
     *
     * @param unionId
     * @param exePid
     * @return
     */
    public SData_Result changeExamineStatus(long unionId, long exePid, int type) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "null == union unionId:{%d} pid :{%d}", unionId, exePid);
        }
        union.getUnionBO().saveExamineStatus(type);
        return SData_Result.make(ErrorCode.Success);

    }

    /**
     * 修改联赛隐藏功能
     *
     * @param unionId
     * @return
     */
    public SData_Result changeHideStatus(long unionId, int type) {
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
        if (Objects.isNull(union)) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "null == union unionId:{%d} ", unionId);
        }
        union.getUnionBO().saveHideStatus(type);
        return SData_Result.make(ErrorCode.Success);

    }
}
