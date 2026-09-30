package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class OptionalRecipeBookCategoriesTest {
    @Test
    void allKnownUncategorizedRecipeTypesAreCoveredBehindTheirOwningMods() {
        assertEquals(Set.of(
                "farmersrespite:brewing",
                "farmersrespite:kettle_pouring",
                "ae2:charger",
                "brewinandchewin:fermenting"
        ), OptionalRecipeBookCategories.RECIPE_TYPES.values().stream()
                .flatMap(java.util.Collection::stream)
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet()));
    }
}
