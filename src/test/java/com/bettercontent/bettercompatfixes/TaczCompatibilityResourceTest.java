package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class TaczCompatibilityResourceTest {
    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/bettercontent/bettercompatfixes");

    @Test
    void taczRecoveryIsClientOnlyAndVersionGated() throws IOException {
        final var config = JsonParser.parseReader(Files.newBufferedReader(
                Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();
        final String client = config.getAsJsonArray("client").toString();
        assertTrue(client.contains("tacz.ClientIndexManagerMixin"));
        assertTrue(client.contains("tacz.ClientAssetsManagerAccessor"));

        final String plugin = Files.readString(SOURCE_ROOT.resolve("mixin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("hasVersion(mods, \"tacz\", \"1.20.1-1.1.8-hotfix\")"));
    }

    @Test
    void buildPinsTheInspectedTaczArtifactAndRecoveryUsesNativeDisplayInitialization() throws IOException {
        final String build = Files.readString(Path.of("build.gradle.kts"));
        final String recovery = Files.readString(SOURCE_ROOT.resolve("compat/tacz/TaczDisplayCacheRecovery.java"));
        assertTrue(build.contains("curse.maven:timeless-and-classics-zero-1028108:8141310"));
        assertTrue(recovery.contains("display/guns"));
        assertTrue(recovery.contains("display/ammo"));
        assertTrue(recovery.contains("display/attachments"));
        assertTrue(recovery.contains("display/blocks"));
        assertTrue(recovery.contains("display.init()"));
        assertTrue(recovery.contains("ClientAssetsManager.GSON"));
        assertTrue(recovery.contains("manager.getAllData().put(displayId, display)"));
    }
}
