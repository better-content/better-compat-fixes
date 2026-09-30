package com.bettercontent.bettercompatfixes.mixin.adpother.littlelogistics;

import com.endertech.minecraft.mods.adpother.events.WorldEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import dev.murad.shipping.util.FuelItemStackHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SteamTugEntity.class, remap = false)
public abstract class SteamTugEntityMixin {
    @Shadow
    @Final
    private FuelItemStackHandler fuelItemHandler;

    @WrapOperation(
            method = "tickFuel",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/murad/shipping/util/FuelItemStackHandler;tryConsumeFuel()I"
            ),
            require = 1
    )
    private int betterContentFixes$emitFuelPollution(
            final FuelItemStackHandler handler,
            final Operation<Integer> original
    ) {
        final ItemStack consumedFuel = fuelItemHandler.getStackInSlot(0).copy();
        final int burnTime = original.call(handler);
        if (burnTime > 0 && !consumedFuel.isEmpty()) {
            consumedFuel.setCount(1);
            WorldEvents.onFuelBurned(
                    consumedFuel,
                    1,
                    (Entity) (Object) this,
                    WorldEvents.Alignment.CENTER
            );
        }
        return burnTime;
    }
}
