package com.bettercontent.bettercompatfixes.compat;

/** Pack fall entry follows the source world's actual bottom, not vanilla's absolute Y=-70. */
public final class DeepVoidFallBoundary {
    private DeepVoidFallBoundary() {
    }

    public static double threshold(final int minimumBuildHeight) {
        // Preserve the native default's six-block margin below vanilla's -64 floor.
        // Convert before subtracting so even extreme height accessors cannot overflow.
        return (double) minimumBuildHeight - 6.0D;
    }
}
