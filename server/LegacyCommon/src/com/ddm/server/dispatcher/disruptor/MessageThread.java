package com.ddm.server.dispatcher.disruptor;

import com.ddm.server.common.utils.NamedThreadFactory;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
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
                new BlockingWaitStrategy()
        );

        disruptor.handleEventsWith(handler);
        disruptor.setDefaultExceptionHandler(new ErrorHandler<>());
    }

    public void start() {
        this.disruptor.start();
    }

    public void publish(BaseExecutor executor) {
        final long seq = getRingBuffer().next();
        final MessageBuffer buffer = getRingBuffer().get(seq);
        buffer.setExecutor(executor);
        this.disruptor.getRingBuffer().publish(seq);
    }

    public RingBuffer<MessageBuffer> getRingBuffer() {
        return this.disruptor.getRingBuffer();
    }

}
