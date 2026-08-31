/*
 * Decompiled with CFR 0.152.
 */
package BaseTask.SyncTask;

import BaseCommon.CommLog;
import BaseTask.SyncTask.SyncTaskWrapper;
import java.util.TimerTask;

public class SyncTimerTask
extends TimerTask {
    SyncTaskWrapper task;

    public SyncTimerTask(SyncTaskWrapper task) {
        this.task = task;
    }

    @Override
    public void run() {
        Thread runningThread = this.task.getThread();
        String threadName = "";
        try {
            threadName = runningThread.getName();
        }
        catch (Exception exception) {
            // empty catch block
        }
        StringBuffer info = new StringBuffer();
        info.append(String.format("[%s] [SyncTaskManager][timeout][id]:%s/[pool]:%s [run] > 3000ms, [timer]:%s, exInfo:%s \n", threadName, this.task.getID(), this.task.getParentQueue().getNormalTaskSize(), this.task.getTimer(), this.task.getInfo()));
        info.append("Regist StackTrace: \n");
        int dept = 0;
        int ignoreDept = this.task.getTimer() > 0L ? 5 : 2;
        StackTraceElement[] stackTraceElementArray = this.task.getException().getStackTrace();
        int n = stackTraceElementArray.length;
        int n2 = 0;
        while (n2 < n) {
            StackTraceElement st = stackTraceElementArray[n2];
            if (++dept > ignoreDept) {
                info.append("  ");
                info.append(st.toString());
                info.append("\n");
            }
            ++n2;
        }
        info.append(String.valueOf(threadName) + " Block StackTrace: \n");
        if (runningThread != null) {
            StackTraceElement[] stackTrace = runningThread.getStackTrace();
            if (stackTrace == null) {
                info.append("  SyncTask does not contain statckInfo\n");
            } else {
                StackTraceElement[] stackTraceElementArray2 = stackTrace;
                int n3 = stackTrace.length;
                n = 0;
                while (n < n3) {
                    StackTraceElement st = stackTraceElementArray2[n];
                    info.append("  " + st.toString() + "\n");
                    ++n;
                }
            }
        } else {
            info.append("  SyncTask never run! Boring in the task_queue!\n");
        }
        CommLog.warn(info.toString());
    }
}

