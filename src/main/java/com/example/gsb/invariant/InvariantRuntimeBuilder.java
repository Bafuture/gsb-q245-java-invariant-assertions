package com.example.gsb.invariant;

/**
 * Fluent builder for a standalone {@link InvariantRuntime}.
 *
 * <pre>{@code
 * InvariantRuntime rt = new InvariantRuntimeBuilder()
 *         .samplingRate(0.1)
 *         .failurePolicy(FailurePolicy.LOG_AND_CONTINUE)
 *         .build();
 * }</pre>
 */
public final class InvariantRuntimeBuilder {

    private boolean enabled = true;
    private Sampler sampler = Sampler.ALWAYS;
    private FailurePolicy failurePolicy = FailurePolicy.ABORT;
    private FailureListener listener = FailureListener.stdErr();
    private int maxRecordedFailures = 64;

    public InvariantRuntimeBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public InvariantRuntimeBuilder sampler(Sampler sampler) {
        this.sampler = sampler;
        return this;
    }

    /** Convenience: deterministic proportional sampling, see {@link Sampler#rate(double)}. */
    public InvariantRuntimeBuilder samplingRate(double rate) {
        this.sampler = Sampler.rate(rate);
        return this;
    }

    public InvariantRuntimeBuilder failurePolicy(FailurePolicy failurePolicy) {
        this.failurePolicy = failurePolicy;
        return this;
    }

    public InvariantRuntimeBuilder failureListener(FailureListener listener) {
        this.listener = listener;
        return this;
    }

    /** Capacity of the in-memory ring of recent violations (default 64). */
    public InvariantRuntimeBuilder maxRecordedFailures(int maxRecordedFailures) {
        this.maxRecordedFailures = maxRecordedFailures;
        return this;
    }

    public InvariantRuntime build() {
        if (!enabled) {
            return NoopInvariantRuntime.INSTANCE;
        }
        return new DefaultInvariantRuntime(sampler, failurePolicy, listener, maxRecordedFailures);
    }
}
