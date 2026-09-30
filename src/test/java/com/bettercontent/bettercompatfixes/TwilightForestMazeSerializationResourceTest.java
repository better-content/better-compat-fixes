package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class TwilightForestMazeSerializationResourceTest {
    private static final Path MIXIN = Path.of(
            "src/main/java/com/bettercontent/bettercompatfixes/mixin/twilightforest/TFMazeMixin.java");
    private static final Path STRONGHOLD_MIXIN = Path.of(
            "src/main/java/com/bettercontent/bettercompatfixes/mixin/twilightforest/ConquerableStructureMixin.java");

    @Test
    void commonMixinLocksOnlyTheSharedMazeRandomAroundPlacement() throws IOException {
        final JsonObject config = JsonParser.parseReader(Files.newBufferedReader(
                Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();
        final String source = Files.readString(MIXIN);

        assertTrue(config.getAsJsonArray("mixins").toString().contains("twilightforest.TFMazeMixin"));
        assertTrue(source.contains("twilightforest.world.components.structures.TFMaze"));
        assertTrue(source.contains("@Shadow") && source.contains("public RandomSource rand;"));
        assertTrue(source.contains("runSerialized(") && source.contains("rand,"));
        assertTrue(source.contains("original.call(")
                && source.contains("twilightForestSerializeC2meMazePlacement()"));
    }

    @Test
    void optionalTargetIsPinnedToTheInspectedTwilightForestArtifact() throws IOException {
        final String plugin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));
        final String manifest = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        final String build = Files.readString(Path.of("build.gradle.kts"));

        assertTrue(plugin.contains("hasVersion(mods, \"twilightforest\", \"4.3.2508\")"));
        assertTrue(manifest.contains("modId=\"twilightforest\"")
                && manifest.contains("versionRange=\"[4.3.2508]\""));
        assertTrue(build.contains("compileOnly(fg.deobf(\"curse.maven:the-twilight-forest-227639:5468648\"))"));
    }

    @Test
    void knightStrongholdGenerationSerializesSharedStaticPieceState() throws IOException {
        final JsonObject config = JsonParser.parseReader(Files.newBufferedReader(
                Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();
        final String source = Files.readString(STRONGHOLD_MIXIN);
        final String helper = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/compat/TwilightForestMazeSerialization.java"));

        assertTrue(config.getAsJsonArray("mixins").toString().contains("twilightforest.ConquerableStructureMixin"));
        assertTrue(source.contains("twilightforest.world.components.structures.util.ConquerableStructure")
                && source.contains("KnightStrongholdStructure")
                && source.contains("@WrapMethod(method = \"generateCustom\")")
                && source.contains("callStrongholdSerialized"));
        assertTrue(helper.contains("STRONGHOLD_GENERATION_LOCK")
                && helper.contains("callStrongholdSerialized"));
    }
}
