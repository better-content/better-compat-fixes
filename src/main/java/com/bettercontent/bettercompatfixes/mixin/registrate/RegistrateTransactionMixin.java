package com.bettercontent.bettercompatfixes.mixin.registrate;

import com.bettercontent.bettercompatfixes.compat.registrate.RegistrateTransaction;
import com.google.common.collect.Table;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;

/** Original calls/collections retained. Supported callbacks do not perform blocking/reentrant registry mutation in hash/equals. */
@Pseudo
@Mixin(targets = "com.tterrag.registrate.AbstractRegistrate", remap = false)
abstract class RegistrateTransactionMixin {
    @Unique private final RegistrateTransaction bc$registrateTransaction = new RegistrateTransaction();
    @Unique private static final String ACCEPT = "accept(Ljava/lang/String;Lnet/minecraft/resources/ResourceKey;Lcom/tterrag/registrate/builders/Builder;Lcom/tterrag/registrate/util/nullness/NonNullSupplier;Lcom/tterrag/registrate/util/nullness/NonNullFunction;)Lcom/tterrag/registrate/util/entry/RegistryEntry;";
    @Unique private static final String ADD = "addRegisterCallback(Ljava/lang/String;Lnet/minecraft/resources/ResourceKey;Lcom/tterrag/registrate/util/nullness/NonNullConsumer;)Lcom/tterrag/registrate/AbstractRegistrate;";

    @WrapMethod(method = ACCEPT, require = 1, allow = 1, remap = false)
    private RegistryEntry<?> bc$acceptScope(String name, ResourceKey<?> type, Builder<?, ?, ?, ?> builder,
            NonNullSupplier<?> creator, NonNullFunction<?, ?> factory, Operation<RegistryEntry<?>> original) {
        int baseline = bc$registrateTransaction.baseline();
        try { return original.call(name, type, builder, creator, factory); }
        finally { bc$registrateTransaction.restore(baseline); }
    }
    @Inject(method = ACCEPT, at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Multimap;removeAll(Ljava/lang/Object;)Ljava/util/Collection;"), require = 1, allow = 1, remap = false)
    private void bc$beginAccept(CallbackInfoReturnable<?> callback) { bc$registrateTransaction.begin(); }
    @Inject(method = ACCEPT, at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Table;put(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER), require = 1, allow = 1, remap = false)
    private void bc$endAccept(CallbackInfoReturnable<?> callback) { bc$registrateTransaction.end(); }

    @WrapMethod(method = ADD, require = 1, allow = 1, remap = false)
    private AbstractRegistrate<?> bc$addScope(String name, ResourceKey<?> type, NonNullConsumer<?> callback,
            Operation<AbstractRegistrate<?>> original) {
        int baseline = bc$registrateTransaction.baseline();
        try { return original.call(name, type, callback); }
        finally { bc$registrateTransaction.restore(baseline); }
    }
    @Inject(method = ADD, at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/AbstractRegistrate;getRegistrationUnchecked(Ljava/lang/String;Lnet/minecraft/resources/ResourceKey;)Lcom/tterrag/registrate/AbstractRegistrate$Registration;"), require = 1, allow = 1, remap = false)
    private void bc$beginAdd(CallbackInfoReturnable<?> callback) { bc$registrateTransaction.begin(); }
    @Inject(method = ADD, at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/AbstractRegistrate;self()Lcom/tterrag/registrate/AbstractRegistrate;"), require = 1, allow = 1, remap = false)
    private void bc$endAdd(CallbackInfoReturnable<?> callback) { bc$registrateTransaction.end(); }

    @WrapOperation(method = "getRegistrationUnchecked(Ljava/lang/String;Lnet/minecraft/resources/ResourceKey;)Lcom/tterrag/registrate/AbstractRegistrate$Registration;",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Table;get(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), require = 1, allow = 1, remap = false)
    private Object bc$readTable(Table<?, ?, ?> table, Object row, Object column, Operation<Object> original) {
        bc$registrateTransaction.begin();
        try { return original.call(table, row, column); }
        finally { bc$registrateTransaction.end(); }
    }
    @WrapMethod(method = "getAll(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Collection;", require = 1, allow = 1, remap = false)
    private Collection<?> bc$readAll(ResourceKey<?> type, Operation<Collection<?>> original) {
        bc$registrateTransaction.begin();
        try { return original.call(type); }
        finally { bc$registrateTransaction.end(); }
    }
}
