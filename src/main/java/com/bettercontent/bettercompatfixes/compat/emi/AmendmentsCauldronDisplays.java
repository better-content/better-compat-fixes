package com.bettercontent.bettercompatfixes.compat.emi;

import com.mojang.logging.LogUtils;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import net.mehvahdjukaar.amendments.common.recipe.CauldronRecipe;
import net.mehvahdjukaar.amendments.common.recipe.CauldronRecipeUtils;
import net.mehvahdjukaar.amendments.configs.CommonConfigs;
import net.mehvahdjukaar.amendments.reg.ModTags;
import net.mehvahdjukaar.moonlight.api.fluids.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Enumerates candidates, then asks the pinned native evaluator what actually crafts.
 * Never turns an arbitrary fluid-containing grid recipe into a claimed cauldron recipe.
 * All stacks are copies: registration neither touches a block nor spends player resources.
 */
final class AmendmentsCauldronDisplays {
    private static final int COMBINATION_LIMIT = 16384;
    private AmendmentsCauldronDisplays() {}

    static void register(EmiRegistry registry) {
        registry.addIngredientSerializer(CauldronFluidStack.class, new CauldronFluidStack.Serializer());
        var level = Minecraft.getInstance().level;
        // The pinned upstream checks HAND_CRAFTING even on its thrown-item path.
        if (level == null || !CommonConfigs.LIQUID_CAULDRON.get() || !CommonConfigs.CAULDRON_HAND_CRAFTING.get()) return;
        var category = new EmiRecipeCategory(id("cauldron_crafting"), EmiStack.of(Items.CAULDRON));
        registry.addCategory(category);
        registry.addWorkstation(category, EmiStack.of(Items.CAULDRON));
        var shown = new HashSet<String>();
        var tested = new HashSet<String>();
        var fluids = new LinkedHashMap<String, SoftFluidStack>();
        for (var holder : SoftFluidRegistry.getRegistry(level.registryAccess()).holders().toList()) {
            var fluid = SoftFluidStack.of(holder, 1);
            if (!fluid.isEmpty()) fluids.put(fluid.toString(), fluid);
        }
        // Includes ingredient-specific potion/other NBT rather than only bare fluid identities.
        for (var recipe : registry.getRecipeManager().getRecipes()) {
            for (var ingredient : recipe.getIngredients()) for (var item : ingredient.getItems()) {
                var pair = SoftFluidStack.fromItem(item.copy());
                if (pair != null && !pair.getFirst().isEmpty()) {
                    var fluid = pair.getFirst().copyWithCount(1);
                    fluids.put(fluid.save(new CompoundTag()).toString(), fluid);
                }
            }
        }
        for (var recipe : registry.getRecipeManager().getRecipes().stream()
                .sorted(Comparator.comparing(r -> r.getId().toString())).toList()) {
            var ingredients = recipe.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
            if (ingredients.isEmpty() || ingredients.size() > 9) continue;
            if (recipe instanceof CauldronRecipe) {
                var options = ingredients.stream().map(i -> List.of(i.getItems())).toList();
                boolean complete = RecipeCombinations.visit(options, COMBINATION_LIMIT, inputs -> {
                    for (var fluid : fluids.values()) evaluate(registry, category, level, fluid, inputs, shown, tested);
                });
                if (!complete) warn(recipe);
            } else if (recipe instanceof CraftingRecipe && !(recipe instanceof CustomRecipe)) {
                for (int slot = 0; slot < ingredients.size(); slot++) {
                    var others = new ArrayList<List<ItemStack>>();
                    for (int i = 0; i < ingredients.size(); i++) if (i != slot) others.add(List.of(ingredients.get(i).getItems()));
                    if (others.isEmpty()) continue;
                    for (var filled : ingredients.get(slot).getItems()) {
                        var pair = SoftFluidStack.fromItem(filled.copy());
                        // Amendments also substitutes boiling water for an empty bowl when
                        // making a bowl-based food fluid (its native dummy water-bowl category).
                        var fluid = filled.is(Items.BOWL) ? SoftFluidStack.of(BuiltInSoftFluids.WATER.getHolder(), 1)
                                : pair == null ? SoftFluidStack.empty() : pair.getFirst();
                        if (fluid.isEmpty()) continue;
                        boolean complete = RecipeCombinations.visit(others, COMBINATION_LIMIT,
                                inputs -> evaluate(registry, category, level, fluid, inputs, shown, tested));
                        if (!complete) warn(recipe);
                    }
                }
            }
        }
        LogUtils.getLogger().info("Registered {} native-validated Amendments cauldron examples", shown.size());
    }

    private static void warn(Recipe<?> recipe) {
        LogUtils.getLogger().warn("Cauldron EMI examples truncated at {} combinations for {}; not all ingredient combinations are displayed",
                COMBINATION_LIMIT, recipe.getId());
    }

    private static void evaluate(EmiRegistry registry, EmiRecipeCategory category, Level level,
                                 SoftFluidStack fluid, List<ItemStack> inputs, Set<String> shown, Set<String> tested) {
        if (inputs.isEmpty() || inputs.size() > 8 || inputs.stream().anyMatch(s -> registry.isStackDisabled(EmiStack.of(s)))) return;
        // Compact equal ingredients so native split/surround-eight paths are also represented.
        var compact = new ArrayList<ItemStack>();
        for (var input : inputs) {
            var existing = compact.stream().filter(s -> ItemStack.isSameItemSameTags(s, input)).findFirst();
            if (existing.isPresent()) existing.get().grow(1); else compact.add(input.copyWithCount(1));
        }
        evaluateForm(registry, category, level, fluid, compact, shown, tested);
        // Native custom-cauldron recipes count occupied inputs, not stack size. Keep the
        // separate-item-entity form as well; compacting repeated ingredients can change a match.
        if (compact.size() != inputs.size()) evaluateForm(registry, category, level, fluid,
                inputs.stream().map(s -> s.copyWithCount(1)).toList(), shown, tested);
    }

