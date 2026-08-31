package com.ddm.server.http.client;

import BaseTask.SyncTask.SyncTaskManager;
import com.ddm.server.common.CommLogD;
/**
 * @author Abe
 */
public abstract class IResponseHandler {

    public class CancelledException extends Exception {
        private static final long serialVersionUID = -421378063733917547L;

    }

    public abstract void compeleted(String response);

    final void completed(String body) {
        SyncTaskManager.task(() -> compeleted(body));
    }

    public abstract void failed(Exception exception);

    final void cancelled() {
        SyncTaskManager.task(() -> failed(new CancelledException()));
    }
}
