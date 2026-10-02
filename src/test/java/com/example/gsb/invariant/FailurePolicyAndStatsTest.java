package com.example.gsb.invariant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FailurePolicyAndStatsTest {

    static final class Box {
        int value;

        Box(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Box(value=" + value + ")";
        }
    }

    @Test
    void logAndContinueRecordsViolationAndKeepsGoing() {
        List<FailureRecord> seen = new ArrayList<>();
        InvariantRuntime rt = new InvariantRuntimeBuilder()
                .failurePolicy(FailurePolicy.LOG_AND_CONTINUE)
                .failureListener(seen::add)
                .build();
        rt.register(Box.class, Invariant.of("positive", (box, ctx) -> box.value > 0));

        rt.afterOperation("set", new Box(1));
        rt.afterOperation("set", new Box(-1));
        rt.afterOperation("set", new Box(2));
        rt.afterOperation("set", new Box(-5));

        // No exception escaped; operation flow continued normally.
        assertThat(rt.stats().checksExecuted()).isEqualTo(4);
        assertThat(rt.stats().failures()).isEqualTo(2);

        assertThat(seen).hasSize(2);
        assertThat(rt.failures()).hasSize(2);
        FailureRecord first = rt.failures().get(0);
        assertThat(first.targetSnapshot()).isEqualTo("Box(value=-1)");
        assertThat(first.context().operationType()).isEqualTo("set");
        assertThat(first.detail()).isEqualTo("checker returned false");

        assertThat(rt.stats().averageCheckNanos()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void recentFailuresRingIsBounded() {
        InvariantRuntime rt = new InvariantRuntimeBuilder()
                .failurePolicy(FailurePolicy.LOG_AND_CONTINUE)
                .failureListener(FailureListener.noop())
                .maxRecordedFailures(3)
                .build();
        rt.register(Box.class, Invariant.of("positive", (box, ctx) -> box.value > 0));

        for (int i = 0; i < 10; i++) {
            rt.afterOperation("set", new Box(-i));
        }

        assertThat(rt.failures()).hasSize(3);
        // Newest three are kept in chronological order.
        assertThat(rt.failures().get(0).targetSnapshot()).isEqualTo("Box(value=-7)");
        assertThat(rt.failures().get(2).targetSnapshot()).isEqualTo("Box(value=-9)");
    }

    @Test
    void averageCheckTimeIsTracked() {
        InvariantRuntime rt = new InvariantRuntimeBuilder()
                .failurePolicy(FailurePolicy.LOG_AND_CONTINUE)
                .failureListener(FailureListener.noop())
                .build();
        rt.register(Box.class, Invariant.of("slow", (box, ctx) -> {
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }));

        for (int i = 0; i < 5; i++) {
            rt.afterOperation("op", new Box(1));
        }

        assertThat(rt.stats().checksExecuted()).isEqualTo(5);
        assertThat(rt.stats().averageCheckNanos()).isGreaterThanOrEqualTo(1_500_000L);
        assertThat(rt.stats().totalCheckNanos()).isGreaterThanOrEqualTo(5L * 1_500_000L);
    }

    @Test
    void resetClearsCountersAndFailures() {
        InvariantRuntime rt = new InvariantRuntimeBuilder()
                .failurePolicy(FailurePolicy.LOG_AND_CONTINUE)
                .failureListener(FailureListener.noop())
                .build();
        AtomicInteger count = new AtomicInteger();
        rt.register(Box.class, Invariant.of("counted", (box, ctx) -> {
            count.incrementAndGet();
            return false;
        }));

        rt.afterOperation("op", new Box(-1));
        assertThat(rt.stats().failures()).isEqualTo(1);
        assertThat(rt.failures()).hasSize(1);

        rt.reset();
        assertThat(rt.stats()).isEqualTo(new InvariantStats(0, 0, 0, 0));
        assertThat(rt.failures()).isEmpty();

        // Registrations survive a reset.
        rt.afterOperation("op", new Box(-2));
        assertThat(count).hasValue(2);
    }
}
