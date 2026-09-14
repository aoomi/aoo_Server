package core.logger.flow.disruptor.log;

import core.db.entity.BaseClarkLogEntity;
import lombok.Data;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务调度器服务
 */
@Data
public class BatchDbLogService {


    /**
     * 线程管理集合 Key:DispatchType,value:List<MessageThread>
     */
    private Map<Integer, BatchDbLogThread> threadMap = new ConcurrentHashMap<>();

    public void addThread(String name, int threadId, int bufferSize) {
        if (!getThreadMap().containsKey(threadId)) {
            getThreadMap().put(threadId, new BatchDbLogThread(name, bufferSize));
        }
    }

    /**
     * 开始任务
     */
    public void start() {
        getThreadMap().forEach((key, value) -> value.start());
    }

    public void publish(BaseClarkLogEntity executor) {
        BatchDbLogThread messageThread = this.getThreadMap().get(executor.threadId());
        if (Objects.isNull(messageThread)) {
            return;
        }
        messageThread.publish(executor);
    }

}
