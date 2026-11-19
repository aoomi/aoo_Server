package com.ddm.server.dispatcher.disruptor;

import com.ddm.server.common.utils.BasicThreadFactory;
import lombok.Data;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Data
public class HttpBufferWorkerPoolFactory {

    private static class SingletonHolder {
        static final HttpBufferWorkerPoolFactory instance = new HttpBufferWorkerPoolFactory();
    }

    private HttpBufferWorkerPoolFactory() {
        initAndStart();
    }

    public static HttpBufferWorkerPoolFactory getInstance() {
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
        if (availableProcessors >= 4) {
            // 设置实际消费者数
            this.setEventHandleSize(4);
            // 设置实际线程数
            this.setCorePoolSize(5);
        } else {
            // 设置实际消费者数
            this.setEventHandleSize(2);
            // 设置实际线程数
            this.setCorePoolSize(3);
        }
    }

    public void initAndStart() {
        // 初始线程数和消费者数
        this.initCorePoolSize();
        this.executor = new ThreadPoolExecutor(
                getEventHandleSize(), getEventHandleSize(), 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1 << 15),
                new BasicThreadFactory.Builder().namingPattern("http-dispatch-pool-%d").build(),
                new ThreadPoolExecutor.AbortPolicy());
    }


    public Producer publish() {
        return new Producer(this.executor);
    }
}

