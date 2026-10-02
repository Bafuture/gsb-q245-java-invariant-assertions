package com.example.gsb.invariant;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Global facade for the invariant assertion framework.
 *
 * <p><b>Production zero overhead.</b> The runtime is disabled by default and
 * then backed by {@link NoopInvariantRuntime}: every hook is an empty,
 * inlinable virtual call with no allocation, no sampling decision and no
 * statistics writes. Hot call sites JIT-compile away entirely. Enable it in
 * debug/stress builds either programmatically:
 *
 * <pre>{@code
 * Invariants.configure(new InvariantRuntimeBuilder()
 *         .samplingRate(0.1)
 *         .failurePolicy(FailurePolicy.LOG_AND_CONTINUE));
 * }</pre>
 *
 * or with the system property {@code -Dinvariant.assertions.enabled=true}
 * (optional knobs: {@code invariant.sampling.rate},
 * {@code invariant.failure.policy=ABORT|LOG_AND_CONTINUE}).
 */
public final class Invariants {

    /** System property that turns the global runtime on. */
    public static final String PROP_ENABLED = "invariant.assertions.enabled";
    public static final String PROP_SAMPLING_RATE = "invariant.sampling.rate";
    public static final String PROP_FAILURE_POLICY = "invariant.failure.policy";

    private static final AtomicReference<InvariantRuntime> RUNTIME =
            new AtomicReference<>(createDefault());

    private Invariants() {
    }

    /** The active global runtime (never {@code null}; a no-op when disabled). */
    public static InvariantRuntime runtime() {
        return RUNTIME.get();
    }

    /** Replaces the global runtime (typically called once during startup/test setup). */
    public static void configure(InvariantRuntime runtime) {
        if (runtime == null) {
            throw new IllegalArgumentException("runtime must not be null");
        }
        RUNTIME.set(runtime);
    }

    /** Replaces the global runtime from a builder (typically called once during startup). */
    public static void configure(InvariantRuntimeBuilder builder) {
        configure(builder.build());
    }

    /** Disables the framework globally, restoring the zero-overhead no-op. */
    public static void disable() {
        RUNTIME.set(NoopInvariantRuntime.INSTANCE);
    }

    private static InvariantRuntime createDefault() {
        if (!Boolean.getBoolean(PROP_ENABLED)) {
            return NoopInvariantRuntime.INSTANCE;
        }
        InvariantRuntimeBuilder builder = new InvariantRuntimeBuilder().enabled(true);
        String rate = System.getProperty(PROP_SAMPLING_RATE);
        if (rate != null && !rate.isBlank()) {
            builder.samplingRate(Double.parseDouble(rate.trim()));
        }
        String policy = System.getProperty(PROP_FAILURE_POLICY);
        if (policy != null && !policy.isBlank()) {
            builder.failurePolicy(FailurePolicy.valueOf(policy.trim().toUpperCase()));
        }
        return builder.build();
    }
}
