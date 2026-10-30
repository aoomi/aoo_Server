
package com.ddm.server.dispatcher.disruptor;

import com.ddm.server.common.utils.NamedThreadFactory;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;


public class MessageThread {

    private Disruptor<MessageBuffer> disruptor;

    public MessageThread(String threadName, int bufferSize) {
        this(threadName, bufferSize, new MessageEventHandler());
    }

    public MessageThread(String threadName, int bufferSize, EventHandler<MessageBuffer> handler) {
        this.disruptor = new Disruptor<>(
                () -> new MessageBuffer(),
                bufferSize,
                new NamedThreadFactory(threadName),
                ProducerType.MULTI,
                new SleepingWaitStrategy()
        );

        disruptor.handleEventsWith(handler);
        disruptor.setDefaultExceptionHandler(new ErrorHandler<>(this.getClass().getSimpleName()));
    }

    public void start() {
        this.disruptor.start();
    }

    public boolean tryPublish(BaseExecutor executor) {
        final long seq;
        try {
            seq = getRingBuffer().tryNext();
        } catch (Exception exception) {
            return false;
        }
        final MessageBuffer buffer = getRingBuffer().get(seq);
        buffer.setExecutor(executor);
        this.disruptor.getRingBuffer().publish(seq);
        return true;
    }

    public void publish(BaseExecutor executor) {
        tryPublish(executor);
    }

    public RingBuffer<MessageBuffer> getRingBuffer() {
        return this.disruptor.getRingBuffer();
    }

}
