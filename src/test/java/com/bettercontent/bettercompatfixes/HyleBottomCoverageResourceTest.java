package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class HyleBottomCoverageResourceTest {
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/better_compat_fixes.mixins.json");
    private static final Path STONE_REPLACER_MIXIN =
            Path.of("src/main/java/com/bettercontent/bettercompatfixes/mixin/hyle/StoneReplacerMixin.java");
    private static final Path BIOME_INJECTOR_MIXIN =
            Path.of("src/main/java/com/bettercontent/bettercompatfixes/mixin/hyle/BiomeInjectorMixin.java");

    @Test
    void bottomSectionUsesNearestGeneratedHyleStratum() throws IOException {
        JsonObject config = JsonParser.parseReader(Files.newBufferedReader(MIXIN_CONFIG)).getAsJsonObject();
        JsonArray mixins = config.getAsJsonArray("mixins");
        String source = Files.readString(STONE_REPLACER_MIXIN);

        assertTrue(contains(mixins, "hyle.StoneReplacerMixin"),
                "Hyle bottom-section repair mixin must be enabled");
        assertTrue(source.contains("final int minY = chunk.getMinBuildHeight()"),
                "the repair must derive the actual world bottom");
        assertTrue(source.contains("nearestReplacement(columnTypes, generatedIndex, original)"),
                "the repair must preserve the generated column's nearest valid stratum");
    }

    @Test
    void replacementRunsAfterUndergroundFeatureOutputs() throws IOException {
        JsonObject config = JsonParser.parseReader(Files.newBufferedReader(MIXIN_CONFIG)).getAsJsonObject();
        JsonArray mixins = config.getAsJsonArray("mixins");
        String source = Files.readString(BIOME_INJECTOR_MIXIN);

        assertTrue(contains(mixins, "hyle.BiomeInjectorMixin"),
                "Hyle injection timing mixin must be enabled");
        assertTrue(source.contains("GenerationStep.Decoration.UNDERGROUND_DECORATION"),
                "Hyle must run after underground structures, ores, and decoration outputs");
        assertTrue(source.contains("GenerationStep.Decoration.LOCAL_MODIFICATIONS"),
                "disabling the compatibility option must restore Hyle's upstream timing");
    }

    private static boolean contains(JsonArray values, String expected) {
        for (int index = 0; index < values.size(); index++) {
            if (expected.equals(values.get(index).getAsString())) {
                return true;
            }
        }
        return false;
    }
}
