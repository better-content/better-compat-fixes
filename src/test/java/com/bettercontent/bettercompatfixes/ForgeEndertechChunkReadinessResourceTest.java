package com.bettercontent.bettercompatfixes;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForgeEndertechChunkReadinessResourceTest {
    private static final Path MAIN = Path.of("src/main/java/com/bettercontent/bettercompatfixes");
    @Test void notificationUsesResidentChunksWithoutReplacingNativeTickLifecycle() throws Exception {
        String hook = Files.readString(MAIN.resolve("mixin/forgeendertech/WorldDataChunkReadinessMixin.java"));
        assertTrue(hook.contains("ChunkSource;hasChunk(II)Z") && hook.contains("ChunkSource;m_5563_(II)Z"));
        assertTrue(hook.contains("ServerLevel;getChunk(II)") && hook.contains("ServerLevel;m_6325_(II)"));
        assertTrue(hook.contains("server.getChunkNow(x, z) != null"));
        assertTrue(hook.contains("level.getChunkSource().getChunkNow(x, z)"));
        assertTrue(hook.contains("original.call(source, x, z)"));
        assertEquals(2, hook.split("require = 1", -1).length - 1);
        for (String forbidden : new String[]{"@Inject", "cancel", "scheduledExplosions", "freshlyLoadedChunks", "getChunkFuture", "addRegionTicket", "original.call(level"}) assertFalse(hook.contains(forbidden), forbidden);
    }
    @Test void versionPinnedNativeStateRegressionIsRegisteredAndPackaged() throws Exception {
        String plugin = Files.readString(MAIN.resolve("mixin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("hasVersion(mods, \"forgeendertech\", \"11.1.10.2\")"));
        String config = Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"));
        assertTrue(config.contains("forgeendertech.WorldDataChunkReadinessMixin") && config.contains("forgeendertech.WorldDataAccessor"));
        String entry = Files.readString(MAIN.resolve("BetterContentFixes.java"));
        assertTrue(entry.contains("if (ModList.get().isLoaded(\"forgeendertech\")) {\n            event.register(ForgeEndertechChunkReadyGameTests.class);\n        }"),
                "consumer test classpaths without ForgeEndertech must not resolve its native event regression");
        String tests = Files.readString(MAIN.resolve("gametest/ForgeEndertechChunkReadyGameTests.java"));
        assertTrue(tests.contains("GameWorld.WorldData.onLevelTick(tick)"));
        assertTrue(tests.contains("delivered.get() == 1"));
        assertTrue(tests.contains("pending.containsKey(missing)") && tests.contains("level.getGameTime() - 100"));
        assertFalse(tests.contains("java.lang.reflect"));
    }
}
