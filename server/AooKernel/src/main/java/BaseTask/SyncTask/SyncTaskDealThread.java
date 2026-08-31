/*
 * Decompiled with CFR 0.152.
 */
package BaseTask.SyncTask;

import BaseCommon.CommLog;
import BaseTask.SyncTask.SyncTaskQueue;
import BaseThread.ThreadManager;
import BaseThread.ThreadMutexInfo;

public class SyncTaskDealThread
extends Thread {
    private ThreadMutexInfo _m_tmiThreadMutexInfo = null;
    private volatile boolean _m_bThreadExit = false;
    private SyncTaskQueue taskManager;
    private int _id = 0;

    public SyncTaskDealThread(SyncTaskQueue _mgr, int id) {
        this.taskManager = _mgr;
        this._id = id;
        this.setName("STT-" + _mgr.getTag() + "-" + this._id);
    }

    public void dispose() {
        this._m_bThreadExit = true;
        this.taskManager.wakeDealer();
    }

    @Override
    public void run() {
        boolean checkDeadLock = ThreadManager.getInstance().getCheckDeadLock();
        if (checkDeadLock) {
            this._m_tmiThreadMutexInfo = ThreadManager.getInstance().regThread(Thread.currentThread().getId());
            if (this._m_tmiThreadMutexInfo == null) {
                return;
            }
        }

        try {
            while (!this._m_bThreadExit) {
                SyncTaskWrapper curTask = this.taskManager.PopTask();
                if (this._m_bThreadExit) return;
                if (curTask == null) continue;
                try {
                    curTask.run();
                } catch (Exception e) {
                    CommLog.error(curTask.getClass().getName() + " Error!!", e);
                    if (checkDeadLock) this._m_tmiThreadMutexInfo.releaseAllMutex();
                }
                if (checkDeadLock && !this._m_tmiThreadMutexInfo.judgeAllMutexRelease()) {
                    CommLog.error(curTask.getClass().getName()
                            + " Still get some mutexs are not released, info:" + curTask.getInfo());
                    this._m_tmiThreadMutexInfo.releaseAllMutex();
                }
            }
        } finally {
            if (checkDeadLock) ThreadManager.getInstance().unregThread();
        }
    }
}
