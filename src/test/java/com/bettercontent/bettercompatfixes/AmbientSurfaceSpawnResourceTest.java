package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class AmbientSurfaceSpawnResourceTest {
    private static final Set<String> EXPECTED_SURFACES = Set.of(
            "minecraft:grass_block",
            "unearthed:beige_limestone_grassy_regolith",
            "unearthed:conglomerate_grassy_regolith",
            "unearthed:dolomite_grassy_regolith",
            "unearthed:gabbro_grassy_regolith",
            "unearthed:granodiorite_grassy_regolith",
            "unearthed:grey_limestone_grassy_regolith",
            "unearthed:kimberlite_grassy_regolith",
            "unearthed:limestone_grassy_regolith",
            "unearthed:mudstone_grassy_regolith",
            "unearthed:overgrown_andesite",
            "unearthed:overgrown_diorite",
            "unearthed:overgrown_granite",
            "unearthed:phyllite_grassy_regolith",
            "unearthed:quartzite_grassy_regolith",
            "unearthed:rhyolite_grassy_regolith",
            "unearthed:sandstone_grassy_regolith",
            "unearthed:siltstone_grassy_regolith",
            "unearthed:slate_grassy_regolith",
            "unearthed:stone_grassy_regolith",
            "unearthed:white_granite_grassy_regolith");

    @Test
    void deniedSurfaceTagContainsExactlyTheOverworldGrassGroundSet() throws IOException {
        final Path path = Path.of(
                "src/main/resources/data/better_compat_fixes/tags/blocks/ambient_spawn_denied_surfaces.json");
        final JsonObject root = JsonParser.parseReader(Files.newBufferedReader(path)).getAsJsonObject();
        final JsonArray values = root.getAsJsonArray("values");
        final Set<String> actual = new LinkedHashSet<>();

        for (JsonElement value : values) {
            if (value.isJsonPrimitive()) {
                actual.add(value.getAsString());
                continue;
            }
            final JsonObject optionalEntry = value.getAsJsonObject();
            final String id = optionalEntry.get("id").getAsString();
            assertFalse(optionalEntry.get("required").getAsBoolean(), id + " must remain optional");
            actual.add(id);
        }

        assertEquals(EXPECTED_SURFACES, actual);
        assertEquals(EXPECTED_SURFACES.size(), values.size(), "duplicate denied-surface entries");
    }
}
