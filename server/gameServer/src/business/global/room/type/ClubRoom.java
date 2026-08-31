package business.global.room.type;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.config.LuckDrawConfigMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.RoomTyepImpl;
import business.player.Player;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerClub;
import business.player.feature.PlayerClubRoom;
import business.player.feature.PlayerLuckDraw;
import cenum.ItemFlow;
import cenum.LuckDrawEnum.LuckDrawType;
import cenum.RoomSortedEnum;
import cenum.RoomTypeEnum;
import cenum.room.CKickOutType;
import cenum.room.PaymentRoomCardType;
import cenum.room.RoomState;
import com.ddm.server.common.Config;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.luckdraw.AssignCrowdAndConditionItem;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.ClubRoomConfig;
import jsproto.c2s.cclass.room.RoomInfoItem;
import lombok.Data;

import java.util.Objects;

/**
 * 亲友圈房间信息
 *
 * @author
 */
@Data
public class ClubRoom extends RoomTyepImpl {
    /**
     * 亲友圈信息
     */
    private Club club = null;

    public ClubRoom(AbsBaseRoom room) {
        super(room, room.getBaseRoomConfigure().getBaseCreateRoom());
        this.initRoom();
    }

    /**
     * 清除
     */
    @Override
    public void clear() {
        super.clear();
        this.setClub(null);
    }

