package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.Weather2LoadedChunkQueries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class Weather2LoadedChunkGameTests {
    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "daylight_platform")
    public static void loadedNativeHeightAndBiomeAreUnchanged(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        for (var type : new Heightmap.Types[]{Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.WORLD_SURFACE}) {
            helper.assertTrue(Weather2LoadedChunkQueries.precipitationHeight(level, pos, type).equals(level.getHeightmapPos(type, pos)), "Loaded precipitation height must preserve native heightmap +1");
        }
        helper.assertTrue(Weather2LoadedChunkQueries.biome(level, pos).equals(level.getBiome(pos)), "Loaded zoomed biome must remain native");
        helper.succeed();
    }
    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "daylight_platform", timeoutTicks = 40)
    public static void unloadedPositiveAndNegativeColumnsNeverCreateChunks(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int coordinate : new int[]{20_000_003, -20_000_003}) {
            var pos = new BlockPos(coordinate, 0, coordinate);
            helper.assertTrue(level.getChunkSource().getChunkNow(coordinate >> 4, coordinate >> 4) == null, "Far column must begin absent");
            var result = Weather2LoadedChunkQueries.precipitationHeight(level, pos, Heightmap.Types.MOTION_BLOCKING);
            helper.assertTrue(result.equals(new BlockPos(coordinate, -255, coordinate)), "Preserve Weather2 unavailable height and exact x/z");
            helper.assertTrue(Weather2LoadedChunkQueries.biome(level, result) != null, "Native noise biome must remain available without terrain generation");
            helper.assertTrue(level.getChunkSource().getChunkNow(coordinate >> 4, coordinate >> 4) == null, "Weather observation must not synchronously generate/load the column");
        }
        helper.succeed();
    }
}
