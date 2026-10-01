package com.example.gsb.invariant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class InvariantCheckerConcurrencyTest {

    private static final int THREADS = 8;
    private static final int OPS_PER_THREAD = 1_000;

    @Test
    void concurrentInvocationsKeepStatisticsConsistent() throws Exception {
        InvariantChecker<AtomicLong> checker = InvariantCheckers.create(
                InvariantCheckerConfig.<AtomicLong>builder()
                        .sampleRate(0.5)
                        .failurePolicy(FailurePolicy.LOG_AND_CONTINUE)
                        .violationListener(v -> {
                        })
                        .build());
        AtomicLong counter = new AtomicLong();
        checker.register("counter-even", c -> c.get() % 2 == 0 ? null : "odd counter");

        runConcurrently(THREADS, OPS_PER_THREAD,
                () -> checker.run("increment", counter, counter::incrementAndGet));

        StatsSnapshot stats = checker.statistics();
        long invocations = (long) THREADS * OPS_PER_THREAD * 2; // BEFORE + AFTER per op
        assertThat(stats.totalChecks() + stats.skippedBySampling()).isEqualTo(invocations);
        assertThat(stats.totalChecks()).isEqualTo(invocations / 2);
        assertThat(stats.skippedBySampling()).isEqualTo(invocations / 2);
        // one registered invariant, evaluated on every performed check
        assertThat(stats.failures()).isLessThanOrEqualTo(stats.totalChecks());
        assertThat(counter.get()).isEqualTo((long) THREADS * OPS_PER_THREAD);
    }

    @Test
    void violationsFromManyThreadsAreAllRecorded() throws Exception {
        List<InvariantViolationException> recorded = new CopyOnWriteArrayList<>();
        InvariantChecker<Object> checker = InvariantCheckers.create(
                InvariantCheckerConfig.builder()
                        .failurePolicy(FailurePolicy.LOG_AND_CONTINUE)
                        .violationListener(recorded::add)
                        .build());
        checker.register("always-broken", o -> "boom");
        Object target = new Object();

        runConcurrently(THREADS, OPS_PER_THREAD,
                () -> checker.checkBefore("op", target));

        long invocations = (long) THREADS * OPS_PER_THREAD;
        assertThat(recorded).hasSize((int) invocations);
        assertThat(checker.statistics().failures()).isEqualTo(invocations);
        assertThat(recorded)
                .allSatisfy(ex -> assertThat(ex.context().threadName()).startsWith("stress-"));
    }

    private static void runConcurrently(int threads, int opsPerThread, Runnable op)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                Thread.currentThread().setName("stress-" + Thread.currentThread().getId());
                start.await();
                for (int i = 0; i < opsPerThread; i++) {
                    op.run();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdownNow();
    }
}
