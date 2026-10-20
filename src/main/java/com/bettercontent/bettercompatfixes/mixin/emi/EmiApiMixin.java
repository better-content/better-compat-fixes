package com.bettercontent.bettercompatfixes.mixin.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.screen.RecipeScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EmiApi.class, remap = false)
public abstract class EmiApiMixin {
    @Redirect(method = "displayRecipes", at = @At(value = "INVOKE",
            target = "Ldev/emi/emi/api/EmiApi;focusRecipe(Ldev/emi/emi/api/recipe/EmiRecipe;)V"), require = 1)
    private static void betterCompat$genericOpeningStartsFirst(EmiRecipe preferred) {
        // Generic output lookup starts at the promoted result; explicit focusRecipe is untouched.
    }

    @Redirect(method = "setPages", at = @At(value = "INVOKE",
            target = "Ldev/emi/emi/screen/RecipeScreen;focusCategory(Ldev/emi/emi/api/recipe/EmiRecipeCategory;)V"), require = 1)
    private static void betterCompat$newViewStartsFirst(RecipeScreen screen, EmiRecipeCategory previous) {
        // Do not carry the previous view's category past this new snapshot's first promoted tab.
    }
}
