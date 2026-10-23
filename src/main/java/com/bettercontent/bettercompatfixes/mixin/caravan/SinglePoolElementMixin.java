package com.bettercontent.bettercompatfixes.mixin.caravan;

import com.bettercontent.bettercompatfixes.compat.caravan.CaravanSettings;
import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exact jigsaw resource-key route only; does not cover the separate native item-use procedure. */
@Mixin(SinglePoolElement.class)
public abstract class SinglePoolElementMixin {
    @Shadow @Final protected Either<ResourceLocation, StructureTemplate> template;

    @Inject(method = "getSettings(Lnet/minecraft/world/level/block/Rotation;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Z)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;",
            at = @At("RETURN"), cancellable = true, require = 1, allow = 1)
    private void betterCompat$caravanAnchor(Rotation rotation, BoundingBox bounds, boolean replaceJigsaw,
                                           CallbackInfoReturnable<StructurePlaceSettings> callback) {
        StructurePlaceSettings original = callback.getReturnValue();
        StructurePlaceSettings result = CaravanSettings.attach(template, original);
        if (result != original) callback.setReturnValue(result);
    }
}
