package com.bettercontent.bettercompatfixes.mixin.emi;

import com.bettercontent.bettercompatfixes.compat.emi.IngredientOrder;
import com.bettercontent.bettercompatfixes.compat.emi.IngredientOrderSnapshot;
import dev.emi.emi.api.recipe.EmiIngredientRecipe;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.RecipeTab;
import dev.emi.emi.screen.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    @Shadow public AbstractContainerScreen<?> old;
    @Unique private final Map<EmiRecipe, Boolean> betterCompat$available = new IdentityHashMap<>();
    @Unique private final Map<EmiRecipeCategory, Boolean> betterCompat$categories = new IdentityHashMap<>();
    @Unique private Map<EmiRecipeCategory, List<EmiRecipe>> betterCompat$source;
    @Unique private EmiPlayerInventory betterCompat$inventory;
    @Unique private Object betterCompat$menu;
    @Unique private long betterCompat$checkedTick = Long.MIN_VALUE;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void betterCompat$rememberSource(AbstractContainerScreen<?> old,
            Map<EmiRecipeCategory, List<EmiRecipe>> input, CallbackInfo ci) {
        // Never repartition an already partitioned list after a transfer or a resize.
        betterCompat$source = new LinkedHashMap<>();
        input.forEach((category, list) -> betterCompat$source.put(category, List.copyOf(list)));
    }

    @Inject(method = {"init", "m_7856_"}, at = @At("HEAD"), require = 1)
    private void betterCompat$refreshBeforeNativeInit(CallbackInfo ci) {
        betterCompat$refresh(EmiPlayerInventory.of(Minecraft.getInstance().player));
    }

    // Production EMI ships the Forge/SRG name; the development workspace uses render.
    @Inject(method = {"render", "m_88315_"}, at = @At("HEAD"), require = 1)
    private void betterCompat$refreshAfterTransfer(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen != (Object) this || client.player == null) return;
        long tick = client.player.tickCount;
        if (betterCompat$checkedTick == tick) return;
        betterCompat$checkedTick = tick; // at most one inventory comparison and one native rebuild per tick
        if (old != null && old.getMenu() != client.player.containerMenu) return; // obsolete native menu
        EmiPlayerInventory current = EmiPlayerInventory.of(client.player);
        if (betterCompat$inventory == null || betterCompat$menu != client.player.containerMenu
                || !betterCompat$inventory.isEqual(current)) {
            betterCompat$refresh(current);
            // Native init preserves the focused recipe, rebuilds tabs/pages and resets widgets.
            // Calling its full screen initializer (not init() directly) clears old widgets first.
            ((RecipeScreen) (Object) this).init(client, client.getWindow().getGuiScaledWidth(),
                    client.getWindow().getGuiScaledHeight());
        }
    }

    @Unique private void betterCompat$refresh(EmiPlayerInventory inventory) {
        if (betterCompat$source == null) return;
        Minecraft client = Minecraft.getInstance();
        Object menu = client.player == null ? null : client.player.containerMenu;
        if (betterCompat$inventory != null && betterCompat$menu == menu && betterCompat$inventory.isEqual(inventory)) return;
        betterCompat$inventory = inventory;
        betterCompat$menu = menu;
        betterCompat$available.clear();
        betterCompat$categories.clear();
        Map<EmiRecipeCategory, List<EmiRecipe>> ordered = new LinkedHashMap<>();
        betterCompat$source.forEach((category, list) -> {
            if (!betterCompat$promotableCategory(category)) {
                ordered.put(category, list);
                return;
            }
            List<EmiRecipe> partition = IngredientOrder.availableFirst(list, recipe ->
                    betterCompat$available.computeIfAbsent(recipe,
                        r -> betterCompat$promotableRecipe(r) && inventory.canCraft(r, 1)));
            ordered.put(category, partition);
            betterCompat$categories.put(category, partition.stream().anyMatch(this::betterCompat$ingredientsAvailable));
        });
        recipes = ordered;
    }

    @Unique private static boolean betterCompat$promotableCategory(EmiRecipeCategory category) {
        // These are informational displays, not executable crafting categories.
        return category != VanillaEmiRecipeCategories.INFO
                && category != VanillaEmiRecipeCategories.FUEL
                && category != VanillaEmiRecipeCategories.COMPOSTING
                && category != VanillaEmiRecipeCategories.WORLD_INTERACTION;
    }

    @Unique private static boolean betterCompat$promotableRecipe(EmiRecipe recipe) {
        // EMI's canCraft returns true for an empty ingredient list; that is not availability.
        return !(recipe instanceof EmiInfoRecipe) && !(recipe instanceof EmiIngredientRecipe)
                && !recipe.hideCraftable() && recipe.getInputs().stream().anyMatch(input -> !input.isEmpty());
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
