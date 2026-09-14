package business.global.room.task;

import BaseCommon.CommLog;
import BaseThread.BaseMutexInstance;
import business.global.room.NormalRoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import cenum.LockLevelEnum;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.task.ScheduledExecutorServiceMgr;
import com.ddm.server.common.utils.CommTime;
import core.db.persistence.BaseDao;
import lombok.Data;

import java.util.concurrent.ScheduledFuture;

/**
 * 游戏房间线程父类
 *
 * @author Administrator
 */
@Data
public abstract class AbsBaseTaskRoom {
    /**
     * 创建时间
     */
    private int createSec;
    /**
     * 结束时间
     */
    private int endSec;
    /**
     * 开始时间
     */
    private int startSec;
    /**
     * 初始等待时间戳（秒）
     */
    private int initWaitSec;
    /**
     * 等待时间（秒）
     */
    private int waitTime;
    /**
     * 房间状态
     */
    protected volatile RoomState roomState = RoomState.Init;
    /**
     * 房间结束存在时间 60s
     */
    private final int roomCloseTime = 5;
    /**
     * 房间线程锁
     */
    private final BaseMutexInstance _lock = new BaseMutexInstance();
    /**
     * 房间信息
     */
    private AbsBaseRoom room = null;
    /**
     * 任务触发器
     */
    @SuppressWarnings("rawtypes")
    private ScheduledFuture scheduledFuture;

    public AbsBaseTaskRoom(AbsBaseRoom room, long timer) {
        // 获取高级锁,当前锁等级 11级
        this._lock.reduceMutexLevel(LockLevelEnum.LEVLE_1.value());
        // 房间信息
        this.setRoom(room);
        // 创建时间
        this.setCreateSec(CommTime.nowSecond());
        // 初始房间状态
        this.setRoomState(RoomState.Init);
        // 开启定时器
        this.startTimer(timer);
    }

    /**
     * 清空
     */
    public void clear() {
        // 房间信息
        this.setRoom(null);
        this.clearTask();

    }

    /**
     * 清空线程
     */
    protected abstract void clearTask();

    /**
     * 开启定时器
     *
     * @param timer 时间
     */
    public void startTimer(long timer) {
        this.setScheduledFuture(ScheduledExecutorServiceMgr.getInstance().getScheduledFuture(() -> {
            try {
                boolean end = update();
                if (end) {
                    if (getRoom() != null) {
                        AbsBaseRoom absBaseRoom = NormalRoomMgr.getInstance().getRoomByKey(getRoom().getRoomKey());
                        ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(getRoom().getRoomKey());
                        if (absBaseRoom != null) {
                            CommLogD.error("room end in normalRoom roomId={}, roomKey={}", absBaseRoom.getRoomID(), absBaseRoom.getRoomKey());
                        }
                        if (shareRoom != null) {
                            CommLogD.error("room end in shareRoom roomId={}, roomKey={}", shareRoom.getRoomId(), shareRoom.getRoomKey());
                        }
                        //本地缓存已经清掉，redis还有存在的房间这里清理掉
                        if (absBaseRoom == null && shareRoom != null) {
                            CommLogD.error("remove shareRoom roomId={}, roomKey={}", shareRoom.getRoomId(), shareRoom.getRoomKey());
                            ShareRoomMgr.getInstance().removeShareRoom(shareRoom.getRoomKey());
                        }
                    }
                    getRoom().clearEndRoom();
                    this.cancelTimer();
                    clear();
                }
            } catch (Exception e) {
                BaseDao.stackTrace("startTimer", e);
                CommLog.error("[AbsBaseTaskRoom]:[{}] roomId:{} error:{}", getRoom().getBaseRoomConfigure().getGameType().toString(), getRoom().getRoomID(), e.getMessage(), e);
            }

        }, 0, timer));
    }

    /**
     * 关闭定时器
     */
    public void cancelTimer() {
        if (null != this.getScheduledFuture()) {
            this.getScheduledFuture().cancel(true);
            this.getScheduledFuture().isCancelled();
            this.setScheduledFuture(null);
        }
    }

    /**
     * 更新
     *
     * @return
     */
    protected abstract boolean update();

    /**
     * 锁
     */
    public void lock() {
        _lock.lock();
    }

    /**
     * 解锁
     */
    public void unlock() {
        _lock.unlock();
    }


    /**
     * 房间开始后生存时间，过期自动解散 有效期3小时
     *
     * @return
     */
    public abstract int getRoomLifeTime();

    /**
     * 房间生存时间，过期自动解散 有效期1小时
     *
     * @return
     */
    public abstract int getRoomSurvivalTime();

    public RoomState getRoomState() {
        return RoomState.Waiting.equals(this.roomState) ? RoomState.Playing : this.roomState;
    }

    /**
     * 检查自动准备游戏超时
     */
    protected void autoReadyGameOvertime() {
        if (this.getRoom().autoReadyGame()) {
            // 设置所有玩家自动准备
            if (null != this.getRoom().getCurSet() && this.getRoom().getCurSet().getSetID() != 1) {
                this.getRoom().getRoomPosMgr().checkAllAutoReadyGameOvertime();
            }
        } else {
            //手动准备模式，即人满后玩家如果在30秒内未处于准备状态，则自动剔除房间，返回提示：“由于长时间未准备，您已被请出房间”
            if (this.getRoom().is30SencondTimeOut()) {
                this.getRoom().getRoomPosMgr().checkKickOutTimeOutPlayer();
            }
        }
    }

    /**
     * 修改房间状态
     *
     * @param roomState
     */
    public void setRoomState(RoomState roomState) {
        this.roomState = roomState;
        if (Config.isShare()) {
            ShareRoomMgr.getInstance().setRoomState(this.room.getRoomKey(), roomState);
        }
    }
}
