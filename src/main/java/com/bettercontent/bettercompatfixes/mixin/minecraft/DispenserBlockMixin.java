package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Collections;
import java.util.Map;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.item.Item;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {
    // Wrap the one native assignment instead of shadowing a public field: public-field aliases
    // are illegal and a production-only SRG shadow makes the entire mixin fail in development.
    @WrapOperation(method = "<clinit>", at = {
            @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/DispenserBlock;DISPENSER_REGISTRY:Ljava/util/Map;", opcode = Opcodes.PUTSTATIC),
            @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/DispenserBlock;f_52661_:Ljava/util/Map;", opcode = Opcodes.PUTSTATIC)
    }, remap = false, require = 1, allow = 1)
    private static void better_compat_fixes$makeBehaviorRegistryThreadSafe(
            final Map<Item, DispenseItemBehavior> registry, final Operation<Void> original) {
        original.call(Collections.synchronizedMap(registry));
    }

    @WrapMethod(method = {"registerBehavior", "m_52672_"}, remap = false)
    private static void better_compat_fixes$serializeBehaviorRegistration(
            final ItemLike item,
            final DispenseItemBehavior behavior,
            final Operation<Void> original
    ) {
        synchronized (DispenserBlock.class) {
            original.call(item, behavior);
        }
    }
}
