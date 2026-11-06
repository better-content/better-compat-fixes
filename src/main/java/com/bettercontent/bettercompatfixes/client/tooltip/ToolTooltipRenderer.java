package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

/** Semantic rows only. Vanilla/Forge does pixel wrapping; eight is never a cutoff. */
public final class ToolTooltipRenderer {
    public static final int TARGET_WIDTH = 300;
    private ToolTooltipRenderer() {}

    public static List<Component> render(ToolTooltipSnapshot snapshot) {
        var rows = new ArrayList<Component>();
        rows.add(snapshot.name());
        rows.addAll(snapshot.state());
        rows.addAll(snapshot.performance());
        snapshot.actions().forEach(action -> rows.add(accent(action, ChatFormatting.AQUA)));
        var seen = new HashSet<String>();
        snapshot.effects().stream().filter(e -> e.visible(snapshot.capabilities()))
                .sorted(Comparator.comparing(ToolTooltipEffect::category).thenComparing(ToolTooltipEffect::semanticId))
                .filter(e -> seen.add(e.semanticId())).forEach(e -> rows.add(accent(e.text(), switch (e.category()) {
                    case RESTRICTION -> ChatFormatting.RED;
                    case CONDITIONAL -> ChatFormatting.GOLD;
                    default -> ChatFormatting.GREEN;
                })));
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
    /** Apply a category accent only where the source has not supplied its own style. */
    public static Component accent(Component source, ChatFormatting color) {
        var copy = source.copy();
        var existing = copy.getStyle().getColor();
        if (existing == null || existing.equals(TextColor.fromLegacyFormat(ChatFormatting.GRAY))) copy.withStyle(color);
        return copy;
    }
    public static Component fact(String key, String fallback, Object value) {
        Object display = value instanceof String text
                ? Component.literal(text).withStyle(ChatFormatting.AQUA) : value;
        return Component.translatableWithFallback("tooltip.better_compat_fixes." + key, fallback + " %s", display)
                .withStyle(ChatFormatting.GRAY);
    }
    public static String number(double value) {
        if (!Double.isFinite(value)) return "?";
        if (Math.abs(value) < 0.0005) return "0";
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.?0+$", "");
    }
    public static String amount(int value) { return String.format(Locale.ROOT, "%,d", value); }
}
