package core.logger.flow.disruptor.log;

import com.ddm.server.common.utils.NamedThreadFactory;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import core.db.entity.BaseClarkLogEntity;
import lombok.Data;

@Data
public class BatchDbLogThread {


    private Disruptor<BatchDbLogBuffer> disruptor;

    public BatchDbLogThread(String threadName, int bufferSize) {
        this(threadName, bufferSize, new BatchDbLogEventHandler());

    }

    private BatchDbLogThread(String threadName, int bufferSize, EventHandler<BatchDbLogBuffer> handler) {
        this.setDisruptor(new Disruptor<BatchDbLogBuffer>(
                () -> new BatchDbLogBuffer(),
                bufferSize,
                new NamedThreadFactory(String.format("%sDbLog", threadName)),
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        ));
        this.getDisruptor().handleEventsWith(handler);
        this.getDisruptor().setDefaultExceptionHandler(new BatchDbLogErrorHandler());
    }

    public void publish(BaseClarkLogEntity executor) {
        final long seq = getRingBuffer().next();
        try {
            final BatchDbLogBuffer buffer = getRingBuffer().get(seq);
            buffer.setSequence(seq);
            buffer.setExecutor(executor);
        } finally {
            this.getDisruptor().getRingBuffer().publish(seq);
        }
    }

    public RingBuffer<BatchDbLogBuffer> getRingBuffer() {
        return this.getDisruptor().getRingBuffer();
    }

    public void start() {
        this.getDisruptor().start();
    }


}



