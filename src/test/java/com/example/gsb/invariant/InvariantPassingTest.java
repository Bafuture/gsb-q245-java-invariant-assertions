package com.example.gsb.invariant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InvariantPassingTest {

    /** A tiny concurrent structure under test: balance must never be negative. */
    static final class Account {
        int balance = 100;
    }

    private final Invariant<Account> nonNegative =
            Invariant.of("balance-nonnegative", (account, ctx) -> account.balance >= 0);

    @Test
    void runsRegisteredInvariantBeforeAndAfterOperation() {
        InvariantRuntime rt = new InvariantRuntimeBuilder().build();
        AtomicInteger runs = new AtomicInteger();
        rt.register(Account.class, Invariant.of("counting", (a, ctx) -> {
            runs.incrementAndGet();
            return true;
        }));

        Account account = new Account();
        rt.beforeOperation("withdraw", account, 40);
        account.balance -= 40;
        rt.afterOperation("withdraw", account, 40);

        assertThat(runs).hasValue(2);
        assertThat(rt.stats().checksExecuted()).isEqualTo(2);
        assertThat(rt.stats().failures()).isZero();
        assertThat(rt.stats().skippedBySampling()).isZero();
    }

    @Test
    void runOperationChecksOnBothSidesEvenWhenBodyThrows() {
        InvariantRuntime rt = new InvariantRuntimeBuilder().build();
        AtomicInteger runs = new AtomicInteger();
        rt.register(Account.class, Invariant.of("counting", (a, ctx) -> {
            runs.incrementAndGet();
            return true;
        }));

        Account account = new Account();
        assertThatThrown(() -> rt.runOperation("boom", account,
                (Runnable) () -> {
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(runs).hasValue(2);
    }

    @Test
    void onlyInvariantsMatchingTargetTypeAreInvoked() {
        InvariantRuntime rt = new InvariantRuntimeBuilder().build();
        AtomicInteger accountChecks = new AtomicInteger();
        rt.register(Account.class, Invariant.of("account-only", (a, ctx) -> {
            accountChecks.incrementAndGet();
            return true;
        }));

        rt.beforeOperation("other", "not-an-account");
        assertThat(accountChecks).hasValue(0);

        rt.beforeOperation("account-op", new Account());
        assertThat(accountChecks).hasValue(1);
    }

    private static org.assertj.core.api.AbstractThrowableAssert<?, ? extends Throwable> assertThatThrown(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        return org.assertj.core.api.Assertions.assertThatThrownBy(callable);
    }
}
