package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class EpicFightBattleModeResourceTest {
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/better_compat_fixes.mixins.json");
    private static final Path MIXIN_ROOT = Path.of(
            "src/main/java/com/bettercontent/bettercompatfixes/mixin/epicfight");

    @Test
    void permanentBattleModeMixinsArePackagedOnTheCorrectSides() throws IOException {
        JsonObject config = JsonParser.parseReader(Files.newBufferedReader(MIXIN_CONFIG)).getAsJsonObject();
        String common = config.getAsJsonArray("mixins").toString();
        String client = config.getAsJsonArray("client").toString();

        assertTrue(common.contains("epicfight.PlayerPatchMixin"));
        assertTrue(common.contains("epicfight.ServerPlayerPatchMixin"));
        assertTrue(client.contains("epicfight.LocalPlayerPatchMixin"));
        assertTrue(client.contains("epicfight.EpicFightKeyMappingsMixin"));
        assertTrue(client.contains("epicfight.EpicFightControlifyEntrypointMixin"));
    }

    @Test
    void everyVanillaModeTransitionIsRedirectedToBattleMode() throws IOException {
        for (String sourceName : new String[]{"PlayerPatchMixin.java", "ServerPlayerPatchMixin.java",
                "LocalPlayerPatchMixin.java"}) {
            String source = Files.readString(MIXIN_ROOT.resolve(sourceName));
            assertTrue(source.contains("method = \"toVanillaMode\""), sourceName);
            assertTrue(source.contains("toEpicFightMode(synchronize)"), sourceName);
            assertTrue(source.contains("ci.cancel()"), sourceName);
        }
    }

    @Test
    void switchModeIsFilteredFromKeyboardAndControllerRegistration() throws IOException {
        String keySource = Files.readString(MIXIN_ROOT.resolve("EpicFightKeyMappingsMixin.java"));
        String controllerSource = Files.readString(MIXIN_ROOT.resolve("EpicFightControlifyEntrypointMixin.java"));
        String pluginSource = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));

        assertTrue(keySource.contains("keyMapping != EpicFightKeyMappings.SWITCH_MODE"));
        assertTrue(controllerSource.contains("action != EpicFightInputAction.SWITCH_MODE"));
        assertTrue(pluginSource.contains("getModFileById(\"epicfight\")"));
        assertTrue(pluginSource.contains("getModFileById(\"controlify\")"));
    }
}
