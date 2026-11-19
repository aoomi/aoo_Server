/*
 * Decompiled with CFR 0.152.
 */
package BaseThread;

import BaseCommon.CommLog;
import BaseThread.CosMutexException;
import BaseThread.ThreadManager;
import BaseThread.ThreadMutexInfo;
import java.util.concurrent.locks.ReentrantLock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class CosMutex {
    private ReentrantLock _m_lMutex = new ReentrantLock(true);
    private int _m_eMutexLevel;
    private final long timeoutNanos;

    public CosMutex(int _mutexLevel) {
        this(_mutexLevel, Duration.ofSeconds(5));
    }

    public CosMutex(int _mutexLevel, Duration timeout) {
        this._m_eMutexLevel = _mutexLevel;
        if (timeout == null || timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("positive lock timeout required");
        this.timeoutNanos = timeout.toNanos();
    }

    public int getMutexLevel() {
        return this._m_eMutexLevel;
    }

    public void addMutexLevel() {
        --this._m_eMutexLevel;
    }

    public void addMutexLevel(int _level) {
        this._m_eMutexLevel -= _level;
    }

    public void reduceMutexLevel() {
        ++this._m_eMutexLevel;
    }

    public void reduceMutexLevel(int _level) {
        this._m_eMutexLevel += _level;
    }

    public void lock() {
        boolean tracked = false;
        ThreadMutexInfo threadMutexInfo = null;
        long curThreadID = Thread.currentThread().getId();
        try {
            if (ThreadManager.getInstance().getCheckDeadLock()) {
                threadMutexInfo = ThreadManager.getInstance().getThreadMutexInfo(curThreadID);
                if (threadMutexInfo == null) throw new IllegalStateException("unregistered thread cannot acquire tracked mutex");
                threadMutexInfo.tryLock(this); tracked = true;
            }
            if (!this._m_lMutex.tryLock(timeoutNanos, TimeUnit.NANOSECONDS)) {
                if (tracked) threadMutexInfo.tryUnlock(this);
                System.getLogger(CosMutex.class.getName()).log(System.Logger.Level.ERROR,
                        "Mutex timeout level=" + this._m_eMutexLevel + " queue=" + this._m_lMutex.getQueueLength());
                throw new IllegalStateException("mutex acquisition timed out");
            }
        }
        catch (InterruptedException e) {
            if (tracked) try { threadMutexInfo.tryUnlock(this); } catch (CosMutexException ignored) { }
            Thread.currentThread().interrupt(); throw new IllegalStateException("mutex acquisition interrupted", e);
        }
        catch (CosMutexException e) {
            throw new IllegalStateException("mutex order violation", e);
        }
    }

    public void unlock() {
        if (ThreadManager.getInstance().getCheckDeadLock()) {
            long curThreadID = Thread.currentThread().getId();
            ThreadMutexInfo threadMutexInfo = ThreadManager.getInstance().getThreadMutexInfo(curThreadID);
            if (threadMutexInfo == null) {
                CommLog.warn("Unreg Thread try to release mutex", new Throwable());
                return;
            }
            try {
                threadMutexInfo.tryUnlock(this);
            }
            catch (CosMutexException e) {
                CommLog.warn("\u5c1d\u8bd5\u91ca\u653e\u672a\u83b7\u53d6\u7684\u9501", e);
            }
        }
        if (!this._m_lMutex.isHeldByCurrentThread()) throw new IllegalMonitorStateException("current thread does not own mutex");
        this._m_lMutex.unlock();
    }
}
