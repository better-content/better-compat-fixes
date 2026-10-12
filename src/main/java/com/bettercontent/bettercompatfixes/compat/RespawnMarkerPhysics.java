package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Identity-only exclusion. Vanilla crying obsidian and every other block remain native. */
public final class RespawnMarkerPhysics {
    private static final ResourceLocation MARKER = new ResourceLocation("better_spawns", "respawn_marker");

    private RespawnMarkerPhysics() {}

    public static boolean isExcluded(final BlockState state) {
        return state != null && isExcludedId(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    static boolean isExcludedId(final ResourceLocation id) {
        return MARKER.equals(id);
    }
}
