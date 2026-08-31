package com.ddm.server.dispatcher.disruptor;

import com.ddm.server.common.utils.BasicThreadFactory;
import lombok.Data;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Data
public class SubWorkerPoolFactory {

    private static class SingletonHolder {
        static final SubWorkerPoolFactory instance = new SubWorkerPoolFactory();
    }

    private SubWorkerPoolFactory() {
        initAndStart();
    }

    public static SubWorkerPoolFactory getInstance() {
        return SingletonHolder.instance;
    }

    private ThreadPoolExecutor executor;

    /**
     * 默认消费者数量
     */
    private static final int DEFAULT_EVENT_HANDLE_SIZE = 2;

    /**
     * 消费者数量
     */
    private int eventHandleSize = DEFAULT_EVENT_HANDLE_SIZE;

    /**
     * 线程数
     */
    private int corePoolSize = DEFAULT_EVENT_HANDLE_SIZE;


    /**
     * 初始线程数和消费者数
     *
     * @return
     */
    public final void initCorePoolSize() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (availableProcessors <= DEFAULT_EVENT_HANDLE_SIZE) {
            // 线程数 <= 消费者数量 = 消费者数量;
            return;
        }
        // 设置实际消费者数
        this.setEventHandleSize(availableProcessors);
        // 设置实际线程数
        this.setCorePoolSize(availableProcessors + 1);
    }

    public void initAndStart() {
        // 初始线程数和消费者数
        this.initCorePoolSize();
        this.executor = new ThreadPoolExecutor(
                getEventHandleSize(), getEventHandleSize(), 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1 << 15),
                new BasicThreadFactory.Builder().namingPattern("sub-dispatch-pool-%d").build(),
                new ThreadPoolExecutor.AbortPolicy());
    }


    public Producer publish() {
        return new Producer(this.executor);
    }
}

