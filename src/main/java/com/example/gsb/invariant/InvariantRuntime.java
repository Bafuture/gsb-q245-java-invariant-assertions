package com.example.gsb.invariant;

import java.util.List;
import java.util.function.Supplier;

/**
 * Central entry point of the invariant assertion framework.
 *
 * <p>Obtain an instance via {@link Invariants#runtime()} (global singleton)
 * or build an isolated one with {@link InvariantRuntimeBuilder}.
 *
 * <p>All methods are thread-safe. When the runtime is disabled
 * ({@link Invariants#configure} with {@code enabled=false}), every hook is a
 * no-op: {@link NoopInvariantRuntime} performs a single boolean check per
 * call, allocates nothing and is inlined away by the JIT.
 */
public interface InvariantRuntime {

    /**
     * Registers an invariant that applies to every target object of the
     * given type (checked with {@link Class#isInstance}, so supertypes and
     * interfaces match subtypes).
     */
    <T> void register(Class<T> targetType, Invariant<? super T> invariant);

    /** Registers an invariant that applies to every target object. */
    default void register(Invariant<Object> invariant) {
        register(Object.class, invariant);
    }

    /** Run all applicable invariants before the operation executes. */
    void beforeOperation(String operationType, Object target, Object... params);

    /** Run all applicable invariants after the operation executed. */
    void afterOperation(String operationType, Object target, Object... params);

    /**
     * Runs {@code operation} between {@link #beforeOperation} and
     * {@link #afterOperation} checks.
     */
    default <R> R runOperation(String operationType, Object target, Supplier<R> operation, Object... params) {
        beforeOperation(operationType, target, params);
        try {
            return operation.get();
        } finally {
            afterOperation(operationType, target, params);
        }
    }

    /** Void variant of {@link #runOperation}. */
    default void runOperation(String operationType, Object target, Runnable operation, Object... params) {
        runOperation(operationType, target, () -> {
            operation.run();
            return null;
        }, params);
    }

    /** Snapshot of the aggregate counters. */
    InvariantStats stats();

    /** Recent violations recorded under {@link FailurePolicy#LOG_AND_CONTINUE}. */
    List<FailureRecord> failures();

    /** {@code true} if this runtime actually performs checks. */
    boolean isEnabled();

    /** Resets all statistics and recorded failures (mainly for tests). */
    void reset();
}
