package com.bettercontent.bettercompatfixes.mixin.tooltip;

import com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipOwner;
import org.violetmoon.zeta.client.event.play.ZGatherTooltipComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.violetmoon.quark.content.client.tooltip.AttributeTooltips", remap = false)
public abstract class QuarkTooltipMixin {
    @Inject(method = "makeTooltip", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bettercontent$skipOwnedAttributes(ZGatherTooltipComponents event, CallbackInfo ci) {
        if (ToolTooltipOwner.owns(event.getItemStack())) ci.cancel();
    }
}
