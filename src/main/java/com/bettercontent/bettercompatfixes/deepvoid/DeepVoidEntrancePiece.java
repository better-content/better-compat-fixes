package com.bettercontent.bettercompatfixes.deepvoid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Native decoration plus a separate guaranteed bore, so even a three-wide ruin survives. */
public final class DeepVoidEntrancePiece extends TemplateStructurePiece {
    private final BlockPos bore;

    private static StructurePlaceSettings settings() {
        return new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    public DeepVoidEntrancePiece(StructureTemplateManager manager, ResourceLocation template, BlockPos bore) {
        super(DeepVoidStructures.PIECE.get(), 0, manager, template, template.toString(), settings(),
                origin(manager, template, bore));
        this.bore = bore.immutable();
        expandBounds();
    }

    public DeepVoidEntrancePiece(StructureTemplateManager manager, CompoundTag tag) {
        super(DeepVoidStructures.PIECE.get(), tag, manager, ignored -> settings());
        bore = BlockPos.of(tag.getLong("Bore"));
        expandBounds();
    }

    private static BlockPos origin(StructureTemplateManager manager, ResourceLocation name, BlockPos bore) {
        var size = manager.getOrCreate(name).getSize();
        return bore.offset(-size.getX() / 2, 0, -size.getZ() - 3);
    }

    private void expandBounds() {
        boundingBox.encapsulate(new BoundingBox(bore.getX() - 3, bore.getY(), bore.getZ() - 3,
                bore.getX() + 3, bore.getY() + Math.max(4, template.getSize().getY() - 1), bore.getZ() + 3));
    }

    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putLong("Bore", bore.asLong());
    }

    @Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                     RandomSource random, BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
        // Do not call TemplateStructurePiece.postProcess: it mutates the shared bounds/settings.
        // Native templates here have no jigsaws/data markers; each chunk gets private placement settings.
        template.placeInWorld(level, templatePosition, pivot, settings().setBoundingBox(clip), random, 2);
        final int bottom = bore.getY();
        final int top = boundingBox.maxY();
        // Floor/chamber around the hole. Connect to the decoration and outside at its far face.
        final int approachStart = templatePosition.getZ() + template.getSize().getZ() / 2;
        for (int z = approachStart; z <= bore.getZ() + 3; z++) {
            int radius = z >= bore.getZ() - 3 ? 3 : 1;
            for (int x = bore.getX() - radius; x <= bore.getX() + radius; x++) {
                set(level, clip, new BlockPos(x, bottom, z), false);
                for (int y = bottom + 1; y <= bottom + 4; y++) set(level, clip, new BlockPos(x, y, z), true);
            }
        }
        // A lined 3x3 shaft from the lowest legal layer through the top of the piece.
        for (int y = bottom; y <= top; y++) {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                boolean inside = Math.abs(x) <= 1 && Math.abs(z) <= 1;
                if (inside || y > bottom + 4) set(level, clip, bore.offset(x, y - bottom, z), inside);
            }
        }
    }

    private void set(WorldGenLevel level, BoundingBox clip, BlockPos pos, boolean air) {
        // Explicitly clip to this piece AND this chunk: no distant writes under C2ME.
        if (clip.isInside(pos) && boundingBox.isInside(pos) && !level.isOutsideBuildHeight(pos)) {
            level.setBlock(pos, air ? Blocks.AIR.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState(), 2);
        }
    }

    @Override protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                                               RandomSource random, BoundingBox bounds) { }
    public BlockPos bore() { return bore; }
}
