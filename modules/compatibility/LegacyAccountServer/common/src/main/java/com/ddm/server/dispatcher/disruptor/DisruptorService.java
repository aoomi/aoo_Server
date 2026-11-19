package com.ddm.server.dispatcher.disruptor;

import com.ddm.server.common.utils.CommLogD;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务调度器服务
 */
@Data
public class DisruptorService {


    /**
     * 线程管理集合 Key:DispatchType,value:List<MessageThread>
     */
    private Map<Integer, MessageThread> threadMap = new ConcurrentHashMap<>();

    public void addThread(String name, int threadId, int bufferSize) {
        if (!getThreadMap().containsKey(threadId)) {
            getThreadMap().put(threadId, new MessageThread(name, bufferSize));
        }
    }

    /**
     * 开始任务
     */
    public void start() {
        getThreadMap().forEach((key, value) -> value.start());
    }

    public void publish(BaseExecutor executor) {
        MessageThread messageThread = this.getThreadMap().get(executor.threadId());
        if (Objects.isNull(messageThread)) {
            CommLogD.error("[DisruptorService]: dispatcher thread not found,id:{}", executor.threadId());
            return;
        }
        if (!messageThread.tryPublish(executor)) {
            CommLogD.error("[DisruptorService]: dispatcher queue is full,id:{}", executor.threadId());
        }
    }

}
