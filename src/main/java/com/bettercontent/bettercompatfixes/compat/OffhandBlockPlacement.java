package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Deny the item-use leg only: clicked blocks can still open or otherwise interact. */
public final class OffhandBlockPlacement {
    private OffhandBlockPlacement() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.OFF_HAND && event.getItemStack().getItem() instanceof BlockItem) {
            event.setUseItem(Event.Result.DENY);
        }
    }
}
