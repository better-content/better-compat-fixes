package com.bettercontent.bettercompatfixes.mixin.lostcities;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.bettercontent.bettercompatfixes.compat.LostCitiesC2meDhSerialization;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    @WrapMethod(method = "m_213609_")
    private void better_compat_fixes$serializeLostCityBiomeDecoration(
            final WorldGenLevel level,
            final ChunkAccess chunk,
            final StructureManager structureManager,
            final Operation<Void> original) {
        if (LostCitiesC2meDhSerialization.shouldSkipGeneration(level)) {
            return;
        }

        if (!LostCitiesC2meDhSerialization.shouldSerialize(level)) {
            original.call(level, chunk, structureManager);
            return;
        }

        LostCitiesC2meDhSerialization.runSerialized(
                () -> original.call(level, chunk, structureManager));
    }
}
