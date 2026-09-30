package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FalloutStructurePlacementResourceTest {
    private static final Path MIXIN = Path.of(
            "src/main/java/com/bettercontent/bettercompatfixes/mixin/falloutwastelands/StructureFeatureMixin.java");

    @Test
    void commonMixinRedirectsOnlyFalloutsTemplatePlacementCall() throws IOException {
        final JsonObject config = JsonParser.parseReader(Files.newBufferedReader(
                Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();
        final String source = Files.readString(MIXIN);
        final String joinedSource = source.replaceAll("\"\\s*\\+\\s*\"", "");

        assertTrue(config.getAsJsonArray("mixins").toString()
                .contains("falloutwastelands.StructureFeatureMixin"));
        assertTrue(source.contains("net.mcreator.falloutwastelands.world.features.StructureFeature"));
        assertTrue(source.contains("method = \"m_142674_\"")
                && joinedSource.contains("StructureTemplate;m_230328_"));
        assertTrue(source.contains("level instanceof WorldGenRegion region")
                && source.contains("region.getCenter()")
                && source.contains("return false;")
                && source.contains("require = 1"));
    }

    @Test
    void optionalTargetIsPinnedToTheInspectedFalloutArtifact() throws IOException {
        final String plugin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));
        final String manifest = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        final String build = Files.readString(Path.of("build.gradle.kts"));

        assertTrue(plugin.contains("hasVersion(mods, \"fallout_wastelands_\", \"1.0.0\")"));
        assertTrue(manifest.contains("modId=\"fallout_wastelands_\"")
                && manifest.contains("versionRange=\"[1.0.0]\""));
        assertTrue(build.contains("compileOnly(fg.deobf(\"curse.maven:fallout-wastelands-431248:7127023\"))"));
    }
}
