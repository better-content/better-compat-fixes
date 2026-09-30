package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class LostCitiesSectionBoundsResourceTest {
    private static final Path MIXIN = Path.of(
            "src/main/java/com/bettercontent/bettercompatfixes/mixin/lostcities/ChunkDriverMixin.java");

    @Test
    void guardsOnlyOutOfHeightReadsAndWritesAroundTheExactPrivateMethods() throws IOException {
        final JsonObject config = JsonParser.parseReader(Files.newBufferedReader(
                Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();
        final String source = Files.readString(MIXIN);

        assertTrue(config.getAsJsonArray("mixins").toString().contains("lostcities.ChunkDriverMixin"));
        assertTrue(source.contains("mcjty.lostcities.worldgen.ChunkDriver")
                && source.contains("method = \"getBlockSafe\"")
                && source.contains("method = \"setBlock(Lnet/minecraft/core/BlockPos;\"")
                && source.contains("at = @At(\"HEAD\")")
                && source.contains("cancellable = true")
                && source.contains("require = 1"));
        assertTrue(source.contains("!LostCitiesSectionBounds.contains(region, pos)")
                && source.contains("cir.setReturnValue(region.getBlockState(pos))")
                && source.contains("ci.cancel()"));
    }

    @Test
    void optionalTargetIsPinnedToTheInspectedLostCitiesRelease() throws IOException {
        final String plugin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));
        final String manifest = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));

        assertTrue(plugin.contains("LOST_CITIES_SECTION_BOUNDS_MIXIN")
                && plugin.contains("hasVersion(mods, \"lostcities\", \"1.20-7.4.11\")"));
        assertTrue(manifest.contains("modId=\"lostcities\"")
                && manifest.contains("versionRange=\"[1.20-7.4.11]\""));
    }
}
