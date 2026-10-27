package com.bettercontent.bettercompatfixes.compat.tconcombat;

import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/** Loaded only after both optional mods are present. No melee-class or material assumptions. */
public final class TconCombatFirstIntegration {
    private TconCombatFirstIntegration() {}

    public static boolean deniesBlockAttack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable tool)) return false;
        var definition = tool.getToolDefinition();
        if (definition == null || !definition.isDataLoaded()
                || definition.getData().getHooks().hasHook(ToolHooks.IS_EFFECTIVE)) return false;
        var capability = EpicFightCapabilities.getItemStackCapability(stack);
        // Resolve conditional variants against this stack, not the registry's item default.
        if (capability != null) capability = capability.getResult(stack);
        return capability != null && !capability.isEmpty();
    }
}
