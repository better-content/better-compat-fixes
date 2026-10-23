package com.bettercontent.bettercompatfixes.nativefixture;

import com.bettercontent.bettercompatfixes.nativewitness.RootySoilLedger;
import com.bettercontent.bettercompatfixes.nativewitness.RootySoilWitness;

import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/** Actual DT1.4.10 method/LVT/lambda observation, never a soil repair or WARN suppression. */
@Mixin(value = Species.class, remap = false)
public abstract class RootySpeciesWitnessMixin {
    @WrapMethod(method = "placeRootyDirtBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)Z",
            remap = false, require = 1)
    public boolean betterCompat$rootyCall(LevelAccessor level, BlockPos root, int fertility, Operation<Boolean> original) {
        RootySoilLedger.Call call = RootySoilWitness.begin();
        boolean returned;
        try {
            returned = original.call(level, root, fertility);
        } catch (RuntimeException | Error failure) {
            RootySoilWitness.finish(call, null, failure.getClass().getName());
            throw failure;
        }
        RootySoilWitness.finish(call, returned, null);
        return returned;
    }

    @Inject(method = "placeRootyDirtBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)Z",
            at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;)V", remap = false),
            locals = LocalCapture.CAPTURE_FAILHARD, remap = false, require = 1)
    private void betterCompat$unsupported(LevelAccessor level, BlockPos root, int fertility,
                                         CallbackInfoReturnable<Boolean> callback, BlockState dirtState, Block dirt) {
        // Native LVT4/5 already exist at WARN offset53. Tile-entity local6 is NOT initialized here.
        RootySoilWitness.unsupported((Species) (Object) this, root, fertility, dirtState, dirt);
    }

    @WrapOperation(method = "lambda$placeRootyDirtBlock$10", remap = false, require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", remap = false))
    public boolean betterCompat$setBlock(LevelAccessor receiver, BlockPos pos, BlockState state, int flags,
                                          Operation<Boolean> original) {
        // Copy scalar arguments BEFORE the call; preserve original receiver/pos/state/flags and native POP.
        RootySoilLedger.Call call = RootySoilWitness.setStart(pos, state, flags);
        boolean returned;
        try {
            returned = original.call(receiver, pos, state, flags);
        } catch (RuntimeException | Error failure) {
            RootySoilWitness.setThrown(call, failure.getClass().getName());
            throw failure;
        }
        RootySoilWitness.setReturn(call, returned);
        return returned;
    }
}
