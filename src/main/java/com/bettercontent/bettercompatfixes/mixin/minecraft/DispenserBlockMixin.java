package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Collections;
import java.util.Map;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {
    // Modify the initialized map before its ORIGINAL final-field assignment in native <clinit>.
    // Wrapping PUTSTATIC would move the write into an illegal final-field bridge method in
    // consumers without this development workspace's field access transformations.
    @ModifyExpressionValue(method = "<clinit>", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/Util;make(Ljava/lang/Object;Ljava/util/function/Consumer;)Ljava/lang/Object;"),
            @At(value = "INVOKE", target = "Lnet/minecraft/Util;m_137469_(Ljava/lang/Object;Ljava/util/function/Consumer;)Ljava/lang/Object;")
    }, remap = false, require = 1, allow = 1)
    private static Object better_compat_fixes$makeBehaviorRegistryThreadSafe(final Object initialized) {
        @SuppressWarnings("unchecked")
        var registry = (Map<Item, DispenseItemBehavior>) initialized;
        return Collections.synchronizedMap(registry);
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
