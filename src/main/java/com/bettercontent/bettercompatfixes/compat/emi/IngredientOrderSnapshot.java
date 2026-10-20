package com.bettercontent.bettercompatfixes.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import java.util.List;

/** Read-only opening-time result; native recipe screen and transfer handlers retain authority. */
public interface IngredientOrderSnapshot {
    boolean betterCompat$ingredientsAvailable(EmiRecipe recipe);
    /** Actual native baked tab order, not the input map's insertion order. */
    List<EmiRecipeCategory> betterCompat$presentedCategories();
    List<EmiRecipe> betterCompat$orderedRecipes(EmiRecipeCategory category);
    /** Actual native current-page widget groups; an upstream render-error group can have null recipe. */
    List<EmiRecipe> betterCompat$visibleRecipes();
    int betterCompat$pageIndex();
    int betterCompat$categoryIndex();
}
