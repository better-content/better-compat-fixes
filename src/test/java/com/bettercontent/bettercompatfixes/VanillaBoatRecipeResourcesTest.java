package com.bettercontent.bettercompatfixes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaBoatRecipeResourcesTest {
    private static final Path RECIPES = Path.of("src/main/resources/data/minecraft/recipes");
    private static final List<String> WOODS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo");

    @Test
    void everyVanillaWoodFamilyHasOneTconstructReinforcedBoatAndChestRecipe() throws IOException {
        for (String wood : WOODS) {
            final String boat = wood.equals("bamboo") ? "bamboo_raft" : wood + "_boat";
            final String chestBoat = wood.equals("bamboo") ? "bamboo_chest_raft" : wood + "_chest_boat";
            assertBoatRecipe(wood, boat);
            assertChestBoatRecipe(boat, chestBoat);
        }
    }

    private static void assertBoatRecipe(final String wood, final String boat) throws IOException {
        final JsonObject recipe = activeRecipe(boat);
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("minecraft:" + boat, recipe.getAsJsonObject("result").get("item").getAsString());
        assertEquals("minecraft:" + wood + "_planks", recipe.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString());
        final JsonObject handle = recipe.getAsJsonObject("key").getAsJsonObject("H");
        assertEquals("tconstruct:material", handle.get("type").getAsString());
        assertEquals("tconstruct:tool_handle", handle.get("item").getAsString());
    }

    private static void assertChestBoatRecipe(final String boat, final String chestBoat) throws IOException {
        final JsonObject recipe = activeRecipe(chestBoat);
        assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
        assertEquals("minecraft:" + chestBoat, recipe.getAsJsonObject("result").get("item").getAsString());
        final JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        assertTrue(ingredients.asList().stream()
                .map(element -> element.getAsJsonObject())
                .anyMatch(element -> element.has("item") && element.get("item").getAsString().equals("minecraft:" + boat)));
        assertEquals(2L, ingredients.asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(element -> element.has("type") && element.get("type").getAsString().equals("tconstruct:material"))
                .filter(element -> element.get("item").getAsString().equals("tconstruct:tool_binding"))
                .count());
    }

    private static JsonObject activeRecipe(final String id) throws IOException {
        final JsonObject root = JsonParser.parseString(Files.readString(RECIPES.resolve(id + ".json"))).getAsJsonObject();
        assertEquals("forge:conditional", root.get("type").getAsString());
        final JsonObject entry = root.getAsJsonArray("recipes").get(0).getAsJsonObject();
        assertEquals("forge:mod_loaded", entry.getAsJsonArray("conditions").get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("tconstruct", entry.getAsJsonArray("conditions").get(0).getAsJsonObject().get("modid").getAsString());
        return entry.getAsJsonObject("recipe");
    }
}
