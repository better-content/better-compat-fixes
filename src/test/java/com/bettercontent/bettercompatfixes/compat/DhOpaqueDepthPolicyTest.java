package com.bettercontent.bettercompatfixes.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DhOpaqueDepthPolicyTest {
    @Test
    void disabledTransparencyUsesLiveDepthInsteadOfUnrefreshedCopy() {
        assertEquals(41, DhOpaqueDepthPolicy.selectDepth(true, 41, 72));
        assertEquals(41, DhOpaqueDepthPolicy.selectDepth(true, 41, 0));
    }

    @Test
    void enabledTransparencyPreservesOpaqueCopyAndDoesNotUseWaterDepth() {
        assertEquals(72, DhOpaqueDepthPolicy.selectDepth(false, 41, 72));
        assertEquals(0, DhOpaqueDepthPolicy.selectDepth(false, 41, 0));
    }

    @Test
    void initializationAndTeardownPreserveNativeFallback() {
        assertEquals(0, DhOpaqueDepthPolicy.selectDepth(true, 0, 0));
        assertEquals(72, DhOpaqueDepthPolicy.selectDepth(true, 0, 72));
        assertEquals(0, DhOpaqueDepthPolicy.selectDepth(true, -1, 0));
        assertEquals(72, DhOpaqueDepthPolicy.selectDepth(true, -1, 72));
    }

    @Test
    void selectionTracksSettingAndFramebufferChangesWithoutCaching() {
        assertEquals(41, DhOpaqueDepthPolicy.selectDepth(true, 41, 72));
        assertEquals(72, DhOpaqueDepthPolicy.selectDepth(false, 41, 72));
        assertEquals(53, DhOpaqueDepthPolicy.selectDepth(true, 53, 72));
        assertEquals(72, DhOpaqueDepthPolicy.selectDepth(false, 53, 72));
    }
}
