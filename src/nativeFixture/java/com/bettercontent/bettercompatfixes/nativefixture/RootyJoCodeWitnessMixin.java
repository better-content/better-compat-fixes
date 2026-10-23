package com.bettercontent.bettercompatfixes.nativefixture;

import com.bettercontent.bettercompatfixes.nativewitness.RootySoilLedger;
import com.bettercontent.bettercompatfixes.nativewitness.RootySoilWitness;

import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.worldgen.GenerationContext;
import com.ferreusveritas.dynamictrees.worldgen.JoCode;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = JoCode.class, remap = false)
public abstract class RootyJoCodeWitnessMixin {
    @WrapOperation(method = "generate(Lcom/ferreusveritas/dynamictrees/worldgen/GenerationContext;)V",
            remap = false, require = 1,
            at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictrees/tree/species/Species;placeRootyDirtBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)Z", remap = false))
    public boolean betterCompat$generation(Species species, LevelAccessor level, BlockPos pos, int fertility,
                                             Operation<Boolean> original, GenerationContext context) {
        RootySoilLedger.Generation previous = RootySoilWitness.enter(context);
        try {
            // Same virtual receiver/arguments/result; the actual native caller still POPs this result.
            return original.call(species, level, pos, fertility);
        } finally {
            RootySoilWitness.leave(previous);
        }
    }
}
