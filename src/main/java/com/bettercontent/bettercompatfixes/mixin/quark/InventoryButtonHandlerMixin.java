package com.bettercontent.bettercompatfixes.mixin.quark;

import com.bettercontent.bettercompatfixes.client.quark.NativeSortRouting;
import com.google.common.collect.Multimap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.violetmoon.quark.base.client.handler.InventoryButtonHandler;
import org.violetmoon.quark.base.client.handler.InventoryButtonHandler.ButtonTargetType;
import org.violetmoon.zeta.client.event.play.ZScreen;

@Mixin(value = InventoryButtonHandler.class, remap = false)
public abstract class InventoryButtonHandlerMixin {
    @Shadow @Final private static Multimap<ButtonTargetType, ?> providers;

    @Inject(method = "mouseInputEvent", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void bc$singleNativeMiddleSort(ZScreen.MouseButtonPressed.Pre event, CallbackInfo ci) {
        if (event.getButton() != 2 || !NativeSortRouting.ownsMiddleBinding(providers)) return;
        // Always suppress the original two-target loop, even if no valid sorting slot was hit.
        // Do not cancel the input event unless route actually invoked a native sort callback.
        ci.cancel();
        NativeSortRouting.route(event, providers);
    }
}
