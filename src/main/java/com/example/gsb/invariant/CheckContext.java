package com.example.gsb.invariant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable description of one invariant-check execution: which thread
 * triggered it, for which operation (with arguments), and how long the
 * check took.
 */
public record CheckContext(
        String threadName,
        long threadId,
        String operationType,
        CheckPhase phase,
        List<Object> arguments,
        Instant timestamp,
        long checkDurationNanos) {

    public CheckContext {
        arguments = arguments == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(arguments));
    }
}
