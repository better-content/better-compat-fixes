package com.bettercontent.bettercompatfixes.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmbientSoundPlaybackRecoveryTest {
    @Test
    void retiresOnlyInactiveStreamsThatNeverStarted() {
        assertTrue(AmbientSoundPlaybackRecovery.shouldRetireUnstarted(false, false));
        assertFalse(AmbientSoundPlaybackRecovery.shouldRetireUnstarted(true, false));
        assertFalse(AmbientSoundPlaybackRecovery.shouldRetireUnstarted(false, true));
        assertFalse(AmbientSoundPlaybackRecovery.shouldRetireUnstarted(true, true));
    }
}
