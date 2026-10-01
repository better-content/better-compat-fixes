package com.bettercontent.bettercompatfixes.mixin.curios;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

/** Blocks direct handler growth as well as slot modifiers routed through the wrapper. */
@Mixin(value = CurioStacksHandler.class, remap = false)
public abstract class CurioStacksHandlerMixin {
    @Shadow @Final private ICuriosItemHandler itemHandler;

    @Inject(method = "grow", at = @At("HEAD"), cancellable = true)
    private void betterContent$fixedLegacyGrowth(int amount, CallbackInfo callback) {
        if (itemHandler.getWearer() instanceof Player) callback.cancel();
    }

    @Inject(method = "addTransientModifier", at = @At("HEAD"), cancellable = true)
    private void betterContent$fixedTransient(AttributeModifier modifier, CallbackInfo callback) {
        if (itemHandler.getWearer() instanceof Player) callback.cancel();
    }

    @Inject(method = "addPermanentModifier", at = @At("HEAD"), cancellable = true)
    private void betterContent$fixedPermanent(AttributeModifier modifier, CallbackInfo callback) {
        if (itemHandler.getWearer() instanceof Player) callback.cancel();
    }
}
