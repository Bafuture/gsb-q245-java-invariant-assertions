package com.example.gsb.invariant;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

/**
 * Best-effort, allocation-light one-line summary of an inspected object.
 *
 * <p>{@code toString()} is used when the object overrides it (value-style
 * classes such as {@code Integer}, {@code String}, collections and records).
 * Objects that inherit the default identity {@code toString()} are rendered
 * as {@code ClassName@identityHash} plus size hints for common container
 * types, so a broken state never depends on the target implementing
 * {@code toString}, and a container cannot flood the log via a huge
 * self-describing {@code toString}.
 */
final class Snapshot {

    /** Cap on the rendered summary length; longer summaries are truncated. */
    static final int MAX_LENGTH = 512;

    private Snapshot() {
    }

    static String summarize(Object target) {
        String s;
        if (target == null) {
            s = "null";
        } else if (target.getClass().isArray()) {
            s = arraySummary(target);
        } else if (target instanceof Iterable<?> iterable) {
            s = iterableSummary(target, iterable);
        } else if (target instanceof Map<?, ?> map) {
            s = target.getClass().getName() + "{size=" + map.size() + "}";
        } else if (usesIdentityToString(target)) {
            s = target.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(target));
        } else {
            s = String.valueOf(target);
        }
        s = s.replace('\n', ' ').replace('\r', ' ');
        if (s.length() > MAX_LENGTH) {
            s = s.substring(0, MAX_LENGTH) + "...(truncated)";
        }
        return s;
    }

    private static boolean usesIdentityToString(Object target) {
        try {
            Class<?> declaring = target.getClass().getMethod("toString").getDeclaringClass();
            return declaring == Object.class;
        } catch (NoSuchMethodException e) {
            return true;
        }
    }

    private static String arraySummary(Object array) {
        int len = Array.getLength(array);
        Class<?> component = array.getClass().getComponentType();
        StringBuilder sb = new StringBuilder()
                .append(component.getSimpleName())
                .append("[len=").append(len).append("]{");
        int limit = Math.min(len, 8);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(summarize(Array.get(array, i)));
        }
        if (len > limit) {
            sb.append(", ...(+").append(len - limit).append(" more)");
        }
        return sb.append('}').toString();
    }

    private static String iterableSummary(Object target, Iterable<?> iterable) {
        int size = -1;
        if (iterable instanceof java.util.Collection<?> c) {
            size = c.size();
        }
        StringBuilder sb = new StringBuilder()
                .append(target.getClass().getName())
                .append(size >= 0 ? "{size=" + size + "}" : "").append('[');
        Iterator<?> it = iterable.iterator();
        int n = 0;
        while (it.hasNext() && n < 8) {
            if (n > 0) {
                sb.append(", ");
            }
            sb.append(summarize(it.next()));
            n++;
        }
        if (it.hasNext()) {
            sb.append(", ...");
        }
        return sb.append(']').toString();
    }
}
