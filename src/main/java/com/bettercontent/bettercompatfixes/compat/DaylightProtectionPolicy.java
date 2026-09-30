package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;

public final class DaylightProtectionPolicy {
    private DaylightProtectionPolicy() {
    }

    public static boolean disablesSunBurnTick(final Mob mob) {
        return BcFixesConfig.mobsDisableSunBurnTick() && !(mob instanceof Phantom);
    }
}
