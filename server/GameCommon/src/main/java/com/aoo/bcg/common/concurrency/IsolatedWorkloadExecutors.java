package com.aoo.bcg.common.concurrency;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Isolates latency-sensitive work so replay or background saturation cannot starve a room turn. */
public final class IsolatedWorkloadExecutors implements AutoCloseable {
    public enum Workload { GAMEPLAY, CHAT, REPLAY, BACKGROUND }
    public record PoolBudget(int concurrency, int queueCapacity, Duration queueWait) {
        public PoolBudget {
            if (concurrency < 1 || queueCapacity < 1 || queueWait == null || queueWait.isNegative() || queueWait.isZero())
                throw new IllegalArgumentException("positive pool budget required");
        }
    }
    private final Map<Workload, Lane> lanes = new EnumMap<>(Workload.class);

    public IsolatedWorkloadExecutors(Map<Workload, PoolBudget> budgets) {
        Objects.requireNonNull(budgets);
        for (Workload workload : Workload.values()) {
            PoolBudget budget = Objects.requireNonNull(budgets.get(workload), "missing budget for " + workload);
            lanes.put(workload, new Lane(workload, budget));
        }
    }

    public static IsolatedWorkloadExecutors productionDefaults() {
        return new IsolatedWorkloadExecutors(Map.of(
                Workload.GAMEPLAY, new PoolBudget(64, 4096, Duration.ofMillis(100)),
                Workload.CHAT, new PoolBudget(16, 1024, Duration.ofMillis(250)),
                Workload.REPLAY, new PoolBudget(8, 256, Duration.ofSeconds(1)),
                Workload.BACKGROUND, new PoolBudget(4, 128, Duration.ofSeconds(2))));
    }

    public <T> CompletableFuture<T> submit(Workload workload, Callable<T> task) {
        return lane(workload).submit(task);
    }

    public int queued(Workload workload) { return lane(workload).queued.get(); }
    private Lane lane(Workload workload) { return Objects.requireNonNull(lanes.get(workload), "unknown workload"); }

    @Override public void close() { lanes.values().forEach(Lane::close); }

    private static final class Lane implements AutoCloseable {
        private final Workload workload;
        private final PoolBudget budget;
        private final Semaphore admission;
        private final ExecutorService executor;
        private final AtomicInteger queued = new AtomicInteger();
        private Lane(Workload workload, PoolBudget budget) {
            this.workload = workload;
            this.budget = budget;
            this.admission = new Semaphore(budget.concurrency() + budget.queueCapacity(), true);
            this.executor = Executors.newFixedThreadPool(budget.concurrency(), Thread.ofPlatform()
                    .name("aoo-" + workload.name().toLowerCase() + "-", 0).factory());
        }
        private <T> CompletableFuture<T> submit(Callable<T> task) {
            Objects.requireNonNull(task);
            boolean admitted;
            try { admitted = admission.tryAcquire(budget.queueWait().toMillis(), TimeUnit.MILLISECONDS); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return CompletableFuture.failedFuture(interrupted);
            }
            if (!admitted) return CompletableFuture.failedFuture(new RejectedExecutionException(
                    workload + " starvation budget exceeded: queue=" + queued.get()));
            queued.incrementAndGet();
            var result = new CompletableFuture<T>();
            try {
                executor.execute(() -> {
                    queued.decrementAndGet();
                    try { result.complete(task.call()); }
                    catch (Throwable failure) { result.completeExceptionally(failure); }
                    finally { admission.release(); }
                });
            } catch (RejectedExecutionException rejected) {
                queued.decrementAndGet(); admission.release(); result.completeExceptionally(rejected);
            }
            return result;
        }
        @Override public void close() { executor.shutdownNow(); }
    }
}
