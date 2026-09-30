package com.bettercontent.bettercompatfixes.compat.tconstruct.polymorph.client;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = BetterContentFixes.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class TconPolymorphClientBootstrap {
    private TconPolymorphClientBootstrap() {
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("tconstruct") && ModList.get().isLoaded("polymorph")) {
            event.enqueueWork(TconPolymorphClient::register);
        }
    }
}
