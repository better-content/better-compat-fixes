package com.bettercontent.bettercompatfixes.client.tooltip;

import com.hollingsworth.arsnouveau.common.spell.casters.ReactiveCaster;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Loaded only when Ars Nouveau is present and the detached tool carries Reactive. */
final class ArsReactiveTooltipAdapter {
    private ArsReactiveTooltipAdapter() {}

    static ToolTooltipEffect describe(String description) {
        return new ToolTooltipEffect("ars_nouveau:reactive", Set.of(), ToolTooltipEffect.Category.CONDITIONAL,
                Component.translatableWithFallback("tooltip.better_compat_fixes.reactive",
                        "Reactive: chance to cast %s on use or hurt; requires mana", description), false);
    }

    static void contribute(ItemStack detached, List<ToolTooltipEffect> effects) {
        var spell = new ReactiveCaster(detached).getSpell();
        if (spell.isValid()) effects.add(describe(spell.getDisplayString()));
    }
}
