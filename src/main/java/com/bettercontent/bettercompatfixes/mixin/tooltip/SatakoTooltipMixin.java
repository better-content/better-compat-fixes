package com.bettercontent.bettercompatfixes.mixin.tooltip;

import com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipOwner;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.buildtool.satako.clientside.TooltipHandler", remap = false)
public abstract class SatakoTooltipMixin {
    @Shadow public static ItemStack targetStack;
    @Shadow static List<Component> originalTooltip;
    @Shadow static List<MutableComponent> properties;

    @Unique private static void bettercontent$clearPanel() {
        targetStack = null;
        originalTooltip = List.of();
        properties = List.of();
    }
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bettercontent$skipOwnedPanel(ItemStack stack, GuiGraphics graphics, CallbackInfo ci) {
        if (ToolTooltipOwner.owns(stack)) {
            bettercontent$clearPanel();
            ci.cancel();
        }
    }
    @Inject(method = "renderHoveringTooltip", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bettercontent$discardCachedOwnedPanel(GuiGraphics graphics, CallbackInfo ci) {
        if (ToolTooltipOwner.owns(targetStack)) {
            bettercontent$clearPanel();
            ci.cancel();
        }
    }
}
