package com.bettercontent.bettercompatfixes.compat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DynamicTreesFellingWearTest {
    @Test void scalesBothNativeContributionsAfterTheirOriginalRounding() {
        Object player = new Object(), tool = new Object();
        for (int multiplier : new int[]{1, 10, 100}) {
            for (int radius = 1; radius <= 24; radius++) {
                int extra = Math.max(0, Math.max(1, radius) / 2 - 1);
                try (var ignored = DynamicTreesFellingWear.enter(player, tool, multiplier)) {
                    // Forge axes pay one during mineBlock; TCon breakBlock skips that charge
                    // when DT onDestroyedByPlayer queues the fall and returns false.
                    assertEquals(multiplier * (1 + extra),
                            DynamicTreesFellingWear.scale(player, tool, 1)
                                    + DynamicTreesFellingWear.scale(player, tool, extra));
                    assertEquals(multiplier * extra, DynamicTreesFellingWear.scale(player, tool, extra));
                }
            }
        }
    }

    @Test void zeroAndNonPositiveChargesNeverAcquireAnInventedFloor() {
        assertEquals(0, DynamicTreesFellingWear.multiply(0, 10));
        assertEquals(-1, DynamicTreesFellingWear.multiply(-1, 10));
        assertEquals(30, DynamicTreesFellingWear.multiply(3, 10));
    }

    @Test void configurationBoundsAndOverflowFailExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> DynamicTreesFellingWear.multiply(1, 0));
        assertThrows(IllegalArgumentException.class, () -> DynamicTreesFellingWear.multiply(1, 101));
        assertThrows(ArithmeticException.class, () -> DynamicTreesFellingWear.multiply(Integer.MAX_VALUE, 10));
        assertEquals(Integer.MAX_VALUE, DynamicTreesFellingWear.multiply(Integer.MAX_VALUE, 1));
    }

    @Test void matchesPlayerAndStackByIdentityAndDoesNotLeakToLaterOperations() {
        Object player = new Object(), tool = new Object(), otherPlayer = new Object(), otherTool = new Object();
        try (var ignored = DynamicTreesFellingWear.enter(player, tool, 10)) {
            assertEquals(10, DynamicTreesFellingWear.scale(player, tool, 1));
            assertEquals(1, DynamicTreesFellingWear.scale(otherPlayer, tool, 1));
            assertEquals(1, DynamicTreesFellingWear.scale(player, otherTool, 1));
        }
        assertEquals(1, DynamicTreesFellingWear.scale(player, tool, 1));
    }

    @Test void nestedActionsRestorePreviousScopeWithoutMultiplyingTwice() {
        Object player = new Object(), tool = new Object(), other = new Object();
        try (var outer = DynamicTreesFellingWear.enter(player, tool, 10)) {
            try (var nested = DynamicTreesFellingWear.enter(player, tool, 10)) {
                assertEquals(30, DynamicTreesFellingWear.scale(player, tool, 3));
            }
            try (var nested = DynamicTreesFellingWear.enter(player, other, 2)) {
                assertEquals(3, DynamicTreesFellingWear.scale(player, tool, 3));
                assertEquals(6, DynamicTreesFellingWear.scale(player, other, 3));
            }
            assertEquals(30, DynamicTreesFellingWear.scale(player, tool, 3));
        }
    }

    @Test void exceptionAndThreadBoundariesClearChargeContext() throws Exception {
        Object player = new Object(), tool = new Object();
        assertThrows(IllegalStateException.class, () -> {
            try (var ignored = DynamicTreesFellingWear.enter(player, tool, 10)) {
                throw new IllegalStateException("native failure");
            }
        });
        assertEquals(1, DynamicTreesFellingWear.scale(player, tool, 1));
        try (var ignored = DynamicTreesFellingWear.enter(player, tool, 10)) {
            int[] otherThread = {-1};
            Thread thread = new Thread(() -> otherThread[0] = DynamicTreesFellingWear.scale(player, tool, 1));
            thread.start();
            thread.join();
            assertEquals(1, otherThread[0]);
        }
    }
}