    private static void evaluateForm(EmiRegistry registry, EmiRecipeCategory category, Level level,
                                     SoftFluidStack fluid, List<ItemStack> compact,
                                     Set<String> shown, Set<String> tested) {
        boolean hand = compact.size() == 1;
        String candidate = compact.stream().map(s -> s.save(new CompoundTag()).toString()).toList()
                + "|" + fluid.copyWithCount(1).save(new CompoundTag());
        if (!tested.add(candidate)) return;
        int capacity = hand && !fluid.is(BuiltInSoftFluids.WATER) ? SoftFluid.BUCKET_COUNT : 3;
        for (int count = 1; count <= capacity; count++) {
            for (boolean boiling : new boolean[]{false, true}) {
                if (boiling && fluid.is(ModTags.CANT_BOIL)) continue;
                var reserve = fluid.copyWithCount(count);
                var working = compact.stream().map(ItemStack::copy).toList();
                var result = CauldronRecipeUtils.craft(level, boiling, capacity, reserve.copy(), working);
                if (result == null) continue;
                if (!result.craftedItem().isEmpty() && registry.isStackDisabled(EmiStack.of(result.craftedItem()))) continue;
                var consumed = new ArrayList<EmiIngredient>();
                for (int i = 0; i < compact.size(); i++) {
                    int used = compact.get(i).getCount() - working.get(i).getCount();
                    if (used > 0) consumed.add(EmiStack.of(compact.get(i).copyWithCount(used)));
                }
                if (consumed.isEmpty()) continue;
                var after = result.resultFluid().copy();
                // Native thrown-item handler's Forge water-to-other-fluid conversion.
                if (!hand && reserve.is(BuiltInSoftFluids.WATER) && count == 3 && after.getCount() == 3) after.setCount(4);
                String key = consumed.stream().map(i -> i.getEmiStacks().get(0).getItemStack().save(new CompoundTag()).toString()).toList()
                        + "|" + reserve.save(new CompoundTag()) + "|"
                        + result.craftedItem().save(new CompoundTag()) + "|" + after.save(new CompoundTag()) + "|" + hand;
                if (shown.add(key)) {
                    var source = new CauldronFluidStack(reserve);
                    var output = new ArrayList<EmiStack>();
                    if (!result.craftedItem().isEmpty()) output.add(EmiStack.of(result.craftedItem()));
                    if (!after.isEmpty()) output.add(new CauldronFluidStack(after));
                    var catalysts = new ArrayList<EmiIngredient>();
                    // Supply U lookup on filled vessels without claiming that a vessel is consumed.
                    reserve.toAllPossibleFilledItems().values().forEach(item -> catalysts.add(EmiStack.of(item)));
                    var forgeFluid = reserve.fluid().getForgeFluid();
                    if (forgeFluid != net.minecraft.world.level.material.Fluids.EMPTY) catalysts.add(EmiStack.of(forgeFluid));
                    registry.addRecipe(new Display(category, id("/cauldron/" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8))),
                            List.copyOf(consumed), source, List.copyOf(output), List.copyOf(catalysts), boiling, hand));
                }
                // Cold success is sufficient; don't duplicate it as a supposedly heat-required recipe.
                break;
            }
        }
    }
    private static ResourceLocation id(String path) { return new ResourceLocation("better_compat_fixes", path); }

    private record Display(EmiRecipeCategory category, ResourceLocation id, List<EmiIngredient> items,
                           EmiStack reserve, List<EmiStack> outputs, List<EmiIngredient> catalysts,
                           boolean boiling, boolean hand) implements EmiRecipe {
        @Override public EmiRecipeCategory getCategory() { return category; }
        @Override public ResourceLocation getId() { return id; }
        @Override public List<EmiIngredient> getInputs() {
            var inputs = new ArrayList<EmiIngredient>(items); inputs.add(reserve); return inputs;
        }
        @Override public List<EmiIngredient> getCatalysts() { return catalysts; }
        @Override public List<EmiStack> getOutputs() { return outputs; }
        @Override public boolean supportsRecipeTree() { return false; }
        @Override public int getDisplayWidth() { return 176; }
        @Override public int getDisplayHeight() { return 120; }
        @Override public void addWidgets(WidgetHolder widgets) {
            for (int i = 0; i < items.size(); i++) widgets.addSlot(items.get(i), (i % 4) * 18, (i / 4) * 18);
            widgets.addSlot(reserve, 0, 40);
            widgets.addText(Component.translatable("better_compat_fixes.emi.cauldron.reserve"), 22, 45, 0x404040, false);
            widgets.addFillingArrow(78, 9, 60);
            for (int i = 0; i < outputs.size(); i++) widgets.addSlot(outputs.get(i), 112 + i * 20, 10).recipeContext(this);
            widgets.addText(Component.translatable("better_compat_fixes.emi.cauldron." + (boiling ? "boiling" : "cold")), 0, 66, 0x404040, false);
            widgets.addText(Component.translatable("better_compat_fixes.emi.cauldron." + (hand ? "hand" : "world")), 0, 80, 0x404040, false);
            widgets.addText(Component.translatable("better_compat_fixes.emi.cauldron.example"), 0, 94, 0x404040, false);
            widgets.addText(Component.translatable("better_compat_fixes.emi.cauldron.remaining"), 0, 108, 0x404040, false);
        }
    }
}
