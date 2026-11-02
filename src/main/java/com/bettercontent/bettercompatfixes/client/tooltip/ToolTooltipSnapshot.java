package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;

/** A detached presentation value: no stack, world, player, input state or mutable registry cache. */
public record ToolTooltipSnapshot(Component name, Set<String> capabilities, List<Component> state,
                                  List<Component> performance, List<Component> actions,
                                  List<ToolTooltipEffect> effects, List<Component> lore) {
    public ToolTooltipSnapshot {
        name = name.copy();
        capabilities = Set.copyOf(capabilities);
        state = copy(state); performance = copy(performance); actions = copy(actions); lore = copy(lore);
        effects = List.copyOf(effects);
    }
    @Override public Component name() { return name.copy(); }
    @Override public List<Component> state() { return copy(state); }
    @Override public List<Component> performance() { return copy(performance); }
    @Override public List<Component> actions() { return copy(actions); }
    @Override public List<Component> lore() { return copy(lore); }
    private static List<Component> copy(List<Component> rows) {
        return rows.stream().map(c -> (Component)c.copy()).toList();
    }
}
