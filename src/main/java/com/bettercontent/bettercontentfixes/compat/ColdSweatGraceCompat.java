package com.bettercontent.bettercontentfixes.compat;

import com.bettercontent.bettercontentfixes.BetterContentFixes;
import com.momosoftworks.coldsweat.util.registries.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID)
public final class ColdSweatGraceCompat {
    private static final int RESPAWN_GRACE_TICKS = 20 * 120;
    private static final String GIVEN_GRACE_TAG = "GivenGracePeriod";
    private static final String ONBOARDING_COMPLETE_TAG = "tmp.onboarding_complete";

    private ColdSweatGraceCompat() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerJoinLevel(EntityJoinLevelEvent event) {
        if (!ModList.get().isLoaded("cold_sweat") || !ModList.get().isLoaded("class_selector")) return;
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) return;
        // Cold Sweat applies its one-time grace on this event, before Class Selector
        // switches an unstarted player to spectator. Reserve it until a real respawn.
        if (!player.getPersistentData().getBoolean(ONBOARDING_COMPLETE_TAG)) {
            player.getPersistentData().putBoolean(GIVEN_GRACE_TAG, true);
            player.removeEffect(ModEffects.GRACE);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!ModList.get().isLoaded("cold_sweat")) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.addEffect(new MobEffectInstance(ModEffects.GRACE, RESPAWN_GRACE_TICKS, 0, false, false, true));
        player.getPersistentData().putBoolean(GIVEN_GRACE_TAG, true);
    }
}
