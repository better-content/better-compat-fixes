package com.bettercontent.bettercompatfixes.mixin.lostcities;

import com.bettercontent.bettercompatfixes.compat.LostCitiesSectionBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes vertical neighbor reads outside Lost Cities' section cache through the level accessor. */
@Mixin(targets = "mcjty.lostcities.worldgen.ChunkDriver", remap = false)
public abstract class ChunkDriverMixin {
    @Shadow(remap = false)
    @Final
    private LevelAccessor region;

    @Inject(
            method = "getBlockSafe",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1)
    private void better_compat_fixes$readOutsideCacheFromRegion(
            final BlockPos pos,
            final CallbackInfoReturnable<BlockState> cir
    ) {
        if (!LostCitiesSectionBounds.contains(region, pos)) {
            cir.setReturnValue(region.getBlockState(pos));
        }
    }

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1)
    private void better_compat_fixes$skipWriteOutsideCache(
            final BlockPos pos,
            final BlockState state,
            final CallbackInfo ci
    ) {
        if (!LostCitiesSectionBounds.contains(region, pos)) {
            ci.cancel();
        }
    }
}
