package com.bettercontent.bettercompatfixes.nativefixture;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.bettercontent.bettercompatfixes.gametest.DynamicTreesFellingWearGameTests;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Read-only test observation of the effective value actually returned to native wear calculations. */
@Mixin(value = BcFixesConfig.class, remap = false)
public abstract class FellingConfigReadMixin {
    @Inject(method = "dynamicTreesFellingDurabilityMultiplier", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private static void observeNativeMultiplier(CallbackInfoReturnable<Integer> callback) {
        DynamicTreesFellingWearGameTests.observeNativeMultiplier(callback.getReturnValueI());
    }
}
