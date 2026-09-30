package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;

/** Keeps Better Caves block carving inside the vertical range represented by the chunk's carving mask. */
public final class BetterCavesCarvingBounds {
    private BetterCavesCarvingBounds() {
    }

    public static boolean contains(final LevelHeightAccessor height, final BlockPos pos) {
        return !height.isOutsideBuildHeight(pos);
    }
}
