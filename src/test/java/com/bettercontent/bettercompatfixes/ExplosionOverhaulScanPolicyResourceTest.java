package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ExplosionOverhaulScanPolicyResourceTest {
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/better_compat_fixes.mixins.json");
    private static final Path MIXIN_ROOT = Path.of(
            "src/main/java/com/bettercontent/bettercompatfixes/mixin/explosionoverhaul");

    @Test
    void affirmativePromptMixinsAreClientOnlyAndPackaged() throws IOException {
        JsonObject config = JsonParser.parseReader(Files.newBufferedReader(MIXIN_CONFIG)).getAsJsonObject();
        String common = config.getAsJsonArray("mixins").toString();
        String client = config.getAsJsonArray("client").toString();

        assertTrue(!common.contains("explosionoverhaul.ScanPromptPacketMixin"));
        assertTrue(!common.contains("explosionoverhaul.ScanLoadPromptPacketMixin"));
        assertTrue(client.contains("explosionoverhaul.ScanPromptPacketMixin"));
        assertTrue(client.contains("explosionoverhaul.ScanLoadPromptPacketMixin"));
    }

    @Test
    void bothPromptsStayHiddenAndSendTheAffirmativePacket() throws IOException {
        String scan = Files.readString(MIXIN_ROOT.resolve("ScanPromptPacketMixin.java"));
        String load = Files.readString(MIXIN_ROOT.resolve("ScanLoadPromptPacketMixin.java"));

        assertTrue(scan.contains("ScanPromptHUD.setVisible(false)"));
        assertTrue(scan.contains("ScanInfoHUD.setVisible(false)"));
        assertTrue(scan.contains("new ScanControlPacket(true)"));
        assertTrue(load.contains("ScanLoadPromptHUD.setVisible(false)"));
        assertTrue(load.contains("ScanLoadInfoHUD.setVisible(false)"));
        assertTrue(load.contains("new ScanLoadControlPacket(true)"));
    }

    @Test
    void optionalTargetIsPinnedToTheInspectedExplosionOverhaulVersion() throws IOException {
        String plugin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));

        assertTrue(plugin.contains("hasVersion(mods, \"explosionoverhaul\", \"0.2.3.0-forge\")"));
    }
}