    /**
     * 初始化亲友圈
     */
    @Override
    public void initRoom() {
        ClubRoomConfig clubConfig = this.getRoom().getBaseRoomConfigure().getClubRoomCfg();
        if (null != clubConfig) {
            if (Config.isShare()) {
                this.setClub(ClubMgr.getInstance().getClubListMgr()
                        .findClubShare(this.getBaseCreateRoom().getClubId()));
            } else {
                // 设置亲友圈信息
                this.setClub(ClubMgr.getInstance().getClubListMgr()
                        .findClub(this.getBaseCreateRoom().getClubId()));
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
        if (Objects.nonNull(getClub())) {
            return getClub().getClubListBO().getName();
        }
        return null;
    }

    /**
     * 获取亲友圈信息
     *
     * @return
     */
    @Override
    public Object getSpecialRoom() {
        // 检查亲友圈是否存在
        if (Config.isShare()) {
            // 获取亲友圈
            this.setClub(ClubMgr.getInstance().getClubListMgr().findClub(this.club.getClubListBO().getId()));
            // 返回
            return this.club;
        } else {
            if (null != this.club) {
                return this.club;
            }
            long clubId = this.getBaseCreateRoom().getClubId();
            if (clubId <= 0L) {
                return null;
            }
            // 获取亲友圈
            this.setClub(ClubMgr.getInstance().getClubListMgr().findClub(clubId));
            // 返回
            return this.club;
        }

    }


    /**
     * 检查亲友圈是否存在
     *
     * @return
     */
    @Override
    public boolean checkSpecialRoom() {
        return null != this.getClub();
    }

    /**
     * 获取亲友圈ID
     *
     * @return
     */
    @Override
    public long getSpecialRoomId() {
        return this.getBaseCreateRoom().getClubId();
    }

    /**
     * 亲友圈创建新房间
     */
    @Override
    public void createNewSetRoom() {
        if (this.isLock()) {
            return;
        }
        this.setLock(true);
        this.getClub().createNewSetRoom(getConfigId(), this.getRoom().getBaseRoomConfigure(), getRoom().getRoomKey());
    }


    /**
     * 获取房间项
     *
     * @return
     */

    public RoomInfoItem getRoomInfoItem() {
        return Objects.nonNull(getClub()) ? ClubMgr.getInstance().getRoomInfoItem(this.getSpecialRoomId(), this.getRoom()) : null;
    }


    /**
     * 亲友圈房间人员状态发生改变
     *
     * @param roomPos
     */
    @Override
    public void roomPlayerChange(AbsRoomPos roomPos) {
        this.getClub().onClubRoomPlayerChange(this.getRoom(), roomPos.getNotify_PosInfo(), this.getRoom().sorted());
    }

    /**
     * 移除亲友圈房间信息
     */
    @Override
    public void onRoomRemove() {
        this.getClub().onClubRoomRemove(getConfigId(), this.getRoom().getRoomKey(), onRoomRemoveSort());
    }

    private int onRoomRemoveSort() {
        return !existNotEmptyPos() && getRoom().getCurSetID() <= 0 ? RoomSortedEnum.NONE_ROOM.ordinal() : RoomSortedEnum.GAME_PLAYING.ordinal();
    }


    /**
     * 亲友圈房间局数改变
     */
    @Override
    public void roomSetIDChange() {
        this.getClub().roomSetIDChange(this.getClub().getClubListBO().getId(), this.getRoom().getRoomID(),
                this.getRoom().getRoomKey(), this.getRoom().getCurSetID(), this.getRoom().sorted());
    }

    @Override
    public void roomStartChange() {
        this.getClub().roomStartChange(this.getClub().getClubListBO().getId(), this.getRoom().getRoomKey());
    }

    @Override
    public void giveBackRoomCard() {
        // 亲友圈房主付
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getPaymentRoomCardType()) {
            if (!business.global.billing.RoomBillingGuard.beginRefund(
                    this.getRoom().getRoomID(), this.getSpecialRoomId())) return;
            // 回退亲友圈消耗钻石
            this.getClub().gainClubRoomCard(this.getRoom().getBaseRoomConfigure(),
                    Club_define.Club_OperationStatus.CLUB_OPERATION_STATUS_GAME);
        } else {
            // 回退亲友圈消耗圈卡
            this.getRoom().getRoomPosMgr().getPlayer().forEach(k -> {
                if (!business.global.billing.RoomBillingGuard.beginRefund(
                        this.getRoom().getRoomID(), k.getPid())) return;
                k.getFeature(PlayerClub.class).clubCardReturnCradRoom(k.getRoomInfo().getConsumeCard(), this.getRoom().getBaseRoomConfigure().getGameType(), this.getSpecialRoomId(), ItemFlow.ClubCardClubRoom);
            });
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public String doChangePlayerNum(BaseRoomConfigure newConfigure, String roomKey) {
        SData_Result result = this.getClub().getOwnerPlayer().getFeature(PlayerClubRoom.class).createClubRoom(newConfigure, roomKey, getSpecialRoomId());
        if (ErrorCode.Success.equals(result.getCode())) {
            return roomKey;
        }
        return null;
    }

    @Override
    public boolean checkDoChangeRefRoomCost(Player player, PaymentRoomCardType paymentRoomCardType, int cost,
                                            int roomCard) {
        // 亲友圈
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.equals(paymentRoomCardType)) {
            // 亲友圈创建者付房费
            // 获取亲友圈创建者信息
            player = getClub().getOwnerPlayer();
            cost = this.getRoom().getBaseRoomConfigure().getClubRoomCfg().getRoomCard() - roomCard;
            // 检查旧配置转新配置钻石够用吗？
            if (this.getRoom().getBaseRoomConfigure().getClubRoomCfg().getRoomCard() == 0 || cost >= 0) {
                return true;
            } else {
                // 不够用,检查创造者身上钻石是否足够补充。
                return player.getFeature(PlayerWallet.class).check(Math.abs(cost));
            }
        } else {
            // 亲友圈圈卡模式
            // 检查旧配置转新配置圈卡够用吗？
            cost = player.getRoomInfo().getConsumeCard() - roomCard;
            if (player.getRoomInfo().getConsumeCard() == 0 || cost >= 0) {
                return true;
            } else {
                // 不够用,检查玩家身上圈卡是否足够补充。
                return player.getFeature(PlayerClub.class).checkClubCard(Math.abs(cost),
                        this.getRoom().getSpecialRoomId());
            }
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
        if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == this.getRoom()
                .getBaseRoomConfigure().getBaseCreateRoom().getPaymentRoomCardType()) {
            // 消耗亲友圈房主的钻石。
            return this.getRoom().getBaseRoomConfigure().getClubRoomCfg().getRoomCard();
        }
        return this.getRoom().getRoomPosMgr().notHomeOwerPaySumConsume();
    }

    /**
     * 房间类型
     */
    @Override
    public RoomTypeEnum getRoomTypeEnum() {
        return RoomTypeEnum.CLUB;
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
                return SData_Result.make(ErrorCode.NotAllow, "kickOut RoomState Init :{%s}", this.getRoom().getRoomState());
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
        if (LuckDrawType.ROOMCARD.equals(assignCrowdAndConditionItem.getLuckDrawType())) {
            if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value() == this.getBaseCreateRoom().getPaymentRoomCardType()) {
                // 房主付，消耗累计在圈主身上。
                this.getClub().getOwnerPlayer().getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem, getSpecialRoomId(), this.getRoom().getBaseRoomConfigure().getClubRoomCfg().getRoomCard());
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
                    if (LuckDrawType.SETCOUNT.equals(assignCrowdAndConditionItem.getLuckDrawType())) {
                        // 局数累计
                        k.getPlayer().getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem, getSpecialRoomId(), size);
                    } else if (LuckDrawType.WINNER.equals(assignCrowdAndConditionItem.getLuckDrawType()) && k.getPlayerRoomAloneBO().getWinner() == 1) {
                        // 大赢家次数累计
                        k.getPlayer().getFeature(PlayerLuckDraw.class).execCondition(assignCrowdAndConditionItem, getSpecialRoomId(), 1);
                    }
                }
            });
        }
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

}
