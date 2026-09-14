/*
 * Decompiled with CFR 0.152.
 */
package BaseTask.SyncTask;

import BaseCommon.CommLog;
import BaseTask.SyncTask.SyncTask;
import BaseTask.SyncTask.SyncTaskQueue;
import BaseTask.SyncTask.SyncTimer;
import java.util.concurrent.ConcurrentHashMap;

public class SyncTaskManager {
    private static final int MAXIMUM_NAMED_QUEUES = 1024;
    private static final SyncTaskManager g_instance = new SyncTaskManager();
    private final ConcurrentHashMap<String, SyncTaskQueue> syncQueues = new ConcurrentHashMap<>();

    public static SyncTaskManager getInstance() {
        return g_instance;
    }

    public synchronized SyncTaskQueue getQueue(String tag) {
        if (tag == null || tag.isBlank()) throw new IllegalArgumentException("sync queue tag is required");
        return this.syncQueues.computeIfAbsent(tag, key -> {
            if (this.syncQueues.size() >= MAXIMUM_NAMED_QUEUES)
                throw new IllegalStateException("sync queue registry capacity exceeded");
            return new SyncTaskQueue(key);
        });
    }

    public synchronized boolean removeQueue(String tag) {
        SyncTaskQueue queue = this.syncQueues.remove(tag);
        if (queue == null) return false;
        queue.dispose();
        return true;
    }

    public static void task(SyncTask _task) {
        SyncTaskManager.getInstance().getQueue("Default").RegisterTask(_task);
    }

    public static void task(SyncTask _task, int _time) {
        SyncTaskManager.getInstance().getQueue("Default").RegisterTask(_task, _time);
    }

    public static void task(SyncTask _task, long _time) {
        SyncTaskManager.getInstance().getQueue("Default").RegisterTask(_task, _time);
    }

    public static void task(SyncTask _task, String info) {
        SyncTaskManager.getInstance().getQueue("Default").RegisterTask(_task, info);
    }

    public static void task(SyncTask _task, int _time, String info) {
        SyncTaskManager.getInstance().getQueue("Default").RegisterTask(_task, _time, info);
    }

    public static void task(SyncTask _task, long _time, String info) {
        SyncTaskManager.getInstance().getQueue("Default").RegisterTask(_task, _time, info);
    }

    public static void schedule(final int interval, final SyncTimer timer) {
        SyncTaskManager.task(new SyncTask(){

            @Override
            public void run() {
                boolean cont = true;
                try {
                    cont = timer.run();
                }
                catch (Exception e) {
                    CommLog.error("schedule Exception", e);
                }
                if (cont) {
                    SyncTaskManager.task((SyncTask)this, interval);
                }
            }
        }, interval);
    }

    public String toString() {
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("SyncTaskManager, size:" + this.syncQueues.size());
        sBuilder.append("\n");
        sBuilder.append(String.format("%-20s%-20s%-20s%-20s\n", "tag", "ThreadSize", "TaskSize", "TimerSize"));
        for (SyncTaskQueue queue : this.syncQueues.values()) {
            sBuilder.append(queue.toString());
        }
        return sBuilder.toString();
    }
}
