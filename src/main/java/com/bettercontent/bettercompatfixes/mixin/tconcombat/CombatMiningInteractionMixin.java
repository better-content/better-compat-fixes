package com.bettercontent.bettercompatfixes.mixin.tconcombat;

import com.bettercontent.bettercompatfixes.client.CombatMiningPolicy.Route;
import com.bettercontent.bettercompatfixes.client.CombatMiningRouting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.client.events.engine.ControlEngine;

/** Bypass only EF's weapon-preference rejection, not other handlers or cancellation. */
@Mixin(value = ControlEngine.Events.class, remap = false)
public abstract class CombatMiningInteractionMixin {
    @ModifyExpressionValue(method = "interactionEvent", at = @At(value = "INVOKE",
            target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z", ordinal = 0), require = 1)
    private static boolean betterContentFixes$allowKnifeMining(boolean combatPreferred) {
        return combatPreferred && CombatMiningRouting.current() != Route.MINE;
    }
}
