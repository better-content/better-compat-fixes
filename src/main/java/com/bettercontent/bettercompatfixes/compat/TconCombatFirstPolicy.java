package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/** Optional-mod-safe entry point. Eligibility is deliberately not cached across swaps/reloads. */
public final class TconCombatFirstPolicy {
    private TconCombatFirstPolicy() {}

    public static boolean deniesBlockAttack(Player player) {
        if (player == null || player.getMainHandItem().isEmpty()) return false;
        var mods = ModList.get();
        return mods != null && mods.isLoaded("tconstruct") && mods.isLoaded("epicfight")
                && com.bettercontent.bettercompatfixes.compat.tconcombat.TconCombatFirstIntegration
                        .deniesBlockAttack(player.getMainHandItem());
    }
}
