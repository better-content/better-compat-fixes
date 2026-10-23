package com.bettercontent.bettercompatfixes.mixin.dynamictrees;

import com.bettercontent.bettercompatfixes.compat.dynamictrees.RootySoilAdmission;
import com.llamalad7.mixinextras.sugar.Local;
import com.ferreusveritas.dynamictrees.worldgen.GenerationContext;
import com.ferreusveritas.dynamictrees.worldgen.JoCode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** After native preGeneration and its final state read; before destructive fallback/tree writes. */
@Mixin(value = JoCode.class, remap = false)
public abstract class JoCodeRootySoilAdmissionMixin {
    @Inject(method = "generate(Lcom/ferreusveritas/dynamictrees/worldgen/GenerationContext;)V",
            at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictrees/tree/species/Species;"
                    + "placeRootyDirtBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)Z",
                    remap = false), cancellable = true, remap = false, require = 1, allow = 1)
    private void better_compat_fixes$admitFinalRoot(GenerationContext context, CallbackInfo callback,
            @Local(index = 5) boolean worldGen, @Local(index = 7) BlockState initialDirtState) {
        if (RootySoilAdmission.reject(worldGen, initialDirtState)) callback.cancel();
    }
}
