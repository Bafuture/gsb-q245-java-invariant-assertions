package com.example.gsb.invariant;

/**
 * Receives violations when the failure policy is
 * {@link FailurePolicy#LOG_AND_CONTINUE}.
 */
@FunctionalInterface
public interface ViolationListener {

    void onViolation(InvariantViolationException violation);

    /** Default listener: prints the violation and its stack trace to stderr. */
    ViolationListener STDERR = violation -> {
        System.err.println("[invariant-violation] " + violation.getMessage());
        violation.printStackTrace(System.err);
    };
}
