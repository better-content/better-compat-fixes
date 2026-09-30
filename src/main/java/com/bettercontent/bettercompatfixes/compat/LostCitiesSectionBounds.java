package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;

/** Keeps Lost Cities' fixed section cache from addressing the exclusive build-height boundaries. */
public final class LostCitiesSectionBounds {
    private LostCitiesSectionBounds() {
    }

    public static boolean contains(final LevelHeightAccessor height, final BlockPos pos) {
        return !height.isOutsideBuildHeight(pos);
    }
}
