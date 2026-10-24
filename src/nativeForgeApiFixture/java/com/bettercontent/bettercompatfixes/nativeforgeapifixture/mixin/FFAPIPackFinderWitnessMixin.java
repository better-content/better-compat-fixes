package com.bettercontent.bettercompatfixes.nativeforgeapifixture.mixin;

import com.bettercontent.bettercompatfixes.nativeforgeapifixture.ForgeApiNativeWitness;
import net.minecraftforge.event.AddPackFindersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.fabricmc.fabric.impl.sinytra.SinytraFabric", remap = false)
public abstract class FFAPIPackFinderWitnessMixin {
    @Inject(method = "addPackFinder(Lnet/minecraftforge/event/AddPackFindersEvent;)V", at = @At("RETURN"), require = 1, remap = false)
    private void betterContentNativeForgeApi$completed(AddPackFindersEvent event, CallbackInfo ci) {
        ForgeApiNativeWitness.packFinderCompleted(event, true);
    }
}
