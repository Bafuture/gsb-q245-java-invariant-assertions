package com.example.gsb.invariant;

import java.util.Arrays;
import java.util.List;

/**
 * Immutable description of why and where a check ran (or failed).
 *
 * <p>Records the triggering thread, the operation type, the checkpoint
 * (before/after), a defensive copy of the operation parameters and the check
 * duration in nanoseconds.
 */
public final class CheckContext {

    private final String invariantName;
    private final CheckPoint checkPoint;
    private final String operationType;
    private final List<Object> operationParams;
    private final long threadId;
    private final String threadName;
    private final long timestampMillis;
    private volatile long elapsedNanos = -1L;

    CheckContext(String invariantName,
                 CheckPoint checkPoint,
                 String operationType,
                 Object[] operationParams,
                 long threadId,
                 String threadName,
                 long timestampMillis) {
        this.invariantName = invariantName;
        this.checkPoint = checkPoint;
        this.operationType = operationType;
        this.operationParams =
                operationParams == null ? List.of() : List.copyOf(Arrays.asList(operationParams));
        this.threadId = threadId;
        this.threadName = threadName;
        this.timestampMillis = timestampMillis;
    }

    public String invariantName() {
        return invariantName;
    }

    public CheckPoint checkPoint() {
        return checkPoint;
    }

    public String operationType() {
        return operationType;
    }

    /** Defensive, unmodifiable copy of the operation parameters. */
    public List<Object> operationParams() {
        return operationParams;
    }

    public long threadId() {
        return threadId;
    }

    public String threadName() {
        return threadName;
    }

    public long timestampMillis() {
        return timestampMillis;
    }

    /** Check duration in nanoseconds; {@code -1} while the check is still running. */
    public long elapsedNanos() {
        return elapsedNanos;
    }

    void setElapsedNanos(long elapsedNanos) {
        this.elapsedNanos = elapsedNanos;
    }

    @Override
    public String toString() {
        return "CheckContext{"
                + "invariant='" + invariantName + '\''
                + ", checkPoint=" + checkPoint
                + ", operationType='" + operationType + '\''
                + ", params=" + operationParams
                + ", thread=" + threadName + "(id=" + threadId + ")"
                + ", at=" + timestampMillis
                + ", elapsedNanos=" + elapsedNanos
                + '}';
    }
}
