package com.bettercontent.bettercompatfixes.tconstruct;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Replaces the physical blank pattern as a recipe ingredient, regardless of serializer. */
public final class PatternRecipeSubstitution {
    private static final String PATTERN = "tconstruct:pattern";
    private static final String CANVAS = "farmersdelight:canvas";

    private PatternRecipeSubstitution() {
    }

    public static Map<ResourceLocation, JsonElement> replace(final Map<ResourceLocation, JsonElement> recipes) {
        Map<ResourceLocation, JsonElement> updated = new LinkedHashMap<>();
        recipes.forEach((id, json) -> updated.put(id, replaceIngredient(json)));
        return updated;
    }

    static JsonElement replaceIngredient(final JsonElement element) {
        if (element == null || element.isJsonNull() || element.isJsonPrimitive()) {
            return element == null ? null : element.deepCopy();
        }
        if (element.isJsonArray()) {
            JsonArray result = new JsonArray();
            element.getAsJsonArray().forEach(child -> result.add(replaceIngredient(child)));
            return result;
        }
        JsonObject result = new JsonObject();
        element.getAsJsonObject().entrySet().forEach(entry -> {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (isOutput(key)) {
                result.add(key, value.deepCopy());
            } else if ("item".equals(key) && value.isJsonPrimitive()
                    && value.getAsJsonPrimitive().isString() && PATTERN.equals(value.getAsString())) {
                result.add(key, new JsonPrimitive(CANVAS));
            } else {
                result.add(key, replaceIngredient(value));
            }
        });
        return result;
    }

    private static boolean isOutput(final String key) {
        return "result".equals(key) || "results".equals(key)
                || "output".equals(key) || "outputs".equals(key);
    }
}
