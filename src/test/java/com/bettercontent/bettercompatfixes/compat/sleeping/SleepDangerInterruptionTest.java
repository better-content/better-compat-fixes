package com.bettercontent.bettercompatfixes.compat.sleeping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SleepDangerInterruptionTest {
    @Test
    void onlyAnActiveTimelapseWithAdmittedImmediateDangerStops() {
        assertTrue(SleepDangerInterruption.shouldInterrupt(true, true));
        assertFalse(SleepDangerInterruption.shouldInterrupt(false, true));
        assertFalse(SleepDangerInterruption.shouldInterrupt(true, false));
    }

    @Test
    void ordinarySimulationSignalsAreNotDanger() {
        assertFalse(SleepDangerInterruption.shouldInterrupt(true, false));
    }
}
