package com.example.gsb.invariant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Real checker implementation.
 *
 * <p>Concurrency notes: invariants live in a {@link CopyOnWriteArrayList} so
 * registration never blocks checking; the global invocation counter is an
 * {@link AtomicLong} shared by all threads, which gives deterministic
 * period-based sampling; statistics use {@link java.util.concurrent.atomic.LongAdder}
 * to avoid hot CAS contention during stress runs.
 */
final class EnabledInvariantChecker<T> implements InvariantChecker<T> {

    private record NamedInvariant<T>(String name, Invariant<T> invariant) {
    }

    private final long samplePeriod;
    private final FailurePolicy failurePolicy;
    private final Snapshotter<? super T> snapshotter;
    private final ViolationListener violationListener;
    private final InvariantStatistics statistics = new InvariantStatistics();
    private final AtomicLong invocationCounter = new AtomicLong();
    private final CopyOnWriteArrayList<NamedInvariant<T>> invariants = new CopyOnWriteArrayList<>();

    EnabledInvariantChecker(InvariantCheckerConfig<T> config) {
        this.samplePeriod = Math.max(1L, Math.round(1.0 / config.sampleRate()));
        this.failurePolicy = config.failurePolicy();
        this.snapshotter = config.snapshotter();
        this.violationListener = config.violationListener();
    }

    @Override
    public void register(String name, Invariant<T> invariant) {
        invariants.add(new NamedInvariant<>(
                Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(invariant, "invariant")));
    }

    @Override
    public void checkBefore(String operationType, T target, Object... args) {
        runChecks(CheckPhase.BEFORE, operationType, target, args);
    }

    @Override
    public void checkAfter(String operationType, T target, Object... args) {
        runChecks(CheckPhase.AFTER, operationType, target, args);
    }

    @Override
    public <R> R execute(String operationType, T target, Supplier<R> operation, Object... args) {
        runChecks(CheckPhase.BEFORE, operationType, target, args);
        R result = operation.get();
        runChecks(CheckPhase.AFTER, operationType, target, args);
        return result;
    }

    @Override
    public StatsSnapshot statistics() {
        return statistics.snapshot();
    }

    private void runChecks(CheckPhase phase, String operationType, T target, Object[] args) {
        if (invariants.isEmpty()) {
            return;
        }
        long invocation = invocationCounter.getAndIncrement();
        if (invocation % samplePeriod != 0L) {
            statistics.recordSkipped();
            return;
        }

        List<Object> arguments = immutableCopy(args);
        long startNanos = System.nanoTime();
        for (NamedInvariant<T> named : invariants) {
            String reason;
            Throwable cause = null;
            try {
                reason = named.invariant().check(target);
            } catch (RuntimeException | Error e) {
                reason = "invariant evaluation threw " + e;
                cause = e;
            }
            if (reason != null) {
                long durationNanos = System.nanoTime() - startNanos;
                statistics.recordFailure();
                CheckContext context = new CheckContext(
                        Thread.currentThread().getName(),
                        Thread.currentThread().getId(),
                        operationType,
                        phase,
                        arguments,
                        Instant.now(),
                        durationNanos);
                InvariantViolationException violation = new InvariantViolationException(
                        named.name(), reason, context, snapshotSafely(target), cause);
                if (failurePolicy == FailurePolicy.ABORT) {
                    statistics.recordCheck(durationNanos);
                    throw violation;
                }
                violationListener.onViolation(violation);
            }
        }
        statistics.recordCheck(System.nanoTime() - startNanos);
    }

    private String snapshotSafely(T target) {
        try {
            return snapshotter.snapshot(target);
        } catch (RuntimeException | Error e) {
            return "<snapshot failed: " + e + ">";
        }
    }

    private static List<Object> immutableCopy(Object[] args) {
        if (args == null || args.length == 0) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(args)));
    }
}
