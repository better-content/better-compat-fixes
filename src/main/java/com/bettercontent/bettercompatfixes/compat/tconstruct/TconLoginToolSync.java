package com.bettercontent.bettercompatfixes.compat.tconstruct;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

public final class TconLoginToolSync {
    private TconLoginToolSync() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDatapackSync(final OnDatapackSyncEvent event) {
        final ServerPlayer player = event.getPlayer();
        if (player == null) return;

        final Inventory inventory = player.getInventory();
        boolean changed = rebuildAll(inventory.items);
        changed |= rebuildAll(inventory.armor);
        changed |= rebuildAll(inventory.offhand);
        if (!changed) return;

        inventory.setChanged();
        player.inventoryMenu.broadcastFullState();
    }

    private static boolean rebuildAll(final Iterable<ItemStack> stacks) {
        boolean changed = false;
        for (ItemStack stack : stacks) {
            changed |= rebuildStack(stack);
        }
        return changed;
    }

    public static boolean rebuildStack(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable)) return false;

        final int damage = stack.getOrCreateTag().getInt("Damage");
        final ToolStack tool = ToolStack.from(stack);
        if (tool.getMaterials().isEmpty()) return false;

        final boolean broken = tool.isBroken();
        tool.rebuildStats();
        tool.setDamage(damage);
        tool.updateStack(stack, false);
        // updateStack may normalize the vanilla root damage against the stale stack copy.
        // Restore the exact player-owned value after the rebuilt tool data is committed.
        stack.getOrCreateTag().putInt("Damage", damage);
        stack.getOrCreateTag().putBoolean(ToolStack.TAG_BROKEN, broken);
        return true;
    }
}
