package com.bettercontent.bettercompatfixes.compat;

/** Depth selection only: no GL copies, allocations, camera transforms or temporal filtering. */
public final class DhOpaqueDepthPolicy {
    private DhOpaqueDepthPolicy() { }

    public static int selectDepth(boolean transparencyDisabled, int liveDepth, int copiedDepth) {
        // Before framebuffer connection (or after teardown), preserve Oculus' native sentinel.
        // With transparency enabled the live texture also contains water, so retain its opaque copy.
        return transparencyDisabled && liveDepth > 0 ? liveDepth : copiedDepth;
    }
}
