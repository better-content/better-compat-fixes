package com.bettercontent.bettercompatfixes.fuelfixture.mixin;

import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import dev.murad.shipping.util.FuelItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = SteamTugEntity.class, remap = false)
public interface SteamTugFuelAccess {
    @Accessor("fuelItemHandler") FuelItemStackHandler betterContentFuelFixture$fuelHandler();
    @Accessor("burnTime") int betterContentFuelFixture$burnTime();
    @Accessor("burnTime") void betterContentFuelFixture$burnTime(int value);
    @Invoker("tickFuel") boolean betterContentFuelFixture$tickFuel();
}
