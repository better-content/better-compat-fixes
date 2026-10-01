package com.bettercontent.bettercompatfixes.mixin.curios;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.common.capability.CurioInventoryCapability;

import java.util.UUID;

/** Slot capacity is a fixed twelve-slot choice for players. */
@Mixin(value = CurioInventoryCapability.CurioInventoryWrapper.class, remap = false)
public abstract class CurioInventoryWrapperMixin {
    @Shadow @Final private LivingEntity wearer;

    @Inject(method = "growSlotType", at = @At("HEAD"), cancellable = true)
    private void betterContent$fixedGrowth(String slot, int amount, CallbackInfo callback) {
        if (wearer instanceof Player) callback.cancel();
    }

    @Inject(method = "addTransientSlotModifiers", at = @At("HEAD"), cancellable = true)
    private void betterContent$fixedTransient(Multimap<String, AttributeModifier> modifiers, CallbackInfo callback) {
        if (wearer instanceof Player) callback.cancel();
    }

    @Inject(method = "addPermanentSlotModifiers", at = @At("HEAD"), cancellable = true)
    private void betterContent$fixedPermanent(Multimap<String, AttributeModifier> modifiers, CallbackInfo callback) {
        if (wearer instanceof Player) callback.cancel();
    }

    @Inject(method = "addTransientSlotModifier", at = @At("HEAD"), cancellable = true)
    private void betterContent$fixedSingleTransient(String slot, UUID id, String name, double amount,
            AttributeModifier.Operation operation, CallbackInfo callback) {
        if (wearer instanceof Player) callback.cancel();
    }

    @Inject(method = "addPermanentSlotModifier", at = @At("HEAD"), cancellable = true)
    private void betterContent$fixedSinglePermanent(String slot, UUID id, String name, double amount,
            AttributeModifier.Operation operation, CallbackInfo callback) {
        if (wearer instanceof Player) callback.cancel();
    }
}
