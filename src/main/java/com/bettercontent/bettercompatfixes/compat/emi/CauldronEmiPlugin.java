package com.bettercontent.bettercompatfixes.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.minecraftforge.fml.ModList;

/** Disabled for the playtest: exhaustive cauldron discovery stalls EMI and races client disconnect.
 * Keep the optional integration code dormant until it can be bounded safely.
 */
public final class CauldronEmiPlugin implements EmiPlugin {
    @Override public void register(EmiRegistry registry) {
        if (ModList.get().isLoaded("amendments")) AmendmentsCauldronDisplays.register(registry);
    }
}
