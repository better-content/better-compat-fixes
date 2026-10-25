package com.bettercontent.bettercompatfixes.compat.eanimod;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Prepare the one vanilla list that the pinned Genetic Animals startup listener appends to. */
@Mod.EventBusSubscriber(modid = "better_compat_fixes", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VillageAnimalPoolMutability {
    private VillageAnimalPoolMutability() { }

    static boolean applies(String version) { return "0.11.12".equals(version); }

    static <T> List<T> mutableIfKnownImmutable(List<T> original) {
        return original instanceof ImmutableList<?> ? new ArrayList<>(original) : original;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeAnimalRegistration(ServerAboutToStartEvent event) {
        String version = ModList.get().getModContainerById("eanimod")
                .map(mod -> mod.getModInfo().getVersion().toString()).orElse(null);
        if (!applies(version)) return;
        var pool = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL)
                .get(new ResourceLocation("minecraft", "village/common/animals"));
        if (pool == null) return; // The upstream listener also leaves an absent pool alone.
        var original = pool.rawTemplates;
        var mutable = mutableIfKnownImmutable(original);
        if (mutable != original) {
            pool.rawTemplates = mutable;
            com.mojang.logging.LogUtils.getLogger().debug(
                    "Prepared mutable village animal weights; preserved {} original entries", original.size());
        }
    }
}
