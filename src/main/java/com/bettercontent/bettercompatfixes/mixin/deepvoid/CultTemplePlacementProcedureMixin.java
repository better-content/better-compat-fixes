package com.bettercontent.bettercompatfixes.mixin.deepvoid;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Move both native templates and their delayed floor openings to the actual world bottom. */
@Pseudo
@Mixin(targets = "net.mcreator.thedeepvoid.procedures.CultTemplePlacementProcedure", remap = false)
public abstract class CultTemplePlacementProcedureMixin {
    @ModifyExpressionValue(
            method = {
                    "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;)V",
                    "lambda$execute$0", "lambda$execute$1"
            },
            at = @At(value = "INVOKE", target = "Ljava/lang/Double;doubleValue()D", remap = false),
            remap = false, require = 6, allow = 6)
    private static double better_compat_fixes$actualWorldBottom(
            final double obsoleteAbsoluteHeight,
            @Local(argsOnly = true) final LevelAccessor level
    ) {
        return level.getMinBuildHeight();
    }
}
