package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import static com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipRenderer.fact;

/** Pinned, narrow optional adapters. Never calls either mod's input-sensitive tooltip handler. */
final class ExternalToolTooltipSummary {
    private ExternalToolTooltipSummary() {}
    static void contribute(ItemStack detached, Set<String> capabilities, List<ToolTooltipEffect> effects, List<Component> state) {
        if (ModList.get().isLoaded("bloodmagic") && detached.getTagElement("anointment_holder") != null) {
            try {
                BloodMagicTooltipAdapter.contribute(detached, capabilities, effects);
            } catch (RuntimeException | LinkageError e) {
                ToolTooltipCatalogue.warnOnce("Blood Magic anointment adapter", e);
                state.add(fact("anointment_unavailable", "Anointment information", "unavailable"));
            }
        }
        if (ModList.get().isLoaded("ars_nouveau")) {
            var enchantment = BuiltInRegistries.ENCHANTMENT.getOptional(new ResourceLocation("ars_nouveau", "reactive"));
            if (enchantment.isPresent() && detached.getEnchantmentLevel(enchantment.get()) > 0) {
                try {
                    ArsReactiveTooltipAdapter.contribute(detached, effects);
                } catch (RuntimeException | LinkageError e) {
                    ToolTooltipCatalogue.warnOnce("Reactive spell adapter", e);
                    state.add(fact("reactive_unavailable", "Reactive spell information", "unavailable"));
                }
            }
        }
    }
}
