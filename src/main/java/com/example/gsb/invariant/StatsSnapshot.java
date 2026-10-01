package com.example.gsb.invariant;

/**
 * Immutable point-in-time view of checker statistics.
 *
 * @param totalChecks             number of check executions actually performed
 * @param failures                number of violated invariants observed
 * @param skippedBySampling       number of check invocations skipped due to sampling
 * @param totalCheckTimeNanos     accumulated time spent inside performed checks
 * @param averageCheckTimeNanos   {@code totalCheckTimeNanos / totalChecks}, 0 when no checks
 */
public record StatsSnapshot(
        long totalChecks,
        long failures,
        long skippedBySampling,
        long totalCheckTimeNanos,
        double averageCheckTimeNanos) {

    public static final StatsSnapshot EMPTY = new StatsSnapshot(0, 0, 0, 0L, 0.0);
}
