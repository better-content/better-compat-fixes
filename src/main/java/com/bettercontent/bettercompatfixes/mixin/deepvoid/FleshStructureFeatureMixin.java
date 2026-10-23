package com.bettercontent.bettercompatfixes.mixin.deepvoid;

import com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplatePlacement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mcreator.thedeepvoid.world.features.configurations.StructureFeatureConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;

/** Exactly one original template call in either actual pinned mapping graph. */
@Mixin(targets = "net.mcreator.thedeepvoid.world.features.StructureFeature", remap = false)
public abstract class FleshStructureFeatureMixin {
    @Group(name = "fleshTemplate", min = 1, max = 1)
    @WrapOperation(method = {"place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z",
            "m_142674_(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;"
                    + "placeInWorld(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;"
                    + "Lnet/minecraft/util/RandomSource;I)Z", remap = false), remap = false, require = 0)
    private boolean better_compat_fixes$mappedFlesh(StructureTemplate template, ServerLevelAccessor level,
            BlockPos position, BlockPos pivot, StructurePlaceSettings settings, RandomSource random,
            int flags, Operation<Boolean> original, FeaturePlaceContext<StructureFeatureConfiguration> context) {
        return FleshTemplatePlacement.place(context.config().structure().toString(), template, level,
                position, pivot, settings, random, flags, original);
    }

    @Group(name = "fleshTemplate", min = 1, max = 1)
    @WrapOperation(method = {"place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z",
            "m_142674_(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;"
                    + "m_230328_(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;"
                    + "Lnet/minecraft/util/RandomSource;I)Z", remap = false), remap = false, require = 0)
    private boolean better_compat_fixes$rawFlesh(StructureTemplate template, ServerLevelAccessor level,
            BlockPos position, BlockPos pivot, StructurePlaceSettings settings, RandomSource random,
            int flags, Operation<Boolean> original, FeaturePlaceContext<StructureFeatureConfiguration> context) {
        return FleshTemplatePlacement.place(context.config().structure().toString(), template, level,
                position, pivot, settings, random, flags, original);
    }
}
