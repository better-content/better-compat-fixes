package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/** Weather observations must not turn a resident/in-flight chunk into a synchronous FULL request. */
public final class Weather2LoadedChunkQueries {
    private Weather2LoadedChunkQueries() {}

    public static BlockPos precipitationHeight(ServerLevel level, BlockPos pos, Heightmap.Types type) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        // Retain Weather2's existing unavailable-column sentinel. Loaded heightmaps
        // have vanilla's +1 surface convention, including negative coordinates.
        int height = chunk == null ? -255 : chunk.getHeight(type, pos.getX() & 15, pos.getZ() & 15) + 1;
        return new BlockPos(pos.getX(), height, pos.getZ());
    }

    public static Holder<Biome> biome(ServerLevel level, BlockPos pos) {
        // Keep vanilla biome zoom and seed, native loaded biome data, and the
        // vanilla uncached-noise fallback. Neither branch creates a chunk ticket.
        return level.getBiomeManager().withDifferentSource((x, y, z) -> {
            var chunk = level.getChunkSource().getChunkNow(QuartPos.toSection(x), QuartPos.toSection(z));
            return chunk == null ? level.getUncachedNoiseBiome(x, y, z) : chunk.getNoiseBiome(x, y, z);
        }).getBiome(pos);
    }
}
