package com.bettercontent.bettercompatfixes;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.bettercontent.bettercompatfixes.compat.AmbientSurfaceSpawnControl;
import com.bettercontent.bettercompatfixes.compat.ButcherKnifeDurability;
import com.bettercontent.bettercompatfixes.compat.CuriosSlotPolicy;
import com.bettercontent.bettercompatfixes.compat.FarmlandTrampleProtection;
import com.bettercontent.bettercompatfixes.compat.ExtendedItemPickup;
import com.bettercontent.bettercompatfixes.compat.FluidMixBlocker;
import com.bettercontent.bettercompatfixes.compat.DynamicTreesUnsupportedTreeFallover;
import com.bettercontent.bettercompatfixes.compat.DecorativeVegetationTrample;
import com.bettercontent.bettercompatfixes.compat.DynamicTreesUnearthedSoils;
import com.bettercontent.bettercompatfixes.compat.DynamicTreesSupportSweepCommand;
import com.bettercontent.bettercompatfixes.compat.VoidWormSpawnRemoval;
import com.bettercontent.bettercompatfixes.compat.ThirstLootModifierCompat;
import com.bettercontent.bettercompatfixes.compat.emi.EmiDefaultsBootstrap;
import com.bettercontent.bettercompatfixes.compat.epicfight.StyleEvents;
import com.bettercontent.bettercompatfixes.compat.epicfight.StyleNetwork;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.bettercontent.bettercompatfixes.config.BcFixesClientConfig;
import com.bettercontent.bettercompatfixes.gametest.AmbientSurfaceSpawnGameTests;
import com.bettercontent.bettercompatfixes.gametest.DaylightProtectionGameTests;
import com.bettercontent.bettercompatfixes.gametest.Weather2LoadedChunkGameTests;
import com.bettercontent.bettercompatfixes.gametest.DecorativeVegetationTrampleGameTests;
import com.bettercontent.bettercompatfixes.gametest.DynamicTreesUnsupportedTreeGameTests;
import com.bettercontent.bettercompatfixes.gametest.FarmlandTrampleProtectionGameTests;
import com.bettercontent.bettercompatfixes.gametest.ExtendedItemPickupGameTests;
import com.bettercontent.bettercompatfixes.gametest.FluidMixBlockerGameTests;
import com.bettercontent.bettercompatfixes.gametest.SophisticatedBarrelHopperGameTests;
import com.bettercontent.bettercompatfixes.gametest.VanillaBoatGameTests;
import com.bettercontent.bettercompatfixes.gametest.WaterWheelBiomePolicyGameTests;
import com.bettercontent.bettercompatfixes.learning.CustomControlEpisodeGameTests;
import com.bettercontent.bettercompatfixes.compat.sleeping.SleepTimelapseEventGameTests;
import com.bettercontent.bettercompatfixes.compat.sleeping.SleepDangerInterruption;
import com.bettercontent.bettercompatfixes.gametest.OptionalIntegrationGameTests;
import com.bettercontent.bettercompatfixes.gametest.PotionStructureSanitizerGameTests;
import com.bettercontent.bettercompatfixes.learning.ParCoolControlLearning;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BetterContentFixes.MOD_ID)
public final class BetterContentFixes {
    public static final String MOD_ID = "better_compat_fixes";

    public BetterContentFixes() {
        MixinExtrasBootstrap.init();
        if (ModList.get().isLoaded("epicfight")) {
            StyleNetwork.register();
            MinecraftForge.EVENT_BUS.register(StyleEvents.class);
        }
        EmiDefaultsBootstrap.seedIfApplicable();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BcFixesConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BcFixesClientConfig.SPEC);
        final var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        if (ModList.get().isLoaded("thirst")) {
            ThirstLootModifierCompat.register(modEventBus);
        }
        VoidWormSpawnRemoval.SERIALIZERS.register(modEventBus);
        if (ModList.get().isLoaded("dynamictrees")) {
            modEventBus.addListener(DynamicTreesUnearthedSoils::onCommonSetup);
        }
        modEventBus.addListener(this::onRegisterGameTests);
        MinecraftForge.EVENT_BUS.register(FarmlandTrampleProtection.class);
        MinecraftForge.EVENT_BUS.register(AmbientSurfaceSpawnControl.class);
        MinecraftForge.EVENT_BUS.register(FluidMixBlocker.class);
        MinecraftForge.EVENT_BUS.register(DecorativeVegetationTrample.class);
        MinecraftForge.EVENT_BUS.register(ExtendedItemPickup.class);
        if (ModList.get().isLoaded("dynamictrees")) {
            MinecraftForge.EVENT_BUS.register(DynamicTreesUnsupportedTreeFallover.class);
            MinecraftForge.EVENT_BUS.register(DynamicTreesSupportSweepCommand.class);
        }
        MinecraftForge.EVENT_BUS.register(ButcherKnifeDurability.class);
        if (ModList.get().isLoaded("curios") && ModList.get().isLoaded("sophisticatedbackpacks")) {
            MinecraftForge.EVENT_BUS.register(CuriosSlotPolicy.class);
        }
        if (ModList.get().isLoaded("parcool")) {
            MinecraftForge.EVENT_BUS.register(ParCoolControlLearning.class);
        }
        if (ModList.get().isLoaded("sleepingoverhaul")) {
            MinecraftForge.EVENT_BUS.register(SleepDangerInterruption.class);
        }
    }

    private void onRegisterGameTests(final RegisterGameTestsEvent event) {
        event.register(AmbientSurfaceSpawnGameTests.class);
        event.register(DaylightProtectionGameTests.class);
        event.register(Weather2LoadedChunkGameTests.class);
        event.register(DecorativeVegetationTrampleGameTests.class);
        event.register(ExtendedItemPickupGameTests.class);
        event.register(FarmlandTrampleProtectionGameTests.class);
        event.register(FluidMixBlockerGameTests.class);
        event.register(VanillaBoatGameTests.class);
        event.register(WaterWheelBiomePolicyGameTests.class);
        event.register(OptionalIntegrationGameTests.class);
        event.register(PotionStructureSanitizerGameTests.class);
        event.register(CustomControlEpisodeGameTests.class);
        event.register(SleepTimelapseEventGameTests.class);
        if (ModList.get().isLoaded("dynamictrees")) {
            event.register(DynamicTreesUnsupportedTreeGameTests.class);
        }
        if (ModList.get().isLoaded("sophisticatedstorage")) {
            event.register(SophisticatedBarrelHopperGameTests.class);
        }
    }
}
