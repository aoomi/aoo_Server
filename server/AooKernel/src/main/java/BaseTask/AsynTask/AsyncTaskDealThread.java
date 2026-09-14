/*
 * Decompiled with CFR 0.152.
 */
package BaseTask.AsynTask;

import BaseCommon.CommLog;
import BaseTask.AsynTask.AsyncTaskQueue;
import BaseTask.AsynTask.AsyncTaskWrapper;

public class AsyncTaskDealThread
extends Thread {
    private volatile boolean _m_bThreadExit = false;
    private int _id = 0;
    private AsyncTaskQueue.AsyncThreadQueue taskManager;

    public AsyncTaskDealThread(AsyncTaskQueue.AsyncThreadQueue _mgr, int id) {
        this.taskManager = _mgr;
        this._id = id;
        this.setName("ATT-" + _mgr.getTag() + "-" + this._id);
    }

    public void dispose() {
        this._m_bThreadExit = true;
        this.taskManager.wake();
    }

    @Override
    public void run() {
        while (!this._m_bThreadExit) {
            AsyncTaskWrapper info = this.taskManager.popFirstAsynTask();
            if (this._m_bThreadExit) return;
            if (info == null) continue;
            try {
                info.run();
            }
            catch (RuntimeException e) {
                CommLog.error("AsyncTaskDealThread.run", e);
            }
        }
    }
}
