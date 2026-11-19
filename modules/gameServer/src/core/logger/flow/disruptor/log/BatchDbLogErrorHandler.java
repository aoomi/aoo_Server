package core.logger.flow.disruptor.log;

import BaseCommon.CommLog;
import com.lmax.disruptor.ExceptionHandler;

public class BatchDbLogErrorHandler implements ExceptionHandler<BatchDbLogBuffer> {

    @Override
    public void handleEventException(Throwable ex, long sequence, BatchDbLogBuffer event) {
        CommLog.error("{}", ex);
        CommLog.error("BatchDbLogErrorHandler = {}", event);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        CommLog.error("Exception during onStart()", ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        CommLog.error("Exception during onShutdown()", ex);
    }
}