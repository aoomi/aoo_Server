package business.global.room.type;

import BaseCommon.CommLog;
import business.global.club.ClubMember;
import business.global.config.LuckDrawConfigMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.RoomTyepImpl;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerLuckDraw;
import business.player.feature.PlayerUnionRoom;
import cenum.LuckDrawEnum;
import cenum.RoomSortedEnum;
import cenum.RoomTypeEnum;
import cenum.room.CKickOutType;
import cenum.room.PaymentRoomCardType;
import cenum.room.RoomQiePaiEnum;
import cenum.room.RoomState;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import core.db.entity.clarkGame.ClubMemberBO;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.luckdraw.AssignCrowdAndConditionItem;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.RoomInfoItem;
import jsproto.c2s.cclass.room.UnionRoomConfig;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.room._SportsPointEnough;
import jsproto.c2s.iclass.room._SportsPointNotEnough;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 赛事房间信息
 *
 * @author
 */
@Data
public class UnionRoom extends RoomTyepImpl {
    /**
     * 2分钟
     */
    private static final int MINUTES_3 = CommTime.MinSec * 2 + 3;
    /**
     * 赛事信息
     */
    private Union union = null;
    /**
     * 比赛分不足倒计时
     */
    private int sportsPointEnoughCountdownTime;

    public UnionRoom(AbsBaseRoom room) {
        super(room, room.getBaseRoomConfigure().getBaseCreateRoom());
        this.initRoom();
    }


    /**
     * 清除
     */
    @Override
    public void clear() {
        super.clear();
        this.setUnion(null);
    }

    /**
     * 初始化赛事
     */
    @Override
    public void initRoom() {
        UnionRoomConfig unionRoomConfig = this.getRoom().getBaseRoomConfigure().getUnionRoomCfg();
        if (null != unionRoomConfig) {
            if (Config.isShare()) {
                this.setUnion(UnionMgr.getInstance().getUnionListMgr().findUnionShare(this.getBaseCreateRoom().getUnionId()));
            } else {
                // 设置赛事信息
                this.setUnion(UnionMgr.getInstance().getUnionListMgr().findUnion(this.getBaseCreateRoom().getUnionId()));
            }
        }

    }

    /**
     * 名字
     *
     * @return
     */
    @Override
    public String name() {
        if (Objects.nonNull(getUnion())) {
            return getUnion().getUnionBO().getName();
        }
        return null;
    }

    /**
     * 获取赛事信息
     *
     * @return
     */
    @Override
    public Object getSpecialRoom() {
        if (Config.isShare()) {
            // 获取赛事
            this.setUnion(UnionMgr.getInstance().getUnionListMgr().findUnion(this.union.getUnionBO().getId()));
            // 返回
            return this.union;
        } else {
            // 检查赛事是否存在
            if (null != this.union) {
                return this.union;
            }
            long unionId = 0L;
            if (unionId <= 0L) {
                return null;
            }
            // 获取赛事
            this.setUnion(UnionMgr.getInstance().getUnionListMgr().findUnion(unionId));
            // 返回
            return this.union;
        }
    }

    /**
     * 检查赛事是否存在
     *
     * @return
     */
    @Override
    public boolean checkSpecialRoom() {
        return null != this.getSpecialRoom();
    }

    @Override
    public long getSpecialRoomId() {
        if (Objects.isNull(this.getUnion())) {
            CommLog.error("UnionRoom getSpecialRoomId");
            return 0L;
        }
        return this.getUnion().getUnionBO().getId();
    }

    /**
     * 赛事创建新房间
     */
    @Override
    public void createNewSetRoom() {
        if (this.isLock()) {
            return;
        }
        this.setLock(true);
        this.getUnion().createNewSetRoom(getConfigId(), this.getRoom().getBaseRoomConfigure(), getRoom().getRoomKey());
    }


    /**
     * 赛事房间人员状态发生改变
     *
     * @param roomPos
     */
    @Override
    public void roomPlayerChange(AbsRoomPos roomPos) {
        this.getUnion().onUnionRoomPlayerChange(this.getRoom(), this.getBaseCreateRoom().getGameIndex(),
                roomPos.getRoomPosInfoShort(), this.getRoom().sorted());
    }


