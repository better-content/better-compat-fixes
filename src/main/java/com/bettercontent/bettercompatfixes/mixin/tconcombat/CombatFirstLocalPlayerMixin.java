package com.bettercontent.bettercompatfixes.mixin.tconcombat;

import com.bettercontent.bettercompatfixes.compat.TconCombatFirstPolicy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

/** Leave EF's chosen FPS/TPS ray and all early mode/entity/lock-on decisions intact. */
@Mixin(value = LocalPlayerPatch.class, remap = false)
public abstract class CombatFirstLocalPlayerMixin {
    @ModifyExpressionValue(method = "canPlayAttackAnimation", at = @At(value = "INVOKE",
            target = "Lyesman/epicfight/config/ClientConfig$PreferenceWork;checkHitResult()Z"), require = 1)
    private boolean betterContentFixes$combatInsteadOfAdaptiveMining(boolean adaptive,
            @Local HitResult selectedHit) {
        return adaptive && !(RenderEngine.asBlockHitResult(selectedHit) != null
                && TconCombatFirstPolicy.deniesBlockAttack(Minecraft.getInstance().player));
    }
}
