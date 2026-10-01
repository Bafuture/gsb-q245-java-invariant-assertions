package com.example.gsb.invariant;

/**
 * Produces a compact, human-readable summary of the checked structure, used
 * in violation reports to make post-mortem debugging possible.
 *
 * @param <T> type of the checked structure
 */
@FunctionalInterface
public interface Snapshotter<T> {

    String snapshot(T target);
}
