package com.bettercontent.bettercompatfixes.mixin.tooltip;

import java.util.Map;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;

@Mixin(value = CapabilityItem.class, remap = false)
public interface CapabilityItemTooltipAccessor {
    @Accessor("attributeMap")
    Map<Style, Map<Attribute, AttributeModifier>> bettercontent$tooltipAttributes();
}
