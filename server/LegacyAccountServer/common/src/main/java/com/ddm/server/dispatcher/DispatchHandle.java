package com.ddm.server.dispatcher;

import com.ddm.server.dispatcher.executor.BaseExecutor;

import lombok.Data;

/**
 * 调度器队列
 */
@Data
public class DispatchHandle implements BaseExecutor {
    /**
     * 消息内容
     */
    private Object message;

    public DispatchHandle() {

    }

    @Override
    public void invoke() {

    }

    @Override
    public int threadId() {
        return 0;
    }
}
