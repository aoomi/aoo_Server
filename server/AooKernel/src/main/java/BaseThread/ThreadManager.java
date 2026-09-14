/*
 * Decompiled with CFR 0.152.
 */
package BaseThread;

import BaseCommon.CommLog;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadManager {
    private static final ThreadManager g_instance = new ThreadManager();
    private volatile boolean g_checkDeadLock = true;
    private final ConcurrentHashMap<Long, ThreadMutexInfo> _m_htThreadMutexInfoTable = new ConcurrentHashMap<>();

    public static ThreadManager getInstance() {
        return g_instance;
    }

    protected ThreadManager() {
    }

    public void regThread() {
        long threadID = Thread.currentThread().getId();
        this.regThread(threadID);
    }

    public ThreadMutexInfo regThread(long _threadID) {
        ThreadMutexInfo threadMutexInfo = new ThreadMutexInfo(_threadID);
        ThreadMutexInfo existing = this._m_htThreadMutexInfoTable.putIfAbsent(_threadID, threadMutexInfo);
        if (existing != null) return null;
        CommLog.info("Reg thread: " + _threadID);
        return threadMutexInfo;
    }

    public ThreadMutexInfo getThreadMutexInfo(long _threadID) {
        return this._m_htThreadMutexInfoTable.get(_threadID);
    }

    public boolean getCheckDeadLock() {
        return this.g_checkDeadLock;
    }

    public void setCheckDeadThread(boolean isCheck) {
        this.g_checkDeadLock = isCheck;
    }

    public void unregThread() {
        this.unregThread(Thread.currentThread().getId());
    }

    public void unregThread(long threadID) {
        this._m_htThreadMutexInfoTable.remove(threadID);
    }
}
