package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Semantic rows only. Vanilla/Forge does pixel wrapping; eight is never a cutoff. */
public final class ToolTooltipRenderer {
    public static final int TARGET_WIDTH = 300;
    private ToolTooltipRenderer() {}

    public static List<Component> render(ToolTooltipSnapshot snapshot) {
        var rows = new ArrayList<Component>();
        rows.add(snapshot.name());
        rows.addAll(snapshot.state());
        rows.addAll(snapshot.performance());
        rows.addAll(snapshot.actions());
        var seen = new HashSet<String>();
        snapshot.effects().stream().filter(e -> e.visible(snapshot.capabilities()))
                .sorted(Comparator.comparing(ToolTooltipEffect::category).thenComparing(ToolTooltipEffect::semanticId))
                .filter(e -> seen.add(e.semanticId())).forEach(e -> rows.add(e.text().copy().withStyle(
                        e.category() == ToolTooltipEffect.Category.RESTRICTION ? ChatFormatting.RED : ChatFormatting.GRAY)));
        rows.addAll(snapshot.lore());
        return rows;
    }

    public static Component row(Component... facts) {
        var row = Component.empty();
        for (var fact : facts) {
            if (!row.getSiblings().isEmpty()) row.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
            row.append(fact);
        }
        return row.withStyle(ChatFormatting.GRAY);
    }
    public static Component fact(String key, String fallback, Object value) {
        return Component.translatableWithFallback("tooltip.better_compat_fixes." + key, fallback + " %s", value);
    }
    public static String number(double value) {
        if (!Double.isFinite(value)) return "?";
        if (Math.abs(value) < 0.0005) return "0";
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.?0+$", "");
    }
    public static String amount(int value) { return String.format(Locale.ROOT, "%,d", value); }
}
