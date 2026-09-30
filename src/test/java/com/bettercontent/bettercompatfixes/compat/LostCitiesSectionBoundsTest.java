package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.junit.jupiter.api.Test;

final class LostCitiesSectionBoundsTest {
    private static final LevelHeightAccessor LOST_CITIES_HEIGHT = LevelHeightAccessor.create(0, 256);

    @Test
    void rejectsTheBuild225SectionIndexAtTheExclusiveUpperLimit() {
        final BlockPos failingNeighbor = new BlockPos(0, 256, 0);

        assertTrue(sectionIndex(failingNeighbor) == 16);
        assertFalse(LostCitiesSectionBounds.contains(LOST_CITIES_HEIGHT, failingNeighbor));
    }

    @Test
    void preservesEveryPositionRepresentedByTheSectionCache() {
        assertTrue(LostCitiesSectionBounds.contains(
                LOST_CITIES_HEIGHT, new BlockPos(0, 0, 0)));
        assertTrue(LostCitiesSectionBounds.contains(
                LOST_CITIES_HEIGHT, new BlockPos(15, 255, 15)));
    }

    @Test
    void rejectsTheLowerExclusiveBoundary() {
        assertFalse(LostCitiesSectionBounds.contains(
                LOST_CITIES_HEIGHT, new BlockPos(0, -1, 0)));
    }

    private static int sectionIndex(final BlockPos pos) {
        return (pos.getY() - LOST_CITIES_HEIGHT.getMinBuildHeight()) / 16;
    }
}
