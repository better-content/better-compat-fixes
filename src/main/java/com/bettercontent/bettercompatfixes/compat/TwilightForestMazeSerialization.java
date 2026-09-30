package com.bettercontent.bettercompatfixes.compat;

import java.util.function.Predicate;
import java.util.function.Supplier;

/** Serializes maze placement only among mazes that share one Twilight Forest random source. */
public final class TwilightForestMazeSerialization {
    private static final Object STRONGHOLD_GENERATION_LOCK = new Object();

    private TwilightForestMazeSerialization() {
    }

    public static boolean dependenciesAvailable(
            final boolean enabled,
            final Predicate<String> modLoaded) {
        return enabled && modLoaded.test("twilightforest") && modLoaded.test("c2me");
    }

    public static void runSerialized(final Object randomSource, final Runnable operation) {
        synchronized (randomSource) {
            operation.run();
        }
    }

    public static <T> T callStrongholdSerialized(final Supplier<T> operation) {
        synchronized (STRONGHOLD_GENERATION_LOCK) {
            return operation.get();
        }
    }
}
