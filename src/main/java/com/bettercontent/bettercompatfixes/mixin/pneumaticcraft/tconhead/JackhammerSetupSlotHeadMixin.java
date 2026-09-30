package com.bettercontent.bettercompatfixes.mixin.pneumaticcraft.tconhead;

import com.bettercontent.bettercompatfixes.compat.pneumaticcraft.TconJackhammerHeadPolicy;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "me.desht.pneumaticcraft.common.inventory.JackhammerSetupMenu$SlotDrillBit", remap = false)
abstract class JackhammerSetupSlotHeadMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterContent$allowAuthenticPickHead(final ItemStack candidate,
                                                       final CallbackInfoReturnable<Boolean> cir) {
        if (TconJackhammerHeadPolicy.isPickHeadIdentity(candidate)) {
            cir.setReturnValue(TconJackhammerHeadPolicy.isSupportedHead(candidate));
        }
    }
}
