package com.bettercontent.bettercompatfixes.compat;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Fits Fallout's feature-placed templates inside WorldGenRegion's writable 3x3 chunk envelope. */
public final class FalloutStructurePlacementBounds {
    public static final int WRITABLE_ENVELOPE_BLOCKS = 48;
    public static final int EDGE_UPDATE_MARGIN_BLOCKS = 1;
    public static final int PLACEABLE_TEMPLATE_BLOCKS =
            WRITABLE_ENVELOPE_BLOCKS - (EDGE_UPDATE_MARGIN_BLOCKS * 2);

    private FalloutStructurePlacementBounds() {
    }

    public static Optional<Placement> fit(
            final BoundingBox templateBounds,
            final BlockPos position,
            final BlockPos pivot,
            final ChunkPos centerChunk
    ) {
        if (templateBounds.getXSpan() > PLACEABLE_TEMPLATE_BLOCKS
                || templateBounds.getZSpan() > PLACEABLE_TEMPLATE_BLOCKS) {
            return Optional.empty();
        }

        // StructureTemplate updates the immediate neighbours of its placed-block outline. Keep
        // that vanilla edge pass inside WorldGenRegion as well as the template blocks themselves.
        final int envelopeMinX = centerChunk.getMinBlockX() - 16 + EDGE_UPDATE_MARGIN_BLOCKS;
        final int envelopeMaxX = centerChunk.getMaxBlockX() + 16 - EDGE_UPDATE_MARGIN_BLOCKS;
        final int envelopeMinZ = centerChunk.getMinBlockZ() - 16 + EDGE_UPDATE_MARGIN_BLOCKS;
        final int envelopeMaxZ = centerChunk.getMaxBlockZ() + 16 - EDGE_UPDATE_MARGIN_BLOCKS;
        final int shiftX = minimalShift(
                templateBounds.minX(), templateBounds.maxX(), envelopeMinX, envelopeMaxX);
        final int shiftZ = minimalShift(
                templateBounds.minZ(), templateBounds.maxZ(), envelopeMinZ, envelopeMaxZ);

        return Optional.of(new Placement(
                position.offset(shiftX, 0, shiftZ),
                pivot.offset(shiftX, 0, shiftZ),
                shiftX,
                shiftZ));
    }

    private static int minimalShift(
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

    public record Placement(BlockPos position, BlockPos pivot, int shiftX, int shiftZ) {
    }
}
