package com.bettercontent.bettercompatfixes.nativeforgeapifixture.mixin;

import com.bettercontent.bettercompatfixes.nativeforgeapifixture.ForgeApiNativeWitness;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.fabricmc.fabric.impl.event.interaction.InteractionEventHooks", remap = false)
public abstract class FFAPIInteractionWitnessMixin {
    @Inject(method = "onRightClickBlock(Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock;)V",
            at = @At("HEAD"), require = 1, remap = false)
    private static void betterContentNativeForgeApi$enter(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) {
        ForgeApiNativeWitness.bridgeEnter(event);
    }

    @Inject(method = "onRightClickBlock(Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock;)V",
            at = @At("RETURN"), require = 1, remap = false)
    private static void betterContentNativeForgeApi$return(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) {
        ForgeApiNativeWitness.bridgeReturn(event);
    }
}
