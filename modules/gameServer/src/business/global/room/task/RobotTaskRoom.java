package business.global.room.task;

import business.global.room.base.AbsBaseRoom;
import cenum.room.RoomState;
import cenum.room.TrusteeshipState;
import com.ddm.server.common.utils.CommTime;

/**
 * 机器人房间线程
 *
 * @author Administrator
 */
public class RobotTaskRoom extends AbsBaseTaskRoom {
    // 检查房间内是否没人
    private int noOneTime = CommTime.nowSecond();

    public RobotTaskRoom(AbsBaseRoom room, long timer) {
        super(room, timer);
    }

    public boolean isAtOnceStart() {
        return getRoom().atOnceStartGame();
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
                    } else {
                        if (this.noOneTime - curSec >= 10) {
                            if (this.getRoom().getRoomPosMgr().checkExistNoOne()) {
                                this.getRoom().doDissolveRoom(false);
                            }
                            this.noOneTime = curSec;
                        }
                        // 检查自动准备游戏超时
                        this.autoReadyGameOvertime();
                        if (!isAtOnceStart()) {
                            this.startGame(curSec);
                        } else {
                            atOnceStartGame();
                        }
                    }
                    break;
                case Playing:
                    this.getRoom().getTrusteeship().trusteeshipTask();
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
                        // 结算
                        this.getRoom().endRoom();
                        break;
                    } else {
                        // room超时，不进行下一局
                        if (curSec - this.getCreateSec() >= this.getRoomLifeTime()) {
                            this.getRoom().endRoom();
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
                default:
                    isClosed = true;
                    break;
            }
        } catch (Throwable e) {
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
        return 2100;
    }

    /**
     * 房间生存时间，过期自动解散 有效期1小时
     */
    @Override
    public final int getRoomSurvivalTime() {
        return 300;
    }

    @Override
    protected void clearTask() {

    }

    /**
     * 开始游戏
     */
    private void startGame(int sec) {
        // 当前线程时间 >= 开始时间 + 2(秒)
        if (this.getStartSec() > 0 && sec >= this.getStartSec() + 2) {
            // 游戏开始
            this.setRoomState(RoomState.Playing);
            return;
        }
        // 检查所有玩家是否存在
        if (this.getRoom().getRoomPosMgr().isAllReady()) {
            // 检查是否设置开始时间
            if (this.getStartSec() <= 0) {
                // 开始时间
                this.setStartSec(sec);
            }
        } else {
            // 重置开始时间
            this.setStartSec(0);
        }
    }

    /**
     * 立即开始游戏
     */
    private void atOnceStartGame() {
        if (this.getRoom().getRoomPosMgr().isAllReady()) {
            // 检查是否设置开始时间
            this.setRoomState(RoomState.Playing);
            return;
        }
    }

    /**
     * 检查自动准备游戏超时
     */
    @Override
    protected void autoReadyGameOvertime() {
        // 设置所有玩家自动准备
        this.getRoom().getRoomPosMgr().checkAllAutoReadyGameOvertime();
    }

}
