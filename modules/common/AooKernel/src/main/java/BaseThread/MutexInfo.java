/*
 * Decompiled with CFR 0.152.
 */
package BaseThread;

import BaseThread.CosMutex;

public class MutexInfo {
    private int _m_iMutexLevel;
    private CosMutex _m_iCosObject;
    private int _m_iLockTime;

    public MutexInfo(CosMutex _cosObj) {
        this._m_iMutexLevel = _cosObj.getMutexLevel();
        this._m_iCosObject = _cosObj;
        this._m_iLockTime = 0;
    }

    public int getLockLevel() {
        return this._m_iMutexLevel;
    }

    public int getLockTime() {
        return this._m_iLockTime;
    }

    public void addLockTime() {
        ++this._m_iLockTime;
    }

    public void reduceLockTime() {
        --this._m_iLockTime;
    }

    public void releaseAllLock() {
        while (this._m_iLockTime > 0) {
            if (this._m_iCosObject != null) {
                this._m_iCosObject.unlock();
                continue;
            }
            this._m_iLockTime = 0;
        }
    }

    public CosMutex getObj() {
        return this._m_iCosObject;
    }
}

