package com.example.gsb.invariant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SamplingTest {

    @Test
    void rateSamplerIsProportionalAndEvenlyDistributed() {
        Sampler sampler = Sampler.rate(0.25);
        int selected = 0;
        int n = 10_000;
        for (int i = 0; i < n; i++) {
            if (sampler.sample()) {
                selected++;
            }
        }
        // Exact Bresenham selection, not a probabilistic approximation.
        assertThat(selected).isEqualTo(n / 4);
    }

    @Test
    void runtimeSkipsCountAsSkippedAndExecutedMatchesRate() {
        InvariantRuntime rt = new InvariantRuntimeBuilder()
                .samplingRate(0.5)
                .build();
        AtomicInteger runs = new AtomicInteger();
        rt.register(Object.class, Invariant.of("half", (o, ctx) -> {
            runs.incrementAndGet();
            return true;
        }));

        for (int i = 0; i < 100; i++) {
            rt.afterOperation("hot", new Object());
        }

        InvariantStats stats = rt.stats();
        assertThat(runs).hasValue(50);
        assertThat(stats.checksExecuted()).isEqualTo(50);
        assertThat(stats.skippedBySampling()).isEqualTo(50);
        assertThat(stats.failures()).isZero();
    }

    @Test
    void boundariesAlwaysNeverAreConstants() {
        assertThat(Sampler.rate(1.0)).isSameAs(Sampler.ALWAYS);
        assertThat(Sampler.rate(0.0)).isSameAs(Sampler.NEVER);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Sampler.rate(1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