    /**
     * 移除赛事房间信息
     */
    @Override
    public void onRoomRemove() {
        this.getUnion().onUnionRoomRemove(this.getBaseCreateRoom().getGameIndex(), this.getRoom().getRoomKey(),
                onRoomRemoveSort());
    }

    private int onRoomRemoveSort() {
        return !existNotEmptyPos() && getRoom().getCurSetID() <= 0 ? RoomSortedEnum.NONE_ROOM.ordinal() :
                RoomSortedEnum.GAME_PLAYING.ordinal();
    }

    /**
     * 赛事房间局数改变
     */
    @Override
    public void roomSetIDChange() {
        this.getUnion().roomSetIDChange(this.getUnion().getUnionBO().getId(), this.getBaseCreateRoom().getGameIndex()
                , this.getRoom().getRoomKey(), this.getRoom().getCurSetID(), this.getRoom().sorted());
    }

    /**
     * 通知房间开始改变
     */
    @Override
    public void roomStartChange() {
        this.getUnion().roomStartChange(this.getUnion().getUnionBO().getId(), this.getBaseCreateRoom().getGameIndex()
                , this.getRoom().getRoomKey());
    }

    @Override
    public void giveBackRoomCard() {
        // 赛事房主付
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getPaymentRoomCardType()) {
            if (!business.global.billing.RoomBillingGuard.beginRefund(
                    this.getRoom().getRoomID(), this.getSpecialRoomId())) return;
            // 回退赛事消耗房卡
            this.getUnion().gainUnionRoomCard(this.getRoom().getBaseRoomConfigure(),
                    UnionDefine.UNION_OPERATION_STATUS.UNION_OPERATION_STATUS_GAME);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public String doChangePlayerNum(BaseRoomConfigure newConfigure, String roomKey) {
        SData_Result result =
                this.getUnion().getOwnerPlayer().getFeature(PlayerUnionRoom.class).createUnionRoom(newConfigure,
                        roomKey, getSpecialRoomId());
        if (ErrorCode.Success.equals(result.getCode())) {
            return roomKey;
        }
        return null;
    }

    @Override
    public boolean checkDoChangeRefRoomCost(Player player, PaymentRoomCardType paymentRoomCardType, int cost,
                                            int roomCard) {
        // 赛事
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.equals(paymentRoomCardType)) {
            // 赛事创建者付房费
            // 获取赛事创建者信息
            player = getUnion().getOwnerPlayer();
            cost = this.getRoom().getBaseRoomConfigure().getUnionRoomCfg().getRoomCard() - roomCard;
            // 检查旧配置转新配置房卡够用吗？
            if (this.getRoom().getBaseRoomConfigure().getUnionRoomCfg().getRoomCard() == 0 || cost >= 0) {
                return true;
            } else {
                // 不够用,检查创造者身上房卡是否足够补充。
                return player.getFeature(PlayerWallet.class).check(Math.abs(cost));
            }
        }
        return false;
    }


    /**
     * 系统维护时强制解散
     */
    @Override
    public void doForceDissolve() {
        this.getRoom().doDissolveRoom();
    }

