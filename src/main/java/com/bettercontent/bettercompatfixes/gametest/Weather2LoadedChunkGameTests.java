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
    public static void seasonalCoroUtilTemperatureUsesNativeBiomeWithoutLoadingTerrain(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(com.corosus.coroutil.util.CoroUtilCompatibility.isSereneSeasonsInstalled(),
                "the actual seasonal CoroUtil branch must be active in this regression");
        var loaded = helper.absolutePos(new BlockPos(2, 1, 2));
        var loadedBiome = level.getBiome(loaded);
        float expected = sereneseasons.season.SeasonHooks.getBiomeTemperature(level, loadedBiome, loaded);
        float actual = com.corosus.coroutil.util.CoroUtilCompatibility.getAdjustedTemperature(level, loadedBiome.value(), loaded);
        helper.assertTrue(Float.compare(expected, actual) == 0, "loaded native seasonal temperature must remain unchanged");
        for (int coordinate : new int[]{20_000_003, -20_000_003}) {
            var pos = new BlockPos(coordinate, 64, coordinate);
            helper.assertTrue(level.getChunkSource().getChunkNow(coordinate >> 4, coordinate >> 4) == null,
                    "far seasonal query fixture must begin absent");
            var biome = Weather2LoadedChunkQueries.biome(level, pos);
            expected = sereneseasons.season.SeasonHooks.getBiomeTemperature(level, biome, pos);
            actual = com.corosus.coroutil.util.CoroUtilCompatibility.getAdjustedTemperature(level, biome.value(), pos);
            helper.assertTrue(Float.compare(expected, actual) == 0 && Float.isFinite(actual),
                    "unloaded query must preserve native seasons and noise-biome temperature");
            helper.assertTrue(level.getChunkSource().getChunkNow(coordinate >> 4, coordinate >> 4) == null,
                    "CoroUtil's actual seasonal path must not create/load terrain");
        }
        helper.assertTrue(com.corosus.coroutil.util.CoroUtilCompatibility.isSereneSeasonsInstalled(),
                "the seasonal bridge must not silently fall back after an error");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "daylight_platform", timeoutTicks = 40)
    public static void nativeStormProgressionDoesNotGenerateMissingTerrain(GameTestHelper helper) {
        helper.runAfterDelay(2, () -> {
            var level = helper.getLevel();
            var manager = weather2.ServerTickHandler.getWeatherManagerFor(level.dimension());
            helper.assertTrue(manager != null, "Native Weather2 server manager must be initialized");
            int priorDelay = weather2.config.ConfigStorm.Storm_AllTypes_TickRateDelay;
            double priorRate = weather2.config.ConfigStorm.Storm_TemperatureAdjustRate;
            long priorLastStorm = manager.lastStormFormed;
            try {
                weather2.config.ConfigStorm.Storm_AllTypes_TickRateDelay = 1;
                weather2.config.ConfigStorm.Storm_TemperatureAdjustRate = 0.1;
                assertNativeStormProgression(helper, helper.absolutePos(new BlockPos(2, 1, 2)));
                for (int coordinate : new int[]{25_000_003, -25_000_003}) {
                    var missing = new BlockPos(coordinate, 128, coordinate);
                    helper.assertTrue(level.getChunkSource().getChunkNow(coordinate >> 4, coordinate >> 4) == null,
                            "Storm regression must start outside resident terrain");
                    assertNativeStormProgression(helper, missing);
                    helper.assertTrue(level.getChunkSource().getChunkNow(coordinate >> 4, coordinate >> 4) == null,
                            "Native creation and progression must not request missing terrain");
                }
                helper.succeed();
            } finally {
                weather2.config.ConfigStorm.Storm_AllTypes_TickRateDelay = priorDelay;
                weather2.config.ConfigStorm.Storm_TemperatureAdjustRate = priorRate;
                manager.lastStormFormed = priorLastStorm;
            }
        });
    }

    private static void assertNativeStormProgression(GameTestHelper helper, BlockPos pos) {
        var manager = weather2.ServerTickHandler.getWeatherManagerFor(helper.getLevel().dimension());
        var storm = new weather2.weathersystem.storm.StormObject(manager);
        storm.pos = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        storm.weatherMachineControlled = true;
        storm.canBeDeadly = false;
        storm.initFirstTime();
        storm.levelTemperature = 1000.0F;
        storm.tickProgression();
        helper.assertTrue(Math.abs(storm.levelTemperature - 999.9F) < 0.001F,
                "Native progression must still perform its temperature adjustment");
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
