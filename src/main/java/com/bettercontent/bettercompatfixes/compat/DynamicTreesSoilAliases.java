package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.block.rooty.SoilHelper;
import com.ferreusveritas.dynamictrees.block.rooty.SoilProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Pack soil compatibility using existing Dynamic Trees rooty blocks, not new registry entries. */
public final class DynamicTreesSoilAliases {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String[] REGOLITH_FAMILIES = {
            "beige_limestone", "conglomerate", "dolomite", "gabbro", "granodiorite", "grey_limestone",
            "kimberlite", "limestone", "mudstone", "phyllite", "quartzite", "rhyolite", "sandstone",
            "siltstone", "slate", "stone", "white_granite"
    };

    private DynamicTreesSoilAliases() {
    }

    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (BcFixesConfig.dynamicTreesUnearthedRegolithSoils()) {
                registerRegolithSoils();
            }
            if (ModList.get().isLoaded("immersive_weathering")) {
                registerWeatheringSoils();
            }
        });
    }

    private static void registerWeatheringSoils() {
        final String namespace = "immersive_weathering";
        final int sand = SoilHelper.getSoilFlags(SoilHelper.SAND_LIKE);
        final int mud = SoilHelper.getSoilFlags(SoilHelper.MUD_LIKE);
        register(namespace, Blocks.DIRT, "loam", "loamy_farmland", "permafrost", "mulch_block");
        register(namespace, Blocks.DIRT, sand, "sandy_dirt", "sandy_farmland");
        register(namespace, Blocks.GRASS_BLOCK, "grassy_permafrost", "rooted_grass_block");
        register(namespace, Blocks.GRASS_BLOCK, sand, "grassy_sandy_dirt");
        register(namespace, Blocks.GRASS_BLOCK, mud, "grassy_earthen_clay", "grassy_silt");
        register(namespace, Blocks.MUD, mud, "earthen_clay", "earthen_clay_farmland", "silt", "silty_farmland");
    }

    private static void registerRegolithSoils() {
        register("unearthed", Blocks.DIRT, "overgrown_andesite", "overgrown_diorite", "overgrown_granite");
        register("unearthed", Blocks.DIRT, withSuffix("_regolith"));
        register("unearthed", Blocks.DIRT, withSuffix("_grassy_regolith"));
        if (ModList.get().isLoaded("better_regolith_farming")) {
            register("better_regolith_farming", Blocks.DIRT, withSuffix("_better_regolith_farming"));
        }
    }

    private static String[] withSuffix(final String suffix) {
        final String[] paths = new String[REGOLITH_FAMILIES.length];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = REGOLITH_FAMILIES[i] + suffix;
        }
        return paths;
    }

    private static void register(final String namespace, final Block substitute, final String... paths) {
        register(namespace, substitute, 0, paths);
    }

    private static void register(final String namespace, final Block substitute, final int additionalFlags,
                                 final String... paths) {
        final RootyBlock rooty = SoilHelper.getProperties(substitute).getBlock().orElse(null);
        if (rooty == null) {
            LOGGER.error("Could not register Dynamic Trees soils for {}: no rooty substitute for {}",
                    namespace, ForgeRegistries.BLOCKS.getKey(substitute));
            return;
        }
        final int soilFlags = SoilHelper.getSoilFlags(SoilHelper.DIRT_LIKE) | additionalFlags;
        int registered = 0;
        for (String path : paths) {
            final ResourceLocation blockId = new ResourceLocation(namespace, path);
            if (!ForgeRegistries.BLOCKS.containsKey(blockId)) {
                LOGGER.warn("Skipping missing Dynamic Trees soil block {}", blockId);
                continue;
            }
            final Block block = ForgeRegistries.BLOCKS.getValue(blockId);
            if (SoilHelper.isSoilRegistered(block)) {
                continue;
            }
            final ResourceLocation soilId = new ResourceLocation("better_compat_fixes", namespace + "_" + path);
            final SoilProperties properties = new SoilProperties(block, soilId);
            properties.setSoilFlags(soilFlags);
            properties.setBlock(rooty);
            SoilHelper.addSoilPropertiesToMap(properties);
            registered++;
        }
        LOGGER.info("Registered {} Dynamic Trees dirt-like soil aliases for {} using {}",
                registered, namespace, ForgeRegistries.BLOCKS.getKey(substitute));
    }
}
