package com.ddm.server.dispatcher.disruptor;

import com.ddm.server.common.utils.BasicThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Compatibility facade for the Disruptor 3 WorkerPool removed in Disruptor 4.
 * Tasks preserve work-queue semantics: every submitted executor is consumed once.
 */
public final class RingBufferWorkerPoolFactory {
    private static final int DEFAULT_WORKERS = 2;
    private static final int QUEUE_CAPACITY = 1 << 15;

    private static class SingletonHolder {
        private static final RingBufferWorkerPoolFactory INSTANCE = new RingBufferWorkerPoolFactory();
    }

    private final ThreadPoolExecutor executor;

    private RingBufferWorkerPoolFactory() {
        int workers = Math.max(DEFAULT_WORKERS, Runtime.getRuntime().availableProcessors());
        executor = new ThreadPoolExecutor(
                workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new BasicThreadFactory.Builder().namingPattern("account-dispatcher-worker-%d").build(),
                new ThreadPoolExecutor.AbortPolicy());
        executor.prestartAllCoreThreads();
    }

    public static RingBufferWorkerPoolFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public Producer publish() {
        return new Producer(executor);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
