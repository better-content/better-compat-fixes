package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.List;
import java.util.Set;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.core.AnointmentRegistrar;
import static com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipRenderer.*;

/** Loaded only after Blood Magic is present and the detached stack has an anointment holder. */
final class BloodMagicTooltipAdapter {
    private BloodMagicTooltipAdapter() {}

    static boolean visible(boolean harvestOnly, Set<String> capabilities) {
        return !harvestOnly || capabilities.contains("mining");
    }

    static int remainingUses(int maximum, int damage) {
        return Math.max(0, maximum - damage);
    }

    static void contribute(ItemStack detached, Set<String> capabilities, List<ToolTooltipEffect> effects) {
        var entries = detached.getTagElement("anointment_holder").getList("anointments", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var tag = entries.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("key"));
            var anointment = AnointmentRegistrar.ANOINTMENT_MAP.get(id);
            if (anointment == null) continue;
            boolean harvestOnly = anointment.consumeOnHarvest()
                    && !anointment.consumeOnAttack() && !anointment.consumeOnUseFinish();
            if (!visible(harvestOnly, capabilities)) continue;
            Component label = Component.translatable(anointment.getTranslationKey()).append(" ")
                    .append(Component.translatable("enchantment.level." + tag.getInt("level")));
            int remaining = remainingUses(tag.getInt("max_damage"), tag.getInt("damage"));
            effects.add(new ToolTooltipEffect("anointment:" + id, Set.of(), ToolTooltipEffect.Category.CONDITIONAL,
                    row(label, fact("anointment_uses", "Uses remaining", amount(remaining))), false));
        }
    }
}
