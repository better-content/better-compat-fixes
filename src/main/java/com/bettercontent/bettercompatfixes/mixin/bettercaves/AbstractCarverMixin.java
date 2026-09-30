package com.bettercontent.bettercompatfixes.mixin.bettercaves;

import com.bettercontent.bettercompatfixes.compat.BetterCavesCarvingBounds;
import com.yungnickyoung.minecraft.bettercaves.worldgen.BetterCavesWorldCarverConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents Better Caves from addressing a carving mask outside a dimension's legal vertical range. */
@Mixin(targets = "com.yungnickyoung.minecraft.bettercaves.worldgen.carver.AbstractCarver", remap = false)
public abstract class AbstractCarverMixin {
    @Inject(
            method = "carveBlock(Lcom/yungnickyoung/minecraft/bettercaves/worldgen/BetterCavesWorldCarverConfig;"
                    + "Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/chunk/CarvingMask;"
                    + "Lnet/minecraft/world/level/levelgen/Aquifer;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1)
    private void better_compat_fixes$skipOutsideChunkHeight(
            final BetterCavesWorldCarverConfig config,
            final ChunkAccess chunk,
            final BlockPos pos,
            final BlockState airBlockState,
            final BlockState liquidBlockState,
            final CarvingMask carvingMask,
            final Aquifer aquifer,
            final CallbackInfo ci
    ) {
        if (!BetterCavesCarvingBounds.contains(chunk, pos)) {
            ci.cancel();
        }
    }
}
