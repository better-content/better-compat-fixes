package com.bettercontent.bettercompatfixes.mixin.sodiumdynamiclights;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "toni.sodiumdynamiclights.SodiumDynamicLights", remap = false)
public abstract class SodiumDynamicLightsMixin {
    @Shadow
    public abstract void onInitializeClient();

    @Inject(method = "clientSetup", at = @At("HEAD"), cancellable = true, require = 1)
    private void betterContentFixes$scheduleClientInitialization(
            final FMLClientSetupEvent event,
            final CallbackInfo ci
    ) {
        event.enqueueWork(this::onInitializeClient);
        ci.cancel();
    }
}