    /**
     * 获取总消耗数
     *
     * @return
     */
    @Override
    public int sumConsume() {
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getPaymentRoomCardType()) {
            // 消耗赛事房主的房卡。
            return this.getRoom().getBaseRoomConfigure().getUnionRoomCfg().getRoomCard();
        }
        return this.getRoom().getRoomPosMgr().notHomeOwerPaySumConsume();
    }

    /**
     * 房间类型
     */
    @Override
    public RoomTypeEnum getRoomTypeEnum() {
        return RoomTypeEnum.UNION;
    }

    /**
     * 踢出房间
     *
     * @param pid      用户ID
     * @param posIndex 位置
     */
    @SuppressWarnings("rawtypes")
    @Override
    public SData_Result kickOut(long pid, int posIndex, String msg) {
        try {
            this.getRoom().lock();
            if (!RoomState.Init.equals(this.getRoom().getRoomState())) {
                // 房间不处于初始阶段
                return SData_Result.make(ErrorCode.NotAllow, "kickOut RoomState Init :{%s}",
                        this.getRoom().getRoomState());
            }
            AbsRoomPos roomPos = this.getRoom().getRoomPosMgr().getPosByPosID(posIndex);
            if (null == roomPos) {
                // 找不到指定的位置信息
                return SData_Result.make(ErrorCode.NotAllow, "kickOut null == roomPos posIndex:{%d}", posIndex);
            }
            roomPos.leave(true, pid, CKickOutType.SPECIAL, msg);
        } finally {
            this.getRoom().unlock();
        }
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 继续游戏
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    @Override
    public SData_Result continueGame(long pid) {
        try {
            this.getRoom().lock();
            if (!RoomState.Playing.equals(this.getRoom().getRoomState())) {
                // 房间不处于游戏阶段
                return SData_Result.make(ErrorCode.NotAllow, "continueGame RoomState Playing :{%s}",
                        this.getRoom().getRoomState());
            }
            AbsRoomPos roomPos = this.getRoom().getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                // 找不到通过pid获取玩家信息
                return SData_Result.make(ErrorCode.NotAllow, "continueGame null == roomPos");
            }
            if (this.getRoom().existDissolveRoom()) {
                return SData_Result.make(ErrorCode.ROOM_RESET_INFO, "continueGame null != getDissolveRoom");
            }
            if (!getRoom().isCanTrusteeshipContinue() && roomPos.isTrusteeship()) {
                // 如果玩家处于托管状态则，不能手动点击继续游戏。
                return SData_Result.make(ErrorCode.Is_Trusteeship, "continueGame isTrusteeship:{%s}",
                        roomPos.isTrusteeship());
            }
//            if (Objects.isNull(this.getRoom().getCurSet()) || !this.getRoom().getCurSet().isEnd()) {
//                // 当局游戏还未结束
//                return SData_Result.make(ErrorCode.NotAllow, "continueGame setState is not End :{%s}",
//                        getRoom().getRoomState());
//            }
            if (roomPos.isGameReady()) {
                return SData_Result.make(ErrorCode.Success);
            }
            SData_Result result = continueGame(roomPos);
            if (!ErrorCode.Success.equals(result.getCode())) {
                return SData_Result.make(result.getCode(), result.getMsg());
            }
            roomPos.setContinue();
            roomPos.setTimeSec(0);
            return SData_Result.make(ErrorCode.Success);
        } finally {
            this.getRoom().unlock();
        }
    }


    /**
     * 联赛继续游戏
     *
     * @return
     */
    public SData_Result continueGame(AbsRoomPos roomPos) {
        if (RoomTypeEnum.UNION.equals(this.getRoomTypeEnum())) {
            double value = this.getBaseCreateRoom().getAutoDismiss();
            if (roomPos.isAutoDismiss(value)) {
                return SData_Result.make(ErrorCode.ROOM_SPORTS_POINT_NOT_ENOUGH, "ROOM_SPORTS_POINT_NOT_ENOUGH");
            }
            List<AbsRoomPos> roomPosList =
                    this.getRoom().getRoomPosMgr().getPosList().stream().filter(k -> Objects.nonNull(k) && k.getPid() > 0L && k.isAutoDismiss(value)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(roomPosList)) {
                StringBuilder builder = new StringBuilder();
                for (AbsRoomPos pos : roomPosList) {
                    builder.append(String.format("@%s", pos.getPlayer().getName())).append("', ");
                }
                builder.deleteCharAt(builder.length() - 2);
                roomPos.getPlayer().pushProto(_SportsPointEnough.make(this.getRoom().getRoomID(), builder.toString(),
                        this.getRoom().getBaseRoomConfigure().getGameType().getName()));
                CommLog.info("continueGame _SportsPointEnough:{}", builder.toString());
            }
            return SData_Result.make(ErrorCode.Success);
        } else {
            return SData_Result.make(ErrorCode.Success);
        }
    }

    /**
     * 检查自动解散房间
     *
     * @return
     */
    @Override
    public boolean checkAutoDissolveRoom() {
        double value = this.getBaseCreateRoom().getAutoDismiss();
        if (this.getRoom().isAutoDismiss()) {
            return this.getRoom().getRoomPosMgr().getPosList().stream().anyMatch(k -> Objects.nonNull(k) && k.getPid() > 0L && k.isAutoDismiss(value));
        }
        Map<Boolean, List<AbsRoomPos>> roomPosMap =
                this.getRoom().getRoomPosMgr().getPosList().stream().filter(k -> Objects.nonNull(k) && k.getPid() > 0L).collect(Collectors.groupingBy(p -> p.isAutoDismiss(value)));
        if (MapUtils.isEmpty(roomPosMap)) {
            this.getRoom().setAutoDismiss(true);
            return false;
        }
        List<AbsRoomPos> autoDismissValueTrueList = roomPosMap.get(Boolean.TRUE);
        if (CollectionUtils.isEmpty(autoDismissValueTrueList)) {
            this.getRoom().setAutoDismiss(true);
            return false;
        }
        StringBuilder builder = new StringBuilder();
        for (AbsRoomPos pos : autoDismissValueTrueList) {
            if (pos.getPid() <= 0L) {
                continue;
            }
            builder.append(String.format("@%s", pos.getPlayer().getName())).append("', ");
            //  竞技点不足，确定已联系管理！取消则发起解散房间
            pos.getPlayer().pushProto(_SportsPointNotEnough.make(this.getRoom().getRoomID(),
                    this.getRoom().getBaseRoomConfigure().getGameType().getName()));
        }
        builder.deleteCharAt(builder.length() - 2);
        List<AbsRoomPos> autoDismissValueFalseList = roomPosMap.get(Boolean.FALSE);
        if (CollectionUtils.isNotEmpty(autoDismissValueFalseList)) {
            for (AbsRoomPos pos : autoDismissValueFalseList) {
                // @玩家名称，@玩家名称，竞技点不足无法继续游戏，请联系管理
                pos.getPlayer().pushProto(_SportsPointEnough.make(this.getRoom().getRoomID(), builder.toString(),
                        this.getRoom().getBaseRoomConfigure().getGameType().getName()));
            }
        }
        CommLog.info("checkAutoDissolveRoom _SportsPointEnough:{}", builder.toString());
        this.getRoom().setAutoDismiss(true);
        return true;
    }


    @Override
    public void execLuckDrawCondition() {
        AssignCrowdAndConditionItem assignCrowdAndConditionItem =
                LuckDrawConfigMgr.getInstance().getAssignCrowdAndCondition();
        if (Objects.isNull(assignCrowdAndConditionItem) || assignCrowdAndConditionItem.isNotExistLuckDraw(getRoomTypeEnum())) {
            // 没有指定人群和抽奖条件
            return;
        }
        int size = getRoom().getHistorySetSize();
        if (size < 1) {
            return;
        }
        // 是否亲友圈
        boolean isClub = LuckDrawEnum.LuckDrawAssignCrowd.CLUB.equals(assignCrowdAndConditionItem.getAssignCrowd());
        // 抽奖条件：钻石消耗
        if (LuckDrawEnum.LuckDrawType.ROOMCARD.equals(assignCrowdAndConditionItem.getLuckDrawType())) {
            if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == this.getBaseCreateRoom().getPaymentRoomCardType()) {
                // 房主付，消耗累计在裁判长身上。
                this.getUnion().getOwnerPlayer().getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem, isClub ? this.getUnion().getUnionBO().getClubId() : getSpecialRoomId(), this.getRoom().getBaseRoomConfigure().getUnionRoomCfg().getRoomCard());
            }
        } else {
            // 其他抽奖条件，针对房间内的每个人。
            this.getRoom().getRoomPosMgr().getPosList().stream().filter(k -> k.isPlayTheGame()).forEach(k -> {
                if (Objects.nonNull(k.getPlayerRoomAloneBO()) && Objects.nonNull(k.getPlayer())) {
                    if (LuckDrawEnum.LuckDrawType.SETCOUNT.equals(assignCrowdAndConditionItem.getLuckDrawType())) {
                        // 局数累计
                        k.getPlayer().getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem,
                                isClub ? k.clubId() : getSpecialRoomId(), size);
                    } else if (LuckDrawEnum.LuckDrawType.WINNER.equals(assignCrowdAndConditionItem.getLuckDrawType()) && k.getPlayerRoomAloneBO().getWinner() == 1) {
                        // 大赢家次数累计
                        k.getPlayer().getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem,
                                isClub ? k.clubId() : getSpecialRoomId(), 1);
                    }
                }
            });
        }
    }

    /**
     * 清空倒计时时间
     */
    @Override
    public void clearCountdownTime() {
        if (this.getSportsPointEnoughCountdownTime() == 0) {
            return;
        }
        this.setSportsPointEnoughCountdownTime(0);
    }

    /**
     * 设置倒计时时间
     */
    @Override
    public void setCountdownTime() {
        this.setSportsPointEnoughCountdownTime(CommTime.nowSecond());
    }

    /**
     * 检查倒计时时间是否到了
     *
     * @return T:时间到,F:没到
     */
    @Override
    public boolean checkCountdownTime() {
        return true;
        //        return this.getSportsPointEnoughCountdownTime() > 0 && CommTime.SecondsBetween(this
        //        .getSportsPointEnoughCountdownTime(),CommTime.nowSecond()) >= MINUTES_3;
    }

    /**
     * 有出现过满人
     *
     * @return
     */
    @Override
    public boolean existNotEmptyPos() {
        if (this.isNotExistEmptyPos()) {
            return true;
        }
        this.setNotExistEmptyPos(this.getRoom().getRoomPosMgr().checkNotExistEmptyPos());
        return this.isNotExistEmptyPos();
    }

    /**
     * 获取房间项
     *
     * @return
     */
    @Override
    public RoomInfoItem getRoomInfoItem() {
        return Objects.nonNull(getUnion()) ? UnionMgr.getInstance().getRoomInfoItem(this.getSpecialRoomId(),
                this.getRoom()) : null;
    }

    /**
     * 房间切牌
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    @Override
    public SData_Result opXiPai(long pid) {
        if (!RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            return SData_Result.make(ErrorCode.NotEnough_RoomCardByXiPai, "not allow qie pai");
        }
        AbsRoomPos roomPos = this.getRoom().getRoomPosMgr().getPosByPid(pid);
        if (roomPos == null) {
            return SData_Result.make(ErrorCode.NotEnough_RoomCardByXiPai, "not in pos");
        }
        RoomQiePaiEnum qiePaiEnum = RoomQiePaiEnum.valueOf(this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getXiaojuqiepai());
        if (RoomQiePaiEnum.RoomQiePaiEnum_Not.equals(qiePaiEnum)) {
            return SData_Result.make(ErrorCode.NotEnough_RoomCardByXiPai, "not allow qie pai");
        }
        double qiePaiConsume = qiePaiEnum.value();
        if (roomPos.getRoomSportsPoint() < qiePaiConsume) {
            return SData_Result.make(ErrorCode.NotEnough_RoomCardByXiPai, "not allow qie pai");
        }
        ClubMemberBO clubMemberBO = null;
        if (Config.isShare()) {
            ClubMemberBO localClubMemberBo = roomPos.getClubMemberBO();
            if (localClubMemberBo != null) {
                ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(localClubMemberBo.getId());
                if (clubMember != null) {
                    clubMemberBO = clubMember.getClubMemberBO();
                }
            }
        } else {
            clubMemberBO = roomPos.getClubMemberBO();
        }
        SData_Result data_result = UnionMgr.getInstance().getUnionMemberMgr().execSportsPointQiePai(qiePaiConsume, this.getUnion(), clubMemberBO, this.getRoom());
        if (ErrorCode.Success.equals(data_result.getCode())) {
            roomPos.setOtherSportsPointConsume(CommMath.addDouble(roomPos.getOtherSportsPointConsume(), qiePaiConsume));
            this.getRoom().getRoomPosMgr().notify2RoomSportsPointChange(pid, roomPos.memberId(), -qiePaiEnum.value());
            this.getRoom().getRoomPosMgr().notify2All(this.getRoom().XiPai(this.getRoom().getRoomID(), roomPos.getPid(),
                    this.getRoom().getBaseRoomConfigure().getGameType().getType()));
        }
        return data_result;

    }

}
