package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.CarvingMask;
import org.junit.jupiter.api.Test;

final class BetterCavesCarvingBoundsTest {
    private static final LevelHeightAccessor LOST_CITIES_HEIGHT = LevelHeightAccessor.create(0, 256);

    @Test
    void rejectsTheBuild210NegativeCarvingMaskIndexBeforeWriting() {
        final BlockPos crashPos = new BlockPos(0, -17, 0);
        final CarvingMask mask = new CarvingMask(
                LOST_CITIES_HEIGHT.getHeight(), LOST_CITIES_HEIGHT.getMinBuildHeight());

        assertEquals(-4352, carvingMaskIndex(crashPos, LOST_CITIES_HEIGHT.getMinBuildHeight()));
        assertThrows(IndexOutOfBoundsException.class,
                () -> mask.set(crashPos.getX(), crashPos.getY(), crashPos.getZ()));
        assertFalse(BetterCavesCarvingBounds.contains(LOST_CITIES_HEIGHT, crashPos));
        assertDoesNotThrow(() -> carveIfInside(mask, crashPos));
    }

    @Test
    void preservesBothEdgesOfTheLegalLostCitiesRange() {
        final CarvingMask mask = new CarvingMask(
                LOST_CITIES_HEIGHT.getHeight(), LOST_CITIES_HEIGHT.getMinBuildHeight());
        final BlockPos bottom = new BlockPos(3, 0, 5);
        final BlockPos top = new BlockPos(7, 255, 9);

        assertTrue(BetterCavesCarvingBounds.contains(LOST_CITIES_HEIGHT, bottom));
        assertTrue(BetterCavesCarvingBounds.contains(LOST_CITIES_HEIGHT, top));
        carveIfInside(mask, bottom);
        carveIfInside(mask, top);
        assertTrue(mask.get(bottom.getX(), bottom.getY(), bottom.getZ()));
        assertTrue(mask.get(top.getX(), top.getY(), top.getZ()));
    }

    @Test
    void rejectsTheExclusiveUpperBuildLimit() {
        assertFalse(BetterCavesCarvingBounds.contains(
                LOST_CITIES_HEIGHT, new BlockPos(0, LOST_CITIES_HEIGHT.getMaxBuildHeight(), 0)));
    }

    private static void carveIfInside(final CarvingMask mask, final BlockPos pos) {
        if (BetterCavesCarvingBounds.contains(LOST_CITIES_HEIGHT, pos)) {
            mask.set(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    private static int carvingMaskIndex(final BlockPos pos, final int minY) {
        return (pos.getX() & 15) | ((pos.getZ() & 15) << 4) | ((pos.getY() - minY) << 8);
    }
}
