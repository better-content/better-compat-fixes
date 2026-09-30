package com.bettercontent.bettercompatfixes.compat.sleeping;

import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/** Deadline-based pacing for Sleeping Overhaul's inner simulated-tick loop. */
public final class TimelapsePacer {
    private static long nextDeadlineNanos;

    private TimelapsePacer() {
    }

    public static void pace(final boolean active, final int targetTicksPerSecond) {
        pace(active, targetTicksPerSecond, System::nanoTime, LockSupport::parkNanos);
    }

    static synchronized void pace(
            final boolean active,
            final int targetTicksPerSecond,
            final LongSupplier clock,
            final LongConsumer park
    ) {
        if (!active || targetTicksPerSecond <= 0) {
            nextDeadlineNanos = 0L;
            return;
        }
        final long interval = 1_000_000_000L / targetTicksPerSecond;
        final long now = clock.getAsLong();
        if (nextDeadlineNanos == 0L || now - nextDeadlineNanos > interval) {
            nextDeadlineNanos = now + interval;
        }
        if (now < nextDeadlineNanos) {
            park.accept(nextDeadlineNanos - now);
        }
        nextDeadlineNanos += interval;
    }

    static synchronized void resetForTest() {
        nextDeadlineNanos = 0L;
    }
}
