package com.bettercontent.bettercompatfixes.mixin.tconstruct;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps shift-click and direct placement out of the hidden physical pattern slot. */
@Mixin(targets = "slimeknights.tconstruct.tables.menu.PartBuilderContainerMenu$PatternSlot", remap = false)
public abstract class PartBuilderPatternSlotPlacementMixin {
    @Inject(
        method = {
            "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z",
            "m_5857_(Lnet/minecraft/world/item/ItemStack;)Z"
        },
        at = @At("HEAD"),
        cancellable = true,
        require = 1,
        remap = false
    )
    private void better_compat_fixes$rejectHiddenPatternInput(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
