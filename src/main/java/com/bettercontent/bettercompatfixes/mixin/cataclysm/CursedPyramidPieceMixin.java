package com.bettercontent.bettercompatfixes.mixin.cataclysm;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cataclysm's marker callback writes blocks outside the chunk currently placing the template. */
@Mixin(targets = "com.github.L_Ender.cataclysm.structures.Cursed_Pyramid_Structure$Piece", remap = false)
public abstract class CursedPyramidPieceMixin {
    @Inject(method = "m_213704_", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void better_compat_fixes$skipMarkerOutsideCurrentChunk(
            final String marker,
            final BlockPos position,
            final ServerLevelAccessor level,
            final RandomSource random,
            final BoundingBox chunkBounds,
            final CallbackInfo callback
    ) {
        if (!chunkBounds.isInside(position)) callback.cancel();
    }
}
