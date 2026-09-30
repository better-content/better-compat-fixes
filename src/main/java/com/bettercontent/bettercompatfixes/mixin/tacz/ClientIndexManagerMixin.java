package com.bettercontent.bettercompatfixes.mixin.tacz;

import com.bettercontent.bettercompatfixes.compat.tacz.TaczDisplayCacheRecovery;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.tacz.guns.client.resource.ClientIndexManager", remap = false)
public abstract class ClientIndexManagerMixin {
    @Inject(
            method = "reload",
            at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/resource/ClientIndexManager;loadGunDisplay()V"),
            require = 1,
            remap = false)
    private static void betterContentFixes$recoverServerDisplays(final CallbackInfo callback) {
        TaczDisplayCacheRecovery.recover(Minecraft.getInstance().getResourceManager());
    }
}
