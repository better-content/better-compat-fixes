package com.bettercontent.bettercompatfixes.nativeforgeapifixture.mixin;

import com.bettercontent.bettercompatfixes.nativeforgeapifixture.ForgeApiNativeWitness;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicResourcePack;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DynamicResourcePack.class, remap = false)
public abstract class MoonlightGeneratedBytesWitnessMixin {
    @Inject(method = "addBytes(Lnet/minecraft/resources/ResourceLocation;[B)V", at = @At("RETURN"), require = 1, remap = false)
    private void betterContentNativeForgeApi$committed(ResourceLocation location, byte[] bytes, CallbackInfo ci) {
        ForgeApiNativeWitness.generatedBytesCommitted((DynamicResourcePack) (Object) this, location, bytes);
    }
}
