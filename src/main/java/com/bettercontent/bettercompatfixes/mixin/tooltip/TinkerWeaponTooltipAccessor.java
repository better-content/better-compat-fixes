package com.bettercontent.bettercompatfixes.mixin.tooltip;

import com.bettercontent.bettercompatfixes.client.tooltip.InnateTooltipAccess;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;

@Pseudo
@Mixin(targets = "com.minhhjjj.epicfighttinkercompat.tool.capabilities.TCWeaponCapability", remap = false)
public interface TinkerWeaponTooltipAccessor extends InnateTooltipAccess {
    @Accessor("innateSkill")
    Map<Style, Function<ItemStack, Skill>> bettercontent$tooltipInnates();
    @Invoker("getWeapon")
    CapabilityItem bettercontent$tooltipDelegatedWeapon();
}
