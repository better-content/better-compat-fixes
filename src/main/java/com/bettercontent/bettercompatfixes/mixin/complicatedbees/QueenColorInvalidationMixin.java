package com.bettercontent.bettercompatfixes.mixin.complicatedbees;

import com.accbdd.complicated_bees.block.entity.ApiaryBlockEntity;
import com.bettercontent.bettercompatfixes.compat.bees.ColonyActivity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pinned native bee handler: observe existing slot index, never re-read its inventory. */
@Mixin(targets = "com.accbdd.complicated_bees.block.entity.ApiaryBlockEntity$6", remap = false)
public abstract class QueenColorInvalidationMixin {
    @Shadow @Final ApiaryBlockEntity this$0;
    @Inject(method = "onContentsChanged(I)V", at = @At("HEAD"), require = 1, allow = 1)
    private void betterCompat$queenChanged(int slot, CallbackInfo callback) {
        if (slot == 0 && this$0 instanceof ColonyActivity state) state.betterCompat$clearColonyColor();
    }
}
