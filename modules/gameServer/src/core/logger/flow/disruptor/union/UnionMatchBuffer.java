package core.logger.flow.disruptor.union;

import jsproto.c2s.cclass.union.UnionMatchLogItem;
import lombok.Data;

@Data
public class UnionMatchBuffer {
    private UnionMatchLogItem executor;

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
