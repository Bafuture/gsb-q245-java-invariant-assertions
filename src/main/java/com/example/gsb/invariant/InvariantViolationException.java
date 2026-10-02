package com.example.gsb.invariant;

/**
 * Thrown when a registered invariant is violated while the runtime is
 * configured with {@link FailurePolicy#ABORT}.
 *
 * <p>The message carries the full {@link CheckContext} (thread, operation
 * type, parameters, checkpoint, check duration) and a snapshot summary of the
 * inspected object. The original checker throwable, if any, is attached as
 * the cause.
 */
public class InvariantViolationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient CheckContext context;
    private final transient String targetSnapshot;

    InvariantViolationException(CheckContext context, String targetSnapshot, String detail, Throwable cause) {
        super(buildMessage(context, targetSnapshot, detail), cause);
        this.context = context;
        this.targetSnapshot = targetSnapshot;
    }

    public CheckContext context() {
        return context;
    }

    public String targetSnapshot() {
        return targetSnapshot;
    }

    private static String buildMessage(CheckContext ctx, String snapshot, String detail) {
        StringBuilder sb = new StringBuilder(256)
                .append("Invariant '").append(ctx.invariantName()).append("' violated @ ")
                .append(ctx.checkPoint()).append(" operation '").append(ctx.operationType()).append("'\n")
                .append("  thread : ").append(ctx.threadName())
                .append(" (id=").append(ctx.threadId()).append(")\n")
                .append("  params : ").append(ctx.operationParams()).append('\n')
                .append("  snapshot: ").append(snapshot).append('\n')
                .append("  check  : ").append(ctx.elapsedNanos()).append(" ns\n")
                .append("  reason : ").append(detail);
        return sb.toString();
    }
}
