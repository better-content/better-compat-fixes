package com.bettercontent.bettercompatfixes.mixin.twilightforest;

import com.bettercontent.bettercompatfixes.compat.TwilightForestMazeSerialization;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;

/** Prevents C2ME workers from racing Twilight Forest's static stronghold piece-selection state. */
@Mixin(targets = "twilightforest.world.components.structures.util.ConquerableStructure", remap = false)
public abstract class ConquerableStructureMixin {
    private static final String KNIGHT_STRONGHOLD =
            "twilightforest.world.components.structures.type.KnightStrongholdStructure";

    @WrapMethod(method = "generateCustom")
    private StructureStart better_compat_fixes$serializeKnightStrongholdGeneration(
            final RegistryAccess registries,
            final ChunkGenerator chunkGenerator,
            final BiomeSource biomeSource,
            final RandomState randomState,
            final StructureTemplateManager templates,
            final long seed,
            final ChunkPos chunk,
            final int references,
            final LevelHeightAccessor heightAccessor,
            final Predicate<Holder<Biome>> validBiome,
            final Operation<StructureStart> original) {
        if (!BcFixesConfig.twilightForestSerializeC2meMazePlacement()
                || !KNIGHT_STRONGHOLD.equals(getClass().getName())) {
            return original.call(registries, chunkGenerator, biomeSource, randomState, templates,
                    seed, chunk, references, heightAccessor, validBiome);
        }
        return TwilightForestMazeSerialization.callStrongholdSerialized(
                () -> original.call(registries, chunkGenerator, biomeSource, randomState, templates,
                        seed, chunk, references, heightAccessor, validBiome));
    }
}
