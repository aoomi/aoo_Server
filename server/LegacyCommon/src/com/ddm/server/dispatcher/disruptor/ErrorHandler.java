package com.ddm.server.dispatcher.disruptor;

import BaseCommon.CommLog;
import com.lmax.disruptor.ExceptionHandler;

/**
 * @author 0x737263
 */
public class ErrorHandler<MessageBuffer> implements ExceptionHandler<MessageBuffer> {

    @Override
    public void handleEventException(Throwable ex, long sequence, MessageBuffer event) {
        CommLog.error("{}", ex);
        CommLog.error("buffer = {}", event);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        CommLog.error("{}", ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        CommLog.error("{}", ex);
    }
}
