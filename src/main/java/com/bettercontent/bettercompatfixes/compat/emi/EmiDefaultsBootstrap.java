package com.bettercontent.bettercompatfixes.compat.emi;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/** Runs before EMI so a new client profile receives the pack's initial, user-editable favorites. */
public final class EmiDefaultsBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();

    private EmiDefaultsBootstrap() {
    }

    public static void seedIfApplicable() {
        if (FMLEnvironment.dist != Dist.CLIENT
                || !ModList.get().isLoaded("emi")
                || !ModList.get().isLoaded("tconstruct")) {
            return;
        }

        final EmiDefaultFavorites.Result result = EmiDefaultFavorites.seed(FMLPaths.GAMEDIR.get());
        if (result.status() == EmiDefaultFavorites.Status.CREATED) {
            LOGGER.info("Seeded editable EMI favorites for the TConstruct Part Builder and Tinker Station");
        } else if (result.status() == EmiDefaultFavorites.Status.FAILED) {
            LOGGER.warn("Could not seed default EMI favorites; startup will continue without changing EMI data",
                    result.failure());
        }
    }
}
