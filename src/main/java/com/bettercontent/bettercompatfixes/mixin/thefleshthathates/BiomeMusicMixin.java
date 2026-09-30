package com.bettercontent.bettercompatfixes.mixin.thefleshthathates;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mcreator.thefleshthathates.FEvents.BiomeMusic", remap = false)
public abstract class BiomeMusicMixin {
    @Inject(method = "onPlayerTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void better_compat_fixes$disableProximityMusic(
            final TickEvent.PlayerTickEvent event,
            final CallbackInfo ci
    ) {
        if (BcFixesConfig.theFleshThatHatesDisableProximityMusic()) {
            ci.cancel();
        }
    }
}
