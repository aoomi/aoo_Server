/*
 * Decompiled with CFR 0.152.
 */
package BaseTask.SyncTask;

import BaseTask.SyncTask.SyncTaskQueue;
import java.util.Calendar;

public class SyncTaskTimerCheckThread
extends Thread {
    private volatile boolean _m_bThreadExit;
    private int _m_iCheckTime = 50;
    private SyncTaskQueue parenTaskQueue;

    public SyncTaskTimerCheckThread(SyncTaskQueue parent) {
        this.parenTaskQueue = parent;
        this._m_bThreadExit = false;
        this.setName(this.parenTaskQueue.getTag());
    }

    public void dispose() {
        this._m_bThreadExit = true;
        this.interrupt();
    }

    @Override
    public void run() {
        this.parenTaskQueue._refreshTimerTaskNowTime();
        long startTime = this.parenTaskQueue._getNowTime();
        long now = Calendar.getInstance().getTimeInMillis();
        while (!this._m_bThreadExit) {
            if (Calendar.getInstance().getTimeInMillis() - now > 10000L) {
                now = Calendar.getInstance().getTimeInMillis();
            }
            long nowTime = this.parenTaskQueue._refreshTimerTaskNowTime();
            this.parenTaskQueue.transTimer2NormalList(startTime);
            startTime = nowTime;
            try {
                SyncTaskTimerCheckThread.sleep(this._m_iCheckTime);
            }
            catch (InterruptedException interruptedException) { if (_m_bThreadExit) return; }
        }
    }
}
