package com.bettercontent.bettercompatfixes.mixin.pneumaticcraft.tconhead;

import com.bettercontent.bettercompatfixes.compat.pneumaticcraft.TconJackhammerHeadPolicy;
import me.desht.pneumaticcraft.common.item.JackHammerItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "me.desht.pneumaticcraft.common.item.JackHammerItem$DrillBitHandler", remap = false)
abstract class JackHammerDrillBitHeadMixin {
    @Shadow @Final private ItemStack jackhammerStack;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void betterContent$loadTconHead(final ItemStack stack, final CallbackInfo ci) {
        if (!((Object) this instanceof JackHammerItem.DrillBitHandler handler)) return;
        if (!stack.hasTag() || !stack.getTag().contains(TconJackhammerHeadPolicy.JACKHAMMER_HEAD_TAG, 10)) return;

        final ItemStack head = TconJackhammerHeadPolicy.readInstalledHead(stack);
        if (TconJackhammerHeadPolicy.isSupportedHead(head)) {
            handler.setStackInSlot(0, head.copy());
        } else {
            // Corrupt or no-longer-supported payloads fail closed; don't silently
            // turn the mirrored enum into a free native replacement bit.
            handler.setStackInSlot(0, ItemStack.EMPTY);
            stack.getOrCreateTag().remove(TconJackhammerHeadPolicy.JACKHAMMER_HEAD_TAG);
        }
    }

    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterContent$validateTconHead(final int slot, final ItemStack candidate,
                                                final CallbackInfoReturnable<Boolean> cir) {
        if (TconJackhammerHeadPolicy.isPickHeadIdentity(candidate)) {
            cir.setReturnValue(TconJackhammerHeadPolicy.isSupportedHead(candidate));
        }
    }

    @Inject(method = "save", at = @At("RETURN"), remap = false)
    private void betterContent$persistTconHead(final CallbackInfo ci) {
        if (!((Object) this instanceof JackHammerItem.DrillBitHandler handler)) return;
        TconJackhammerHeadPolicy.writeInstalledHead(jackhammerStack, handler.getStackInSlot(0));
    }
}
