package com.bettercontent.bettercompatfixes.client.tooltip;

import com.bettercontent.bettercompatfixes.mixin.tooltip.CapabilityItemTooltipAccessor;
import com.bettercontent.bettercompatfixes.mixin.tooltip.TinkerWeaponTooltipAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import static com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipRenderer.*;

/** Uses a detached stack and raw item profile maps; never invokes a player style getter or skill tooltip. */
final class EpicFightTooltipSummary {
    private EpicFightTooltipSummary() {}
    static void contribute(ItemStack detached, List<Component> stats, List<Component> actions, List<ToolTooltipEffect> effects) {
        var base = EpicFightCapabilities.getItemStackCapability(detached);
        if (base == null) return;
        var capability = base.getResult(detached);
        if (capability == null || capability.isEmpty()) return;
        if (capability instanceof CapabilityItemTooltipAccessor accessor) {
            var profiles = accessor.bettercontent$tooltipAttributes();
            var attributes = new LinkedHashMap<Attribute, AttributeModifier>();
            attributes.putAll(profiles.getOrDefault(Styles.COMMON, Map.of()));
            Style defaultStyle = capability.canBePlacedOffhand() ? Styles.ONE_HAND : Styles.TWO_HAND;
            attributes.putAll(profiles.getOrDefault(defaultStyle, Map.of()));
            var facts = new ArrayList<Component>();
            attributes.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getDescriptionId())).forEach(e -> {
                var key = e.getKey();
                var value = e.getValue();
                double total = key.sanitizeValue(IntrinsicAttributeMath.apply(key.getDefaultValue(), List.of(
                        new IntrinsicAttributeMath.Contribution(value.getId(), value.getAmount(), value.getOperation().toValue()))));
                // The resolved capability already incorporates EpicFightPartStatModifier. Do not read and add it again.
                facts.add(Component.translatableWithFallback("tooltip.better_compat_fixes.attribute", "%s %s",
                        Component.translatable(key.getDescriptionId()), number(total)));
            });
            if (!facts.isEmpty()) stats.add(row(facts.toArray(Component[]::new)));
            for (var entry : profiles.entrySet()) {
                if (entry.getKey() == Styles.COMMON || entry.getKey() == defaultStyle) continue;
                if (!entry.getValue().isEmpty() && !entry.getValue().equals(attributes)) {
                    var alternatives = new ArrayList<Component>();
                    entry.getValue().entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getDescriptionId())).forEach(e -> {
                        double amount = e.getValue().getAmount();
                        boolean percent = e.getValue().getOperation() != AttributeModifier.Operation.ADDITION;
                        alternatives.add(Component.translatableWithFallback("tooltip.better_compat_fixes.attribute", "%s %s",
                                Component.translatable(e.getKey().getDescriptionId()), (amount >= 0 ? "+" : "") + number(amount * (percent ? 100 : 1)) + (percent ? "%" : "")));
                    });
                    effects.add(new ToolTooltipEffect("epicfight:style_stats:" + entry.getKey(), Set.of("combat"),
                            ToolTooltipEffect.Category.CONDITIONAL, Component.translatableWithFallback("tooltip.better_compat_fixes.style_attributes",
                            "%s style: %s", styleLabel(entry.getKey()), row(alternatives.toArray(Component[]::new))), false));
                }
            }
        }
        var actionCapability = capability;
        if (capability instanceof TinkerWeaponTooltipAccessor tinker) {
            var delegated = tinker.bettercontent$tooltipDelegatedWeapon();
            if (delegated != null) actionCapability = delegated;
        }
        if (actionCapability instanceof InnateTooltipAccess accessor) {
            var skills = new java.util.TreeMap<String, List<Style>>();
            accessor.bettercontent$tooltipInnates().entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).forEach(e -> {
                var skill = e.getValue().apply(detached);
                if (skill != null) skills.computeIfAbsent(skill.getRegistryName().toString(), id -> new ArrayList<>()).add(e.getKey());
            });
            skills.forEach((id, styles) -> {
                var descriptions = ToolTooltipCatalogue.current().actions().get("skill:" + id);
                Component description;
                if (descriptions == null) {
                    ToolTooltipCatalogue.warnOnce("innate skill " + id, null);
                    description = Component.translatable("skill." + id.replace(':', '.'));
                } else description = row(descriptions.stream().map(f -> ToolTooltipSummaryProvider.fragmentText(f, 1)).toArray(Component[]::new));
                if (skills.size() > 1) {
                    var styleNames = Component.empty();
                    for (var style : styles) {
                        if (!styleNames.getSiblings().isEmpty()) styleNames.append(" / ");
                        styleNames.append(styleLabel(style));
                    }
                    description = Component.translatableWithFallback("tooltip.better_compat_fixes.innate_style", "%s style innate: %s", styleNames, description);
                } else description = Component.translatableWithFallback("tooltip.better_compat_fixes.innate", "Innate: %s", description);
                actions.add(description);
            });
        }
    }
    private static Component styleLabel(Style style) {
        String id = style.toString().toLowerCase(java.util.Locale.ROOT);
        return Component.translatableWithFallback("tooltip.better_compat_fixes.style." + id, id.replace('_', ' '));
    }
}
