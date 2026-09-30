package com.bettercontent.bettercompatfixes.mixin.trashslot;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.blay09.mods.trashslot.TrashSlotSaveState", remap = false)
public abstract class TrashSlotSaveStateMixin {
    @ModifyExpressionValue(
            method = "lambda$getSettings$0",
            at = @At(value = "INVOKE", target = "Lnet/blay09/mods/trashslot/api/IGuiContainerLayout;isEnabledByDefault()Z"))
    private static boolean bettercontent$enableAllSupportedScreensByDefault(boolean ignored) {
        return true;
    }
}
