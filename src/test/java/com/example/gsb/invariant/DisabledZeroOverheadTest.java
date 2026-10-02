package com.example.gsb.invariant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Requirement 4: when disabled, every hook is backed by
 * {@link NoopInvariantRuntime} — an empty inlinable method, no allocation,
 * no sampler work, no counter writes.
 */
class DisabledZeroOverheadTest {

    @AfterEach
    void restore() {
        Invariants.disable();
    }

    @Test
    void disabledRuntimeIsTheNoopSingleton() {
        InvariantRuntime disabled = new InvariantRuntimeBuilder().enabled(false).build();
        assertThat(disabled).isSameAs(NoopInvariantRuntime.INSTANCE);
        assertThat(disabled.isEnabled()).isFalse();

        disabled.register(List.class, Invariant.of("ignored", (l, ctx) -> false));
        disabled.beforeOperation("op", new ArrayList<>(), "param");
        disabled.afterOperation("op", new ArrayList<>(), "param");

        assertThat(disabled.stats()).isEqualTo(new InvariantStats(0, 0, 0, 0));
        assertThat(disabled.failures()).isEmpty();
    }

    @Test
    void globalFacadeIsDisabledByDefaultAndNeverFails() {
        Invariants.disable();
        InvariantRuntime rt = Invariants.runtime();
        assertThat(rt.isEnabled()).isFalse();
        assertThat(rt).isSameAs(NoopInvariantRuntime.INSTANCE);

        // Even a checker that "always violates" is never executed: production is inert.
        rt.register(Object.class, Invariant.of("explode", (o, ctx) -> {
            throw new AssertionError("must never run in production");
        }));
        for (int i = 0; i < 1000; i++) {
            rt.beforeOperation("hot-op", new Object(), i);
            rt.afterOperation("hot-op", new Object(), i);
        }
        assertThat(rt.stats().checksExecuted()).isZero();
        assertThat(rt.stats().failures()).isZero();
    }
}
