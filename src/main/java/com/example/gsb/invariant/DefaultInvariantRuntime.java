package com.example.gsb.invariant;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The real runtime: sampling, per-check context, statistics and configurable
 * failure policy.
 *
 * <p>Registered invariants live in a {@link CopyOnWriteArrayList}; hot-path
 * iteration is lock-free and registration during a stress test is safe.
 * Counters are {@link AtomicLong}s so statistics are exact under concurrency.
 */
final class DefaultInvariantRuntime implements InvariantRuntime {

    private final List<Registration<?>> registrations = new CopyOnWriteArrayList<>();
    private final AtomicLong checksExecuted = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong skippedBySampling = new AtomicLong();
    private final AtomicLong totalCheckNanos = new AtomicLong();

    private final Sampler sampler;
    private final FailurePolicy failurePolicy;
    private final FailureListener listener;
    private final int maxRecordedFailures;
    private final BoundedFailures recentFailures;

    DefaultInvariantRuntime(Sampler sampler,
                            FailurePolicy failurePolicy,
                            FailureListener listener,
                            int maxRecordedFailures) {
        this.sampler = sampler;
        this.failurePolicy = failurePolicy;
        this.listener = listener == null ? FailureListener.stdErr() : listener;
        this.maxRecordedFailures = maxRecordedFailures;
        this.recentFailures = new BoundedFailures(maxRecordedFailures);
    }

    @Override
    public <T> void register(Class<T> targetType, Invariant<? super T> invariant) {
        if (targetType == null || invariant == null) {
            throw new IllegalArgumentException("targetType and invariant must not be null");
        }
        registrations.add(new Registration<>(targetType, invariant));
    }

    @Override
    public void beforeOperation(String operationType, Object target, Object... params) {
        runChecks(CheckPoint.BEFORE, operationType, target, params);
    }

    @Override
    public void afterOperation(String operationType, Object target, Object... params) {
        runChecks(CheckPoint.AFTER, operationType, target, params);
    }

    private void runChecks(CheckPoint checkPoint, String operationType, Object target, Object[] params) {
        if (target == null || registrations.isEmpty()) {
            return;
        }
        for (Registration<?> registration : registrations) {
            if (!registration.targetType.isInstance(target)) {
                continue;
            }
            if (!sampler.sample()) {
                skippedBySampling.incrementAndGet();
                continue;
            }
            executeCheck(registration, checkPoint, operationType, target, params);
        }
    }

    private <T> void executeCheck(Registration<T> registration,
                                  CheckPoint checkPoint,
                                  String operationType,
                                  Object rawTarget,
                                  Object[] params) {
        Thread thread = Thread.currentThread();
        CheckContext context = new CheckContext(
                registration.invariant.name(),
                checkPoint,
                operationType,
                params,
                thread.getId(),
                thread.getName(),
                System.currentTimeMillis());

        T target = registration.targetType.cast(rawTarget);
        long start = System.nanoTime();
        boolean held;
        Throwable checkerError = null;
        try {
            held = registration.invariant.check(target, context);
        } catch (Throwable t) {
            held = false;
            checkerError = t;
        }
        long elapsed = System.nanoTime() - start;
        context.setElapsedNanos(elapsed);
        totalCheckNanos.addAndGet(elapsed);
        checksExecuted.incrementAndGet();

        if (held) {
            return;
        }

        failures.incrementAndGet();
        String snapshot = Snapshot.summarize(target);
        String detail = checkerError != null
                ? checkerError.getClass().getName() + ": " + checkerError.getMessage()
                : "checker returned false";
        FailureRecord record = new FailureRecord(context, snapshot, detail, checkerError);
        recentFailures.add(record);
        listener.onViolation(record);

        if (failurePolicy == FailurePolicy.ABORT) {
            throw new InvariantViolationException(context, snapshot, detail, checkerError);
        }
    }

    @Override
    public InvariantStats stats() {
        return new InvariantStats(
                checksExecuted.get(),
                failures.get(),
                skippedBySampling.get(),
                totalCheckNanos.get());
    }

    @Override
    public List<FailureRecord> failures() {
        return recentFailures.snapshot();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public synchronized void reset() {
        checksExecuted.set(0);
        failures.set(0);
        skippedBySampling.set(0);
        totalCheckNanos.set(0);
        recentFailures.clear();
    }

    private record Registration<T>(Class<T> targetType, Invariant<? super T> invariant) {
    }

    /**
     * Thread-safe bounded ring of recent violations (newest wins; old records
     * are dropped past the capacity to bound memory during long stress runs).
     */
    private static final class BoundedFailures {

        private final FailureRecord[] ring;
        private int size;
        private int head;

        BoundedFailures(int capacity) {
            this.ring = new FailureRecord[Math.max(1, capacity)];
        }

        synchronized void add(FailureRecord record) {
            ring[head] = record;
            head = (head + 1) % ring.length;
            if (size < ring.length) {
                size++;
            }
        }

        synchronized List<FailureRecord> snapshot() {
            List<FailureRecord> out = new ArrayList<>(size);
            int start = size < ring.length ? 0 : head;
            for (int i = 0; i < size; i++) {
                out.add(ring[(start + i) % ring.length]);
            }
            return List.copyOf(out);
        }

        synchronized void clear() {
            size = 0;
            head = 0;
            java.util.Arrays.fill(ring, null);
        }
    }
}
