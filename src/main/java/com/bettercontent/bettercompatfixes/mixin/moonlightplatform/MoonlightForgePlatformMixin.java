package com.bettercontent.bettercompatfixes.mixin.moonlightplatform;

import com.bettercontent.bettercompatfixes.compat.moonlightplatform.NativeForgeApiPlatform;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** This ONE presence test means unsupported provider; never changes global ModList identity. */
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.moonlight.forge.MoonlightForge", remap = false)
public abstract class MoonlightForgePlatformMixin {
    @Redirect(method = "lambda$new$1()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/fml/ModList;isLoaded(Ljava/lang/String;)Z"),
            require = 1, allow = 1, remap = false)
    private static boolean bettercontent$unsupportedFabricApiProvider(ModList mods, String id) {
        return NativeForgeApiPlatform.hasUnsupportedFabricApiProvider(mods, id);
    }
}
