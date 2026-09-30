package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** Uses Minecraft's inspectable river biome tag for Create water-wheel generation. */
public final class WaterWheelBiomePolicy {
    private WaterWheelBiomePolicy() {
    }

    public static boolean allowsGeneration(final Level level, final BlockPos pos) {
        return level.getBiome(pos).is(BiomeTags.IS_RIVER);
    }
}
