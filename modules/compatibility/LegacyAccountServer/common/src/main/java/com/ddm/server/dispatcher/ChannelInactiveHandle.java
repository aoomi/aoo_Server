package com.ddm.server.dispatcher;

import com.ddm.server.dispatcher.executor.BaseExecutor;

import lombok.Data;


/**
 * 通道处理队列
 */
@Data
public class ChannelInactiveHandle implements BaseExecutor {


    public ChannelInactiveHandle() {
    }

    @Override
    public void invoke() {

    }

    @Override
    public int threadId() {
        return 0;
    }
}
