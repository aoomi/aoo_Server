package business.global.room.task;

import business.global.room.base.AbsBaseRoom;
import cenum.PrizeType;
import cenum.room.RoomState;
import cenum.room.TrusteeshipState;
import com.ddm.server.common.utils.CommTime;

public class ArenaTaskRoom extends AbsBaseTaskRoom {

    private long aid;
    protected int m_arenaSec = 0;
    protected final int ArenaContinueTime = 15; //比赛场继续游戏

    public ArenaTaskRoom(AbsBaseRoom room, long timer, long aid) {
        super(room, timer);
        this.aid = aid;
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
                    // 检查自动准备游戏超时
                    this.autoReadyGameOvertime();
                    this.startGame();
                    break;
                case Playing:
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
                        if (curSec - this.getCreateSec() >= this.getRoomLifeTime()) {
                            this.getRoom().endRoom();
                            break;
                        }
                        // 房间满局
                        if (this.getRoom().getCurSetID() >= this.getRoom().getCount()) {
                            this.getRoom().endRoom();
                            break;
                        }
                        // 比赛场-继续游戏
                        this.arenaContinue(curSec);
                        this.getRoom().getTrusteeship().setTrusteeshipState(TrusteeshipState.Continue);
                        // 新一局大家都准备好了
                        if (this.getRoom().getRoomPosMgr().isAllContinue()) {
                            this.getRoom().getTrusteeship().setTrusteeshipState(TrusteeshipState.Wait);
                            this.getRoom().startNewSet();
                            break;
                        }
                    } else {
                        this.m_arenaSec = CommTime.nowSecond();
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
            throw e;
        } finally {
            unlock();
        }
        return isClosed;
    }


    public void arenaEndRoom() {
        if (RoomState.End.equals(this.getRoomState())) {

        } else {

        }
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
     */
    @Override
    public final int getRoomSurvivalTime() {
        return 1 * 3600;
    }

    @Override
    protected void clearTask() {

    }

    /**
     * 开始游戏
     */
    private void startGame() {
        if (this.getRoom().getRoomPosMgr().isAllReady()) {
            this.setRoomState(RoomState.Waiting);
        }
    }

    /**
     * 判断比赛是不是继续
     *
     * @param curSec 时间
     */
    protected void arenaContinue(int curSec) {
        if (getRoom().getBaseRoomConfigure().getPrizeType() == PrizeType.Gold) {
            if (curSec - this.m_arenaSec >= ArenaContinueTime) {
                // 比赛场-继续游戏
                this.getRoom().getTrusteeship().arenaContinue();
            }
        }
    }

}
