package com.example.gsb.invariant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InvariantCheckerTest {

    /** Minimal stand-in for a concurrent structure under test. */
    static final class FakeStructure {
        volatile int size;

        @Override
        public String toString() {
            return "FakeStructure{size=" + size + "}";
        }
    }

    private static InvariantCheckerConfig<FakeStructure> debugConfig() {
        return InvariantCheckerConfig.<FakeStructure>builder().build();
    }

    @Nested
    @DisplayName("invariant passes")
    class Passing {

        @Test
        void executeRunsBeforeAndAfterChecksAndReturnsResult() {
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(debugConfig());
            checker.register("size-non-negative", s -> s.size < 0 ? "size=" + s.size : null);
            FakeStructure structure = new FakeStructure();

            String result = checker.execute("put", structure, () -> {
                structure.size++;
                return "ok";
            }, "key-1", 42);

            assertThat(result).isEqualTo("ok");
            assertThat(structure.size).isEqualTo(1);
            StatsSnapshot stats = checker.statistics();
            assertThat(stats.totalChecks()).isEqualTo(2);
            assertThat(stats.failures()).isZero();
            assertThat(stats.skippedBySampling()).isZero();
        }

        @Test
        void manualBeforeAfterChecksDoNotThrow() {
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(debugConfig());
            checker.register("always-ok", s -> null);

            checker.checkBefore("get", new FakeStructure(), "k");
            checker.checkAfter("get", new FakeStructure(), "k");

            assertThat(checker.statistics().totalChecks()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("invariant violated")
    class Violated {

        @Test
        void abortPolicyThrowsWithFullContextAndSnapshot() {
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(
                    InvariantCheckerConfig.<FakeStructure>builder()
                            .failurePolicy(FailurePolicy.ABORT)
                            .snapshotter(s -> "SNAPSHOT(size=" + s.size + ")")
                            .build());
            checker.register("size-non-negative", s -> "size was negative: " + s.size);
            FakeStructure structure = new FakeStructure();
            structure.size = -1;
            String threadName = Thread.currentThread().getName();
            long threadId = Thread.currentThread().getId();

            assertThatThrownBy(() -> checker.checkAfter("remove", structure, "key-9", null))
                    .isInstanceOfSatisfying(InvariantViolationException.class, ex -> {
                        assertThat(ex.invariantName()).isEqualTo("size-non-negative");
                        assertThat(ex.reason()).isEqualTo("size was negative: -1");
                        assertThat(ex.snapshotSummary()).isEqualTo("SNAPSHOT(size=-1)");

                        CheckContext ctx = ex.context();
                        assertThat(ctx.threadName()).isEqualTo(threadName);
                        assertThat(ctx.threadId()).isEqualTo(threadId);
                        assertThat(ctx.operationType()).isEqualTo("remove");
                        assertThat(ctx.phase()).isEqualTo(CheckPhase.AFTER);
                        assertThat(ctx.arguments()).containsExactly("key-9", null);
                        assertThat(ctx.timestamp()).isNotNull();
                        assertThat(ctx.checkDurationNanos()).isGreaterThanOrEqualTo(0);

                        assertThat(ex.getMessage())
                                .contains("size-non-negative")
                                .contains("size was negative: -1")
                                .contains(threadName)
                                .contains("remove")
                                .contains("AFTER")
                                .contains("key-9")
                                .contains("SNAPSHOT(size=-1)");
                    });

            StatsSnapshot stats = checker.statistics();
            assertThat(stats.totalChecks()).isEqualTo(1);
            assertThat(stats.failures()).isEqualTo(1);
        }

        @Test
        void abortPolicyPreventsTheGuardedOperation() {
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(debugConfig());
            checker.register("always-broken", s -> "boom");
            AtomicBoolean operationRan = new AtomicBoolean(false);

            assertThatThrownBy(() -> checker.execute("put", new FakeStructure(),
                    () -> operationRan.getAndSet(true)))
                    .isInstanceOf(InvariantViolationException.class);

            assertThat(operationRan).isFalse();
        }

        @Test
        void logAndContinueRecordsViolationAndKeepsRunning() {
            List<InvariantViolationException> recorded = new CopyOnWriteArrayList<>();
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(
                    InvariantCheckerConfig.<FakeStructure>builder()
                            .failurePolicy(FailurePolicy.LOG_AND_CONTINUE)
                            .violationListener(recorded::add)
                            .build());
            checker.register("always-broken", s -> "boom");
            FakeStructure structure = new FakeStructure();

            String result = checker.execute("put", structure, () -> "still-ran", "k");

            assertThat(result).isEqualTo("still-ran");
            assertThat(recorded).hasSize(2); // BEFORE and AFTER
            assertThat(recorded).allSatisfy(ex -> {
                assertThat(ex.context().operationType()).isEqualTo("put");
                assertThat(ex.context().arguments()).containsExactly("k");
            });
            assertThat(recorded.get(0).context().phase()).isEqualTo(CheckPhase.BEFORE);
            assertThat(recorded.get(1).context().phase()).isEqualTo(CheckPhase.AFTER);

            StatsSnapshot stats = checker.statistics();
            assertThat(stats.totalChecks()).isEqualTo(2);
            assertThat(stats.failures()).isEqualTo(2);
        }

        @Test
        void invariantThrowingIsReportedAsViolationWithCause() {
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(debugConfig());
            IllegalStateException bug = new IllegalStateException("buggy invariant");
            checker.register("buggy", s -> {
                throw bug;
            });

            assertThatThrownBy(() -> checker.checkBefore("get", new FakeStructure()))
                    .isInstanceOfSatisfying(InvariantViolationException.class, ex -> {
                        assertThat(ex.getCause()).isSameAs(bug);
                        assertThat(ex.reason()).contains("invariant evaluation threw");
                    });
        }
    }

    @Nested
    @DisplayName("disabled (production) mode")
    class Disabled {

        @Test
        void factoryReturnsSharedNoOpSingleton() {
            InvariantChecker<FakeStructure> checker =
                    InvariantCheckers.create(InvariantCheckerConfig.production());

            assertThat(checker).isSameAs(InvariantCheckers.disabled());
        }

        @Test
        void noOpCheckerNeverEvaluatesInvariantsAndKeepsZeroStats() {
            InvariantChecker<FakeStructure> checker =
                    InvariantCheckers.create(InvariantCheckerConfig.production());
            AtomicBoolean invariantEvaluated = new AtomicBoolean(false);
            checker.register("never-run", s -> {
                invariantEvaluated.set(true);
                return "violated";
            });
            FakeStructure structure = new FakeStructure();

            for (int i = 0; i < 10_000; i++) {
                checker.run("put", structure, () -> structure.size++, i);
            }

            assertThat(structure.size).isEqualTo(10_000);
            assertThat(invariantEvaluated).isFalse();
            assertThat(checker.statistics()).isEqualTo(StatsSnapshot.EMPTY);
        }
    }

    @Nested
    @DisplayName("sampling")
    class Sampling {

        @Test
        void fullRateChecksEveryInvocation() {
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(
                    InvariantCheckerConfig.<FakeStructure>builder().sampleRate(1.0).build());
            checker.register("ok", s -> null);

            for (int i = 0; i < 100; i++) {
                checker.checkBefore("op", new FakeStructure());
            }

            StatsSnapshot stats = checker.statistics();
            assertThat(stats.totalChecks()).isEqualTo(100);
            assertThat(stats.skippedBySampling()).isZero();
        }

        @Test
        void halfRateChecksEverySecondInvocation() {
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(
                    InvariantCheckerConfig.<FakeStructure>builder().sampleRate(0.5).build());
            checker.register("ok", s -> null);

            for (int i = 0; i < 100; i++) {
                checker.checkBefore("op", new FakeStructure());
            }

            StatsSnapshot stats = checker.statistics();
            assertThat(stats.totalChecks()).isEqualTo(50);
            assertThat(stats.skippedBySampling()).isEqualTo(50);
        }

        @Test
        void quarterRateChecksOneInFour() {
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(
                    InvariantCheckerConfig.<FakeStructure>builder().sampleRate(0.25).build());
            checker.register("ok", s -> null);

            for (int i = 0; i < 200; i++) {
                checker.checkBefore("op", new FakeStructure());
            }

            StatsSnapshot stats = checker.statistics();
            assertThat(stats.totalChecks()).isEqualTo(50);
            assertThat(stats.skippedBySampling()).isEqualTo(150);
        }

        @Test
        void invalidSampleRateIsRejected() {
            assertThatThrownBy(() -> InvariantCheckerConfig.builder().sampleRate(0.0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> InvariantCheckerConfig.builder().sampleRate(1.5))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("statistics")
    class Statistics {

        @Test
        void averageDurationReflectsMeasuredCheckTime() {
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(debugConfig());
            checker.register("slow", s -> {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });

            for (int i = 0; i < 5; i++) {
                checker.checkBefore("op", new FakeStructure());
            }

            StatsSnapshot stats = checker.statistics();
            assertThat(stats.totalChecks()).isEqualTo(5);
            assertThat(stats.totalCheckTimeNanos()).isGreaterThanOrEqualTo(5_000_000L);
            assertThat(stats.averageCheckTimeNanos())
                    .isCloseTo(stats.totalCheckTimeNanos() / 5.0, within(0.001));
            assertThat(stats.averageCheckTimeNanos()).isGreaterThanOrEqualTo(1_000_000.0);
        }

        @Test
        void emptyCheckerReportsZeroAverage() {
            InvariantChecker<FakeStructure> checker = InvariantCheckers.create(debugConfig());
            assertThat(checker.statistics()).isEqualTo(StatsSnapshot.EMPTY);
        }
    }
}
