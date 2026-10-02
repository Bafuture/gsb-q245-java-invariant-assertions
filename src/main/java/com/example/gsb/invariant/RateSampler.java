package com.example.gsb.invariant;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lock-free, evenly distributed proportional sampler.
 *
 * <p>The 0-based call index {@code i} is selected when crossing a boundary of
 * the arithmetic progression {@code k / rate}: selected iff
 * {@code floor((i+1)*rate) > floor(i*rate)}.
 */
final class RateSampler implements Sampler {

    private final double rate;
    private final AtomicLong calls = new AtomicLong();

    RateSampler(double rate) {
        this.rate = rate;
    }

    @Override
    public boolean sample() {
        long i = calls.getAndIncrement();
        return (long) ((i + 1) * rate) != (long) (i * rate);
    }

    @Override
    public String toString() {
        return "RateSampler(rate=" + rate + ", calls=" + calls.get() + ")";
    }
}
