package business.global.room.type;

import BaseCommon.CommLog;
import business.global.config.LuckDrawConfigMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.RoomTyepImpl;
import business.player.Player;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerLuckDraw;
import business.player.feature.PlayerRoom;
import cenum.LuckDrawEnum;
import cenum.RoomTypeEnum;
import cenum.room.CKickOutType;
import cenum.room.PaymentRoomCardType;
import cenum.room.RoomState;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.luckdraw.AssignCrowdAndConditionItem;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.iclass.room.SRoom_CreateRoom;
import lombok.Data;

import java.util.Objects;

/**
 * 正常房间
 *
 * @author
 */
@Data
public class NormalRoom extends RoomTyepImpl {
    public NormalRoom(AbsBaseRoom room) {
        super(room, room.getBaseRoomConfigure().getBaseCreateRoom());
    }

    /**
     * 房间类型
     */
    @Override
    public RoomTypeEnum getRoomTypeEnum() {
        return RoomTypeEnum.NORMAL;
    }

    @Override
    public void clear() {
        super.clear();
    }


    @Override
    public void giveBackRoomCard() {
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == this.getBaseCreateRoom().getPaymentRoomCardType()) {
            AbsRoomPos pos = getRoom().getRoomPosMgr().getPosByPid(this.getRoom().getOwnerID());
            if (null == pos) {
                pos = getRoom().getRoomPosMgr().getWatchPosByPid(this.getRoom().getOwnerID());
            }
            Player player = pos.getPlayer();
            if (Objects.isNull(player)) {
                CommLog.error(String.format("giveBackRoomCard player:{%d}", this.getRoom().getOwnerID()));
                return;
            }
            if (!business.global.billing.RoomBillingGuard.beginRefund(
                    this.getRoom().getRoomID(), player.getPid())) return;
            player.getFeature(PlayerWallet.class).backConsumeRoom(player.getRoomInfo().getConsumeCard(), this.getRoom().getBaseRoomConfigure().getGameType());
        } else {
            this.getRoom().getRoomPosMgr().getPlayer().forEach(k -> {
                if (!business.global.billing.RoomBillingGuard.beginRefund(
                        this.getRoom().getRoomID(), k.getPid())) return;
                k.getFeature(PlayerWallet.class).backConsumeRoom(k.getRoomInfo().getConsumeCard(), this.getRoom().getBaseRoomConfigure().getGameType());
            });
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public String doChangePlayerNum(BaseRoomConfigure newConfigure, String roomKey) {
        // 新房间
        Player ownerPlayer = this.getRoom().getRoomPosMgr().getPosByPid(this.getRoom().getOwnerID()).getPlayer();
        SData_Result result = ownerPlayer.getFeature(PlayerRoom.class).createRoomAndConsumeCard(newConfigure, roomKey);
        if (ErrorCode.Success.equals(result.getCode())) {
            SRoom_CreateRoom createRoom = (SRoom_CreateRoom) result.getData();
            ownerPlayer.pushProto(this.getRoom().ChangeRoomNum(createRoom.getRoomID(), createRoom.getRoomKey(), newConfigure.getBaseCreateRoom().getCreateType()));
            return createRoom.getRoomKey();
        }
        ownerPlayer = null;
        return roomKey;
    }

    @Override
    public boolean checkDoChangeRefRoomCost(Player player, PaymentRoomCardType paymentRoomCardType, int cost,
                                            int roomCard) {
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.equals(paymentRoomCardType)) {
            player = this.getRoom().getRoomPosMgr().getPosByPid(this.getRoom().getOwnerID()).getPlayer();
        }
        cost = player.getRoomInfo().getConsumeCard() - roomCard;
        if (player.getRoomInfo().getConsumeCard() == 0 || cost >= 0) {
            return true;
        } else {
            return player.getFeature(PlayerWallet.class).check(Math.abs(cost));
        }
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
        return this.getRoom().getRoomPosMgr().notHomeOwerPaySumConsume();
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
            if (pid != this.getRoom().getOwnerID()) {
                // 操作玩家不是房主
                return SData_Result.make(ErrorCode.NotAllow, "kickOut PID:{%d} != OwnerID:{%d}", pid,
                        this.getRoom().getOwnerID());
            }
            if (!RoomState.Init.equals(this.getRoom().getRoomState())) {
                // 房间不处于初始阶段
                return SData_Result.make(ErrorCode.NotAllow, "kickOut RoomState Init :{%s}", this.getRoom().getRoomState());
            }
            AbsRoomPos roomPos = this.getRoom().getRoomPosMgr().getPosByPosID(posIndex);
            if (null == roomPos) {
                // 找不到指定的位置信息
                return SData_Result.make(ErrorCode.NotAllow, "kickOut null == roomPos posIndex:{%d}", posIndex);
            }
            if (roomPos.getPid() == this.getRoom().getOwnerID()) {
                // 房主不能踢自己
                return SData_Result.make(ErrorCode.NotAllow, "kickOut PID:{%d} == OwnerID:{%d}", roomPos.getPid(),
                        this.getRoom().getOwnerID());
            }
            roomPos.leave(true, this.getRoom().getOwnerID(), CKickOutType.None);
        } finally {
            this.getRoom().unlock();
        }
        return SData_Result.make(ErrorCode.Success);
    }

    @Override
    public void execLuckDrawCondition() {
        AssignCrowdAndConditionItem assignCrowdAndConditionItem = LuckDrawConfigMgr.getInstance().getAssignCrowdAndCondition();
        if (Objects.isNull(assignCrowdAndConditionItem) || assignCrowdAndConditionItem.isNotExistLuckDraw(getRoomTypeEnum())) {
            // 没有指定人群和抽奖条件
            return;
        }

        int size = getRoom().getHistorySetSize();
        if (size < 1) {
            return;
        }
        // 抽奖条件：钻石消耗
        if (LuckDrawEnum.LuckDrawType.ROOMCARD.equals(assignCrowdAndConditionItem.getLuckDrawType())) {
            if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == this.getBaseCreateRoom().getPaymentRoomCardType()) {
                // 房主付，消耗累计在圈主身上。
                AbsRoomPos roomPos = this.getRoom().getRoomPosMgr().getPosByPid(this.getRoom().getOwnerID());
                if (Objects.nonNull(roomPos) && Objects.nonNull(roomPos.getPlayer())) {
                    roomPos.getPlayer().getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem, getSpecialRoomId(), roomPos.getPlayer().getRoomInfo().getConsumeCard());
                }
            } else if (PaymentRoomCardType.PaymentRoomCardType_AutoPay.value() == this.getBaseCreateRoom().getPaymentRoomCardType()) {
                // 平分支付，每个人的消耗累计
                this.getRoom().getRoomPosMgr().getPlayer().forEach(k -> {
                    k.getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem, getSpecialRoomId(), k.getRoomInfo().getConsumeCard());
                });
            } else {
                // 大赢家支付，只累计大赢家的
                int winnerSize = this.getRoom().getRoomPosMgr().winnerPayCount();
                this.getRoom().getRoomPosMgr().getPosList().forEach(k -> {
                    if (k.getPid() > 0L && k.getPlayerRoomAloneBO().getWinner() == 1) {
                        Player player = k.getPlayer();
                        player.getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem, getSpecialRoomId(), (int) Math.ceil(player.getRoomInfo().getConsumeCard() * 1.0 / winnerSize));
                    }
                });
            }
        } else {
            // 其他抽奖条件，针对房间内的每个人。
            this.getRoom().getRoomPosMgr().getPosList().stream().filter(k -> k.isPlayTheGame()).forEach(k -> {
                if (Objects.nonNull(k.getPlayerRoomAloneBO()) && Objects.nonNull(k.getPlayer())) {
                    if (LuckDrawEnum.LuckDrawType.SETCOUNT.equals(assignCrowdAndConditionItem.getLuckDrawType())) {
                        // 局数累计
                        k.getPlayer().getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem, getSpecialRoomId(), size);
                    } else if (LuckDrawEnum.LuckDrawType.WINNER.equals(assignCrowdAndConditionItem.getLuckDrawType()) && k.getPlayerRoomAloneBO().getWinner() == 1) {
                        // 大赢家次数累计
                        k.getPlayer().getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem, getSpecialRoomId(), 1);
                    }
                }
            });
        }
    }


}
