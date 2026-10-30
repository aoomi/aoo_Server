package business.global.room.base;

import BaseCommon.CommLog;
import cenum.PrizeType;
import cenum.room.RoomState;
import cenum.room.TrusteeshipState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.data.AbstractRefDataMgr;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import core.config.refdata.ref.RefTrusteeshipTime;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import lombok.Data;

@Data
public class Trusteeship {
    /**
     * 房间信息
     */
    private AbsBaseRoom room = null;
    /**
     * 托管状态
     */
    private TrusteeshipState trusteeshipState = TrusteeshipState.Wait;
    /**
     * 服务器的托管时间
     */
    private int serverTrusteeshipTime = 0;
    /**
     * 切换状态时间
     */
    private long trusteeshipMS = 0;
    /**
     * 初始等待时间
     */
    private int InitTime = 5000;
    /**
     * 托管，继续游戏等待。
     */
    private int ContinueTime = 15000;
    /**
     * 解散游戏等待
     */
    private int DissolveTime = 5000;
    /**
     * 是否存在托管
     */
    private boolean isExistTrusteeship = false;
    private int xianShiValue = 0;
    private int refValue = 0;

    public Trusteeship(AbsBaseRoom room) {
        this.room = room;
        this.checkOvertimeAmbient(room.getBaseRoomConfigure().getGameType(),
                room.getBaseRoomConfigure().getPrizeType());
    }

    public void clean() {
        this.room = null;
    }

    /**
     * 检查超时托管
     */
    public void checkOvertimeAmbient(GameType gameType, PrizeType prizeType) {
        String prizeStr = prizeType == PrizeType.Gold ? "2" : "1";
        String gameTypeValue = (gameType.getId() + 1) + "0" + prizeStr;
        int id = Integer.parseInt(gameTypeValue);
        RefTrusteeshipTime trusteeshipTime = AbstractRefDataMgr.get(RefTrusteeshipTime.class, id);
        if (null != trusteeshipTime) {
            serverTrusteeshipTime = trusteeshipTime.ServerTime;
            refValue = trusteeshipTime.ServerTime;
            CommLogD.info("checkOvertimeAmbient serverTrusteeshipTime=" + serverTrusteeshipTime);
        }
        if (PrizeType.RoomCard.equals(prizeType)) {
            int trusteeshipTimeValue = this.room.trusteeshipTimeValue();
            if (trusteeshipTimeValue <= 0) {
                return;
            }
            xianShiValue = trusteeshipTimeValue;
            serverTrusteeshipTime = trusteeshipTimeValue;
        }

    }

    /**
     * 托管任务
     */
    public void trusteeshipTask() {
        if (!this.isExistTrusteeship()) {
            return;
        }
        switch (trusteeshipState) {
            // 正常打牌状态
            case Normal:
                long ms = CommTime.nowMS();
                if (ms > trusteeshipMS + InitTime) {
                    room.getRoomPosMgr().getPosList().stream().forEach(roomPos -> {
                        if (roomPos.isTrusteeship() && ms >= roomPos.getLatelyOutCardTime() + room.getThinkTime()) {
                            room.roomTrusteeship(roomPos.getPosID());
                        }
                    });
                }
                break;
            // 继续下一局
            case Continue:
                if (CommTime.nowMS() > trusteeshipMS + ContinueTime) {
                    checkTrusteeship(true);
                }
                break;
            // 解散房间
            case Dissolve:
                if (CommTime.nowMS() > trusteeshipMS + DissolveTime) {
                    checkTrusteeship(false);
                }
                break;
            case Wait:
                break;
            case End:
                cancelTimer();
                break;
            default:
                break;
        }
    }


