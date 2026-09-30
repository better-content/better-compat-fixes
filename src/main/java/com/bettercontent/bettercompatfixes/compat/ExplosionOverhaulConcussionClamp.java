package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;

/** Bounds Explosion Overhaul's client-side concussion hold durations and accumulated state. */
public final class ExplosionOverhaulConcussionClamp {
    private static final int TICKS_PER_SECOND = 20;

    private ExplosionOverhaulConcussionClamp() {
    }

    public static int clampSeconds(final int requestedSeconds) {
        return clampSeconds(
                requestedSeconds,
                BcFixesConfig.explosionOverhaulMaxConcussionDurationSeconds(),
                BcFixesConfig.explosionOverhaulClampConcussionDuration());
    }

    public static double clampSeconds(final double requestedSeconds) {
        return clampSeconds(
                requestedSeconds,
                BcFixesConfig.explosionOverhaulMaxConcussionDurationSeconds(),
                BcFixesConfig.explosionOverhaulClampConcussionDuration());
    }

    public static int clampAccumulatedTicks(final int upstreamLimitTicks) {
        return clampAccumulatedTicks(
                upstreamLimitTicks,
                BcFixesConfig.explosionOverhaulMaxConcussionDurationSeconds(),
                BcFixesConfig.explosionOverhaulClampConcussionDuration());
    }

    static int clampSeconds(final int requestedSeconds, final int maximumSeconds, final boolean enabled) {
        return enabled ? Math.min(requestedSeconds, maximumSeconds) : requestedSeconds;
    }

    static double clampSeconds(final double requestedSeconds, final int maximumSeconds, final boolean enabled) {
        return enabled ? Math.min(requestedSeconds, maximumSeconds) : requestedSeconds;
    }

    static int clampAccumulatedTicks(final int upstreamLimitTicks, final int maximumSeconds, final boolean enabled) {
        return enabled ? Math.min(upstreamLimitTicks, maximumSeconds * TICKS_PER_SECOND) : upstreamLimitTicks;
    }
}
