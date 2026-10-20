package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Player-mining gate only; the pack's additive diamond tag owns the required harvest tier. */
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID)
public final class SpawnerMiningPolicy {
    private SpawnerMiningPolicy() {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
        // Always let native abort handling clear an earlier mining session after a tool switch.
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.ABORT
                && shouldDeny(event.getEntity(), event.getLevel().getBlockState(event.getPos()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(final PlayerEvent.BreakSpeed event) {
        if (shouldDeny(event.getEntity(), event.getState())) {
            event.setNewSpeed(0.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(final BlockEvent.BreakEvent event) {
        if (shouldDeny(event.getPlayer(), event.getState())) {
            event.setCanceled(true);
        }
    }

    private static boolean shouldDeny(final Player player, final BlockState state) {
        if (!state.is(Blocks.SPAWNER) || player.isCreative()) {
            return false;
        }
        final ItemStack stack = player.getMainHandItem();
        return stack.isEmpty() || !stack.isCorrectToolForDrops(state);
    }
}
