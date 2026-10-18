package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Optional integrations; no hard dependency on Industrial Heat or Starcatcher. */
public final class SpoilageReuse {
    private SpoilageReuse() {}

    public static void registerProduce(net.minecraft.world.item.Item item) {
        ComposterBlock.COMPOSTABLES.put(item, .65f);
    }

    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var produce = new ResourceLocation("better_industrial_heat", "spoiled_produce");
            if (ForgeRegistries.ITEMS.containsKey(produce)) {
                registerProduce(ForgeRegistries.ITEMS.getValue(produce));
            }
            if (ModList.get().isLoaded("starcatcher")) StarcatcherSpoilage.register();
        });
    }
}
