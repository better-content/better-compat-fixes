package com.bettercontent.bettercompatfixes.mixin.tconcombat;

import com.bettercontent.bettercompatfixes.client.CombatMiningPolicy.Route;
import com.bettercontent.bettercompatfixes.client.CombatMiningRouting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

/** Authoritative routing; no later adaptive branch may reinterpret a different ray. */
@Mixin(value = LocalPlayerPatch.class, remap = false)
public abstract class CombatFirstLocalPlayerMixin {
    @Inject(method = "canPlayAttackAnimation", at = @At("HEAD"), cancellable = true, require = 1)
    private void betterContentFixes$routeAttack(CallbackInfoReturnable<Boolean> cir) {
        var route = CombatMiningRouting.current();
        if (route != Route.NATIVE) cir.setReturnValue(route == Route.COMBAT);
    }
}
