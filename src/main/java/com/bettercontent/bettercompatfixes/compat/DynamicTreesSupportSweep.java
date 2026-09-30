package com.bettercontent.bettercompatfixes.compat;

import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class DynamicTreesSupportSweep {
    private static final List<Block> SOIL_CANDIDATES = List.of(
            Blocks.GRASS_BLOCK,
            Blocks.DIRT,
            Blocks.PODZOL,
            Blocks.ROOTED_DIRT,
            Blocks.MUD,
            Blocks.MOSS_BLOCK,
            Blocks.MYCELIUM,
            Blocks.SAND,
            Blocks.RED_SAND,
            Blocks.TERRACOTTA,
            Blocks.NETHERRACK,
            Blocks.CRIMSON_NYLIUM,
            Blocks.WARPED_NYLIUM,
            Blocks.SOUL_SOIL,
            Blocks.SOUL_SAND
    );
    private static final int CLEAR_RADIUS = 8;
    private static final int CLEAR_HEIGHT = 32;

    private DynamicTreesSupportSweep() {
    }

    public static List<String> run(final ServerLevel level, final BlockPos rootPos) {
        final BlockPos saplingPos = rootPos.above();
        final List<String> failures = new ArrayList<>();

        for (Species species : Species.REGISTRY.getAll()) {
            final ResourceLocation speciesId = species.getRegistryName();
            if (speciesId == null || "null".equals(speciesId.getPath())) {
                continue;
            }
            if (shouldSkipSpecies(speciesId)) {
                continue;
            }

            clearTestColumn(level, rootPos);
            final boolean generated = tryGenerateTree(level, rootPos, saplingPos, species);
            if (!generated) {
                failures.add(speciesId + " did not generate a rooted tree");
                continue;
            }

            level.setBlock(rootPos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            DynamicTreesUnsupportedTreeFallover.destroyUnsupportedTree(level, rootPos);
            if (TreeHelper.isRooty(level.getBlockState(rootPos))) {
                failures.add(speciesId + " root survived support loss");
            }
        }

        return failures;
    }

    private static boolean shouldSkipSpecies(final ResourceLocation speciesId) {
        final String path = speciesId.getPath();
        return path.endsWith("_undergrowth") || path.contains("mushroom");
    }

    private static boolean tryGenerateTree(final ServerLevel level, final BlockPos rootPos, final BlockPos saplingPos,
                                           final Species species) {
        final Block saplingBlock = species.getSapling().orElse(null);
        if (saplingBlock == null) {
            return false;
        }

        for (Block soil : SOIL_CANDIDATES) {
            clearTestColumn(level, rootPos);
            level.setBlock(rootPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(rootPos, soil.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(saplingPos, saplingBlock.defaultBlockState(), Block.UPDATE_ALL);

            if (!species.transitionToTree(level, saplingPos)) {
                continue;
            }
            if (!TreeHelper.isRooty(level.getBlockState(rootPos))) {
                continue;
            }
            if (!hasBranchNear(level, rootPos)) {
                continue;
            }
            return true;
        }

        return false;
    }

    private static boolean hasBranchNear(final ServerLevel level, final BlockPos rootPos) {
        for (int y = 1; y <= 12; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (TreeHelper.isBranch(level.getBlockState(rootPos.offset(x, y, z)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void clearTestColumn(final ServerLevel level, final BlockPos rootPos) {
        final BlockPos min = rootPos.offset(-CLEAR_RADIUS, -1, -CLEAR_RADIUS);
        final BlockPos max = rootPos.offset(CLEAR_RADIUS, CLEAR_HEIGHT, CLEAR_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            final BlockState replacement = pos.getY() == rootPos.getY() - 1
                    ? Blocks.STONE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            level.setBlock(pos, replacement, Block.UPDATE_ALL);
        }
    }

}
