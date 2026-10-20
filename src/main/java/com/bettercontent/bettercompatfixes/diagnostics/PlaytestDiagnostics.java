package com.bettercontent.bettercompatfixes.diagnostics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Native lifecycle bridge only; the bounded sink retains no player, menu or world references. */
@Mod.EventBusSubscriber(modid = "better_compat_fixes")
public final class PlaytestDiagnostics {
    public static final PlaytestLog LOG = new PlaytestLog("better_compat_fixes");
    private PlaytestDiagnostics() {}
    private static String version(String id) {
        return ModList.get().getModContainerById(id)
                .map(mod -> mod.getModInfo().getVersion().toString()).orElse("absent");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && LOG.enabled("summary")) LOG.summary("server");
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { LOG.reset("server"); }

    private static String source() {
        var file = ModList.get().getModFileById("better_compat_fixes");
        return LOG.sourceIdentity(file == null || file.getFile() == null ? (java.nio.file.Path) null
                : file.getFile().findResource("META-INF", "better-content-source.properties"));
    }
    @Mod.EventBusSubscriber(modid = "better_compat_fixes", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Startup {
        @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
            LOG.status("common", version("better_compat_fixes"), source(), () -> PlaytestLog.fields("enabled", true, "sleep_pace_enabled", com.bettercontent.bettercompatfixes.config.BcFixesConfig.sleepingOverhaulPaceTimelapse(), "epicfight_version", version("epicfight"), "curios_version", version("curios"), "sophisticatedbackpacks_version", version("sophisticatedbackpacks"), "parcool_version", version("parcool"), "sleepingoverhaul_version", version("sleepingoverhaul"), "the_deep_void_version", version("the_deep_void")));
        }
    }
    @Mod.EventBusSubscriber(modid = "better_compat_fixes", value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && LOG.enabled("summary")) LOG.summary("client");
        }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { LOG.reset("client"); }
    }
}
