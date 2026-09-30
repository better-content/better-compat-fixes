package com.bettercontent.bettercompatfixes.compat.tconstruct.polymorph.client;

import com.illusivesoulworks.polymorph.client.recipe.widget.PlayerRecipesWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

public final class CraftingStationRecipesWidget extends PlayerRecipesWidget {

    public CraftingStationRecipesWidget(
            final AbstractContainerScreen<?> screen,
            final Slot outputSlot
    ) {
        super(screen, outputSlot);
    }
}
