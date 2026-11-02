package com.bettercontent.bettercompatfixes.mixin.tooltip;

import com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipOwner;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;

/** Below EMI's priority 500: outer wrap includes EMI's return injection, not just vanilla's body. */
@Mixin(value = ItemStack.class, priority = 100)
public abstract class ItemStackTooltipMixin {
    @WrapMethod(method = "getTooltipLines")
    private List<Component> bettercontent$ownedTooltip(Player player, TooltipFlag flag, Operation<List<Component>> original) {
        ItemStack stack = (ItemStack)(Object)this;
        // Do not run modal upstream construction for owned stacks: it can initialize inventory NBT.
        return ToolTooltipOwner.owns(stack) ? ToolTooltipOwner.render(stack) : original.call(player, flag);
    }
}
