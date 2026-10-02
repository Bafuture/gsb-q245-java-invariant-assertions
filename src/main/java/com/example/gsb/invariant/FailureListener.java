package com.example.gsb.invariant;

/**
 * Callback invoked for every observed violation (under both failure policies).
 * Implementations must be thread-safe and non-blocking-ish; the listener runs
 * on the triggering thread.
 */
@FunctionalInterface
public interface FailureListener {

    void onViolation(FailureRecord record);

    /** Default sink: a compact one-line message on {@code System.err}. */
    static FailureListener stdErr() {
        return record -> {
            CheckContext c = record.context();
            System.err.printf(
                    "[INVARIANT VIOLATION] %s @ %s '%s' on thread %s | snapshot=%s | %s%n",
                    c.invariantName(), c.checkPoint(), c.operationType(), c.threadName(),
                    record.targetSnapshot(), record.detail());
        };
    }

    /** Discard violations silently (they remain available via runtime failures()). */
    static FailureListener noop() {
        return record -> {
        };
    }
}
