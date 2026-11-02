package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import static com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipRenderer.*;

/** Pinned, narrow optional adapters. Never calls either mod's input-sensitive tooltip handler. */
final class ExternalToolTooltipSummary {
    private ExternalToolTooltipSummary() {}
    static void contribute(ItemStack detached, Set<String> capabilities, List<ToolTooltipEffect> effects, List<Component> state) {
        if (ModList.get().isLoaded("bloodmagic") && detached.getTagElement("anointment_holder") != null) {
            try {
                var registrar = Class.forName("wayoftime.bloodmagic.core.AnointmentRegistrar");
                var known = (Map<?, ?>)registrar.getField("ANOINTMENT_MAP").get(null);
                var entries = detached.getTagElement("anointment_holder").getList("anointments", Tag.TAG_COMPOUND);
                for (int i = 0; i < entries.size(); i++) {
                    var tag = entries.getCompound(i);
                    ResourceLocation id = ResourceLocation.tryParse(tag.getString("key"));
                    Object anointment = known.get(id);
                    if (anointment == null) continue;
                    boolean harvestOnly = (Boolean)anointment.getClass().getMethod("consumeOnHarvest").invoke(anointment)
                            && !(Boolean)anointment.getClass().getMethod("consumeOnAttack").invoke(anointment)
                            && !(Boolean)anointment.getClass().getMethod("consumeOnUseFinish").invoke(anointment);
                    if (harvestOnly && !capabilities.contains("mining")) continue;
                    String key = (String)anointment.getClass().getMethod("getTranslationKey").invoke(anointment);
                    Component label = Component.translatable(key).append(" ").append(Component.translatable("enchantment.level." + tag.getInt("level")));
                    int remaining = Math.max(0, tag.getInt("max_damage") - tag.getInt("damage"));
                    effects.add(new ToolTooltipEffect("anointment:" + id, Set.of(), ToolTooltipEffect.Category.CONDITIONAL,
                            row(label, fact("anointment_uses", "Uses remaining", amount(remaining))), false));
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                ToolTooltipCatalogue.warnOnce("Blood Magic anointment adapter", e);
                state.add(fact("anointment_unavailable", "Anointment information", "unavailable"));
            }
        }
        if (ModList.get().isLoaded("ars_nouveau")) {
            var enchantment = BuiltInRegistries.ENCHANTMENT.getOptional(new ResourceLocation("ars_nouveau", "reactive"));
            if (enchantment.isPresent() && detached.getEnchantmentLevel(enchantment.get()) > 0) {
                try {
                    var type = Class.forName("com.hollingsworth.arsnouveau.common.spell.casters.ReactiveCaster");
                    Object caster = type.getConstructor(ItemStack.class).newInstance(detached);
                    Object spell = type.getMethod("getSpell").invoke(caster);
                    if ((Boolean)spell.getClass().getMethod("isValid").invoke(spell)) {
                        String description = (String)spell.getClass().getMethod("getDisplayString").invoke(spell);
                        effects.add(new ToolTooltipEffect("ars_nouveau:reactive", Set.of(), ToolTooltipEffect.Category.CONDITIONAL,
                                Component.translatableWithFallback("tooltip.better_compat_fixes.reactive",
                                        "Reactive: chance to cast %s on use or hurt; requires mana", description), false));
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    ToolTooltipCatalogue.warnOnce("Reactive spell adapter", e);
                    state.add(fact("reactive_unavailable", "Reactive spell information", "unavailable"));
                }
            }
        }
    }
}
