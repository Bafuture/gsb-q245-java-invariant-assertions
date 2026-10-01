package com.example.gsb.invariant;

/**
 * A single invariant over a concurrent structure. Implementations must be
 * side-effect free and thread-safe, because they may run concurrently with
 * operations on the checked structure.
 *
 * @param <T> type of the checked structure
 */
@FunctionalInterface
public interface Invariant<T> {

    /**
     * Checks the invariant against the current state of {@code target}.
     *
     * @return {@code null} when the invariant holds, otherwise a
     *         human-readable description of the violation
     */
    String check(T target);
}
