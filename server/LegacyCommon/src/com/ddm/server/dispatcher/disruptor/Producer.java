package com.ddm.server.dispatcher.disruptor;

import BaseCommon.CommLog;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class Producer {

    private final RingBuffer<MessageBuffer> ringBuffer;
    private final Executor executor;

    public Producer(RingBuffer<MessageBuffer> ringBuffer) {
        this.ringBuffer = ringBuffer;
        this.executor = null;
    }

    public Producer(Executor executor) {
        this.ringBuffer = null;
        this.executor = executor;
    }

    /** Non-blocking publication boundary used by network threads. */
    public boolean tryPublish(BaseExecutor executor) {
        if (this.executor != null) {
            if (this.executor instanceof ExecutorService service && service.isShutdown()) return false;
            try {
                this.executor.execute(executor::invoke);
                return true;
            } catch (RejectedExecutionException exception) {
                CommLog.error("[Producer]: executor rejected message", exception);
                return false;
            }
        }
        final long seq;
        try {
            seq = ringBuffer.tryNext();
        } catch (Exception exception) {
            CommLog.error("[Producer]: ring buffer has no capacity", exception);
            return false;
        }
        try {
            MessageBuffer buffer = ringBuffer.get(seq);
            buffer.setExecutor(executor);
        } catch (Exception e) {
            CommLog.error("[Producer]: error:{}", e.getMessage(), e);
        } finally {
            ringBuffer.publish(seq);
        }
        return true;
    }

    public void publish(BaseExecutor executor) {
        tryPublish(executor);
    }
}
