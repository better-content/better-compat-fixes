package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.Set;
import net.minecraft.network.chat.Component;

public record ToolTooltipEffect(String semanticId, Set<String> requiredCapabilities, Category category,
                                Component text, boolean representedInStats) {
    public enum Category { RESTRICTION, CONDITIONAL, BEHAVIOR, INTERNAL, COSMETIC }
    public ToolTooltipEffect {
        requiredCapabilities = Set.copyOf(requiredCapabilities);
        text = text.copy();
    }
    @Override public Component text() { return text.copy(); }
    public boolean visible(Set<String> capabilities) {
        return !representedInStats && category != Category.INTERNAL && category != Category.COSMETIC
                && capabilities.containsAll(requiredCapabilities);
    }
}
