package com.bettercontent.bettercompatfixes.compat.pneumaticcraft;

import me.desht.pneumaticcraft.common.item.JackHammerItem;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Persists wear on the physical pick head after each successful player block break. */
public final class TconJackhammerHeadWear {
    private TconJackhammerHeadWear() {
    }

    @SubscribeEvent
    public static void onBlockBreak(final BlockEvent.BreakEvent event) {
        final ItemStack held = event.getPlayer().getMainHandItem();
        if (!(held.getItem() instanceof JackHammerItem)) return;

        final JackHammerItem.DrillBitHandler handler = JackHammerItem.getDrillBitHandler(held);
        if (handler == null) return;
        final ItemStack head = handler.getStackInSlot(0);
        if (!(head.getItem() instanceof ToolPartItem) || !TconJackhammerHeadPolicy.isSupportedHead(head)) return;
        TconJackhammerHeadPolicy.applySuccessfulBreakWear(head);
        handler.save();
    }
}
