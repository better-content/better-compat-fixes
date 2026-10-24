package com.bettercontent.bettercompatfixes.fuelfixture.mixin;

import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = HearthBlockEntity.class, remap = false)
public interface HearthFuelAccess {
    @Accessor("usingHotFuel") void betterContentFuelFixture$usingHotFuel(boolean value);
    @Accessor("usingColdFuel") void betterContentFuelFixture$usingColdFuel(boolean value);
    @Accessor("ticksExisted") void betterContentFuelFixture$ticksExisted(int value);
    @Invoker("tickDrainFuel") void betterContentFuelFixture$tickDrainFuel();
}
