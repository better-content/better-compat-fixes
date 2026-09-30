package com.bettercontent.bettercompatfixes.client;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.OptionalRecipeBookCategories;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterRecipeBookCategoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RecipeBookCategoryCompat {
    private RecipeBookCategoryCompat() {
    }

    @SubscribeEvent
    public static void onRegisterRecipeBookCategories(final RegisterRecipeBookCategoriesEvent event) {
        OptionalRecipeBookCategories.RECIPE_TYPES.forEach((modId, recipeTypes) -> {
            if (!ModList.get().isLoaded(modId)) {
                return;
            }
            recipeTypes.forEach(id -> BuiltInRegistries.RECIPE_TYPE.getOptional(id)
                    .map(type -> (RecipeType<?>) type)
                    .ifPresent(type -> event.registerRecipeCategoryFinder(
                            type,
                            recipe -> RecipeBookCategories.CRAFTING_MISC
                    )));
        });
    }
}
