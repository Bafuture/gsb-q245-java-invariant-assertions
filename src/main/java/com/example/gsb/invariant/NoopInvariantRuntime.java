package com.example.gsb.invariant;

import java.util.List;

/**
 * Production implementation: every hook is an empty method.
 *
 * <p>Zero-overhead story: {@link Invariants#runtime()} returns this singleton
 * when disabled, so a hot call site becomes {@code singleton.beforeOperation(...)}
 * — a trivially inlinable no-op on a monomorphic receiver. No context object
 * is allocated, no snapshot is rendered, no sampler is consulted, and no
 * statistics are touched. {@code Object...} with zero arguments reuses a
 * shared empty array (JLS 15.12.4.2), so the varargs hooks allocate nothing
 * either.
 */
final class NoopInvariantRuntime implements InvariantRuntime {

    static final NoopInvariantRuntime INSTANCE = new NoopInvariantRuntime();

    private NoopInvariantRuntime() {
    }

    @Override
    public <T> void register(Class<T> targetType, Invariant<? super T> invariant) {
        // intentionally ignored: nothing will ever be checked
    }

    @Override
    public void beforeOperation(String operationType, Object target, Object... params) {
    }

    @Override
    public void afterOperation(String operationType, Object target, Object... params) {
    }

    @Override
    public InvariantStats stats() {
        return new InvariantStats(0, 0, 0, 0);
    }

    @Override
    public List<FailureRecord> failures() {
        return List.of();
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void reset() {
    }
}
