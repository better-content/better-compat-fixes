package com.bettercontent.bettercompatfixes;

import com.bettercontent.bettercompatfixes.compat.ChoppingProgress;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChoppingProgressTest {
    @Test void linearInCompletionNotDurationOrSamplingRate() {
        for (int steps : new int[]{5, 20, 100, 2000}) {
            var budget = new ChoppingProgress();
            int total = 0;
            for (int i = 1; i <= steps; i++) {
                total += budget.advance((double) i / steps);
                assertEquals(0, budget.advance((double) i / steps), "stationary progress emits nothing");
            }
            assertEquals(48, total);
        }
    }

    @Test void fractionalProgressIsRetainedAndCompletionClamped() {
        var budget = new ChoppingProgress();
        assertEquals(0, budget.advance(.01));
        assertEquals(0, budget.advance(.02));
        assertEquals(1, budget.advance(.03));
        assertEquals(47, budget.advance(2));
    }

    @Test void rollbackDropsDebtAndRebasesWithoutReplayingCompletion() {
        var budget = new ChoppingProgress();
        assertEquals(24, budget.advance(.5));
        assertEquals(0, budget.advance(.25));
        assertEquals(0, budget.advance(.26));
        assertEquals(12, budget.advance(.5));
    }

    @Test void stopOrNewTargetDropsFractionalDebt() {
        var budget = new ChoppingProgress();
        budget.advance(.02);
        budget.reset();
        assertEquals(0, budget.advance(.02));
        assertEquals(0, budget.advance(Double.NaN));
        assertEquals(0, budget.advance(.01));
    }
}