    /**
     * 房间托管
     */
    @SuppressWarnings("rawtypes")
    public SData_Result roomTrusteeship(long pid, boolean trusteeship, boolean isOwn) {
        AbsRoomPos pos = this.room.getRoomPosMgr().getPosByPid(pid);
        if (pos == null) {
            return SData_Result.make(ErrorCode.NotAllow, "not in pos");
        }
        if (!RoomState.Playing.equals(this.room.getRoomState())) {
            return SData_Result.make(ErrorCode.NotAllow, "roomState:{%s}", this.room.getRoomState());
        }
        // 如果玩家进入托管，启动定时器。
        if (trusteeship) {
            startTrusteeShipTime();
        }
        pos.setTrusteeship(trusteeship, isOwn);
        if (!trusteeship) {
            // 如果所有玩家取消托管，关闭定时器
            if (!this.room.getRoomPosMgr().checkExistTrusteeship()) {
                cancelTimer();
            }
            this.room.cancelTrusteeship(pos);
        }
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 离线定时托管
     *
     * @param roomPos       房间位置信息
     * @param isLostConnect T:失去连接，F:建立连接
     */
    public void lostConnectTrusteeship(AbsRoomPos roomPos, boolean isLostConnect) {
        // 游戏中才有托管进入状态
        if (RoomState.Playing.equals(this.getRoom().getRoomState())) {
            // 如果建立连接并且玩家处于托管状态
            if (!isLostConnect && roomPos.isTrusteeship()) {
                // 设置托管状态
                roomPos.setTrusteeship(isLostConnect, false);
                // 如果所有玩家取消托管，关闭定时器
                if (this.getRoom().getRoomPosMgr().checkNotExistTrusteeship()) {
                    // 关闭定时器
                    cancelTimer();
                }
                // 取消托管
                this.getRoom().cancelTrusteeship(roomPos);
            }
        }
    }

    /**
     * 检查关于房间托管
     *
     * @param t T: 继续游戏，F:同意解散
     */
    public void checkTrusteeship(boolean t) {
        // 检查是否有处于托管状态的玩家
        for (AbsRoomPos roomPos : this.room.getRoomPosMgr().getPosList()) {
            if (roomPos.isTrusteeship() || roomPos.isRobot()) {
                if (t && !roomPos.isGameReady()) {
                    // 判断玩家的游戏是否处于结束状态，并且还有可用局数。
                    // 默认玩家继续游戏
                    roomPos.setContinue();
                } else if (!t && !roomPos.isDissolveRoom()) {
                    // 检查是否有人发起解散房间
                    if (this.room.getDissolveRoom().deal(roomPos.getPosID(), true)) {
                        // 默认同意解散房间
                        roomPos.setDissolveRoom(true);
                        this.room.getRoomPosMgr().notify2AllPlaying(this.room.PosDealVote(this.room.getRoomID(), roomPos.getPosID(), true, room.getDissolveRoom().reSetDissolve(this.room.getPlayingCount(), this.getRoom().getClassType())));
                        CommLog.info("Trusteeship deal true RoomId:{},RoomKey:{},Pid:{},GameId:{},curSetID:{},DissolveMsg:{}", getRoom().getRoomID(), getRoom().getRoomKey(), roomPos.getPid(), getRoom().getGameRoomBO().getGameType(), getRoom().getCurSetID(), getRoom().getDissolveRoom().getDissolveInfoLog());
                    }
                }
            }
        }
    }

    /**
     * 比赛场继续游戏
     */
    public void arenaContinue() {
        for (AbsRoomPos roomPos : this.room.getRoomPosMgr().getPosList()) {
            // 检查玩家是否存在
            if (null == roomPos) {
                continue;
            }
            if (!roomPos.isGameReady()) {
                // 判断玩家的游戏是否处于结束状态，并且还有可用局数。
                // 默认玩家继续游戏
                roomPos.setContinue();
            }
        }
    }

    /**
     * 关闭定时器
     */
    public void cancelTimer() {
        this.setExistTrusteeship(false);
    }

    /**
     * 启动定时器
     */
    public void startTrusteeShipTime() {
        this.setExistTrusteeship(true);
    }


    /**
     * 设置房间托管状态
     *
     * @param trusteeshipState
     */
    public void setTrusteeshipState(TrusteeshipState trusteeshipState) {
        if (this.trusteeshipState != trusteeshipState) {
            this.trusteeshipState = trusteeshipState;
            this.trusteeshipMS = CommTime.nowMS();
        }
    }

    public void setXianShiValueServerTrusteeshipTime() {
        if (xianShiValue != 0) {
            this.serverTrusteeshipTime = xianShiValue;
        }
    }

    public void setRefServerTrusteeshipTime() {
        if (refValue != 0) {
            this.serverTrusteeshipTime = refValue;
        }
    }
}
