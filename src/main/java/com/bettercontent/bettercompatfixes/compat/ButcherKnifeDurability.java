package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class ButcherKnifeDurability {
    private static final ResourceLocation BUTCHER_KNIFE =
            ResourceLocation.fromNamespaceAndPath("additionalweaponry", "butcher_knife");

    private ButcherKnifeDurability() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGrassCut(final BlockEvent.BreakEvent event) {
        if (event.isCanceled() || event.getPlayer().isCreative()) return;
        if (!event.getState().is(Blocks.GRASS)
                && !event.getState().is(Blocks.TALL_GRASS)
                && !event.getState().is(Blocks.FERN)
                && !event.getState().is(Blocks.LARGE_FERN)) return;
        final Player player = event.getPlayer();
        final ItemStack tool = player.getMainHandItem();
        if (!BUTCHER_KNIFE.equals(ForgeRegistries.ITEMS.getKey(tool.getItem()))) return;
        tool.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(InteractionHand.MAIN_HAND));
    }
}
