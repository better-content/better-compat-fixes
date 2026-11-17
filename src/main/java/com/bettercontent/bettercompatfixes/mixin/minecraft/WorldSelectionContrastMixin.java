package com.bettercontent.bettercompatfixes.mixin.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native world-entry text on the pack's pale FancyMenu background, not a global text recolor. */
@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldSelectionContrastMixin {
    @Shadow @Final private LevelSummary summary;

    private static boolean betterCompat$paleWorldList() {
        return ModList.get().isLoaded("fancymenu");
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void betterCompat$rowBackground(GuiGraphics graphics, int index, int y, int x,
            int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick,
            CallbackInfo ci) {
        if (!betterCompat$paleWorldList()) return;
        var screen = Minecraft.getInstance().screen;
        boolean selected = screen != null && screen.children().stream()
                .filter(WorldSelectionList.class::isInstance).map(WorldSelectionList.class::cast)
                .anyMatch(list -> list.getSelected() == (Object) this);
        int color = summary.isDisabled() ? 0xFFE0D8C4
                : selected ? 0xFFD9C79F : hovered ? 0xFFF9EFD7 : 0xFFEDE0C1;
        // Keep the native outside selection border, icon, warnings, actions and hit regions.
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, color);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"), index = 4)
    private int betterCompat$plainText(int original) {
        if (!betterCompat$paleWorldList()) return original;
        return summary.isDisabled() ? 0x626653 : original == 0xFFFFFF ? 0x254637 : 0x4B5745;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"), index = 4)
    private int betterCompat$detailsText(int original) {
        // Explicit warning colors attached to Component styles still take precedence.
        return betterCompat$paleWorldList() ? summary.isDisabled() ? 0x626653 : 0x4B5745 : original;
    }
}
