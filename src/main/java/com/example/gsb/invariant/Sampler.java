package com.example.gsb.invariant;

/**
 * Decides whether an individual check actually runs.
 *
 * <p>Implementations must be thread-safe: a single sampler is shared by all
 * invoking threads.
 */
@FunctionalInterface
public interface Sampler {

    /**
     * @return {@code true} if the next check should be executed,
     *         {@code false} if it must be skipped (and counted as skipped)
     */
    boolean sample();

    /** Run every check. */
    Sampler ALWAYS = () -> true;

    /** Run no checks (everything is counted as a sampling skip). */
    Sampler NEVER = () -> false;

    /**
     * Deterministic proportional sampler, safe under concurrency.
     *
     * <p>Every invocation increments a counter via CAS. Check {@code i} runs
     * iff {@code floor((i+1)*rate) - floor(i*rate) == 1}, i.e. the classic
     * Bresenham-style fractional-accumulator selection. This yields exactly
     * {@code round(rate*N)} executions over {@code N} calls and spreads them
     * evenly, unlike an independent per-call PRNG which is bursty and
     * probabilistic.
     *
     * @param rate inclusion rate in {@code [0.0, 1.0]}; {@code 1.0} means
     *             always, {@code 0.0} means never
     */
    static Sampler rate(double rate) {
        if (Double.isNaN(rate) || rate < 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("rate must be within [0.0, 1.0], got: " + rate);
        }
        if (rate == 1.0) {
            return ALWAYS;
        }
        if (rate == 0.0) {
            return NEVER;
        }
        return new RateSampler(rate);
    }
}
