package com.bettercontent.bettercompatfixes;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class Weather2LoadedChunkResourceTest {
    private static final Path MAIN = Path.of("src/main/java/com/bettercontent/bettercompatfixes");
    @Test void readsUseOnlyResidentChunksAndNativeNoiseFallback() throws Exception {
        String source = Files.readString(MAIN.resolve("compat/Weather2LoadedChunkQueries.java"));
        assertTrue(source.contains("getChunkNow"));
        assertTrue(source.contains("getHeight(type, pos.getX() & 15, pos.getZ() & 15) + 1"));
        assertTrue(source.contains("chunk == null ? -255"));
        assertTrue(source.contains("withDifferentSource"));
        assertTrue(source.contains("getUncachedNoiseBiome"));
        for (String forbidden : new String[]{"getChunk(", "hasChunkAt", "getChunkFuture", "addRegionTicket", "join(", "getHeightmapPos("}) assertFalse(source.contains(forbidden), forbidden);
    }
    @Test void serverOnlyHooksPreserveStormLifecycleAndAreVersionPinned() throws Exception {
        String height = Files.readString(MAIN.resolve("mixin/weather2/WeatherUtilBlockMixin.java"));
        String storm = Files.readString(MAIN.resolve("mixin/weather2/StormObjectMixin.java"));
        String plugin = Files.readString(MAIN.resolve("mixin/BetterContentMixinPlugin.java"));
        String config = Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"));
        assertTrue(height.contains("world instanceof ServerLevel"));
        assertTrue(height.contains("Heightmap$Types;"));
        assertTrue(storm.contains("original.call(level, pos)"));
        assertTrue(storm.contains("require = 1"));
        assertTrue(storm.contains("Level;getBiome("));
        assertTrue(storm.contains("Level;m_204166_("));
        assertFalse(storm.contains("remap = true"));
        assertTrue(plugin.contains("hasVersion(mods, \"weather2\", \"1.20.1-2.8.3\")"));
        assertTrue(config.contains("weather2.WeatherUtilBlockMixin"));
        assertTrue(config.contains("weather2.StormObjectMixin"));
        assertFalse(storm.contains("cancel"));
        assertFalse(storm.contains("trySpawnStorm"));
    }
}
