package business.global.room.task;

import business.global.room.base.AbsBaseRoom;
import cenum.room.RoomState;
import cenum.room.TrusteeshipState;
import com.ddm.server.common.utils.CommTime;

/**
 * 正常房间线程
 *
 * @author Administrator
 */
public class NoneTaskRoom extends AbsBaseTaskRoom {

    public NoneTaskRoom(AbsBaseRoom room, long timer) {
        super(room, timer);
    }

    @Override
    /**
     * 每200ms更新1次
     *
     * @return
     */
    protected boolean update() {
        boolean isClosed = false;
        int curSec = CommTime.nowSecond();
        try {
            lock();
            // 检测房间是否过期
            switch (this.roomState) {
                case Init:
                    // 房间超时
                    if (curSec - this.getCreateSec() >= this.getRoomSurvivalTime()) {
                        this.getRoom().doDissolveRoom(false);
                    }

                    // 检查房间人数切换
                    if (this.getRoom().getOpChangePlayerRoom().checkPlayerNumChange(curSec)) {
                        return isClosed;
                    }
                    // 检查自动准备游戏超时
                    this.autoReadyGameOvertime();
                    // 检查开始游戏
                    this.startGame();
                    break;
                case Playing:
                    this.getRoom().getTrusteeship().trusteeshipTask();
                    //更新等待时间
                    if (this.getInitWaitSec() <= 0) {
                        this.setInitWaitSec(curSec);
                    }
                    // 正在解散中
                    if (this.getRoom().checkDissolveRoom(curSec)) {
                        break;
                    }
                    // 第一局
                    if (null == this.getRoom().getCurSet()) {
                        this.getRoom().startNewSet();
                        break;
                    }
                    // 检查用户超时
                    this.getRoom().getRoomPosMgr().checkOverTime(this.getRoom().getTrusteeship().getServerTrusteeshipTime());
                    // 当前局
                    boolean isSetClosed = this.getRoom().getCurSetUpdate(curSec);
                    if (isSetClosed) {
                        // 添加历史局
                        this.getRoom().addHistorySet();
                        // room超时，不进行下一局
                        if (curSec - this.getInitWaitSec() >= this.getRoomLifeTime()) {
                            this.getRoom().endRoom();
                            break;
                        }
                        // 房间满局
                        if (this.getRoom().getCurSetID() >= this.getRoom().getEndCount() || this.getRoom().isEnd()) {
                            this.getRoom().endRoom();
                            break;
                        }
                        // 检查自动解散房间
                        if (this.getRoom().getRoomTyepImpl().checkAutoDissolveRoom()) {

                            // 检查倒计时时间是否到了
                            if (this.getRoom().getRoomTyepImpl().checkCountdownTime()) {
                                // 时间到
                                this.getRoom().doDissolveRoom(false);
                            }
                            break;
                        } else {
                            // 清空倒计时时间
                            this.getRoom().getRoomTyepImpl().clearCountdownTime();
                        }
                        //小局自动解散
                        if (this.getRoom().checkNeedDissolve()) {
                            this.getRoom().doDissolveRoom(false);
                        }
                        this.getRoom().getTrusteeship().setTrusteeshipState(TrusteeshipState.Continue);
                        // 新一局大家都准备好了
                        if (this.getRoom().getRoomPosMgr().isAllContinue()) {
                            this.getRoom().getTrusteeship().setTrusteeshipState(TrusteeshipState.Wait);
                            this.getRoom().setAutoDismiss(false);
                            this.getRoom().startNewSet();
                            break;
                        }
                    }
                    break;
                case End:
                    if (this.getEndSec() > 0) {
                        boolean needClose = curSec - this.getEndSec() >= this.getRoomCloseTime();
                        if (needClose) {
                            isClosed = true;
                        }
                    }
                    this.getRoom().getTrusteeship().setTrusteeshipState(TrusteeshipState.End);
                    break;
                case Waiting:
                    if (curSec - this.getInitWaitSec() >= this.getWaitTime()) {
                        // 进入游戏中。
                        this.setRoomState(RoomState.Playing);
                    }
                    break;
                default:
                    isClosed = true;
                    break;
            }
        } catch (Throwable e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            throw e;
        } finally {
            unlock();
        }
        return isClosed;
    }

    /**
     * 房间开始后生存时间，过期自动解散 有效期3小时
     */
    @Override
    public final int getRoomLifeTime() {
        return 3 * 3600;
    }

    /**
     * 房间生存时间，过期自动解散 有效期1小时
     * 联盟房间没人 15秒没有人的话自动解散
     */
    @Override
    public final int getRoomSurvivalTime() {
        if (this.getRoom().getSpecialRoomId() > 0L) {
            if (this.getRoom().getRoomPosMgr().checkExistNoOne()) {
                // 检查存在没人
                return 15;
            }
        }
        return 1 * 3600;
    }

    @Override
    protected void clearTask() {

    }

    /**
     * 开始游戏
     */
    private void startGame() {
        if (!this.getRoom().autoStartGame()) {
            return;
        }
        if (this.getRoom().getRoomPosMgr().isAllReady()) {
            // 等待0秒
            this.setWaitTime(0);
            // 初始等待时间戳（秒）
            this.setInitWaitSec(CommTime.nowSecond());
            // 设置房间状态
            this.setRoomState(RoomState.Waiting);
            // 通知新房间创建
            this.getRoom().getRoomTyepImpl().createNewSetRoom();
            // 游戏开始操作
            this.getRoom().startGameBase();
        }
    }


}
