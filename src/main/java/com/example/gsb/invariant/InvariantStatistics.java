package com.example.gsb.invariant;

import java.util.concurrent.atomic.LongAdder;

/**
 * Mutable, thread-safe counters for one enabled checker.
 * LongAdder is used so concurrent check invocations never contend on a
 * single compare-and-swap hot spot during stress tests.
 */
final class InvariantStatistics {

    private final LongAdder totalChecks = new LongAdder();
    private final LongAdder failures = new LongAdder();
    private final LongAdder skippedBySampling = new LongAdder();
    private final LongAdder totalCheckTimeNanos = new LongAdder();

    void recordCheck(long durationNanos) {
        totalChecks.increment();
        totalCheckTimeNanos.add(durationNanos);
    }

    void recordFailure() {
        failures.increment();
    }

    void recordSkipped() {
        skippedBySampling.increment();
    }

    StatsSnapshot snapshot() {
        long checks = totalChecks.sum();
        long totalTime = totalCheckTimeNanos.sum();
        double average = checks == 0 ? 0.0 : (double) totalTime / checks;
        return new StatsSnapshot(checks, failures.sum(), skippedBySampling.sum(),
                totalTime, average);
    }
}
