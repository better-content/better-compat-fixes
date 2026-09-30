package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class CreateSifterCompatibilityResourceTest {
    @Test
    void sifterCompatibilityIsExactVersionGatedAndLossless() throws IOException {
        final String plugin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("hasVersion(mods, \"createsifter\", \"1.20.1-1.8.6-6.0.6\")"));

        final String outputMixin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/createsifter/SifterBlockEntityMixin.java"));
        assertTrue(outputMixin.contains("ItemHandlerHelper.insertItemStacked"));
        assertTrue(outputMixin.contains("Containers.dropItemStack"));

        final String configMixin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/createsifter/BrassSifterConfigMixin.java"));
        assertTrue(configMixin.contains("return \"itemsPerCycle\""));
        assertTrue(configMixin.contains("return 16"));

        final var mixins = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject().getAsJsonArray("mixins");
        assertTrue(mixins.toString().contains("createsifter.BrassSifterConfigMixin"));
        assertTrue(mixins.toString().contains("createsifter.SifterBlockEntityMixin"));
    }
}
