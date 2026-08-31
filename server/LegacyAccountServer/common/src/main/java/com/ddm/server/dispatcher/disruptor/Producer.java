package com.ddm.server.dispatcher.disruptor;

import com.ddm.server.common.utils.CommLogD;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class Producer {
    private final RingBuffer<MessageBuffer> ringBuffer;
    private final Executor executorService;

    public Producer(RingBuffer<MessageBuffer> ringBuffer) {
        this.ringBuffer = ringBuffer;
        this.executorService = null;
    }

    public Producer(Executor executorService) {
        this.ringBuffer = null;
        this.executorService = executorService;
    }

    public boolean tryPublish(BaseExecutor executor) {
        if (executorService != null) {
            if (executorService instanceof ExecutorService service && service.isShutdown()) return false;
            try {
                executorService.execute(executor::invoke);
                return true;
            } catch (RejectedExecutionException exception) {
                CommLogD.error("[Producer]: executor rejected message", exception);
                return false;
            }
        }
        final long seq;
        try {
            seq = ringBuffer.tryNext();
        } catch (Exception exception) {
            CommLogD.error("[Producer]: ring buffer has no capacity", exception);
            return false;
        }
        try {
            ringBuffer.get(seq).setExecutor(executor);
        } catch (Exception e) {
            CommLogD.error("[Producer]: error:{}", e.getMessage(), e);
        } finally {
            ringBuffer.publish(seq);
        }
        return true;
    }

    public void publish(BaseExecutor executor) {
        tryPublish(executor);
    }
}
