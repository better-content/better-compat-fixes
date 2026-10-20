package com.bettercontent.bettercompatfixes.mixin.emi;

import com.bettercontent.bettercompatfixes.compat.emi.IngredientOrder;
import com.bettercontent.bettercompatfixes.compat.emi.IngredientOrderSnapshot;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.RecipeTab;
import dev.emi.emi.screen.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Mixin(value = RecipeScreen.class, remap = false)
public abstract class RecipeScreenMixin implements IngredientOrderSnapshot {
    @Shadow private Map<EmiRecipeCategory, List<EmiRecipe>> recipes;
    @Shadow private List<RecipeTab> tabs;
    @Shadow private List<WidgetGroup> currentPage;
    @Shadow private int page;
    @Shadow private int tab;
    @Unique private final Map<EmiRecipe, Boolean> betterCompat$available = new IdentityHashMap<>();
    @Unique private final Map<EmiRecipeCategory, Boolean> betterCompat$categories = new IdentityHashMap<>();

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void betterCompat$snapshot(AbstractContainerScreen<?> old,
            Map<EmiRecipeCategory, List<EmiRecipe>> input, CallbackInfo ci) {
        // EMI owns exposed sources: personal 2x2 and separately opened native backpacks are distinct.
        EmiPlayerInventory inventory = EmiPlayerInventory.of(Minecraft.getInstance().player);
        Map<EmiRecipeCategory, List<EmiRecipe>> ordered = new LinkedHashMap<>();
        input.forEach((category, list) -> {
            List<EmiRecipe> partition = IngredientOrder.availableFirst(list,
                    recipe -> betterCompat$available.computeIfAbsent(recipe, r -> inventory.canCraft(r, 1)));
            ordered.put(category, partition);
            betterCompat$categories.put(category, partition.stream().anyMatch(this::betterCompat$ingredientsAvailable));
        });
        recipes = ordered;
    }

    @Redirect(method = {"init", "m_7856_"}, at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"), require = 1)
    private Stream<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>> betterCompat$categoryOrder(
            Stream<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>> stream,
            Comparator<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>> nativeOrder) {
        // Native category order is the tie-breaker, not hash-map iteration order.
        return stream.sorted(Comparator.<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>, Boolean>comparing(
                e -> !betterCompat$categories.getOrDefault(e.getKey(), false)).thenComparing(nativeOrder));
    }

    @Override public List<EmiRecipeCategory> betterCompat$presentedCategories() {
        return tabs.stream().map(t -> t.category).toList();
    }
    @Override public List<EmiRecipe> betterCompat$orderedRecipes(EmiRecipeCategory category) {
        return List.copyOf(recipes.getOrDefault(category, List.of()));
    }
    @Override public List<EmiRecipe> betterCompat$visibleRecipes() {
        return currentPage.stream().map(group -> group.recipe).toList();
    }
    @Override public int betterCompat$pageIndex() { return page; }
    @Override public int betterCompat$categoryIndex() { return tab; }

    @Override
    public boolean betterCompat$ingredientsAvailable(EmiRecipe recipe) {
        return betterCompat$available.getOrDefault(recipe, false);
    }
}
