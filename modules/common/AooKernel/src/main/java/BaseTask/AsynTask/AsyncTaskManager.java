/*
 * Decompiled with CFR 0.152.
 */
package BaseTask.AsynTask;

import BaseTask.AsynTask.AsyncTaskQueue;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncTaskManager {
    private static final int MAXIMUM_NAMED_QUEUES = 1024;
    private static final AsyncTaskManager g_instance = new AsyncTaskManager();
    private final ConcurrentHashMap<String, AsyncTaskQueue> syncQueues = new ConcurrentHashMap<>();

    public static AsyncTaskManager getInstance() {
        return g_instance;
    }

    public synchronized AsyncTaskQueue getQueue(String tag) {
        return this.getQueue(tag, false);
    }

    public synchronized AsyncTaskQueue getQueue(String tag, boolean isMultiQueue) {
        if (tag == null || tag.isBlank()) throw new IllegalArgumentException("async queue tag is required");
        return this.syncQueues.computeIfAbsent(tag, key -> {
            if (this.syncQueues.size() >= MAXIMUM_NAMED_QUEUES)
                throw new IllegalStateException("async queue registry capacity exceeded");
            return new AsyncTaskQueue(key, isMultiQueue);
        });
    }

    public static AsyncTaskQueue getDefaultMultQueue() {
        return AsyncTaskManager.getInstance().getQueue("DB", true);
    }

    public synchronized boolean removeQueue(String tag) {
        AsyncTaskQueue queue = this.syncQueues.remove(tag);
        if (queue == null) return false;
        queue.dispose();
        return true;
    }

    public String toString() {
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("AsyncTaskManager, size:" + this.syncQueues.size());
        sBuilder.append("\n");
        sBuilder.append(String.format("%-20s%-20s%-20s%-20s\n", "tag", "MultQueue", "ThreadSize", "QueueSize"));
        for (AsyncTaskQueue queue : this.syncQueues.values()) {
            sBuilder.append(queue.toString());
        }
        return sBuilder.toString();
    }
}
