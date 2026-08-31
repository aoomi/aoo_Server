/*
 * Decompiled with CFR 0.152.
 */
package BaseServer;

import BaseCommon.CommLog;
import BaseServer._ACleanMemory;
import BaseTask.AsynTask.AsyncTaskManager;
import BaseTask.SyncTask.SyncTaskManager;

public class Monitor
extends Thread {
    private boolean _m_bThreadExit = false;
    private static Monitor instance = new Monitor();
    private _ACleanMemory cleanMemory;
    private static final int MB = 0x100400;
    private boolean needLog = false;

    public static Monitor getInstance() {
        return instance;
    }

    public void regLog() {
        this.needLog = true;
    }

    public void regCleanMemory(_ACleanMemory cleanMemory) {
        this.cleanMemory = cleanMemory;
    }

    public Monitor() {
        this.setName("CosCmdTaskThread");
    }

    public void ExitThread() {
        this._m_bThreadExit = true;
    }

    @Override
    public void run() {
        while (!this._m_bThreadExit) {
            try {
                Monitor.sleep(60000L);
            }
            catch (InterruptedException e) {
                CommLog.error("Monitor", e);
            }
            if (this.needLog) {
                this.needLog = false;
                StringBuilder sBuilder = new StringBuilder();
                sBuilder.append("ThreadMonitor\n");
                sBuilder.append(SyncTaskManager.getInstance().toString());
                sBuilder.append(AsyncTaskManager.getInstance().toString());
                CommLog.warn(sBuilder.toString());
            }
            long total = Runtime.getRuntime().totalMemory() / 0x100400L;
            long free = Runtime.getRuntime().freeMemory() / 0x100400L;
            long max = Runtime.getRuntime().maxMemory() / 0x100400L;
            long usable = max - (total - free);
            if (usable >= max / 5L) continue;
            CommLog.info(String.format("[Memory] max\uff1a%10sMB total\uff1a%10sMB free\uff1a%10sMB available\uff1a%10sMB", max, total, free, usable));
            if (this.cleanMemory == null) continue;
            SyncTaskManager.task(() -> {
                try {
                    CommLog.info("[Memory] \u5269\u4f59\u5185\u5b58\u4e0d\u8db320%\u5c1d\u8bd5\u8fdb\u884c\u5185\u5b58\u91ca\u653e");
                    this.cleanMemory.run();
                }
                catch (Throwable t) {
                    CommLog.error("\u5c1d\u8bd5\u6e05\u7406\u5185\u5b58\u65f6\u53d1\u751f\u5f02\u5e38", t);
                }
            });
        }
    }

    public void outputMemoryInfo() {
        long total = Runtime.getRuntime().totalMemory() / 0x100400L;
        long free = Runtime.getRuntime().freeMemory() / 0x100400L;
        long max = Runtime.getRuntime().maxMemory() / 0x100400L;
        long usable = max - (total - free);
        CommLog.info(String.format("[Memory] max\uff1a%10sMB total\uff1a%10sMB free\uff1a%10sMB available\uff1a%10sMB", max, total, free, usable));
    }
}

