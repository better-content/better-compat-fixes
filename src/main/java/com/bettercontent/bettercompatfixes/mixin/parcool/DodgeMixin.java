package com.bettercontent.bettercompatfixes.mixin.parcool;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.alrex.parcool.common.action.impl.Dodge", remap = false)
public abstract class DodgeMixin {
    @ModifyExpressionValue(
            method = "canStart",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/alrex/parcool/config/ParCoolConfig$Client$Booleans;get()Ljava/lang/Boolean;",
                    ordinal = 0),
            require = 1)
    private Boolean betterContentFixes$disableNativeDirectionalDoubleTap(final Boolean original) {
        return Boolean.FALSE;
    }
}
