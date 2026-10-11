package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DeepVoidFallBoundaryTest {
    private static final int[] MINIMUM_BUILD_HEIGHTS = {
        Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -512, -128, -64, 0, 3, 128, Integer.MAX_VALUE
    };

    @Test
    void thresholdTracksTheActualDimensionBottomWithSixBlocksOfClearance() {
        assertEquals(-70.0D, DeepVoidFallBoundary.threshold(-64));
        assertEquals(-134.0D, DeepVoidFallBoundary.threshold(-128));
        assertEquals(-518.0D, DeepVoidFallBoundary.threshold(-512));
        assertEquals(-6.0D, DeepVoidFallBoundary.threshold(0));
        assertEquals(-3.0D, DeepVoidFallBoundary.threshold(3));
        assertEquals(122.0D, DeepVoidFallBoundary.threshold(128));
    }

    @Test
    void subtractionUsesDoubleArithmeticWithoutIntegerOverflow() {
        assertEquals(-2_147_483_654.0D, DeepVoidFallBoundary.threshold(Integer.MIN_VALUE));
        assertEquals(-2_147_483_653.0D, DeepVoidFallBoundary.threshold(Integer.MIN_VALUE + 1));
        assertEquals(2_147_483_641.0D, DeepVoidFallBoundary.threshold(Integer.MAX_VALUE));
        for (int minimumBuildHeight : MINIMUM_BUILD_HEIGHTS) {
            double threshold = DeepVoidFallBoundary.threshold(minimumBuildHeight);
            assertTrue(Double.isFinite(threshold));
            assertEquals(6.0D, (double) minimumBuildHeight - threshold);
        }
    }

    @Test
    void coordinatesAtOrAboveTheActualBottomNeverCrossTheFallBoundary() {
        for (int minimumBuildHeight : MINIMUM_BUILD_HEIGHTS) {
            double bottom = minimumBuildHeight;
            double threshold = DeepVoidFallBoundary.threshold(minimumBuildHeight);
            double[] buildableSideCoordinates = {
                bottom, Math.nextUp(bottom), bottom + 0.5D, bottom + 1.0D, bottom + 128.0D
            };
            for (double playerY : buildableSideCoordinates) {
                assertFalse(playerY < threshold,
                        "buildable-side playerY=" + playerY + ", minimum=" + minimumBuildHeight);
            }
        }
    }

    @Test
    void theVanillaThresholdDoesNotTriggerInsideExtendedDimensions() {
        for (int minimumBuildHeight : new int[] {-128, -512}) {
            double threshold = DeepVoidFallBoundary.threshold(minimumBuildHeight);
            for (double playerY : new double[] {-70.0D, Math.nextDown(-70.0D), -100.0D}) {
                assertTrue(playerY >= minimumBuildHeight);
                assertFalse(playerY < threshold,
                        "old vanilla boundary must not teleport a buildable player at " + playerY);
            }
        }
    }

    @Test
    void strictComparisonKeepsTheBufferAndTriggersImmediatelyBelowTheThreshold() {
        // Reference contract for the native playerY < threshold comparison, not proof of mixin wiring.
        for (int minimumBuildHeight : MINIMUM_BUILD_HEIGHTS) {
            double threshold = DeepVoidFallBoundary.threshold(minimumBuildHeight);
            double[] safeCoordinates = {
                threshold, Math.nextUp(threshold), threshold + 0.5D,
                (double) minimumBuildHeight - 0.5D, minimumBuildHeight
            };
            for (double playerY : safeCoordinates) {
                assertFalse(playerY < threshold,
                        "player at/above threshold must remain safe: " + playerY);
            }
            assertTrue(Math.nextDown(threshold) < threshold,
                    "the closest representable position below threshold must trigger");
            assertTrue(threshold - 0.5D < threshold);
            assertTrue(threshold - 1.0D < threshold);
        }
    }
}
