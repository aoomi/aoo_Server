package core.logger.flow.disruptor.log;

import core.db.entity.BaseClarkLogEntity;
import lombok.Data;

@Data
public class BatchDbLogBuffer {
    private BaseClarkLogEntity executor;
    private long sequence;

    public void clear() {
        this.executor = null;
    }

    @Override
    public String toString() {
        return "MessageBuffer{" +
                "executor=" + executor +
                '}';
    }
}
