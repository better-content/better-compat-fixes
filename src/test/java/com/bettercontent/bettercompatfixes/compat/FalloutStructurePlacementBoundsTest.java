package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

final class FalloutStructurePlacementBoundsTest {
    private static final ChunkPos EVIDENCE_CENTER = new ChunkPos(62_500, 62_500);
    private static final int MIN_X = EVIDENCE_CENTER.getMinBlockX() - 16;
    private static final int MAX_X = EVIDENCE_CENTER.getMaxBlockX() + 16;
    private static final int MIN_Z = EVIDENCE_CENTER.getMinBlockZ() - 16;
    private static final int MAX_Z = EVIDENCE_CENTER.getMaxBlockZ() + 16;
    private static final int FIT_MIN_X = MIN_X + FalloutStructurePlacementBounds.EDGE_UPDATE_MARGIN_BLOCKS;
    private static final int FIT_MAX_X = MAX_X - FalloutStructurePlacementBounds.EDGE_UPDATE_MARGIN_BLOCKS;
    private static final int FIT_MIN_Z = MIN_Z + FalloutStructurePlacementBounds.EDGE_UPDATE_MARGIN_BLOCKS;
    private static final int FIT_MAX_Z = MAX_Z - FalloutStructurePlacementBounds.EDGE_UPDATE_MARGIN_BLOCKS;

    @Test
    void minimallyFitsTheThirtyEightBlockFalloutRuinForBothHorizontalOrientations() {
        final BlockPos original = new BlockPos(1_000_015, 62, 1_000_015);

        for (final int[] footprint : new int[][]{{38, 12}, {12, 38}}) {
            final BoundingBox originalBounds = new BoundingBox(
                    original.getX(), original.getY(), original.getZ(),
                    original.getX() + footprint[0] - 1, original.getY() + 21,
                    original.getZ() + footprint[1] - 1);
            final FalloutStructurePlacementBounds.Placement fitted = FalloutStructurePlacementBounds.fit(
                    originalBounds, original, original, EVIDENCE_CENTER).orElseThrow();
            final BoundingBox fittedBounds = originalBounds.moved(fitted.shiftX(), 0, fitted.shiftZ());

            assertInsideWritableEnvelope(fittedBounds);
            assertEquals(expectedMinimalShift(
                            originalBounds.minX(), originalBounds.maxX(), FIT_MIN_X, FIT_MAX_X),
                    fitted.shiftX());
            assertEquals(expectedMinimalShift(
                            originalBounds.minZ(), originalBounds.maxZ(), FIT_MIN_Z, FIT_MAX_Z),
                    fitted.shiftZ());
            assertEquals(fitted.position(), fitted.pivot());
        }
    }

    @Test
    void preservesAnAlreadyLegalPlacementExactly() {
        final BlockPos position = new BlockPos(1_000_000, 70, 1_000_000);
        final BlockPos pivot = position.offset(2, 0, 3);
        final BoundingBox bounds = new BoundingBox(999_990, 60, 999_990, 1_000_020, 90, 1_000_020);

        final FalloutStructurePlacementBounds.Placement fitted = FalloutStructurePlacementBounds.fit(
                bounds, position, pivot, EVIDENCE_CENTER).orElseThrow();

        assertEquals(position, fitted.position());
        assertEquals(pivot, fitted.pivot());
        assertEquals(0, fitted.shiftX());
        assertEquals(0, fitted.shiftZ());
    }

    @Test
    void rejectsFutureOversizedTemplatesBeforeAnyPlacement() {
        final BlockPos position = new BlockPos(1_000_000, 70, 1_000_000);
        final BoundingBox fiftyWide = new BoundingBox(999_975, 60, 999_990, 1_000_024, 90, 1_000_020);
        final BoundingBox fiftyDeep = new BoundingBox(999_990, 60, 999_975, 1_000_020, 90, 1_000_024);

        assertFalse(FalloutStructurePlacementBounds.fit(
                fiftyWide, position, position, EVIDENCE_CENTER).isPresent());
        assertFalse(FalloutStructurePlacementBounds.fit(
                fiftyDeep, position, position, EVIDENCE_CENTER).isPresent());
    }

    @Test
    void reservesOneBlockOnEveryHorizontalEdgeForVanillaShapeUpdates() {
        final BlockPos position = new BlockPos(1_000_000, 70, 1_000_000);
        final BoundingBox fullEnvelope = new BoundingBox(MIN_X, 0, MIN_Z, MAX_X, 255, MAX_Z);
        final BoundingBox placeableEnvelope =
                new BoundingBox(FIT_MIN_X, 0, FIT_MIN_Z, FIT_MAX_X, 255, FIT_MAX_Z);

        assertFalse(FalloutStructurePlacementBounds.fit(
                fullEnvelope, position, position, EVIDENCE_CENTER).isPresent());

        final Optional<FalloutStructurePlacementBounds.Placement> fitted =
                FalloutStructurePlacementBounds.fit(
                        placeableEnvelope, position, position, EVIDENCE_CENTER);

        assertTrue(fitted.isPresent());
        assertEquals(48, fullEnvelope.getXSpan());
        assertEquals(48, fullEnvelope.getZSpan());
        assertEquals(46, placeableEnvelope.getXSpan());
        assertEquals(46, placeableEnvelope.getZSpan());
        assertInsideWritableEnvelope(expandHorizontally(placeableEnvelope, 1));
    }

    private static void assertInsideWritableEnvelope(final BoundingBox bounds) {
        assertTrue(bounds.minX() >= MIN_X, bounds::toString);
        assertTrue(bounds.maxX() <= MAX_X, bounds::toString);
        assertTrue(bounds.minZ() >= MIN_Z, bounds::toString);
        assertTrue(bounds.maxZ() <= MAX_Z, bounds::toString);
    }

    private static BoundingBox expandHorizontally(final BoundingBox bounds, final int blocks) {
        return new BoundingBox(
                bounds.minX() - blocks,
                bounds.minY(),
                bounds.minZ() - blocks,
                bounds.maxX() + blocks,
                bounds.maxY(),
                bounds.maxZ() + blocks);
    }

    private static int expectedMinimalShift(
            final int templateMin,
            final int templateMax,
            final int envelopeMin,
            final int envelopeMax
    ) {
        if (templateMin < envelopeMin) {
            return envelopeMin - templateMin;
        }
        if (templateMax > envelopeMax) {
            return envelopeMax - templateMax;
        }
        return 0;
    }
}
