package com.example.gsb.invariant;

/**
 * One observed invariant violation, kept by the runtime under
 * {@link FailurePolicy#LOG_AND_CONTINUE}.
 */
public record FailureRecord(CheckContext context, String targetSnapshot, String detail, Throwable cause) {
}
