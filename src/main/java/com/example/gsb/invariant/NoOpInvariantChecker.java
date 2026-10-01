package com.example.gsb.invariant;

import java.util.function.Supplier;

/**
 * Production-mode checker: a stateless singleton that performs no checks,
 * allocates no context objects and reports empty statistics. Because it is a
 * final class with simple methods, the JIT inlines every call to it, so
 * guarded operations run at raw speed. The surrounding operation still runs
 * normally through {@link #execute}.
 */
final class NoOpInvariantChecker implements InvariantChecker<Object> {

    static final NoOpInvariantChecker INSTANCE = new NoOpInvariantChecker();

    private NoOpInvariantChecker() {
    }

    @Override
    public void register(String name, Invariant<Object> invariant) {
    }

    @Override
    public void checkBefore(String operationType, Object target, Object... args) {
    }

    @Override
    public void checkAfter(String operationType, Object target, Object... args) {
    }

    @Override
    public <R> R execute(String operationType, Object target, Supplier<R> operation, Object... args) {
        return operation.get();
    }

    @Override
    public StatsSnapshot statistics() {
        return StatsSnapshot.EMPTY;
    }
}
