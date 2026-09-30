package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class StructureGenerationImproverUnearthedResourceTest {
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/better_compat_fixes.mixins.json");
    private static final Path SGI_CHUNK_GENERATOR_MIXIN =
            Path.of("src/main/java/com/bettercontent/bettercompatfixes/mixin/sgi/ChunkGeneratorMixin.java");
    private static final Path SGI_TERRAIN_CONFORM_MIXIN =
            Path.of("src/main/java/com/bettercontent/bettercompatfixes/mixin/sgi/TerrainConformUtilMixin.java");

    @Test
    void sgiSurfaceConformingRunsAfterHyleDecoration() throws IOException {
        JsonObject config = JsonParser.parseReader(Files.newBufferedReader(MIXIN_CONFIG)).getAsJsonObject();
        JsonArray mixins = config.getAsJsonArray("mixins");
        String source = Files.readString(SGI_CHUNK_GENERATOR_MIXIN);
        String postPassSource = Files.readString(SGI_TERRAIN_CONFORM_MIXIN);

        assertTrue(contains(mixins, "sgi.ChunkGeneratorMixin"),
                "SGI must defer its terrain conform pass until decoration has finished");
        assertTrue(contains(mixins, "sgi.TerrainConformUtilMixin"),
                "SGI must translate late rock writes to the existing Hyle/Unearthed palette");
        assertTrue(source.contains("TerrainConformUtil;applyDuringSurface"),
                "the SGI surface conform invocation must be redirected");
        assertTrue(source.contains("@At(\"TAIL\")"),
                "the deferred SGI pass must execute after Hyle/Unearthed decoration");
        assertTrue(source.contains("TerrainConformUtil.applyDuringSurface"),
                "the deferred SGI pass must use the pinned public API at decoration tail");
        assertTrue(postPassSource.contains("@WrapMethod(method = \"applyDuringSurface\", remap = false)"),
                "the translation context must cover exactly one SGI conform invocation");
        assertTrue(postPassSource.contains("new ThreadLocal<>()")
                        && postPassSource.contains("BCFIXES_PALETTE_PROBE.remove()"),
                "parallel generation workers must have isolated, promptly cleared translation context");
        assertTrue(postPassSource.contains("method = \"applySingleSlopeWithOgCompare\"")
                        && postPassSource.contains("method = \"fillVerticalCavities\"")
                        && postPassSource.contains("method = \"placeSlopeFill\"")
                        && postPassSource.contains("method = \"applySurfaceLayer\"")
                        && postPassSource.contains("method = \"convertOrganicBelowIfNeeded\""),
                "every audited SGI helper that writes chunk blocks must be wrapped");
        assertTrue(countOccurrences(postPassSource, "ChunkAccess;m_6978_") == 5,
                "each audited helper must target the production SRG ChunkAccess write method");
        assertTrue(postPassSource.contains("require = 1")
                        && postPassSource.contains("require = 2")
                        && postPassSource.contains("require = 3")
                        && postPassSource.contains("allow = 1")
                        && postPassSource.contains("allow = 2")
                        && postPassSource.contains("allow = 3"),
                "audited write counts must fail loudly when the supported SGI version changes");
        assertTrue(postPassSource.contains("Blocks.STONE") && postPassSource.contains("Blocks.GRANITE")
                        && postPassSource.contains("Blocks.DIORITE") && postPassSource.contains("Blocks.ANDESITE")
                        && postPassSource.contains("Blocks.DEEPSLATE") && postPassSource.contains("Blocks.TUFF"),
                "direct translation must cover every audited vanilla rock host");
        assertTrue(postPassSource.contains("BCFIXES_MAX_VERTICAL_PALETTE_SEARCH = 64")
                        && postPassSource.contains("distance <= BCFIXES_MAX_VERTICAL_PALETTE_SEARCH"),
                "direct translation must keep its same-column palette search bounded");
        assertTrue(postPassSource.contains("!path.contains(\"regolith\")")
                        && postPassSource.contains("!path.contains(\"overgrown\")"),
                "direct translation must select stone strata rather than surface soil states");
        assertFalse(postPassSource.contains("stoneReplacer.place")
                        || postPassSource.contains("replaceResidualVanillaRock")
                        || postPassSource.contains("localX < 16")
                        || postPassSource.contains("localZ < 16"),
                "SGI compatibility must not rerun Hyle or scan every block in the chunk");
    }

    private static boolean contains(JsonArray values, String expected) {
        for (int index = 0; index < values.size(); index++) {
            if (expected.equals(values.get(index).getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
