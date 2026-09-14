/*
 * Decompiled with CFR 0.152.
 */
package BaseTask.SyncTask;

import BaseCommon.BaseCommonFun;
import BaseCommon.CommLog;
import BaseTask.SyncTask.SyncTask;
import BaseTask.SyncTask.SyncTaskQueue;
import BaseTask.SyncTask.SyncTimerTask;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class SyncTaskWrapper {
    private static final int TOTAL_TIMEOUT = 3000;
    private static final int RUN_TIMEOUT = 2000;
    private static final Timer _timer = new Timer("aoo-sync-task-timeout", true);
    private SyncTask task;
    private Exception exception;
    private String info;
    private SyncTimerTask _timeout;
    private long timerMS = 0L;
    private Thread thread;
    private long regTime;
    private long id;
    private static AtomicLong idPool = new AtomicLong();
    private SyncTaskQueue parentQueue;

    public SyncTaskWrapper(SyncTask _task, long timerMS, String info, SyncTaskQueue parent) {
        this.info = info;
        this.parentQueue = parent;
        this.task = _task;
        this.timerMS = timerMS;
        try {
            throw new Exception();
        }
        catch (Exception e) {
            this.exception = e;
            this.id = idPool.incrementAndGet();
            if (this.id >= Long.MAX_VALUE) {
                idPool.set(0L);
            }
            this.regTime = BaseCommonFun.getNowTimeMS();
            return;
        }
    }

    public SyncTaskQueue getParentQueue() {
        return this.parentQueue;
    }

    public void run() {
        this.thread = Thread.currentThread();
        long startRun = BaseCommonFun.getNowTimeMS();
        this._timeout = new SyncTimerTask(this);
        _timer.schedule((TimerTask)this._timeout, 3000L);
        try {
            if (this.task != null) this.task.run();
        } finally {
            if (this._timeout != null) this._timeout.cancel();
            long endRun = BaseCommonFun.getNowTimeMS();
            long waitAndRun = endRun - this.regTime;
            long runCost = endRun - startRun;
            if (runCost >= 2000L || waitAndRun >= this.timerMS + 3000L) {
                CommLog.warn(String.format("[SyncTaskManager][done][id]:%s/[pool]:%s [total]:%sms [timer]:%sms [wait]:%sms [run]:%sms [exinfo]:%s", this.id, this.parentQueue.getNormalTaskSize(), waitAndRun, this.timerMS, startRun - this.regTime - this.timerMS, runCost, this.info));
            }
        }
    }

    public long getTimer() {
        return this.timerMS;
    }

    public Thread getThread() {
        return this.thread;
    }

    public Exception getException() {
        return this.exception;
    }

    public long getID() {
        return this.id;
    }

    public String getInfo() {
        return this.info;
    }
}
