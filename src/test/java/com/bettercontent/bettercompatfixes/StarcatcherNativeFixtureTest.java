package com.bettercontent.bettercompatfixes;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StarcatcherNativeFixtureTest {
    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    @Test void pinnedNativeDefectIsReplacedOnlyByExactAuthoredDisabledRecipeInSourceFixture() throws Exception {
        var nativeJar = Path.of(System.getProperty("bc.starcatcher.native"));
        assertEquals("caac4b98df2e0d1512b07e08546862e36fd68245b746093e6fbae135b50011d1", sha256(Files.readAllBytes(nativeJar)));
        try (var zip = new ZipFile(nativeJar.toFile())) {
            var recipe = JsonParser.parseString(new String(zip.getInputStream(
                    zip.getEntry("data/starcatcher/recipes/aquaculture_fillet.json")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(recipe.has("forge:conditions"), "Pinned native defect must remain explicitly identified");
            assertFalse(recipe.has("conditions"), "Forge only recognizes the unprefixed conditions key");
            assertEquals("aquaculture:fish_fillet_raw", recipe.getAsJsonObject("result").get("item").getAsString());
        }
        var authored = Files.readAllBytes(Path.of("../../better-content-modpack/kubejs/data/starcatcher/recipes/aquaculture_fillet.json"));
        assertEquals("ac7bd6b6a6625a7f036590d6109c972b94e528886556b1b750718344a791c0d3", sha256(authored),
                "Do not silently resurrect the pack's deliberately disabled fillet recipe");
        assertArrayEquals(authored, Files.readAllBytes(Path.of("build/resources/nativeFixture/data/starcatcher/recipes/aquaculture_fillet.json")));
        assertFalse(Files.exists(Path.of("build/resources/main/data/starcatcher/recipes/aquaculture_fillet.json")),
                "Fixture resources must not enter the production source set");
    }
}
