package com.bettercontent.bettercompatfixes.mixin.quark;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.violetmoon.quark.base.config.QuarkGeneralConfig;

/** Match Quark's built-in legacy Curios admission for the native V2 screen only. */
@Mixin(value = QuarkGeneralConfig.class, remap = false)
public abstract class QuarkGeneralConfigMixin {
    @Inject(method = "isScreenAllowed", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void bc$allowNativeCuriosV2(Object screen, CallbackInfoReturnable<Boolean> cir) {
        if (screen != null && screen.getClass().getName().equals("top.theillusivec4.curios.client.gui.CuriosScreenV2"))
            cir.setReturnValue(true);
    }
}
