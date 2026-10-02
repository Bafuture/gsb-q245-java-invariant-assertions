package com.example.gsb.invariant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class InvariantViolationTest {

    /** Deliberately does NOT override toString, to exercise the identity snapshot. */
    static final class Bag {
        int size = -3;
    }

    private final Invariant<Bag> nonEmptySize =
            Invariant.of("size-nonnegative", (bag, ctx) -> bag.size >= 0);

    @Test
    void abortPolicyThrowsWithFullContextAndSnapshot() {
        InvariantRuntime rt = new InvariantRuntimeBuilder()
                .failurePolicy(FailurePolicy.ABORT)
                .build();
        rt.register(Bag.class, nonEmptySize);

        assertThatThrownBy(() -> rt.afterOperation("shrink", new Bag(), "key", 42))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("size-nonnegative")
                .hasMessageContaining("snapshot:");
    }

    @Test
    void exceptionMessageAndContextAreComplete() {
        InvariantRuntime rt = new InvariantRuntimeBuilder()
                .failurePolicy(FailurePolicy.ABORT)
                .build();
        rt.register(Bag.class, nonEmptySize);

        Bag bag = new Bag();
        Thread current = Thread.currentThread();
        try {
            rt.afterOperation("shrink", bag, "key", 42);
            assertThat(false).as("expected violation").isTrue();
        } catch (InvariantViolationException e) {
            CheckContext ctx = e.context();
            assertThat(ctx.invariantName()).isEqualTo("size-nonnegative");
            assertThat(ctx.checkPoint()).isEqualTo(CheckPoint.AFTER);
            assertThat(ctx.operationType()).isEqualTo("shrink");
            assertThat(ctx.operationParams()).containsExactly("key", 42);
            assertThat(ctx.threadId()).isEqualTo(current.getId());
            assertThat(ctx.threadName()).isEqualTo(current.getName());
            assertThat(ctx.elapsedNanos()).isGreaterThanOrEqualTo(0L);
            assertThat(ctx.timestampMillis()).isPositive();

            assertThat(e.getMessage())
                    .contains("size-nonnegative")
                    .contains("AFTER")
                    .contains("shrink")
                    .contains("[key, 42]")
                    .contains(current.getName())
                    .contains("checker returned false");

            assertThat(e.targetSnapshot())
                    .startsWith(Bag.class.getName() + "@")
                    .doesNotContain("@" + Integer.toHexString(System.identityHashCode(bag)) + "@");
            assertThat(e.targetSnapshot())
                    .contains(Integer.toHexString(System.identityHashCode(bag)));
        }
    }

    @Test
    void snapshotOfValueStyleObjectUsesToString() {
        InvariantRuntime rt = new InvariantRuntimeBuilder()
                .failurePolicy(FailurePolicy.ABORT)
                .build();
        rt.register(List.class, Invariant.of("no-empty-list",
                (list, ctx) -> !((List<?>) list).isEmpty()));

        try {
            rt.beforeOperation("read", List.of());
            assertThat(false).as("expected violation").isTrue();
        } catch (InvariantViolationException e) {
            assertThat(e.targetSnapshot()).contains("size=0");
        }
    }

    @Test
    void checkerExceptionBecomesCauseAndIsReported() {
        InvariantRuntime rt = new InvariantRuntimeBuilder()
                .failurePolicy(FailurePolicy.ABORT)
                .build();
        IllegalStateException boom = new IllegalStateException("structure corrupted");
        rt.register(Bag.class, Invariant.of("throwing-check", (bag, ctx) -> {
            throw boom;
        }));

        assertThatThrownBy(() -> rt.afterOperation("corrupt", new Bag()))
                .isInstanceOf(InvariantViolationException.class)
                .hasCause(boom)
                .hasMessageContaining("IllegalStateException")
                .hasMessageContaining("structure corrupted");
    }
}
