package com.bettercontent.bettercompatfixes.compat;

import dev.ghen.thirst.foundation.common.loot.ModLootModifiers;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ThirstLootModifierCompat {
    private static boolean registered;

    private ThirstLootModifierCompat() {
    }

    public static synchronized void register(final IEventBus modEventBus) {
        if (!registered) {
            ModLootModifiers.LOOT_MODIFIERS.register(modEventBus);
            registered = true;
        }
    }
}
