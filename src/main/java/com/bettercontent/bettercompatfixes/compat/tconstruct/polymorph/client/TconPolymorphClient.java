package com.bettercontent.bettercompatfixes.compat.tconstruct.polymorph.client;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.bettercontent.bettercompatfixes.compat.tconstruct.polymorph.CraftingStationOutputSlot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import slimeknights.tconstruct.tables.client.inventory.CraftingStationScreen;

public final class TconPolymorphClient {
    private TconPolymorphClient() {
    }

    public static void register() {
        PolymorphApi.client().registerWidget(TconPolymorphClient::createWidget);
    }

    private static CraftingStationRecipesWidget createWidget(final AbstractContainerScreen<?> screen) {
        if (!(screen instanceof CraftingStationScreen stationScreen)) return null;
        final Slot output = CraftingStationOutputSlot.find(stationScreen.getMenu());
        return output == null ? null : new CraftingStationRecipesWidget(screen, output);
    }
}
