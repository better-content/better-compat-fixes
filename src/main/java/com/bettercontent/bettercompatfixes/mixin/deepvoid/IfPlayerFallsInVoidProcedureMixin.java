package com.bettercontent.bettercompatfixes.mixin.deepvoid;

import com.bettercontent.bettercompatfixes.compat.DeepVoidFallBoundary;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** All three pinned native fall routes retain their guards and teleport implementation. */
@Pseudo
@Mixin(targets = "net.mcreator.thedeepvoid.procedures.IfPlayerFallsInVoidProcedure", remap = false)
public abstract class IfPlayerFallsInVoidProcedureMixin {
    @ModifyExpressionValue(
            method = "execute(Lnet/minecraftforge/eventbus/api/Event;DDLnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Double;doubleValue()D", remap = false),
            remap = false, require = 3, allow = 3)
    private static double better_compat_fixes$actualWorldBottom(
            final double obsoleteAbsoluteHeight,
            @Local(argsOnly = true) final Entity entity
    ) {
        return DeepVoidFallBoundary.threshold(entity.level().getMinBuildHeight());
    }
}
