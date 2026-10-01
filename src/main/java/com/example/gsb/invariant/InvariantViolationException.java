package com.example.gsb.invariant;

/**
 * Thrown (or reported, depending on the failure policy) when an invariant
 * does not hold. Carries the full {@link CheckContext} plus a snapshot
 * summary of the checked structure at the moment of the violation.
 */
public class InvariantViolationException extends RuntimeException {

    private final String invariantName;
    private final String reason;
    private final CheckContext context;
    private final String snapshotSummary;

    public InvariantViolationException(String invariantName, String reason,
                                       CheckContext context, String snapshotSummary) {
        this(invariantName, reason, context, snapshotSummary, null);
    }

    public InvariantViolationException(String invariantName, String reason,
                                       CheckContext context, String snapshotSummary,
                                       Throwable cause) {
        super(buildMessage(invariantName, reason, context, snapshotSummary), cause);
        this.invariantName = invariantName;
        this.reason = reason;
        this.context = context;
        this.snapshotSummary = snapshotSummary;
    }

    public String invariantName() {
        return invariantName;
    }

    public String reason() {
        return reason;
    }

    public CheckContext context() {
        return context;
    }

    public String snapshotSummary() {
        return snapshotSummary;
    }

    private static String buildMessage(String invariantName, String reason,
                                       CheckContext context, String snapshotSummary) {
        return "Invariant '" + invariantName + "' violated"
                + "\n  reason: " + reason
                + "\n  thread: " + context.threadName() + " (id=" + context.threadId() + ")"
                + "\n  operation: " + context.operationType()
                + "\n  phase: " + context.phase()
                + "\n  arguments: " + context.arguments()
                + "\n  timestamp: " + context.timestamp()
                + "\n  check duration: " + context.checkDurationNanos() + " ns"
                + "\n  snapshot: " + snapshotSummary;
    }
}
