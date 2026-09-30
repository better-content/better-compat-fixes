package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.block.rooty.SoilHelper;
import com.ferreusveritas.dynamictrees.block.rooty.SoilProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public final class DynamicTreesUnearthedSoils {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String[] REGOLITH_SOILS = {
            "beige_limestone_grassy_regolith",
            "beige_limestone_regolith",
            "conglomerate_grassy_regolith",
            "conglomerate_regolith",
            "dolomite_grassy_regolith",
            "dolomite_regolith",
            "gabbro_grassy_regolith",
            "gabbro_regolith",
            "granodiorite_grassy_regolith",
            "granodiorite_regolith",
            "grey_limestone_grassy_regolith",
            "grey_limestone_regolith",
            "kimberlite_grassy_regolith",
            "kimberlite_regolith",
            "limestone_grassy_regolith",
            "limestone_regolith",
            "mudstone_grassy_regolith",
            "mudstone_regolith",
            "overgrown_andesite",
            "overgrown_diorite",
            "overgrown_granite",
            "phyllite_grassy_regolith",
            "phyllite_regolith",
            "quartzite_grassy_regolith",
            "quartzite_regolith",
            "rhyolite_grassy_regolith",
            "rhyolite_regolith",
            "sandstone_grassy_regolith",
            "sandstone_regolith",
            "siltstone_grassy_regolith",
            "siltstone_regolith",
            "slate_grassy_regolith",
            "slate_regolith",
            "stone_grassy_regolith",
            "stone_regolith",
            "white_granite_grassy_regolith",
            "white_granite_regolith"
    };

    private DynamicTreesUnearthedSoils() {
    }

    @SubscribeEvent
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        if (!BcFixesConfig.dynamicTreesUnearthedRegolithSoils()) {
            return;
        }
        event.enqueueWork(DynamicTreesUnearthedSoils::registerUnearthedSoils);
    }

    private static void registerUnearthedSoils() {
        final RootyBlock rootyDirt = SoilHelper.getProperties(Blocks.DIRT).getBlock().orElse(null);
        if (rootyDirt == null) {
            LOGGER.error("Could not register Unearthed regolith blocks as Dynamic Trees soils: rooty dirt is unavailable");
            return;
        }
        final Integer dirtLikeFlags = SoilHelper.getSoilFlags(SoilHelper.DIRT_LIKE);

        int registered = 0;
        for (String path : REGOLITH_SOILS) {
            ResourceLocation blockId = new ResourceLocation("unearthed", path);
            Block block = ForgeRegistries.BLOCKS.getValue(blockId);
            if (block == null) {
                LOGGER.warn("Skipping missing Unearthed soil block {}", blockId);
                continue;
            }
            ResourceLocation soilId = new ResourceLocation("better_compat_fixes", "unearthed_" + path);
            SoilProperties unearthedProperties = new SoilProperties(block, soilId);
            unearthedProperties.setSoilFlags(dirtLikeFlags);
            unearthedProperties.setBlock(rootyDirt);
            SoilHelper.addSoilPropertiesToMap(unearthedProperties);
            registered++;
        }
        LOGGER.info("Registered {} Unearthed regolith blocks as Dynamic Trees dirt-like soil aliases", registered);
    }
}
