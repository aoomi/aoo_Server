/*
 * Decompiled with CFR 0.152.
 */
package BaseThread;

import BaseThread.CosMutex;
import BaseThread.CosMutexException;
import BaseThread.MutexInfo;
import java.util.ArrayList;

public class ThreadMutexInfo {
    private long _m_lThreadID;
    private ArrayList<MutexInfo> _m_lThreadMutexList;

    public ThreadMutexInfo(long _threadID) {
        this._m_lThreadID = _threadID;
        this._m_lThreadMutexList = new ArrayList();
    }

    public long getThreadID() {
        return this._m_lThreadID;
    }

    public void tryLock(CosMutex _cosObj) throws CosMutexException {
        MutexInfo topLvMutexInfo;
        if (_cosObj == null) {
            return;
        }
        int objMutexLevel = _cosObj.getMutexLevel();
        MutexInfo mutexInfo = null;
        if (!(this._m_lThreadMutexList.isEmpty() || (topLvMutexInfo = this._m_lThreadMutexList.get(0)).getLockLevel() < _cosObj.getMutexLevel() || (mutexInfo = this._getMutexLevelInfo(objMutexLevel)) != null && mutexInfo.getObj() == _cosObj)) {
            throw new CosMutexException("\u65e0\u6cd5\u83b7\u53d6\u9ad8\u7b49\u7ea7\u9501");
        }
        if (mutexInfo == null) {
            mutexInfo = new MutexInfo(_cosObj);
            this._m_lThreadMutexList.add(0, mutexInfo);
        }
        mutexInfo.addLockTime();
    }

    public void tryUnlock(CosMutex _cosObj) throws CosMutexException {
        if (_cosObj == null) {
            return;
        }
        int objMutexLevel = _cosObj.getMutexLevel();
        MutexInfo mutexInfo = this._getMutexLevelInfo(objMutexLevel);
        if (mutexInfo == null || mutexInfo.getObj() != _cosObj) {
            throw new CosMutexException("\u5c1d\u8bd5\u91ca\u653e\u672a\u83b7\u53d6\u7684\u9501");
        }
        mutexInfo.reduceLockTime();
        if (mutexInfo.getLockTime() <= 0) {
            this._m_lThreadMutexList.remove(mutexInfo);
        }
    }

    public boolean judgeAllMutexRelease() {
        return this._m_lThreadMutexList.isEmpty();
    }

    public void releaseAllMutex() {
        while (!this._m_lThreadMutexList.isEmpty()) {
            MutexInfo info = this._m_lThreadMutexList.get(0);
            if (info == null) continue;
            if (info.getLockTime() <= 0) {
                this._m_lThreadMutexList.remove(info);
            }
            info.releaseAllLock();
        }
    }

    protected MutexInfo _getMutexLevelInfo(int _level) {
        int i = 0;
        while (i < this._m_lThreadMutexList.size()) {
            MutexInfo info = this._m_lThreadMutexList.get(i);
            if (info.getLockLevel() == _level) {
                return info;
            }
            ++i;
        }
        return null;
    }
}

