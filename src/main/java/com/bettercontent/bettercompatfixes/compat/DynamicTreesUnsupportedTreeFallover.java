package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.entity.FallingTreeEntity;
import com.ferreusveritas.dynamictrees.util.BranchDestructionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public final class DynamicTreesUnsupportedTreeFallover {
    private static final int MAX_CHUNK_SWEEPS_PER_TICK = 2;
    private static final Queue<QueuedChunkSweep> PENDING_CHUNK_SWEEPS = new ArrayDeque<>();
    private static final Set<QueuedChunkSweep> PENDING_CHUNK_SWEEP_KEYS = new HashSet<>();

    private DynamicTreesUnsupportedTreeFallover() {
    }

    @SubscribeEvent
    public static void onNeighborNotify(final BlockEvent.NeighborNotifyEvent event) {
        if (!BcFixesConfig.dynamicTreesDestroyUnsupportedTrees() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!event.getNotifiedSides().contains(Direction.UP)) {
            return;
        }
        destroyUnsupportedTree(level, event.getPos().above());
    }

    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load event) {
        if (!BcFixesConfig.dynamicTreesDestroyUnsupportedTrees() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        enqueueChunkSweep(level, chunk);
    }

    @SubscribeEvent
    public static void onServerLevelTick(final TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (!BcFixesConfig.dynamicTreesDestroyUnsupportedTrees() || PENDING_CHUNK_SWEEPS.isEmpty()) {
            return;
        }

        int processed = 0;
        while (processed < MAX_CHUNK_SWEEPS_PER_TICK) {
            final QueuedChunkSweep sweep = PENDING_CHUNK_SWEEPS.peek();
            if (sweep == null) {
                return;
            }
            if (!sweep.dimension().equals(level.dimension())) {
                return;
            }

            PENDING_CHUNK_SWEEPS.poll();
            PENDING_CHUNK_SWEEP_KEYS.remove(sweep);
            processed++;

            if (!level.hasChunk(sweep.chunkX(), sweep.chunkZ())) {
                continue;
            }

            final ChunkAccess chunkAccess = level.getChunkSource().getChunkNow(sweep.chunkX(), sweep.chunkZ());
            if (!(chunkAccess instanceof LevelChunk chunk)) {
                continue;
            }

            scanChunkForUnsupportedTrees(level, chunk);
        }
    }

    private static void scanChunkForUnsupportedTrees(final ServerLevel level, final LevelChunk chunk) {
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        final int minSectionY = chunk.getMinSection();
        final LevelChunkSection[] sections = chunk.getSections();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            final LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }

            final int sectionY = (minSectionY + sectionIndex) << 4;
            final PalettedContainer<BlockState> states = section.getStates();
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        final BlockState state = states.get(localX, localY, localZ);
                        if (!isRootyBlock(state.getBlock())) {
                            continue;
                        }
                        cursor.set(
                                chunk.getPos().getMinBlockX() + localX,
                                sectionY + localY,
                                chunk.getPos().getMinBlockZ() + localZ
                        );
                        destroyUnsupportedTree(level, cursor);
                    }
                }
            }
        }
    }

    static void destroyUnsupportedTree(final ServerLevel level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RootyBlock rootyBlock) || isSupported(level, pos)) {
            return;
        }

        if (!dropUnsupportedTree(level, pos)) {
            rootyBlock.destroyTree(level, pos);
        }
        if (level.getBlockState(pos).getBlock() == state.getBlock()) {
            final BlockState decayState = rootyBlock.getDecayBlockState(state, level, pos);
            level.setBlock(pos, decayState, Block.UPDATE_ALL);
        }
    }

    private static boolean dropUnsupportedTree(final ServerLevel level, final BlockPos rootPos) {
        final BlockPos branchPos = rootPos.above();
        final BlockState branchState = level.getBlockState(branchPos);
        if (!(branchState.getBlock() instanceof BranchBlock branchBlock)) {
            return false;
        }

        final BranchDestructionData destructionData = branchBlock.destroyBranchFromNode(
                level, branchPos, Direction.DOWN, true, null);
        if (destructionData == null || destructionData.species == null || destructionData.woodVolume == null) {
            return false;
        }
        return FallingTreeEntity.dropTree(
                level,
                destructionData,
                destructionData.species.getBranchesDrops(level, destructionData.woodVolume),
                FallingTreeEntity.DestroyType.HARVEST) != null;
    }

    private static boolean isRootyBlock(final Block block) {
        return block instanceof RootyBlock;
    }

    private static boolean isSupported(final LevelAccessor level, final BlockPos pos) {
        final BlockPos belowPos = pos.below();
        final BlockState belowState = level.getBlockState(belowPos);
        return belowState.isFaceSturdy(level, belowPos, Direction.UP);
    }

    private static void enqueueChunkSweep(final ServerLevel level, final LevelChunk chunk) {
        final QueuedChunkSweep sweep = new QueuedChunkSweep(level.dimension(), chunk.getPos().x, chunk.getPos().z);
        if (PENDING_CHUNK_SWEEP_KEYS.add(sweep)) {
            PENDING_CHUNK_SWEEPS.offer(sweep);
        }
    }

    private record QueuedChunkSweep(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        private QueuedChunkSweep {
            Objects.requireNonNull(dimension, "dimension");
        }
    }
}
