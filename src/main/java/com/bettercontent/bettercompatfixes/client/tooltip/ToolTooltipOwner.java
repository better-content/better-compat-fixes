package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Optional-safe entry point shared by the hover boundary and independent panel guards. */
public final class ToolTooltipOwner {
    private ToolTooltipOwner() {}
    public static boolean owns(ItemStack stack) {
        return stack != null && !stack.isEmpty() && supported("tconstruct", "3.11.2.166")
                && ToolTooltipSummaryProvider.isAssembled(stack);
    }
    static boolean supported(String mod, String version) {
        var mods = ModList.get();
        return mods != null && mods.getModContainerById(mod)
                .map(container -> version.equals(container.getModInfo().getVersion().toString())).orElse(false);
    }
    public static List<Component> render(ItemStack original) {
        try {
            return ToolTooltipRenderer.render(ToolTooltipSummaryProvider.snapshot(original.copy()));
        } catch (RuntimeException | LinkageError failure) {
            ToolTooltipCatalogue.warnOnce(original.getItem().getDescriptionId(), failure);
            return List.of(Component.translatable(original.getItem().getDescriptionId()),
                    Component.translatableWithFallback("tooltip.better_compat_fixes.unavailable", "Tool information unavailable")
                            .withStyle(ChatFormatting.GRAY));
        }
    }
}
