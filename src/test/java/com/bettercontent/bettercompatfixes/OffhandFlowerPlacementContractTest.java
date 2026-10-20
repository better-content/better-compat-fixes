package com.bettercontent.bettercompatfixes;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source/fixture contracts only; the GameTests own actual native interaction observations. */
class OffhandFlowerPlacementContractTest {
    @Test void offhandAndBonemealUseTheSameExplicitClassificationWithoutForcingAdmission() throws Exception {
        Path compat = Path.of("src/main/java/com/bettercontent/bettercompatfixes/compat");
        String policy = Files.readString(compat.resolve("DecorativeFlowerPolicy.java"));
        String offhand = Files.readString(compat.resolve("OffhandBlockPlacement.java"));
        String bonemeal = Files.readString(compat.resolve("FlowerBonemealGuard.java"));
        assertTrue(policy.contains("new ResourceLocation(\"better_bumblezone_crops\", \"decorative_flowers\")"));
        assertFalse(policy.contains("BlockTags.FLOWERS"));
        assertTrue(offhand.contains("event.getHand() == InteractionHand.OFF_HAND"));
        assertTrue(offhand.contains("instanceof BlockItem blockItem"));
        assertTrue(offhand.contains("!DecorativeFlowerPolicy.isDecorative(blockItem.getBlock().defaultBlockState())"));
        assertTrue(offhand.contains("event.setUseItem(Event.Result.DENY)"));
        assertFalse(offhand.contains("Event.Result.ALLOW"));
        assertTrue(bonemeal.contains("DecorativeFlowerPolicy.isDecorative(event.getBlock())"));
    }

    @Test void standaloneNativeFixtureContainsOnlyDeclaredRepresentativeFlowers() throws Exception {
        var tag = JsonParser.parseString(Files.readString(Path.of(
            "src/nativeFixture/resources/data/better_bumblezone_crops/tags/blocks/decorative_flowers.json"))).getAsJsonObject();
        Set<String> values = new HashSet<>();
        tag.getAsJsonArray("values").forEach(v -> values.add(v.getAsString()));
        assertEquals(Set.of("minecraft:dandelion", "minecraft:sunflower", "minecraft:pink_petals", "minecraft:spore_blossom"), values);
        assertFalse(values.contains("minecraft:cobblestone"));
        assertFalse(values.contains("minecraft:torch"));
        assertFalse(values.contains("minecraft:wheat"));
        assertFalse(values.contains("minecraft:flowering_azalea"));
        assertFalse(Files.exists(Path.of("src/main/resources/data/better_bumblezone_crops/tags/blocks/decorative_flowers.json")),
            "Compat must not ship a duplicate of the Cultivars-owned flower classification");
    }
}
