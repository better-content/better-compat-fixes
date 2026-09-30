package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.compat.DaylightProtectionPolicy;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public final class MobMixin {
    @Inject(
            method = {"isSunBurnTick", "m_21527_"},
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void better_compat_fixes$disableSunBurnTick(final CallbackInfoReturnable<Boolean> cir) {
        if (DaylightProtectionPolicy.disablesSunBurnTick((Mob) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
