package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID)
public final class RealisticHandsCompat {
    private RealisticHandsCompat() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(final PlayerEvent.BreakSpeed event) {
        if (!shouldDeny(event.getEntity(), event.getState())) {
            return;
        }
        event.setNewSpeed(0.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
        final BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!shouldDeny(event.getEntity(), state)) {
            return;
        }
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreakBlock(final BlockEvent.BreakEvent event) {
        final Player player = event.getPlayer();
        final BlockState state = event.getState();
        if (shouldDeny(player, state)) {
            event.setCanceled(true);
        }
    }

    private static boolean shouldDeny(final Player player, final BlockState state) {
        if (player == null || state == null || hasCreativeBypass(player)) {
            return false;
        }
        if (!state.is(RealisticHandsTags.AXE)) {
            return false;
        }
        final ItemStack stack = player.getMainHandItem();
        return stack.isEmpty() || !stack.is(RealisticHandsTags.AXE_TOOLS);
    }

    private static boolean hasCreativeBypass(final Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        return player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
    }
}
