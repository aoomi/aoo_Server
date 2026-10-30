/*
 * Decompiled with CFR 0.152.
 */
package BaseTask.AsynTask;

import BaseCommon.CommLog;
import BaseTask.AsynTask.AsyncCallBackTaskBase;
import BaseTask.AsynTask.AsyncTaskBase;
import BaseTask.SyncTask.SyncTaskManager;

public class AsyncTaskWrapper<T> {
    private AsyncTaskBase<T> _m_tCallObj;
    private AsyncCallBackTaskBase<T> _m_tCallBackObj;

    public AsyncTaskWrapper(AsyncTaskBase<T> _callObj, AsyncCallBackTaskBase<T> _callBackObj) {
        this._m_tCallObj = _callObj;
        this._m_tCallBackObj = _callBackObj;
    }

    public void run() {
        T object = null;
        try {
            if (this._m_tCallObj != null) {
                object = this._m_tCallObj.doAsynTask();
            }
        }
        catch (Exception e) {
            CommLog.error("AsyncTaskWrapper.run", e);
        }
        if (this._m_tCallBackObj != null) {
            this._m_tCallBackObj.setCallBackParam(object);
            SyncTaskManager.getInstance().getQueue("AsyncCallBack").RegisterTask(this._m_tCallBackObj, "FromCosAsynTaskInfo");
        }
    }
}
