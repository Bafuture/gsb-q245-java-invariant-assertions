package com.example.gsb.invariant;

/**
 * Entry point for obtaining {@link InvariantChecker} instances.
 *
 * <p>Zero-overhead story: {@code create()} inspects the {@code enabled} flag
 * exactly once, at wiring time. With {@code enabled == false} it returns the
 * global {@link NoOpInvariantChecker} singleton; no enabled-checking code or
 * flag read is ever executed on the hot path, and since the concrete type is
 * known, the JIT inlines its empty methods away.
 */
public final class InvariantCheckers {

    private InvariantCheckers() {
    }

    /**
     * Creates a real checker, or the no-op singleton when
     * {@link InvariantCheckerConfig#enabled()} is {@code false}.
     */
    public static <T> InvariantChecker<T> create(InvariantCheckerConfig<T> config) {
        if (!config.enabled()) {
            return disabled();
        }
        return new EnabledInvariantChecker<>(config);
    }

    /** Returns the shared zero-overhead no-op checker. */
    @SuppressWarnings("unchecked")
    public static <T> InvariantChecker<T> disabled() {
        return (InvariantChecker<T>) NoOpInvariantChecker.INSTANCE;
    }
}
