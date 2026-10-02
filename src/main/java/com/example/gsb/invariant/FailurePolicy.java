package com.example.gsb.invariant;

/**
 * What the runtime does when an invariant is violated.
 */
public enum FailurePolicy {
    /** Abort the triggering operation by throwing {@link InvariantViolationException}. */
    ABORT,
    /** Record the violation (listener + bounded ring buffer) and continue. */
    LOG_AND_CONTINUE
}
