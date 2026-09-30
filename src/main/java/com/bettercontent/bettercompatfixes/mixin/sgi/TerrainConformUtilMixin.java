package com.bettercontent.bettercompatfixes.mixin.sgi;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.mcreator.structuregenerationimprover.TerrainConformUtil", remap = false)
public abstract class TerrainConformUtilMixin {
    @Unique
    private static final Set<Block> BCFIXES_VANILLA_ROCK_HOSTS = Set.of(
            Blocks.STONE,
            Blocks.GRANITE,
            Blocks.DIORITE,
            Blocks.ANDESITE,
            Blocks.DEEPSLATE,
            Blocks.TUFF);

    @Unique
    private static final int BCFIXES_MAX_VERTICAL_PALETTE_SEARCH = 64;

    @Unique
    private static final ThreadLocal<BlockPos.MutableBlockPos> BCFIXES_PALETTE_PROBE = new ThreadLocal<>();

    @WrapMethod(method = "applyDuringSurface", remap = false)
    private static void better_compat_fixes$translateSgiRockWrites(
            final WorldGenLevel level,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final Operation<Void> original) {
        if (!BcFixesConfig.sgiRerunHyleAfterSurfaceConform()) {
            original.call(level, structureManager, chunk);
            return;
        }

        final BlockPos.MutableBlockPos previousProbe = BCFIXES_PALETTE_PROBE.get();
        BCFIXES_PALETTE_PROBE.set(new BlockPos.MutableBlockPos());
        try {
            original.call(level, structureManager, chunk);
        } finally {
            if (previousProbe == null) {
                BCFIXES_PALETTE_PROBE.remove();
            } else {
                BCFIXES_PALETTE_PROBE.set(previousProbe);
            }
        }
    }

    @WrapOperation(
            method = "applySingleSlopeWithOgCompare",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
                    remap = false),
            remap = false,
            require = 1,
            allow = 1)
    private static BlockState better_compat_fixes$translateSingleSlopeRock(
            final ChunkAccess chunk,
            final BlockPos position,
            final BlockState proposedState,
            final boolean moved,
            final Operation<BlockState> original) {
        return better_compat_fixes$translateRockWrite(chunk, position, proposedState, moved, original);
    }

    @WrapOperation(
            method = "fillVerticalCavities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
                    remap = false),
            remap = false,
            require = 2,
            allow = 2)
    private static BlockState better_compat_fixes$translateVerticalCavityRock(
            final ChunkAccess chunk,
            final BlockPos position,
            final BlockState proposedState,
            final boolean moved,
            final Operation<BlockState> original) {
        return better_compat_fixes$translateRockWrite(chunk, position, proposedState, moved, original);
    }

    @WrapOperation(
            method = "placeSlopeFill",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
                    remap = false),
            remap = false,
            require = 3,
            allow = 3)
    private static BlockState better_compat_fixes$translateSlopeFillRock(
            final ChunkAccess chunk,
            final BlockPos position,
            final BlockState proposedState,
            final boolean moved,
            final Operation<BlockState> original) {
        return better_compat_fixes$translateRockWrite(chunk, position, proposedState, moved, original);
    }

    @WrapOperation(
            method = "applySurfaceLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
                    remap = false),
            remap = false,
            require = 2,
            allow = 2)
    private static BlockState better_compat_fixes$translateSurfaceLayerRock(
            final ChunkAccess chunk,
            final BlockPos position,
            final BlockState proposedState,
            final boolean moved,
            final Operation<BlockState> original) {
        return better_compat_fixes$translateRockWrite(chunk, position, proposedState, moved, original);
    }

    @WrapOperation(
            method = "convertOrganicBelowIfNeeded",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;m_6978_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
                    remap = false),
            remap = false,
            require = 1,
            allow = 1)
    private static BlockState better_compat_fixes$translateOrganicConversionRock(
            final ChunkAccess chunk,
            final BlockPos position,
            final BlockState proposedState,
            final boolean moved,
            final Operation<BlockState> original) {
        return better_compat_fixes$translateRockWrite(chunk, position, proposedState, moved, original);
    }

    @Unique
    private static BlockState better_compat_fixes$translateRockWrite(
            final ChunkAccess chunk,
            final BlockPos position,
            final BlockState proposedState,
            final boolean moved,
            final Operation<BlockState> original) {
        final BlockPos.MutableBlockPos probe = BCFIXES_PALETTE_PROBE.get();
        if (probe == null || !BCFIXES_VANILLA_ROCK_HOSTS.contains(proposedState.getBlock())) {
            return original.call(chunk, position, proposedState, moved);
        }

        final BlockState replacement = better_compat_fixes$nearestUnearthedStone(chunk, probe, position);
        return original.call(chunk, position, replacement == null ? proposedState : replacement, moved);
    }

    @Unique
    private static BlockState better_compat_fixes$nearestUnearthedStone(
            final ChunkAccess chunk,
            final BlockPos.MutableBlockPos probe,
            final BlockPos target) {
        final int minY = chunk.getMinBuildHeight();
        final int maxY = chunk.getMaxBuildHeight() - 1;
        for (int distance = 1; distance <= BCFIXES_MAX_VERTICAL_PALETTE_SEARCH; distance++) {
            if (target.getY() - distance >= minY) {
                final BlockState below = chunk.getBlockState(
                        probe.set(target.getX(), target.getY() - distance, target.getZ()));
                if (better_compat_fixes$isUnearthedStone(below)) {
                    return below;
                }
            }
            if (target.getY() + distance <= maxY) {
                final BlockState above = chunk.getBlockState(
                        probe.set(target.getX(), target.getY() + distance, target.getZ()));
                if (better_compat_fixes$isUnearthedStone(above)) {
                    return above;
                }
            }
        }
        return null;
    }

    @Unique
    private static boolean better_compat_fixes$isUnearthedStone(final BlockState state) {
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!"unearthed".equals(id.getNamespace())) {
            return false;
        }
        final String path = id.getPath();
        return !path.contains("regolith") && !path.contains("overgrown");
    }
}
