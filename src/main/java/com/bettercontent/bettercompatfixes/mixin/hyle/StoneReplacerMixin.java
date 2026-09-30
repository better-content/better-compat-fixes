package com.bettercontent.bettercompatfixes.mixin.hyle;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import lilypuree.hyle.world.feature.gen.StoneType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "lilypuree.hyle.world.feature.StoneReplacer", remap = false)
public abstract class StoneReplacerMixin {
    private static final int BCFIXES_SECTION_HEIGHT = 16;

    @Inject(method = "replaceAll", at = @At("RETURN"), remap = false)
    private void better_compat_fixes$completeBottomSection(
            final ChunkAccess chunk,
            final int baseY,
            final int[][] heights,
            final StoneType[][][] generatedStoneTypes,
            final CallbackInfo ci) {
        if (!BcFixesConfig.hyleCompleteBottomSection() || generatedStoneTypes == null) {
            return;
        }

        final int minY = chunk.getMinBuildHeight();
        final LevelChunkSection bottomSection = chunk.getSection(chunk.getSectionIndex(minY));
        if (bottomSection.hasOnlyAir()) {
            return;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final StoneType[] columnTypes = generatedStoneTypes[x][z];
                if (columnTypes == null || columnTypes.length == 0) {
                    continue;
                }

                for (int localY = 0; localY < BCFIXES_SECTION_HEIGHT; localY++) {
                    final int worldY = minY + localY;
                    if (worldY > heights[x][z]) {
                        continue;
                    }

                    final BlockState original = bottomSection.getBlockState(x, localY, z);
                    final int generatedIndex = Math.max(0, worldY - baseY);
                    final BlockState replacement = nearestReplacement(columnTypes, generatedIndex, original);
                    if (replacement != original) {
                        bottomSection.setBlockState(x, localY, z, replacement, false);
                    }
                }
            }
        }
    }

    private static BlockState nearestReplacement(
            final StoneType[] columnTypes,
            final int generatedIndex,
            final BlockState original) {
        for (int index = Math.min(generatedIndex, columnTypes.length - 1); index < columnTypes.length; index++) {
            final StoneType stoneType = columnTypes[index];
            if (stoneType == null) {
                continue;
            }
            final BlockState replacement = stoneType.replace(original);
            if (replacement != original) {
                return replacement;
            }
        }
        return original;
    }
}
