package com.example.gsb.invariant;

import java.util.Objects;

/**
 * Immutable configuration for an {@link InvariantChecker}.
 *
 * @param <T> type of the checked structure
 */
public final class InvariantCheckerConfig<T> {

    private final boolean enabled;
    private final double sampleRate;
    private final FailurePolicy failurePolicy;
    private final Snapshotter<? super T> snapshotter;
    private final ViolationListener violationListener;

    private InvariantCheckerConfig(Builder<T> builder) {
        this.enabled = builder.enabled;
        this.sampleRate = builder.sampleRate;
        this.failurePolicy = builder.failurePolicy;
        this.snapshotter = builder.snapshotter;
        this.violationListener = builder.violationListener;
    }

    /** When {@code false} the factory returns the zero-overhead no-op checker. */
    public boolean enabled() {
        return enabled;
    }

    /** Fraction in {@code (0, 1]} of check invocations that are actually evaluated. */
    public double sampleRate() {
        return sampleRate;
    }

    public FailurePolicy failurePolicy() {
        return failurePolicy;
    }

    public Snapshotter<? super T> snapshotter() {
        return snapshotter;
    }

    public ViolationListener violationListener() {
        return violationListener;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /** Debug-mode defaults: enabled, every call sampled, abort on first violation. */
    public static <T> InvariantCheckerConfig<T> debugDefaults() {
        return InvariantCheckerConfig.<T>builder().build();
    }

    /** Production configuration: disabled, so callers get the no-op checker. */
    public static <T> InvariantCheckerConfig<T> production() {
        return InvariantCheckerConfig.<T>builder().enabled(false).build();
    }

    public static final class Builder<T> {

        private boolean enabled = true;
        private double sampleRate = 1.0;
        private FailurePolicy failurePolicy = FailurePolicy.ABORT;
        private Snapshotter<? super T> snapshotter = String::valueOf;
        private ViolationListener violationListener = ViolationListener.STDERR;

        public Builder<T> enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * @param sampleRate fraction in {@code (0, 1]}; implemented as
         *                   "evaluate one out of round(1/sampleRate) invocations"
         */
        public Builder<T> sampleRate(double sampleRate) {
            if (Double.isNaN(sampleRate) || sampleRate <= 0.0 || sampleRate > 1.0) {
                throw new IllegalArgumentException(
                        "sampleRate must be in (0, 1], got: " + sampleRate);
            }
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder<T> failurePolicy(FailurePolicy failurePolicy) {
            this.failurePolicy = Objects.requireNonNull(failurePolicy, "failurePolicy");
            return this;
        }

        public Builder<T> snapshotter(Snapshotter<? super T> snapshotter) {
            this.snapshotter = Objects.requireNonNull(snapshotter, "snapshotter");
            return this;
        }

        public Builder<T> violationListener(ViolationListener violationListener) {
            this.violationListener = Objects.requireNonNull(violationListener, "violationListener");
            return this;
        }

        public InvariantCheckerConfig<T> build() {
            return new InvariantCheckerConfig<>(this);
        }
    }
}
