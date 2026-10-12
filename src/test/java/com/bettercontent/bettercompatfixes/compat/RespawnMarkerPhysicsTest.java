package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RespawnMarkerPhysicsTest {
    @Test
    void onlyExactMarkerIdentityIsExcluded() {
        assertTrue(RespawnMarkerPhysics.isExcludedId(new ResourceLocation("better_spawns", "respawn_marker")));
        assertFalse(RespawnMarkerPhysics.isExcludedId(new ResourceLocation("minecraft", "crying_obsidian")));
        assertFalse(RespawnMarkerPhysics.isExcludedId(new ResourceLocation("minecraft", "respawn_marker")));
        assertFalse(RespawnMarkerPhysics.isExcludedId(new ResourceLocation("better_spawns", "other_marker")));
        assertFalse(RespawnMarkerPhysics.isExcludedId(null));
    }
}
