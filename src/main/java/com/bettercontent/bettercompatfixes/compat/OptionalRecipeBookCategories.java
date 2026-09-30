package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public final class OptionalRecipeBookCategories {
    public static final Map<String, List<ResourceLocation>> RECIPE_TYPES = Map.of(
            "farmersrespite", List.of(
                    new ResourceLocation("farmersrespite", "brewing"),
                    new ResourceLocation("farmersrespite", "kettle_pouring")
            ),
            "ae2", List.of(new ResourceLocation("ae2", "charger")),
            "brewinandchewin", List.of(new ResourceLocation("brewinandchewin", "fermenting"))
    );

    private OptionalRecipeBookCategories() {
    }
}
