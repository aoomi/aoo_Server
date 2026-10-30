package com.ddm.server.dispatcher.disruptor;

import com.ddm.server.dispatcher.executor.BaseExecutor;

/**
 * @author 0x737263
 */
public class MessageBuffer {

    private BaseExecutor executor;

    public void setExecutor(BaseExecutor executor) {
        this.executor = executor;
    }

    public void execute() {
        this.executor.invoke();
    }

    public void clear() {
        this.executor = null;
    }

    @Override
    public String toString() {
        return "MessageBuffer{" +
                "executor=" + executor +
                '}';
    }
}
