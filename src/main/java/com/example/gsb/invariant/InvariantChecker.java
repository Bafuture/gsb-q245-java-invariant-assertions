package com.example.gsb.invariant;

import java.util.function.Supplier;

/**
 * Registers invariants for a concurrent structure and evaluates them
 * around its critical operations. All methods are thread-safe.
 *
 * @param <T> type of the checked structure
 */
public interface InvariantChecker<T> {

    /** Registers a named invariant. {@code invariant} must be side-effect free. */
    void register(String name, Invariant<T> invariant);

    /** Runs all registered invariants before an operation. */
    void checkBefore(String operationType, T target, Object... args);

    /** Runs all registered invariants after an operation. */
    void checkAfter(String operationType, T target, Object... args);

    /**
     * Runs BEFORE checks, then {@code operation}, then AFTER checks, returning
     * its result. With the {@link FailurePolicy#ABORT} policy a BEFORE failure
     * prevents the operation from executing.
     */
    <R> R execute(String operationType, T target, Supplier<R> operation, Object... args);

    /** Void-returning variant of {@link #execute}. */
    default void run(String operationType, T target, Runnable operation, Object... args) {
        execute(operationType, target, () -> {
            operation.run();
            return null;
        }, args);
    }

    /** Returns an immutable snapshot of the accumulated statistics. */
    StatsSnapshot statistics();
}
