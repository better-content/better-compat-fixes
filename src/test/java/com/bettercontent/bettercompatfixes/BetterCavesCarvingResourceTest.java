package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class BetterCavesCarvingResourceTest {
    private static final Path MIXIN = Path.of(
            "src/main/java/com/bettercontent/bettercompatfixes/mixin/bettercaves/AbstractCarverMixin.java");

    @Test
    void commonMixinCancelsTheExactUnsafeCarveBlockOverloadAtEntry() throws IOException {
        final JsonObject config = JsonParser.parseReader(Files.newBufferedReader(
                Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();
        final String source = Files.readString(MIXIN);
        final String joinedSource = source.replaceAll("\"\\s*\\+\\s*\"", "");

        assertTrue(config.getAsJsonArray("mixins").toString().contains("bettercaves.AbstractCarverMixin"));
        assertTrue(joinedSource.contains("BetterCavesWorldCarverConfig;")
                && joinedSource.contains("BlockState;Lnet/minecraft/world/level/block/state/BlockState;")
                && joinedSource.contains("CarvingMask;")
                && joinedSource.contains("Aquifer;)V"));
        assertTrue(source.contains("at = @At(\"HEAD\")")
                && source.contains("cancellable = true")
                && source.contains("require = 1"));
        assertTrue(source.contains("BetterCavesCarvingBounds.contains(chunk, pos)"));
    }

    @Test
    void optionalTargetIsPinnedToTheInspectedBetterCavesRelease() throws IOException {
        final String plugin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));
        final String manifest = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));

        assertTrue(plugin.contains("hasVersion(mods, \"bettercaves\", \"1.20.1-Forge-2.0.6\")"));
        assertTrue(manifest.contains("modId=\"bettercaves\"")
                && manifest.contains("versionRange=\"[1.20.1-Forge-2.0.6]\""));
    }
}
