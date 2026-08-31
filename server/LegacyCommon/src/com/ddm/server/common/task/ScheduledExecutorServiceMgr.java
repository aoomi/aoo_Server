package com.ddm.server.common.task;

import com.ddm.server.common.utils.BasicThreadFactory;
import lombok.Data;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

/**
 * 托管任务定时器
 *
 * @author
 */
@Data
public class ScheduledExecutorServiceMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static ScheduledExecutorServiceMgr instance = new ScheduledExecutorServiceMgr();
    }

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    // 私有化构造方法
    private ScheduledExecutorServiceMgr() {
        this.initCorePoolSize();
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                corePoolSize, new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build());
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);

    }

    /**
     * 默认消费者数量
     */
    private static final int DEFAULT_EVENT_HANDLE_SIZE = 4;

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
            // 不足4核
            return;
        }
        // 设置实际线程数
        this.setCorePoolSize(availableProcessors);
    }

    // 获取单例
    public static ScheduledExecutorServiceMgr getInstance() {
        return SingletonHolder.instance;
    }

    public ScheduledFuture getScheduledFuture(Runnable command, long initialDelay,
                                              long delay) {
        return getScheduledThreadPoolExecutor().scheduleWithFixedDelay(command, initialDelay, delay, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> schedule(Runnable command, long delayMillis) {
        return getScheduledThreadPoolExecutor().schedule(command, delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止整个线程
     */
    public void shutdown() {
        // 如果调用了shutdown()方法，则线程池处于SHUTDOWN状态，此时线程池不能够接受新的任务，它会等待所有任务执行完毕；
        this.getScheduledThreadPoolExecutor().shutdown();
        try {
            if (!this.getScheduledThreadPoolExecutor().awaitTermination(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS))
                this.getScheduledThreadPoolExecutor().shutdownNow();
        } catch (InterruptedException interrupted) {
            this.getScheduledThreadPoolExecutor().shutdownNow();
            Thread.currentThread().interrupt();
        }

    }
}
