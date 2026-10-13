package com.bettercontent.bettercompatfixes.mixin.backpackequipment;

import com.bettercontent.bettercompatfixes.compat.BackpackEquipmentPolicy;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

/** Native equip-from-use slot search also observes the exclusive placement rule. */
@Mixin(value = CuriosApi.class, remap = false)
public abstract class CuriosApiMixin {
    // Curios supplies its implementation with a cancellable HEAD injection. Wrap the entire
    // transformed method: a RETURN injector on the API stub misses that early callback return.
    @WrapMethod(method = "isStackValid(Ltop/theillusivec4/curios/api/SlotContext;Lnet/minecraft/world/item/ItemStack;)Z")
    private static boolean betterContent$exclusiveBackpack(SlotContext context, ItemStack stack,
                                                           Operation<Boolean> original) {
        boolean nativeResult = original.call(context, stack);
        return BackpackEquipmentPolicy.applies(context, stack)
                ? BackpackEquipmentPolicy.allowed(context, stack) : nativeResult;
    }
}
