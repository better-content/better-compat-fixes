package com.bettercontent.bettercompatfixes.compat.tconstruct;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.gametest.TconCompatGameTests;
import com.bettercontent.bettercompatfixes.compat.pneumaticcraft.TconJackhammerHeadWear;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TconCompatBootstrap {
    private TconCompatBootstrap() {
    }

    @SubscribeEvent
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded("tconstruct")) return;

        MinecraftForge.EVENT_BUS.register(TconLoginToolSync.class);
        if (ModList.get().isLoaded("pneumaticcraft")) {
            MinecraftForge.EVENT_BUS.register(TconJackhammerHeadWear.class);
        }
    }

    @SubscribeEvent
    public static void onRegisterGameTests(final RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("tconstruct") && ModList.get().isLoaded("polymorph")) {
            event.register(TconCompatGameTests.class);
        }
    }
}
