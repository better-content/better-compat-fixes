package com.bettercontent.bettercompatfixes;

import com.bettercontent.bettercompatfixes.compat.ChoppingProgress;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChoppingProgressTest {
    @Test void completionMapsLinearlyToParticlesPerSecondNotProgressDelta() {
        assertEquals(2, ChoppingProgress.rate(0));
        assertEquals(6, ChoppingProgress.rate(.25));
        assertEquals(10, ChoppingProgress.rate(.5));
        assertEquals(14, ChoppingProgress.rate(.75));
        assertEquals(18, ChoppingProgress.rate(1));
        for (double progress : new double[]{0, .25, .5, .75, 1}) {
            var budget = new ChoppingProgress();
            int total = 0;
            for (int tick = 0; tick < 200; tick++) {
                total += budget.advance(progress, ChoppingProgress.NATIVE_TICK_SECONDS);
            }
            assertEquals((int) (10 * ChoppingProgress.rate(progress)), total,
                    "Holding the same completion over native ticks still emits its configured rate");
        }
    }

    @Test void constantSpeedCutHasHigherLaterQuarterRateMatchingLinearFormula() {
        var firstQuarter = new ChoppingProgress();
        var lastQuarter = new ChoppingProgress();
        int early = 0, late = 0;
        double expectedEarly = 0, expectedLate = 0;
        for (int tick = 1; tick <= 20; tick++) {
            double earlyProgress = tick / 80.0;
            double lateProgress = (tick + 60) / 80.0;
            early += firstQuarter.advance(earlyProgress, .05);
            late += lastQuarter.advance(lateProgress, .05);
            expectedEarly += ChoppingProgress.rate(earlyProgress) * .05;
            expectedLate += ChoppingProgress.rate(lateProgress) * .05;
        }
        assertEquals((int) Math.floor(expectedEarly + 1e-6), early);
        assertEquals((int) Math.floor(expectedLate + 1e-6), late);
        assertEquals(4, early);
        assertEquals(16, late);
        assertTrue(late > early, "A constant mining speed must not produce a constant particle rate");
    }

    @Test void renderingFpsCannotChangeNativeTickBudget() {
        for (int fps : new int[]{15, 30, 60, 144, 240}) {
            var budget = new ChoppingProgress();
            int ticks = 0, total = 0;
            for (int frame = 1; frame <= 4 * fps; frame++) {
                int dueTicks = Math.min(80, (int) Math.floor(frame * 20.0 / fps + 1e-6));
                while (ticks < dueTicks) {
                    ticks++;
                    total += budget.advance(ticks / 80.0, .05);
                }
            }
            assertEquals(80, ticks);
            assertEquals(40, total, "Rendering FPS must not affect a four-second native cut");
        }
    }

    @Test void fractionalRateIsRetainedButStopsNewTargetsAndRollbackDropDebt() {
        var budget = new ChoppingProgress();
        assertEquals(0, budget.advance(.5, .05));
        assertEquals(1, budget.advance(.5, .05));
        assertEquals(0, budget.advance(.5, .05));
        budget.reset();
        assertEquals(0, budget.advance(.5, .05), "Stopping or switching targets drops half-chip debt");
        assertEquals(0, budget.advance(.25, .05), "Rollback discards debt and rebases without emission");
        assertEquals(0, budget.advance(.25, .05));
        assertEquals(0, budget.advance(Double.NaN, .05));
        assertEquals(0, budget.advance(.5, .05));
    }
}
