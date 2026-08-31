package business.global.room.base;

import BaseCommon.CommLog;
import business.player.Player;
import business.player.feature.PlayerRoom;
import cenum.room.PaymentRoomCardType;
import cenum.room.RoomState;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.EncryptUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import core.config.refdata.ref.RefRoomCost;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.iclass.room.SRoom_EnterRoom;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;

@Data
public class OpChangePlayerRoom implements Serializable {
    // 房间信息
    private AbsBaseRoom room;
    // 切换人数房间管理
    protected transient ChangePlayerNumRoom changePlayerNumRoom = null;
    // 改变人数
    private int changePlayerNum = -1;

    public OpChangePlayerRoom(AbsBaseRoom room) {
        this.setRoom(room);
    }

    public void clear() {
        this.setRoom(null);
        this.setChangePlayerNumRoom(null);
    }

    /**
     * 检查房间人数切换
     *
     * @param curSec 当前时间
     * @return
     */
    public boolean checkPlayerNumChange(int curSec) {
        // 检查是否有切换人数操作
        if (null != this.getChangePlayerNumRoom()) {
            if (this.getChangePlayerNumRoom().isAllAgree()) {
                // 操作切换人数
                this.doChangePlayerNum();
                return true;
            } else if (this.getChangePlayerNumRoom().isDelay(curSec)) {
                // 是否已超时
                AbsRoomPos roomPos = this.getRoom().getRoomPosMgr().getPosByPosID(this.getChangePlayerNumRoom().notOpPos());
                if (null != roomPos) {
                    // 操作同意切换人数
                    this.changePlayerNumAgree(roomPos.getPid(), 2);
                }
                return false;
            }
        }
        // 没有切换人数操作
        return false;
    }

