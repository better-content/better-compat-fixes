package com.bettercontent.bettercompatfixes.compat.epicfightvs.client;

import net.minecraft.client.Camera;

public final class EpicFightVsCameraState {
    private static Camera pendingCamera;

    private EpicFightVsCameraState() {
    }

    public static void mark(final Camera camera) {
        pendingCamera = camera;
    }

    public static boolean consume(final Camera camera) {
        final boolean matches = pendingCamera == camera;
        pendingCamera = null;
        return matches;
    }
}
