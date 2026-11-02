package com.bettercontent.bettercompatfixes.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.minecraftforge.fml.ModList;

/** Keeps all optional Amendments/Moonlight classes behind the loaded-mod boundary. */
@EmiEntrypoint
public final class CauldronEmiPlugin implements EmiPlugin {
    @Override public void register(EmiRegistry registry) {
        if (ModList.get().isLoaded("amendments")) AmendmentsCauldronDisplays.register(registry);
    }
}