    /**
     * 切换人数
     *
     * @param pid 玩家PID
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result changePlayerNum(long pid) {
        try {
            this.getRoom().lock();

            if (!this.getRoom().isCanChangePlayerNum()) {
                // 不允许房间人数切换
                return SData_Result.make(ErrorCode.NotAllow, "房间人数不可修改");
            }

            if (!RoomState.Init.equals(this.getRoom().getRoomState())) {
                // 游戏开始不能进行人数切换。
                return SData_Result.make(ErrorCode.NotAllow, "已经开始游戏不能进行人数修改");
            }
            int playerNum = this.getRoom().getRoomPosMgr().getFullPosCount();

            if (playerNum < 2) {
                return SData_Result.make(ErrorCode.NotAllow, "位置上的玩家人数不能小于2个人");
            }

            if (playerNum == this.getRoom().getPlayerNum()) {
                // 切换的房间人数大于等于当前房间人数
                return SData_Result.make(ErrorCode.NotAllow, "切换的房间人数等于当前房间人数");
            }

            if (playerNum > this.getRoom().getPlayerNum()) {
                // 切换的房间人数大于等于当前房间人数
                return SData_Result.make(ErrorCode.NotAllow, "切换的房间人数大于当前房间人数");
            }

            // 通过pid获取玩家信息
            AbsRoomPos roomPos = this.getRoom().getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                // 找不到玩家信息
                return SData_Result.make(ErrorCode.NotAllow, "没有找到该玩家，不能修改房间人数");
            }

            if (this.getRoom().getRoomPosMgr().checkExistNoOne()) {
                // 存在没人
                return SData_Result.make(ErrorCode.NotAllow, "人数不能少于一个");
            }

            if (playerNum < this.getRoom().getRoomPosMgr().getFullPosCount()) {
                // 切换后的人数不能少于已加入当前房间的人数
                return SData_Result.make(ErrorCode.NotAllow, "切换后的人数不能少于已加入当前房间的人数");
            }
            BaseRoomConfigure newConfigure = this.getRoom().getBaseRoomConfigure().deepClone();
            newConfigure.getBaseCreateRoom().setPlayerMinNum(playerNum);
            newConfigure.getBaseCreateRoom().setPlayerNum(playerNum);
            // 检查房主
            if (!this.checkRefRoomCost(newConfigure, roomPos)) {
                return SData_Result.make(ErrorCode.NotAllow, "房卡不足，不能进行房间人数切换，");
            }
            // 设置发起切换人数
            this.setChangePlayerNumRoom(
                    new ChangePlayerNumRoom(this.getRoom(), roomPos.getPosID(), this.getRoom().getRoomPosMgr().getFullPosCount(), playerNum, room.getPlayerNum()));
            // 通知发起切换人数
            this.getRoom().getRoomPosMgr().notify2All(this.getRoom().ChangePlayerNum(this.getRoom().getRoomID(), roomPos.getPosID(), this.getChangePlayerNumRoom().getEndSec(), playerNum));
            // 获取空位数量
            if (this.getRoom().getRoomPosMgr().getFullPosCount() == 1) {
                this.doChangePlayerNum();
            }
            return SData_Result.make(ErrorCode.Success);
        } finally {
            this.getRoom().unlock();
        }
    }


    /**
     * 检查房卡配置和玩家房卡
     *
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    private boolean checkRefRoomCost(BaseRoomConfigure newConfigure, AbsRoomPos roomPos) {
        SData_Result result = RefRoomCost.GetCost(newConfigure);
        // 检查卡配置是否正常
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 房卡配置有误.
            return false;
        }
        Player player = roomPos.getPlayer();
        // 消耗差值
        int cost = 0;
        // 获取消耗
        int roomCard = (int) result.getCustom();
        // 付费方式
        PaymentRoomCardType paymentRoomCardType = PaymentRoomCardType.valueOf(this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getPaymentRoomCardType());
        return this.getRoom().getRoomTyepImpl().checkDoChangeRefRoomCost(player, paymentRoomCardType, cost, roomCard);

    }

    /**
     * 解散房间操作
     *
     * @param pid 用户ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result changePlayerNumAgree(long pid, int agree) {
        try {
            this.getRoom().lock();
            if (null == this.getChangePlayerNumRoom()) {
                return SData_Result.make(ErrorCode.Success);
            }
            AbsRoomPos roomPos = this.getRoom().getRoomPosMgr().getPosByPid(pid);
            if (null == roomPos) {
                return SData_Result.make(ErrorCode.NotAllow, "没有找到改玩家，不可修改");
            }
            BaseRoomConfigure newConfigure = this.getRoom().getBaseRoomConfigure().deepClone();
            newConfigure.getBaseCreateRoom().setPlayerMinNum(this.getChangePlayerNumRoom().getChangePlayerNum());
            newConfigure.getBaseCreateRoom().setPlayerNum(this.getChangePlayerNumRoom().getChangePlayerNum());
            if (!this.checkRefRoomCost(newConfigure, roomPos)) {
                return SData_Result.make(ErrorCode.NotAllow, "房卡不足，不能进行房间人数切换，");
            }
            this.getChangePlayerNumRoom().deal(roomPos.getPosID(), agree);
            this.getRoom().getRoomPosMgr().notify2All(this.getRoom().ChangePlayerNumAgree(this.getRoom().getRoomID(), roomPos.getPosID(), agree == 1));
            if (this.getChangePlayerNumRoom().isRefused()) {
                this.setChangePlayerNumRoom(null);
            }
            return SData_Result.make(ErrorCode.Success);
        } finally {
            this.getRoom().unlock();
        }
    }


    /**
     * 房间切换人数时
     * 加入房间*
     *
     * @param posId 位置ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result changePlayerNumEnterRoom(int posId) {
        int agree = 2;
        if (null == this.getChangePlayerNumRoom()) {
            return SData_Result.make(ErrorCode.Success);
        }
        this.getChangePlayerNumRoom().deal(posId, agree);
        this.getRoom().getRoomPosMgr().notify2All(this.getRoom().ChangePlayerNumAgree(this.getRoom().getRoomID(), posId, agree == 1));
        if (this.getChangePlayerNumRoom().isRefused()) {
            this.setChangePlayerNumRoom(null);
        }
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 操作切换房间人数
     */
    @SuppressWarnings("rawtypes")
    public void doChangePlayerNum() {
        String roomKey = this.getRoom().getRoomKey();
        // 获取玩家信息列表
        List<AbsRoomPos> roomPosList = this.getRoom().getRoomPosMgr().getRoomPosList();
        this.getRoom().setRoomKey("");
        // 消耗回退
        this.getRoom().giveBackRoomCard();
        // 房间结算
        this.getRoom().endRoom();
        // 解锁
        this.getRoom().unlock();
        // 获取新配置
        BaseRoomConfigure newConfigure = this.getRoom().getBaseRoomConfigure().clone();
        newConfigure.getBaseCreateRoom().setPlayerMinNum(this.getChangePlayerNumRoom().getChangePlayerNum());
        newConfigure.getBaseCreateRoom().setPlayerNum(this.getChangePlayerNumRoom().getChangePlayerNum());
        SData_Result result = null;
        // 根据房间类型操作人数切换
        //共享情况需要更新共享字段
        if (Config.isShare()) {
            if (!StringUtils.isEmpty(newConfigure.getBaseCreateRoom().getPassword())) {
                newConfigure.getBaseCreateRoom().setPassword(EncryptUtils.decryptDES(newConfigure.getBaseCreateRoom().getPassword()));
            }
            newConfigure.setShareBaseCreateRoom(new Gson().toJson(newConfigure.getBaseCreateRoom()));
        }
        roomKey = this.getRoom().getRoomTyepImpl().doChangePlayerNum(newConfigure, roomKey);
        if (StringUtils.isEmpty(roomKey)) {
            CommLog.error("doChangePlayerNum ");
        }
        // 进入房间
        for (AbsRoomPos roomPos : roomPosList) {
            result = roomPos.getPlayer().getFeature(PlayerRoom.class).findAndEnter(-1, roomKey, roomPos.clubId(), Config.isShare() ? newConfigure.getBaseCreateRoom().getPassword() : EncryptUtils.decryptDES(newConfigure.getBaseCreateRoom().getPassword()));
            if (ErrorCode.Success.equals(result.getCode())) {
                roomPos.getPlayer().pushProto(this.getRoom().ChangeRoomNum(((SRoom_EnterRoom) result.getData()).getRoomID(), roomKey, newConfigure.getBaseCreateRoom().getCreateType()));
            }
        }
        roomPosList = null;
        this.setChangePlayerNumRoom(null);
        // 加锁
        this.getRoom().lock();
    }


}
