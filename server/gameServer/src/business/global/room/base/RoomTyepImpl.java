package business.global.room.base;

import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerCurrency;
import cenum.ClassType;
import cenum.ItemFlow;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.room.GaoJiTypeEnum;
import cenum.room.PaymentRoomCardType;
import cenum.room.RoomState;
import com.ddm.server.common.GameConfig;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.RoomInfoItem;
import lombok.Data;

import java.util.Objects;

/**
 * 房间信息
 * 正常房间
 * 亲友圈房间，大赛事房间
 */
@Data
public abstract class RoomTyepImpl {
    /**
     * 是否锁住
     */
    private boolean isLock = false;
    /**
     * 房间信息
     */
    private AbsBaseRoom room;
    /**
     * 房间公共配置
     */
    private BaseCreateRoom baseCreateRoom;

    /**
     * 出现满人过
     */
    private boolean notExistEmptyPos = false;

    public RoomTyepImpl(AbsBaseRoom room, BaseCreateRoom baseCreateRoom) {
        this.room = room;
        this.baseCreateRoom = baseCreateRoom;
    }

    /**
     * 清除
     */
    public void clear() {
        this.setRoom(null);
        this.setBaseCreateRoom(null);
    }

    /**
     * 初始化
     */
    public void initRoom() {

    }


    /**
     * 获取配置Id
     *
     * @return
     */
    public long getConfigId() {
        return this.getBaseCreateRoom().getGameIndex();
    }

    /**
     * 房间类型
     *
     * @return
     */
    public abstract RoomTypeEnum getRoomTypeEnum();

    /**
     * 获取信息
     *
     * @return
     */
    public Object getSpecialRoom() {
        return null;
    }

    /**
     * 检查是否存在
     *
     * @return
     */
    public boolean checkSpecialRoom() {
        return false;
    }

    /**
     * 获取ID
     *
     * @return
     */
    public long getSpecialRoomId() {
        return 0L;
    }


    /**
     * 创建新房间
     */
    public void createNewSetRoom() {
        return;
    }


    /**
     * 房间人员状态发生改变
     *
     * @param roomPos
     */
    public void roomPlayerChange(AbsRoomPos roomPos) {
        return;
    }

    /**
     * 移除房间信息
     */
    public void onRoomRemove() {
        return;
    }


    /**
     * 房间局数改变
     */
    public void roomSetIDChange() {
        return;
    }

    /**
     * 通知房间开始改变
     * 2019/12/9 不再使用这个消息通知,因为有了局数改变的接口。
     */
    @Deprecated
    public void roomStartChange() {
        return;
    }

    /**
     * 返回房卡
     */
    public void giveBackRoomCard() {
        return;
    }

    /**
     * 操作房间人数切换
     */
    @SuppressWarnings("rawtypes")
    public String doChangePlayerNum(BaseRoomConfigure newConfigure, String roomKey) {
        return roomKey;
    }

    /**
     * 检查操作房间人数切换房卡消耗是否满足。
     *
     * @param player              玩家
     * @param paymentRoomCardType 支付类型
     * @param cost                消耗类型
     * @param roomCard            房卡
     * @return
     */
    public boolean checkDoChangeRefRoomCost(Player player, PaymentRoomCardType paymentRoomCardType, int cost, int roomCard) {
        return false;
    }

    /**
     * 系统维护时强制解散
     */
    public abstract void doForceDissolve();

    /**
     * 获取总消耗数
     *
     * @return
     */
    public int sumConsume() {
        return 0;
    }


    /**
     * 竞技点倍数
     *
     * @return
     */
    public double getSportsDouble() {
        Double sports = this.getBaseCreateRoom().getSportsDouble().doubleValue();
        return Objects.isNull(sports) ? 1D : sports.doubleValue();
    }

    /**
     * 踢出房间
     *
     * @param pid      用户ID
     * @param posIndex 位置
     */
    public SData_Result kickOut(long pid, int posIndex, String msg) {
        return SData_Result.make(ErrorCode.NotAllow, "NotAllow");
    }

