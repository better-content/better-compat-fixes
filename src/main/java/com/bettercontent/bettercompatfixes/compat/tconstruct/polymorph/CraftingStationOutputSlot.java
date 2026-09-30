package com.bettercontent.bettercompatfixes.compat.tconstruct.polymorph;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;
import slimeknights.tconstruct.tables.menu.CraftingStationContainerMenu;

/** Finds the station output by its inventory contract, independent of TCon's slot subclass. */
public final class CraftingStationOutputSlot {
    private CraftingStationOutputSlot() {
    }

    public static Slot find(final CraftingStationContainerMenu menu) {
        if (menu == null) return null;
        final CraftingStationBlockEntity station = menu.getTile();
        if (station == null) return null;
        return find(menu.slots, station.getCraftingResult());
    }

    public static Slot find(final Iterable<Slot> slots, final Container resultContainer) {
        if (slots == null || resultContainer == null) return null;
        for (Slot slot : slots) {
            if (slot.container == resultContainer && slot.getContainerSlot() == 0) {
                return slot;
            }
        }
        return null;
    }
}
