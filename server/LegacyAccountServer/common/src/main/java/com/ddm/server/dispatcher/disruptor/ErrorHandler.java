package com.ddm.server.dispatcher.disruptor;


import com.ddm.server.common.utils.CommLog;
import com.lmax.disruptor.ExceptionHandler;

/**
 * 自定义异常
 * @param <MessageBuffer>
 */
public class ErrorHandler<MessageBuffer> implements ExceptionHandler<MessageBuffer> {
    /**
     * 标题
     */
    private final String title;

    public ErrorHandler(String title) {
        this.title = title;
    }

    @Override
    public void handleEventException(Throwable ex, long sequence, MessageBuffer event) {
        CommLog.error("title:{},handleEventException sequence:{},Throwable:{},MessageBuffer:{}",this.title,sequence,ex,event);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        CommLog.error("title:{},handleOnStartException Throwable:{}",this.title,ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        CommLog.error("title:{},handleOnShutdownException Throwable:{}",this.title,ex);
    }
}
