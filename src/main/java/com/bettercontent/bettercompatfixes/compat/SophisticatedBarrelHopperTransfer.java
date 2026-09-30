package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

public final class SophisticatedBarrelHopperTransfer {
    private SophisticatedBarrelHopperTransfer() {
    }

    public static boolean tryMoveOneItem(final IItemHandlerModifiable source, final Hopper hopper) {
        final InvWrapper destination = new InvWrapper(hopper);
        for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
            final ItemStack simulatedExtraction = source.extractItem(sourceSlot, 1, true);
            if (simulatedExtraction.isEmpty()
                    || !ItemHandlerHelper.insertItem(destination, simulatedExtraction, true).isEmpty()) {
                continue;
            }

            final ItemStack originalSourceStack = source.getStackInSlot(sourceSlot).copy();
            final ItemStack extracted = source.extractItem(sourceSlot, 1, false);
            if (extracted.isEmpty()) {
                continue;
            }

            final ItemStack remainder = ItemHandlerHelper.insertItem(destination, extracted, false);
            if (!remainder.isEmpty()) {
                source.setStackInSlot(sourceSlot, originalSourceStack);
                return false;
            }

            hopper.setChanged();
            return true;
        }
        return false;
    }

}
