package com.example.gsb.invariant;

/**
 * Immutable aggregate counters produced by {@link InvariantRuntime#stats()}.
 *
 * @param checksExecuted number of invariant invocations actually performed
 * @param failures       number of violations observed (both aborting and logged)
 * @param skippedBySampling number of invocations skipped because the sampler rejected them
 * @param totalCheckNanos cumulative time spent inside checkers (plus snapshot rendering)
 */
public record InvariantStats(long checksExecuted,
                             long failures,
                             long skippedBySampling,
                             long totalCheckNanos) {

    public long averageCheckNanos() {
        return checksExecuted == 0 ? 0L : totalCheckNanos / checksExecuted;
    }

    @Override
    public String toString() {
        return "InvariantStats{checks=" + checksExecuted
                + ", failures=" + failures
                + ", skippedBySampling=" + skippedBySampling
                + ", avgCheckNanos=" + averageCheckNanos()
                + '}';
    }
}
