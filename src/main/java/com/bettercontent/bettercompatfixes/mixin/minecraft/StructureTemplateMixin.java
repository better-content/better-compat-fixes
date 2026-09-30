package com.bettercontent.bettercompatfixes.mixin.minecraft;

import java.util.ArrayList;
import java.util.List;

import com.bettercontent.bettercompatfixes.compat.PotionAcquisitionPolicy;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sanitizes final block-entity NBT after structure processors have run. */
@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
    @ModifyReturnValue(
            method = "processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;"
                    + "Ljava/util/List;"
                    + "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)Ljava/util/List;",
            at = @At("RETURN"),
            require = 1)
    private static List<StructureTemplate.StructureBlockInfo> betterContentFixes$sanitizePotionStacks(
            final List<StructureTemplate.StructureBlockInfo> processed) {
        final List<StructureTemplate.StructureBlockInfo> sanitized = new ArrayList<>(processed.size());
        for (final StructureTemplate.StructureBlockInfo info : processed) {
            sanitized.add(info.nbt() == null
                    ? info
                    : new StructureTemplate.StructureBlockInfo(
                            info.pos(), info.state(), PotionAcquisitionPolicy.sanitizeBlockEntityNbt(info.nbt())));
        }
        return sanitized;
    }
}
