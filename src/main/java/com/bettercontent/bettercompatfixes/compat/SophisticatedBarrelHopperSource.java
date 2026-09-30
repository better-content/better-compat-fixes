package com.bettercontent.bettercompatfixes.compat;

import net.minecraftforge.items.IItemHandlerModifiable;

public interface SophisticatedBarrelHopperSource {
    boolean betterContent$isBarrel();

    IItemHandlerModifiable betterContent$getInventoryForInputOutput();
}
