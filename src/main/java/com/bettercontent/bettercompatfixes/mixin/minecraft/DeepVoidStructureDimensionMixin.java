package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.deepvoid.DeepVoidEntranceStructure;
import com.bettercontent.bettercompatfixes.deepvoid.DeepVoidStructureLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Biome tags alone do not constrain a structure to a dimension. */
@Mixin(value = ChunkGenerator.class, remap = false)
public abstract class DeepVoidStructureDimensionMixin {
    @Inject(method = {"tryGenerateStructure", "m_223104_"}, at = @At("HEAD"), cancellable = true, remap = false, require = 1, allow = 1)
    private void better_compat_fixes$onlyOverworld(StructureSet.StructureSelectionEntry selection,
            StructureManager structures, net.minecraft.core.RegistryAccess registries,
            net.minecraft.world.level.levelgen.RandomState randomState,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates,
            long seed, net.minecraft.world.level.chunk.ChunkAccess chunk, net.minecraft.world.level.ChunkPos pos,
            net.minecraft.core.SectionPos section, CallbackInfoReturnable<Boolean> callback) {
        if (selection.structure().value() instanceof DeepVoidEntranceStructure
                && !((DeepVoidStructureLevel) structures).better_compat_fixes$isOverworld()) callback.setReturnValue(false);
    }
}
