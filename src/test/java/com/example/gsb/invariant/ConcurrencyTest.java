package com.example.gsb.invariant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ConcurrencyTest {

    /** Simulates a occasionally-corrupted structure: tracks check-context thread attribution. */
    @Test
    void countersAreExactUnderConcurrentLoad() throws Exception {
        int threads = 8;
        int perThread = 2_000;

        ConcurrentHashMap<String, Long> threadsSeen = new ConcurrentHashMap<>();
        AtomicLong violatingTargets = new AtomicLong();
        InvariantRuntime rt = new InvariantRuntimeBuilder()
                .samplingRate(0.25)
                .failurePolicy(FailurePolicy.LOG_AND_CONTINUE)
                .failureListener(record -> {
                    threadsSeen.merge(record.context().threadName(), 1L, Long::sum);
                    violatingTargets.incrementAndGet();
                })
                .build();
        rt.register(Long.class, Invariant.of("even-only", (value, ctx) -> {
            // Context must report the actual triggering thread.
            assertThat(ctx.threadName()).isEqualTo(Thread.currentThread().getName());
            return value % 2 == 0;
        }));

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            long base = t;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        long value = base * perThread + i;
                        rt.afterOperation("inc", value, value);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        InvariantStats stats = rt.stats();
        long total = (long) threads * perThread;
        assertThat(stats.checksExecuted() + stats.skippedBySampling()).isEqualTo(total);
        assertThat(stats.checksExecuted()).isEqualTo(total / 4);
        assertThat(stats.failures()).isEqualTo(violatingTargets.get());
        assertThat(stats.failures()).isPositive();
        assertThat(rt.failures()).isNotEmpty();
        assertThat(threadsSeen).hasSize(threads);
    }
}
