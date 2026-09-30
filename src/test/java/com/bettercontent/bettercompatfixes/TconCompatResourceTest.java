package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class TconCompatResourceTest {
    @Test
    void craftingStationMixinStaysConditionallyRegistered() throws IOException {
        JsonObject config = JsonParser.parseReader(Files.newBufferedReader(
                Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();

        assertEquals(
                "com.bettercontent.bettercompatfixes.mixin.BetterContentMixinPlugin",
                config.get("plugin").getAsString());
        assertTrue(config.getAsJsonArray("mixins").toString()
                .contains("tconstruct.CraftingStationPolymorphMixin"));
    }

    @Test
    void buildPinsThePackTconAndPolymorphReleases() throws IOException {
        String build = Files.readString(Path.of("build.gradle.kts"));
        String modsToml = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));

        assertTrue(build.contains("curse.maven:mantle-74924:7563777"));
        assertTrue(build.contains("curse.maven:tinkers-construct-74072:7449219"));
        assertTrue(build.contains("curse.maven:polymorph-388800:6450982"));
        assertTrue(modsToml.contains("modId=\"tconstruct\""));
        assertTrue(modsToml.contains("versionRange=\"[3.11.2.166,3.12)\""));
        assertTrue(modsToml.contains("modId=\"polymorph\""));
        assertTrue(modsToml.contains("versionRange=\"[0.49.10,0.50)\""));
    }

    @Test
    void pneumaticJackhammerHeadBridgeIsRegisteredOnlyWithBothProviders() throws IOException {
        final String mixins = Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"));
        final String plugin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));
        final String policy = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/compat/pneumaticcraft/TconJackhammerHeadPolicy.java"));

        assertTrue(mixins.contains("pneumaticcraft.tconhead.JackHammerDrillBitHeadMixin"));
        assertTrue(mixins.contains("pneumaticcraft.tconhead.JackhammerSetupSlotHeadMixin"));
        assertTrue(mixins.contains("pneumaticcraft.tconhead.JackHammerTconHeadMixin"));
        assertTrue(plugin.contains("PNEUMATICCRAFT_TCON_HEAD_MIXIN_PREFIX"));
        assertTrue(plugin.contains("hasMods(mods, \"pneumaticcraft\", \"tconstruct\")"));
        assertTrue(policy.contains("new ResourceLocation(\"tconstruct\", \"pick_head\")"));
        assertTrue(policy.contains("head.save(new CompoundTag())"));
    }
}
