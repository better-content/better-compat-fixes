package com.bettercontent.bettercompatfixes.fuelfixture.mixin;

import com.bettercontent.bettercompatfixes.nativewitness.fuel.FuelEmissionWitness;
import com.endertech.minecraft.mods.adpother.blocks.Pollutant;
import com.endertech.minecraft.mods.adpother.pollution.ChunkPollution;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the native fixture loads this config; every original call/return is preserved. */
@Mixin(value = ChunkPollution.class, remap = false)
public abstract class ChunkPollutionWitnessMixin {
    private static final String FUEL_INCREASE = "increaseBy(Lcom/endertech/minecraft/mods/adpother/sources/Emitter;"
            + "Lcom/endertech/minecraft/forge/blocks/IEmitter$Type;Lnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/core/BlockPos;Lcom/endertech/minecraft/mods/adpother/sources/Fuel;I)V";

    @WrapOperation(method = FUEL_INCREASE, at = @At(value = "INVOKE",
            target = "Lcom/endertech/minecraft/mods/adpother/pollution/ChunkPollution;"
                    + "getIntAndUpdateFractAmount(Lcom/endertech/minecraft/mods/adpother/blocks/Pollutant;D)I"),
            require = 1)
    private int betterContentFuelFixture$measureNativeFraction(ChunkPollution pollution,
            Pollutant<?> pollutant, double amount, Operation<Integer> original) {
        double before = pollution.getInfoFor(pollutant).map(info -> info.getFraction()).orElse(0.0);
        int result = original.call(pollution, pollutant, amount);
        double after = pollution.getInfoFor(pollutant).map(info -> info.getFraction()).orElse(0.0);
        FuelEmissionWitness.amount(pollutant, result, before, after);
        return result;
    }

    @Inject(method = FUEL_INCREASE, at = @At("RETURN"), require = 1)
    private void betterContentFuelFixture$completedNativeIncrease(CallbackInfo ci) {
        FuelEmissionWitness.completed();
    }
}
