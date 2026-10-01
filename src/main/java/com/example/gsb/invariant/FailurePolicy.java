package com.example.gsb.invariant;

/** Degradation strategy applied when an invariant is violated. */
public enum FailurePolicy {

    /** Throw {@link InvariantViolationException} immediately, aborting the operation. */
    ABORT,

    /** Report the violation to the configured listener and keep running. */
    LOG_AND_CONTINUE
}
