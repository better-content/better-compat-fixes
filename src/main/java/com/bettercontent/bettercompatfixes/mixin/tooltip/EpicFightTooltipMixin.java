package com.bettercontent.bettercompatfixes.mixin.tooltip;

import com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipOwner;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "yesman.epicfight.client.events.engine.RenderEngine$Events", remap = false)
public abstract class EpicFightTooltipMixin {
    @Inject(method = "itemTooltip", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bettercontent$ordinaryOwnedHover(ItemTooltipEvent event, CallbackInfo ci) {
        if (ToolTooltipOwner.owns(event.getItemStack())) ci.cancel();
    }
}
