/*
 * Decompiled with CFR 0.152.
 */
package BaseTask.SyncTask;

import BaseServer.Monitor;
import BaseTask.SyncTask.SyncTask;
import BaseTask.SyncTask.SyncTaskDealThread;
import BaseTask.SyncTask.SyncTaskTimerCheckThread;
import BaseTask.SyncTask.SyncTaskWrapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class SyncTaskQueue {
    private String tag;
    private LinkedList<SyncTaskWrapper> _m_lNormalTaskList;
    private ReentrantLock _m_lNormalTaskMutex;
    private List<SyncTaskDealThread> taskDealThread = new ArrayList<SyncTaskDealThread>();
    private SyncTaskTimerCheckThread timerCheck;
    private HashMap<Long, ArrayList<SyncTaskWrapper>> _m_htTimerTaskTable;
    private ReentrantLock _m_lTimerTaskMutex;
    private int _m_iTimerTaskCheckTime = 50;
    private volatile long _m_lNowTime;
    private Semaphore _m_sTaskEvent;
    private final int maximumQueuedTasks;
    private final AtomicInteger outstandingTasks = new AtomicInteger();
    private volatile boolean closed;

    public SyncTaskQueue(String tag) {
        this(tag, 10000, Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    public SyncTaskQueue(String tag, int maximumQueuedTasks, int dealerCount) {
        if (tag == null || tag.isBlank() || maximumQueuedTasks < 1 || dealerCount < 1)
            throw new IllegalArgumentException("valid tag, queue budget and dealer count required");
        this.tag = tag;
        this.maximumQueuedTasks = maximumQueuedTasks;
        this._m_lNormalTaskList = new LinkedList();
        this._m_lNormalTaskMutex = new ReentrantLock();
        this._m_htTimerTaskTable = new HashMap();
        this._m_lTimerTaskMutex = new ReentrantLock();
        this._m_sTaskEvent = new Semaphore(0);
        this._refreshTimerTaskNowTime();
        this.timerCheck = new SyncTaskTimerCheckThread(this);
        this.timerCheck.start();
        this.setDealerCount(dealerCount);
    }

    public synchronized void setDealerCount(int dealderCount) {
        if (dealderCount < 0) throw new IllegalArgumentException("dealer count must not be negative");
        if (closed && dealderCount > 0) throw new IllegalStateException("sync queue is disposed");
        int addCount = dealderCount - this.taskDealThread.size();
        while (addCount > 0) {
            SyncTaskDealThread newTaskDealThread = new SyncTaskDealThread(this, this.taskDealThread.size());
            newTaskDealThread.start();
            this.taskDealThread.add(newTaskDealThread);
            --addCount;
        }
        while (addCount < 0) {
            if (this.taskDealThread.size() == 0) break;
            SyncTaskDealThread toRemoveDealThread = this.taskDealThread.remove(this.taskDealThread.size() - 1);
            toRemoveDealThread.dispose();
            ++addCount;
        }
    }

    public void dispose() {
        this.closed = true;
        this.timerCheck.dispose();
        this.setDealerCount(0);
        this._lockNormalTaskList();
        try {
            this.outstandingTasks.addAndGet(-this._m_lNormalTaskList.size());
            this._m_lNormalTaskList.clear();
        }
        finally { this._unlockNormalTaskList(); }
        this._lockTimerTaskList();
        try {
            for (ArrayList<SyncTaskWrapper> tasks : this._m_htTimerTaskTable.values())
                this.outstandingTasks.addAndGet(-tasks.size());
            this._m_htTimerTaskTable.clear();
        }
        finally { this._unlockTimerTaskList(); }
    }

    public String getTag() {
        return this.tag;
    }

    public void RegisterTask(SyncTask _task, String info) {
        if (_task == null) throw new IllegalArgumentException("sync task is required");
        reserve();
        this._lockNormalTaskList();
        try {
            if (this.closed) throw new RejectedExecutionException("sync queue is disposed");
            this._m_lNormalTaskList.add(new SyncTaskWrapper(_task, 0L, info, this));
            this._releaseTaskEvent();
        } catch (RuntimeException failure) {
            this.outstandingTasks.decrementAndGet();
            throw failure;
        } finally {
            this._unlockNormalTaskList();
        }
    }

    public void RegisterTask(SyncTask _task) {
        this.RegisterTask(_task, "");
    }

    public void RegisterTask(SyncTask _task, int _time) {
        if (_time <= 0) {
            this.RegisterTask(_task);
            return;
        }
        this._addTimerTask(_time, _task, "");
    }

    public void RegisterTask(SyncTask _task, int _time, String info) {
        this._addTimerTask(_time, _task, info);
    }

    public void RegisterTask(SyncTask _task, long _time) {
        this._addTimerTask(_time, _task, "");
    }

    public void RegisterTask(SyncTask _task, long _time, String info) {
        this._addTimerTask(_time, _task, info);
    }

    public SyncTaskWrapper PopTask() {
        this._acquireTaskEvent();
        this._lockNormalTaskList();
        try {
            if (this._m_lNormalTaskList.isEmpty()) return null;
            SyncTaskWrapper task = this._m_lNormalTaskList.removeFirst();
            this.outstandingTasks.decrementAndGet();
            return task;
        } finally {
            this._unlockNormalTaskList();
        }
    }

    public void transTimer2NormalList(long _startTime) {
        ArrayList<SyncTaskWrapper> needAddTaskList = this._popTimerTask(_startTime);
        if (needAddTaskList != null && !needAddTaskList.isEmpty()) {
            this.RegisterTaskList(needAddTaskList);
        }
    }

    private void RegisterTaskList(ArrayList<SyncTaskWrapper> _taskList) {
        this._lockNormalTaskList();
        try {
            int taskCount = _taskList.size();
            if (this.closed) {
                this.outstandingTasks.addAndGet(-taskCount);
                return;
            }
            this._m_lNormalTaskList.addAll(_taskList);
            this._releaseTaskEvent(taskCount);
        } finally { this._unlockNormalTaskList(); }
    }

    protected long _getNowTime() {
        return this._m_lNowTime;
    }

    protected long _refreshTimerTaskNowTime() {
        long nowTime = new Date().getTime();
        int deltaTime = (int)(nowTime % (long)this._m_iTimerTaskCheckTime);
        this._m_lNowTime = nowTime - (long)deltaTime;
        return this._m_lNowTime;
    }

    protected void _lockNormalTaskList() {
        this._m_lNormalTaskMutex.lock();
    }

    protected void _unlockNormalTaskList() {
        this._m_lNormalTaskMutex.unlock();
    }

    protected void _lockTimerTaskList() {
        this._m_lTimerTaskMutex.lock();
    }

    protected void _unlockTimerTaskList() {
        this._m_lTimerTaskMutex.unlock();
    }

    protected void _releaseTaskEvent() {
        this._m_sTaskEvent.release();
    }

    protected void _releaseTaskEvent(int count) {
        this._m_sTaskEvent.release(count);
    }

    protected void _acquireTaskEvent() {
        this._m_sTaskEvent.acquireUninterruptibly();
    }

    public String toString() {
        return String.format("%-20s%-20s%-20s%-20s\n", this.tag, this.taskDealThread.size(), this.getNormalTaskSize(), this.getTimerTaskSize());
    }

    public int getTimerTaskSize() {
        this._lockTimerTaskList();
        try {
            int ret = 0;
            for (ArrayList<SyncTaskWrapper> cnt : this._m_htTimerTaskTable.values()) ret += cnt.size();
            return ret;
        } finally { this._unlockTimerTaskList(); }
    }

    public int getNormalTaskSize() {
        this._lockNormalTaskList();
        try { return this._m_lNormalTaskList.size(); }
        finally { this._unlockNormalTaskList(); }
    }

    protected void _addTimerTask(long _waitTime, SyncTask _task, String info) {
        if (_waitTime <= 0 || _task == null) throw new IllegalArgumentException("positive delay and task required");
        reserve();
        try {
            long _dealTime = Math.addExact(this._getNowTime(), _waitTime);
            int deltaTime = (int)(_dealTime % (long)this._m_iTimerTaskCheckTime);
            long realDealTime = deltaTime == 0 ? _dealTime
                    : Math.addExact(_dealTime - (long)deltaTime, (long)this._m_iTimerTaskCheckTime);
            this._lockTimerTaskList();
            try {
                if (this.closed) throw new RejectedExecutionException("sync queue is disposed");
                ArrayList<SyncTaskWrapper> taskList = this._m_htTimerTaskTable.get(realDealTime);
                if (taskList == null) {
                    taskList = new ArrayList();
                    this._m_htTimerTaskTable.put(realDealTime, taskList);
                }
                taskList.add(new SyncTaskWrapper(_task, _waitTime, info, this));
            } finally {
                this._unlockTimerTaskList();
            }
        } catch (RuntimeException failure) {
            this.outstandingTasks.decrementAndGet();
            throw failure;
        }
    }

    protected ArrayList<SyncTaskWrapper> _popTimerTask(long _startTime) {
        ArrayList<SyncTaskWrapper> list = new ArrayList<SyncTaskWrapper>();
        this._lockTimerTaskList();
        try {
            long endTime = this._getNowTime();
            long time = _startTime;
            while (time <= endTime) {
                ArrayList<SyncTaskWrapper> tmpList = this._m_htTimerTaskTable.remove(time);
                if (tmpList != null) list.addAll(tmpList);
                if (time > Long.MAX_VALUE - (long)this._m_iTimerTaskCheckTime) break;
                time += (long)this._m_iTimerTaskCheckTime;
            }
            return list;
        } finally { this._unlockTimerTaskList(); }
    }

    void wakeDealer() { this._releaseTaskEvent(); }

    private void reserve() {
        int current;
        do {
            if (this.closed) throw new RejectedExecutionException("sync queue is disposed");
            current = outstandingTasks.get();
            if (current >= maximumQueuedTasks) {
                Monitor.getInstance().regLog();
                throw new RejectedExecutionException("sync queue capacity exceeded: " + maximumQueuedTasks);
            }
        } while (!outstandingTasks.compareAndSet(current, current + 1));
    }
}
