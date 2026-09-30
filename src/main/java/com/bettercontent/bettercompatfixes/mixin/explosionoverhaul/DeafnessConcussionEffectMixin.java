package com.bettercontent.bettercompatfixes.mixin.explosionoverhaul;

import com.bettercontent.bettercompatfixes.compat.ExplosionOverhaulConcussionClamp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "com.vinlanx.explosionoverhaul.client.DeafnessConcussionEffect", remap = false)
public abstract class DeafnessConcussionEffectMixin {
    @ModifyVariable(
            method = "start(FDDLjava/lang/String;I)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 1,
            remap = false)
    private static double betterContentFixes$clampInitialDuration(final double durationSeconds) {
        return ExplosionOverhaulConcussionClamp.clampSeconds(durationSeconds);
    }

    @ModifyConstant(
            method = "start(FDDLjava/lang/String;I)Z",
            constant = @Constant(intValue = 2_000),
            require = 1,
            remap = false)
    private static int betterContentFixes$clampAccumulatedDuration(final int upstreamLimitTicks) {
        return ExplosionOverhaulConcussionClamp.clampAccumulatedTicks(upstreamLimitTicks);
    }
}
