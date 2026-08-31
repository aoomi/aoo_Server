package core.logger.flow.disruptor.union;

import BaseCommon.CommLog;
import com.lmax.disruptor.ExceptionHandler;

public class UnionMatchErrorHandler implements ExceptionHandler<UnionMatchBuffer> {

    @Override
    public void handleEventException(Throwable ex, long sequence, UnionMatchBuffer event) {
        CommLog.error("{}", ex);
        CommLog.error("UnionMatchErrorHandler = {}", event);
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