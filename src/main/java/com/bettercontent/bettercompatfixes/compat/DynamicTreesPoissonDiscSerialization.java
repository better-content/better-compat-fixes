package com.bettercontent.bettercompatfixes.compat;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Shares Dynamic Trees' existing provider monitor with asynchronous chunk-data callbacks.
 */
public final class DynamicTreesPoissonDiscSerialization {
    private DynamicTreesPoissonDiscSerialization() {
    }

    public static void run(final Object provider, final Runnable action) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(action, "action");
        synchronized (provider) {
            action.run();
        }
    }

    public static <T> T call(final Object provider, final Supplier<T> action) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(action, "action");
        synchronized (provider) {
            return action.get();
        }
    }
}
