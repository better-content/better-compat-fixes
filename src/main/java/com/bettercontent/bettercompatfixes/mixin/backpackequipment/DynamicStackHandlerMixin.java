package com.bettercontent.bettercompatfixes.mixin.backpackequipment;

import com.bettercontent.bettercompatfixes.compat.BackpackEquipmentPolicy;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.common.inventory.DynamicStackHandler;
import java.util.function.Function;

/** Final admission after ALL CurioEquipEvent listeners, predicates and canEquip callbacks. */
@Mixin(value = DynamicStackHandler.class, remap = false)
public abstract class DynamicStackHandlerMixin {
    @Shadow protected Function<Integer, SlotContext> ctxBuilder;

    @Inject(method = "isItemValid(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"),
            cancellable = true, require = 1)
    private void betterContent$exclusiveBackpack(int index, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        SlotContext context = ctxBuilder.apply(index);
        if (BackpackEquipmentPolicy.applies(context, stack)) {
            callback.setReturnValue(BackpackEquipmentPolicy.allowed(context, stack));
        }
    }
}
