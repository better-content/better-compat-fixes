package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ExplosionOverhaulConcussionClampTest {
    @Test
    void clampsIntegerDurationsAtConfiguredMaximum() {
        assertEquals(44, ExplosionOverhaulConcussionClamp.clampSeconds(44, 45, true));
        assertEquals(45, ExplosionOverhaulConcussionClamp.clampSeconds(45, 45, true));
        assertEquals(45, ExplosionOverhaulConcussionClamp.clampSeconds(46, 45, true));
    }

    @Test
    void clampsFractionalDurationsWithoutRounding() {
        assertEquals(44.5D, ExplosionOverhaulConcussionClamp.clampSeconds(44.5D, 45, true));
        assertEquals(45.0D, ExplosionOverhaulConcussionClamp.clampSeconds(70.25D, 45, true));
    }

    @Test
    void clampsAccumulatedDurationToConfiguredTickBudget() {
        assertEquals(800, ExplosionOverhaulConcussionClamp.clampAccumulatedTicks(800, 45, true));
        assertEquals(900, ExplosionOverhaulConcussionClamp.clampAccumulatedTicks(2_000, 45, true));
    }

    @Test
    void disabledPolicyPreservesUpstreamDurations() {
        assertEquals(80, ExplosionOverhaulConcussionClamp.clampSeconds(80, 45, false));
        assertEquals(80.5D, ExplosionOverhaulConcussionClamp.clampSeconds(80.5D, 45, false));
        assertEquals(2_000, ExplosionOverhaulConcussionClamp.clampAccumulatedTicks(2_000, 45, false));
    }
}
