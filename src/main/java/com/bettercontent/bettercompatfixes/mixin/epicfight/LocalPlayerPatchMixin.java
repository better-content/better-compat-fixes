package com.bettercontent.bettercompatfixes.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@Mixin(value = LocalPlayerPatch.class, remap = false)
public abstract class LocalPlayerPatchMixin {
    @Inject(method = "toVanillaMode", at = @At("HEAD"), cancellable = true, require = 1)
    private void betterContentFixes$keepBattleMode(final boolean synchronize, final CallbackInfo ci) {
        ((LocalPlayerPatch) (Object) this).toEpicFightMode(synchronize);
        ci.cancel();
    }
}
