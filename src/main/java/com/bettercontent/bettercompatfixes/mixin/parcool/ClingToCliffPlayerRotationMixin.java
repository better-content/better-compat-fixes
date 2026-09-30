package com.bettercontent.bettercompatfixes.mixin.parcool;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps ParCool from turning the player's body toward the grabbed wall every render tick. */
@Mixin(targets = "com.alrex.parcool.common.action.impl.ClingToCliff", remap = false)
public abstract class ClingToCliffPlayerRotationMixin {
    @Inject(method = "onRenderTick", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void betterContentFixes$preservePlayerLook(final CallbackInfo callbackInfo) {
        // ParCool's entire onRenderTick method only writes body yaw; climb motion and camera input
        // are handled elsewhere and remain owned by their existing systems.
        callbackInfo.cancel();
    }
}
