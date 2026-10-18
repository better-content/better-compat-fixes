package com.bettercontent.bettercompatfixes.mixin.wom;

import com.bettercontent.bettercompatfixes.compat.SaulomonkSpawnDistance;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Keep the pinned native difficulty, lighting, surface and spawn checks intact. */
@Pseudo
@Mixin(targets = "reascer.wom.world.entity.mob.Saulomonk", remap = false)
public abstract class SaulomonkMixin {
    @ModifyExpressionValue(method = "checkSaulomonkSpawnRules", remap = false,
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;sqrt(D)D", remap = false),
            require = 1, allow = 1)
    private static double better_compat_fixes$personalStartingPoint(final double originDistance,
            @Local(argsOnly = true) final ServerLevelAccessor world,
            @Local(argsOnly = true) final BlockPos candidate) {
        return SaulomonkSpawnDistance.fromNearestPlayer(world, candidate);
    }

    @ModifyConstant(method = "checkSaulomonkSpawnRules", remap = false,
            constant = @Constant(doubleValue = 5000.0D), require = 1, allow = 1)
    private static double better_compat_fixes$twoKilometres(final double oldMinimum) {
        return SaulomonkSpawnDistance.MINIMUM_DISTANCE;
    }
}
