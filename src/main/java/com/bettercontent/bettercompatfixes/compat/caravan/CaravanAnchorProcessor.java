package com.bettercontent.bettercompatfixes.compat.caravan;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Attached only to the exact original caravan jigsaw template; never a global entity/NBT hook. */
public final class CaravanAnchorProcessor extends StructureProcessor {
    public static final CaravanAnchorProcessor INSTANCE = new CaravanAnchorProcessor();
    public static final Codec<CaravanAnchorProcessor> CODEC = Codec.unit(() -> INSTANCE);
    public static final StructureProcessorType<CaravanAnchorProcessor> TYPE = () -> CODEC;
    private CaravanAnchorProcessor() { }

    @Override
    protected StructureProcessorType<?> getType() { return TYPE; }

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(LevelReader level, BlockPos origin,
            StructureTemplate.StructureEntityInfo raw, StructureTemplate.StructureEntityInfo processed,
            StructurePlaceSettings settings, StructureTemplate template) {
        CompoundTag original = processed.nbt;
        if (original == null || !original.getString("id").equals("minecraft:item_frame")
                || !original.contains("TileX", Tag.TAG_INT) || !original.contains("TileY", Tag.TAG_INT)
                || !original.contains("TileZ", Tag.TAG_INT)) return processed;
        CompoundTag copy = original.copy();
        // SDK already transformed this anchor to WORLD coordinates; it overwrites Pos later itself.
        copy.putInt("TileX", processed.blockPos.getX());
        copy.putInt("TileY", processed.blockPos.getY());
        copy.putInt("TileZ", processed.blockPos.getZ());
        return new StructureTemplate.StructureEntityInfo(processed.pos, processed.blockPos, copy);
    }
}
