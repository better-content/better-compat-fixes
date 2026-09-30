package com.bettercontent.bettercompatfixes.compat;

public final class AmbientSoundPlaybackRecovery {
    private AmbientSoundPlaybackRecovery() {
    }

    public static boolean shouldRetireUnstarted(final boolean active, final boolean playedOnce) {
        return !active && !playedOnce;
    }
}