    /**
     * 检查自动解散房间
     *
     * @return
     */
    public boolean checkAutoDissolveRoom() {
        return false;
    }


    /**
     * 获取游戏类型：扑克、麻将
     *
     * @return
     */
    public ClassType getClassType() {
        return this.getRoom().getBaseRoomConfigure().getGameType().getType();
    }


    /**
     * 执行抽奖条件任务
     */
    public void execLuckDrawCondition() {

    }


    /**
     * 是否禁用魔法表情
     *
     * @return
     */
    public boolean isNotGift() {
        return this.getBaseCreateRoom().getGaoji().contains(GaoJiTypeEnum.NOT_GIFT.ordinal());
    }


    /**
     * 继续游戏
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
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
            if (!getRoom().isCanTrusteeshipContinue() && roomPos.isTrusteeship() && null != roomPos.getLiuzuoSec()) {
                // 如果玩家处于托管状态则，不能手动点击继续游戏。
                return SData_Result.make(ErrorCode.Is_Trusteeship, "continueGame isTrusteeship:{%s}",
                        roomPos.isTrusteeship());
            }
//            if (Objects.isNull(this.getRoom().getCurSet()) || !this.getRoom().getCurSet().isEnd()) {
//                // 当局游戏还未结束
//                return SData_Result.make(ErrorCode.NotAllow, "continueGame setState is not End :{%s}", getRoom().getRoomState());
//            }
            if (roomPos.isGameReady()) {
                return SData_Result.make(ErrorCode.Success);
            }
            roomPos.setContinue();
            roomPos.setTimeSec(0);
            return SData_Result.make(ErrorCode.Success);
        } finally {
            this.getRoom().unlock();
        }
    }

    /**
     * 检查高级选项
     *
     * @return 高级选项
     */
    public boolean checkGaoJiXuanXiang(GaoJiTypeEnum typeEnum) {
        if (Objects.isNull(this.baseCreateRoom)) {
            return false;
        }
        return baseCreateRoom.getGaoji().contains(typeEnum.ordinal());
    }


    /**
     * 清空倒计时时间
     */
    public void clearCountdownTime() {

    }

    /**
     * 设置倒计时时间
     */
    public void setCountdownTime() {

    }

    /**
     * 检查倒计时时间是否到了
     *
     * @return T:时间到,F:没到
     */
    public boolean checkCountdownTime() {
        return false;
    }


    /**
     * 有出现过满人
     *
     * @return
     */
    public boolean existNotEmptyPos() {
        return false;
    }


    /**
     * 名字
     *
     * @return
     */
    public String name() {
        return null;
    }


    /**
     * 获取房间项
     *
     * @return
     */
    public RoomInfoItem getRoomInfoItem() {
        return null;
    }


    /**
     * 房间洗牌
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result opXiPai(long pid) {
        Player player = PlayerMgr.getInstance().getPlayer(pid);
        if (!player.getFeature(PlayerCurrency.class).checkAndConsumeItemFlow(PrizeType.RoomCard, GameConfig.XiPaiCost(), ItemFlow.XiPai)) {
            return SData_Result.make(ErrorCode.NotEnough_RoomCardByXiPai, "checkAndConsume" + this.getRoom().getRoomID());
        }
        try {
            this.getRoom().lock();
            AbsRoomPos roomPos = this.getRoom().getRoomPosMgr().getPosByPid(pid);
            if (roomPos == null) {
                return SData_Result.make(ErrorCode.NotAllow, "not in pos");
            }

            // 检查Pid是否存在
            if (this.getRoom().getXiPaiList().contains(pid)) {
                // 增加PID
                this.getRoom().getXiPaiList().add(pid);
            }
            this.getRoom().getRoomPosMgr().notify2All(this.getRoom().XiPai(this.getRoom().getRoomID(), roomPos.getPid(),
                    this.getRoom().getBaseRoomConfigure().getGameType().getType()));
            return SData_Result.make(ErrorCode.Success);
        } finally {
            this.getRoom().unlock();
        }
    }

}
