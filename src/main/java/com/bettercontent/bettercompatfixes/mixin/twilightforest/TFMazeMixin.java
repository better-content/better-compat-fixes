package com.bettercontent.bettercompatfixes.mixin.twilightforest;

import com.bettercontent.bettercompatfixes.compat.TwilightForestMazeSerialization;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import twilightforest.world.components.structures.TFStructureComponentOld;

/** Prevents parallel chunk placement from using the same maze random source concurrently. */
@Mixin(targets = "twilightforest.world.components.structures.TFMaze", remap = false)
public abstract class TFMazeMixin {
    @Shadow
    @Final
    public RandomSource rand;

    @WrapMethod(method = "copyToStructure")
    private void better_compat_fixes$serializeSharedMazeRandom(
            final WorldGenLevel level,
            final StructureManager structureManager,
            final ChunkGenerator chunkGenerator,
            final int x,
            final int y,
            final int z,
            final TFStructureComponentOld component,
            final BoundingBox bounds,
            final Operation<Void> original) {
        if (!BcFixesConfig.twilightForestSerializeC2meMazePlacement()) {
            original.call(level, structureManager, chunkGenerator, x, y, z, component, bounds);
            return;
        }

        TwilightForestMazeSerialization.runSerialized(
                rand,
                () -> original.call(level, structureManager, chunkGenerator, x, y, z, component, bounds));
    }
}
