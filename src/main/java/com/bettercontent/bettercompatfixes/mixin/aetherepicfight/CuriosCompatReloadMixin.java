package com.bettercontent.bettercompatfixes.mixin.aetherepicfight;

import com.bettercontent.bettercompatfixes.client.aether.AetherCurioMeshes;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "yesman.epicfight.compat.CuriosCompat", remap = false)
public abstract class CuriosCompatReloadMixin {
    @Inject(method = "lambda$onModEventBusClient$1", at = @At("TAIL"), require = 1, allow = 1)
    private static void bc$clearAccessoryMeshes(EntityRenderersEvent.AddLayers event, CallbackInfo ci) {
        AetherCurioMeshes.clear();
    }
}
