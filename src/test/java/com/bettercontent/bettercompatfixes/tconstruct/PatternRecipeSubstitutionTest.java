package com.bettercontent.bettercompatfixes.tconstruct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

final class PatternRecipeSubstitutionTest {
    @Test
    void replacesStandardCraftingAndSpecialSerializerInputsWithoutChangingPatternOutput() {
        var shaped = PatternRecipeSubstitution.replaceIngredient(JsonParser.parseString("""
                {"type":"mantle:crafting_shaped_retextured","key":{"p":{"item":"tconstruct:pattern"}},
                 "pattern":["pp"],"result":{"item":"tconstruct:part_builder"}}
                """));
        assertEquals("farmersdelight:canvas", shaped.getAsJsonObject().getAsJsonObject("key")
                .getAsJsonObject("p").get("item").getAsString());

        var cast = PatternRecipeSubstitution.replaceIngredient(JsonParser.parseString("""
                {"type":"tconstruct:casting_table","cast":{"item":"tconstruct:pattern"},
                 "result":{"item":"tconstruct:pattern"}}
                """));
        assertEquals("farmersdelight:canvas", cast.getAsJsonObject().getAsJsonObject("cast")
                .get("item").getAsString());
        assertEquals("tconstruct:pattern", cast.getAsJsonObject().getAsJsonObject("result")
                .get("item").getAsString());

        var partBuilder = PatternRecipeSubstitution.replaceIngredient(JsonParser.parseString("""
                {"type":"tconstruct:part_builder","pattern_item":[{"tag":"tconstruct:patterns/default"}],
                 "result":{"item":"tconstruct:pick_head"}}
                """));
        assertTrue(partBuilder.toString().contains("tconstruct:patterns/default"));
    }
}
