/*
 * Decompiled with CFR 0.152.
 */
package BaseTask.AsynTask;

import BaseServer.Monitor;
import BaseTask.AsynTask.AsyncCallBackTaskBase;
import BaseTask.AsynTask.AsyncTaskBase;
import BaseTask.AsynTask.AsyncTaskDealThread;
import BaseTask.AsynTask.AsyncTaskWrapper;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncTaskQueue {
    private ReentrantLock _m_rMutex = new ReentrantLock();
    private List<AsyncThreadQueue> taskDealQueue = new ArrayList<AsyncThreadQueue>();
    private List<AsyncTaskDealThread> taskDealThread = new ArrayList<AsyncTaskDealThread>();
    private String tag;
    private boolean isMultQueue;
    private final int maximumQueuedTasks;
    private volatile boolean closed;

    public AsyncTaskQueue(String tag, boolean isMultQueue) {
        this(tag, isMultQueue, 10000, Math.max(4, Runtime.getRuntime().availableProcessors()));
    }

    public AsyncTaskQueue(String tag, boolean isMultQueue, int maximumQueuedTasks, int dealerCount) {
        if (tag == null || tag.isBlank() || maximumQueuedTasks < 1 || dealerCount < 1)
            throw new IllegalArgumentException("valid tag, queue budget and dealer count required");
        this.tag = tag;
        this.isMultQueue = isMultQueue;
        this.maximumQueuedTasks = maximumQueuedTasks;
        this.setDealerCount(dealerCount);
    }

    private synchronized AsyncThreadQueue getSubQueue(int id) {
        if (this.isMultQueue) {
            if (this.taskDealQueue.size() <= id) {
                this.taskDealQueue.add(new AsyncThreadQueue(this.tag, id));
            }
            return this.taskDealQueue.get(id);
        }
        if (this.taskDealQueue.size() == 0) {
            this.taskDealQueue.add(new AsyncThreadQueue(this.tag, 0));
        }
        return this.taskDealQueue.get(0);
    }

    private void removeSubQueue(int id) {
        if (this.isMultQueue) {
            this.taskDealQueue.remove(id);
        } else if (id == 0) {
            this.taskDealQueue.remove(id);
        }
    }

    public void setDealerCount(int dealderCount) {
        if (dealderCount < 0) throw new IllegalArgumentException("dealer count must not be negative");
        if (closed && dealderCount > 0) throw new IllegalStateException("async queue is disposed");
        this._lock();
        try {
            int addCount = dealderCount - this.taskDealThread.size();
            while (addCount > 0) {
                int id = this.taskDealThread.size();
                AsyncThreadQueue queue = this.getSubQueue(id);
                AsyncTaskDealThread dealer = new AsyncTaskDealThread(queue, id);
                dealer.start();
                this.taskDealThread.add(dealer);
                --addCount;
            }
            while (addCount < 0) {
                if (this.taskDealThread.size() == 0) break;
                int removedId = this.taskDealThread.size() - 1;
                AsyncTaskDealThread toRemoveDealThread = this.taskDealThread.remove(removedId);
                toRemoveDealThread.dispose();
                this.removeSubQueue(removedId);
                ++addCount;
            }
        } finally {
            this._unlock();
        }
    }

    public String toString() {
        return String.format("%-20s%-20s%-20s%-20s\n", this.tag, this.isMultQueue, this.taskDealThread.size(), this.getQueueInfo());
    }

    public String getQueueInfo() {
        this._lock();
        try {
            StringBuilder sBuilder = new StringBuilder();
            int index = 0;
            while (index < this.taskDealQueue.size()) {
                AsyncThreadQueue queue = this.taskDealQueue.get(index);
                sBuilder.append(String.format("{%s:%s}", queue.getID(), queue.getSize()));
                ++index;
            }
            return sBuilder.toString();
        } finally { this._unlock(); }
    }

    public <T> void regAsynTask(AsyncTaskBase<T> _callObj, AsyncCallBackTaskBase<T> _callBackObj, long index) {
        if (_callObj == null) throw new IllegalArgumentException("async task is required");
        AsyncTaskWrapper<T> info = new AsyncTaskWrapper<T>(_callObj, _callBackObj);
        this._lock();
        try {
            if (this.closed) throw new RejectedExecutionException("async queue is disposed");
            if (this.taskDealQueue.size() == 0) throw new RejectedExecutionException("async queue has no dealer");
            if (this.isMultQueue) {
                int id = Math.floorMod(index, this.taskDealQueue.size());
                AsyncThreadQueue mgr = this.taskDealQueue.get(id);
                mgr.regAsynTask(info);
            } else {
                this.taskDealQueue.get(0).regAsynTask(info);
            }
        } finally {
            this._unlock();
        }
    }

    public <T> void regAsynTask(AsyncTaskBase<T> _callObj, AsyncCallBackTaskBase<T> _callBackObj) {
        this.regAsynTask(_callObj, _callBackObj, 0L);
    }

    public void dispose() {
        this.closed = true;
        this.setDealerCount(0);
    }

    protected void _lock() {
        this._m_rMutex.lock();
    }

    protected void _unlock() {
        this._m_rMutex.unlock();
    }

    public class AsyncThreadQueue {
        private LinkedList<AsyncTaskWrapper> infoList = new LinkedList();
        private Semaphore _m_sSemaphore = new Semaphore(0);
        private ReentrantLock _m_rMutex = new ReentrantLock();
        private String tag;
        private int id;

        public AsyncThreadQueue(String tag, int id) {
            this.tag = tag;
            this.id = id;
        }

        public int getID() {
            return this.id;
        }

        public String getTag() {
            return this.tag;
        }

        protected void _lock() {
            this._m_rMutex.lock();
        }

        protected void _unlock() {
            this._m_rMutex.unlock();
        }

        public <T> void regAsynTask(AsyncTaskWrapper<T> obj) {
            this._lock();
            try {
                if (this.infoList.size() >= maximumQueuedTasks) {
                    Monitor.getInstance().regLog();
                    throw new RejectedExecutionException("async queue capacity exceeded: " + maximumQueuedTasks);
                }
                this.infoList.add(obj);
                this._m_sSemaphore.release();
            } finally {
                this._unlock();
            }
        }

        public AsyncTaskWrapper popFirstAsynTask() {
            this._m_sSemaphore.acquireUninterruptibly();
            this._lock();
            try { return this.infoList.isEmpty() ? null : this.infoList.pop(); }
            finally { this._unlock(); }
        }

        public int getSize() {
            this._lock();
            try { return this.infoList.size(); }
            finally { this._unlock(); }
        }

        void wake() { this._m_sSemaphore.release(); }
    }
}
