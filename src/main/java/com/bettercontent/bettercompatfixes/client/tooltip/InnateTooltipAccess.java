package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.item.Style;

/** Implemented on both native Epic Fight and TCon's separate capability class. */
public interface InnateTooltipAccess {
    Map<Style, Function<ItemStack, Skill>> bettercontent$tooltipInnates();
}
