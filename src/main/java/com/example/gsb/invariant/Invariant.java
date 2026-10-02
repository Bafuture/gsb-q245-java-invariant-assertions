package com.example.gsb.invariant;

/**
 * A named invariant check registered against target objects of type {@code T}.
 *
 * <p>The checker must return normally when the invariant holds, and signal a
 * violation in one of two ways:
 * <ul>
 *   <li>return {@code false}, or</li>
 *   <li>throw any exception (typically an {@link AssertionError} from
 *       {@code assert} / AssertJ). The thrown throwable is captured as the
 *       violation cause.</li>
 * </ul>
 *
 * @param <T> type of the object being inspected
 */
@FunctionalInterface
public interface Invariant<T> {

    /**
     * @param target  the object under check; the same reference passed to the
     *                runtime invocation (never {@code null})
     * @param context the fully populated check context
     * @return {@code true} if the invariant holds
     */
    boolean check(T target, CheckContext context) throws Exception;

    /** Stable identifier used in violation messages and statistics. */
    default String name() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this));
    }

    /** Adapter for checkers expressed as a {@code Predicate}-style function. */
    static <T> Invariant<T> of(String name, Checker<T> checker) {
        String n = name;
        return new Invariant<>() {
            @Override
            public boolean check(T target, CheckContext context) throws Exception {
                return checker.check(target, context);
            }

            @Override
            public String name() {
                return n;
            }
        };
    }

    @FunctionalInterface
    interface Checker<T> {
        boolean check(T target, CheckContext context) throws Exception;
    }
}
