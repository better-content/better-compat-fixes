package com.bettercontent.bettercompatfixes.mixin.backpackequipment;

import com.bettercontent.bettercompatfixes.compat.BackpackEquipmentPolicy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dispenser selection is separate from Forge armor-slot admission. Commands are not intercepted. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = {"canTakeItem(Lnet/minecraft/world/item/ItemStack;)Z", "m_7066_(Lnet/minecraft/world/item/ItemStack;)Z"},
            at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private void betterContent$denyDispenserBackpack(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (BackpackEquipmentPolicy.backpack(stack)) callback.setReturnValue(false);
    }
}
