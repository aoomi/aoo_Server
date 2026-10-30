package core.logger.flow.disruptor.union;

import com.ddm.server.common.utils.NamedThreadFactory;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jsproto.c2s.cclass.union.UnionMatchLogItem;
import lombok.Data;

@Data
public class UnionMatchFactory {

    private static class SingletonHolder {
        static final UnionMatchFactory instance = new UnionMatchFactory();
    }


    public static UnionMatchFactory getInstance() {
        return SingletonHolder.instance;
    }


    private Disruptor<UnionMatchBuffer> disruptor;


    private UnionMatchFactory() {
        this.setDisruptor(new Disruptor<UnionMatchBuffer>(
                () -> new UnionMatchBuffer(),
                1024 * 8,
                new NamedThreadFactory("UnionMatchFactory"),
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        ));
        this.getDisruptor().handleEventsWith(new UnionMatchLogEventHandler());
        this.getDisruptor().setDefaultExceptionHandler(new UnionMatchErrorHandler());
        this.getDisruptor().start();
    }


    public void publish(UnionMatchLogItem executor) {
        final long seq = getRingBuffer().next();
        try {
            final UnionMatchBuffer buffer = getRingBuffer().get(seq);
            buffer.setExecutor(executor);
        } finally {
            this.getDisruptor().getRingBuffer().publish(seq);

        }
    }

    public RingBuffer<UnionMatchBuffer> getRingBuffer() {
        return this.disruptor.getRingBuffer();
    }


}



