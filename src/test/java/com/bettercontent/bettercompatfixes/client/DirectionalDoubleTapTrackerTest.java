package com.bettercontent.bettercompatfixes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DirectionalDoubleTapTrackerTest {
    @Test
    void triggersEachDirectionAfterARelease() {
        for (int direction = 0; direction < 4; direction++) {
            final DirectionalDoubleTapTracker tracker = new DirectionalDoubleTapTracker();
            assertFalse(updateDirection(tracker, direction, true, 7));
            assertFalse(updateDirection(tracker, direction, false, 7));
            assertTrue(updateDirection(tracker, direction, true, 7));
        }
    }

    @Test
    void acceptsTheInclusiveWindowBoundary() {
        final DirectionalDoubleTapTracker tracker = new DirectionalDoubleTapTracker();
        assertFalse(tracker.update(true, false, false, false, 7));
        for (int tick = 0; tick < 6; tick++) {
            assertFalse(tracker.update(false, false, false, false, 7));
        }
        assertTrue(tracker.update(true, false, false, false, 7));
    }

    @Test
    void expiresAfterTheWindow() {
        final DirectionalDoubleTapTracker tracker = new DirectionalDoubleTapTracker();
        assertFalse(tracker.update(true, false, false, false, 7));
        for (int tick = 0; tick < 7; tick++) {
            assertFalse(tracker.update(false, false, false, false, 7));
        }
        assertFalse(tracker.update(true, false, false, false, 7));
    }

    @Test
    void holdingAndAlternatingDirectionsDoNotTrigger() {
        final DirectionalDoubleTapTracker tracker = new DirectionalDoubleTapTracker();
        assertFalse(tracker.update(true, false, false, false, 7));
        assertFalse(tracker.update(true, false, false, false, 7));
        assertFalse(tracker.update(false, false, false, false, 7));
        assertFalse(tracker.update(false, false, true, false, 7));
    }

    @Test
    void simultaneousSecondPressesCoalesceAndClearEveryDirection() {
        final DirectionalDoubleTapTracker tracker = new DirectionalDoubleTapTracker();
        assertFalse(tracker.update(true, false, true, false, 7));
        assertFalse(tracker.update(false, false, false, false, 7));
        assertTrue(tracker.update(true, false, true, false, 7));
        assertFalse(tracker.update(false, false, false, false, 7));
        assertFalse(tracker.update(true, false, false, false, 7));
    }

    @Test
    void resetDropsArmedAndHeldState() {
        final DirectionalDoubleTapTracker tracker = new DirectionalDoubleTapTracker();
        assertFalse(tracker.update(true, false, false, false, 7));
        assertFalse(tracker.update(false, false, false, false, 7));
        tracker.reset();
        assertFalse(tracker.update(true, false, false, false, 7));
    }

    @Test
    void axisImpulseRejectsOpposingInputsAndPreservesDirection() {
        assertEquals(1.0F, DirectionalDoubleTapTracker.axisImpulse(true, false));
        assertEquals(-1.0F, DirectionalDoubleTapTracker.axisImpulse(false, true));
        assertEquals(0.0F, DirectionalDoubleTapTracker.axisImpulse(false, false));
        assertEquals(0.0F, DirectionalDoubleTapTracker.axisImpulse(true, true));
    }

    private static boolean updateDirection(
            final DirectionalDoubleTapTracker tracker,
            final int direction,
            final boolean down,
            final int window
    ) {
        return tracker.update(
                direction == 0 && down,
                direction == 1 && down,
                direction == 2 && down,
                direction == 3 && down,
                window);
    }
}
