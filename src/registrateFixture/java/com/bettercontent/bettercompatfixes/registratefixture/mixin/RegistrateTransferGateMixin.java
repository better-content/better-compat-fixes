package com.bettercontent.bettercompatfixes.registratefixture.mixin;

import com.bettercontent.bettercompatfixes.registratefixture.TransferGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.tterrag.registrate.AbstractRegistrate", remap = false)
abstract class RegistrateTransferGateMixin {
    @Inject(method = "accept(Ljava/lang/String;Lnet/minecraft/resources/ResourceKey;Lcom/tterrag/registrate/builders/Builder;Lcom/tterrag/registrate/util/nullness/NonNullSupplier;Lcom/tterrag/registrate/util/nullness/NonNullFunction;)Lcom/tterrag/registrate/util/entry/RegistryEntry;",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Multimap;removeAll(Ljava/lang/Object;)Ljava/util/Collection;", shift = At.Shift.AFTER), require = 1, allow = 1, remap = false)
    private void bc$originalTransferGap(CallbackInfoReturnable<?> callback) { TransferGate.afterRemove(this); }
}
