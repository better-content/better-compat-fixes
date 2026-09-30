package com.bettercontent.bettercompatfixes.learning;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Emits learning evidence only after ParCool has accepted and started a configured custom action. */
public final class ParCoolControlLearning {
    private static final Map<String, String> QUALIFYING_ACTIONS = Map.of(
            "Dodge", "parcool:dodge",
            "FastRun", "parcool:fast_run",
            "ClingToCliff", "parcool:cling_to_cliff",
            "Vault", "parcool:vault",
            "RideZipline", "parcool:ride_zipline",
            "HangDown", "parcool:hang_down",
            "WallSlide", "parcool:wall_slide");

    private ParCoolControlLearning() {
    }

    @SubscribeEvent
    public static void onActionStarted(final ParCoolActionEvent.Start.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        final String actionId = QUALIFYING_ACTIONS.get(event.getAction().getClass().getSimpleName());
        if (actionId != null) {
            CustomControlEpisodes.record(player, actionId);
        }
    }
}
