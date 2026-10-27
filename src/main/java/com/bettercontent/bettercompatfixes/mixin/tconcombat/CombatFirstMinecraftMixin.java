package com.bettercontent.bettercompatfixes.mixin.tconcombat;

import com.bettercontent.bettercompatfixes.compat.TconCombatFirstPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Run before EF's own HEAD cancellation so tool swaps still clear native mining progress. */
@Mixin(value = Minecraft.class, priority = 1100)
public abstract class CombatFirstMinecraftMixin {
    @Inject(method = {"startAttack()Z", "m_202354_()Z"}, at = @At("HEAD"),
            cancellable = true, remap = false, require = 1)
    private void betterContentFixes$neverStartBlockAttack(CallbackInfoReturnable<Boolean> cir) {
        var minecraft = (Minecraft) (Object) this;
        if (minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK
                && TconCombatFirstPolicy.deniesBlockAttack(minecraft.player)) {
            if (minecraft.gameMode != null) minecraft.gameMode.stopDestroyBlock();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = {"continueAttack(Z)V", "m_91386_(Z)V"}, at = @At("HEAD"),
            cancellable = true, remap = false, require = 1)
    private void betterContentFixes$neverContinueBlockAttack(boolean held, CallbackInfo ci) {
        var minecraft = (Minecraft) (Object) this;
        if (TconCombatFirstPolicy.deniesBlockAttack(minecraft.player)) {
            // Do not touch key mappings: EF must still see normal attack/innate input.
            if (minecraft.gameMode != null) minecraft.gameMode.stopDestroyBlock();
            ci.cancel();
        }
    }
}
