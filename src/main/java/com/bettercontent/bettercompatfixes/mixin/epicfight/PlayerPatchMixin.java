package com.bettercontent.bettercompatfixes.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = PlayerPatch.class, remap = false)
public abstract class PlayerPatchMixin {
    @Inject(method = "toVanillaMode", at = @At("HEAD"), cancellable = true, require = 1)
    private void betterContentFixes$keepBattleMode(final boolean synchronize, final CallbackInfo ci) {
        ((PlayerPatch<?>) (Object) this).toEpicFightMode(synchronize);
        ci.cancel();
    }
}
