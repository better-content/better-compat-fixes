package com.bettercontent.bettercompatfixes.compat.emi;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Source boundary checks, not a claim of rendered-client or native crafting acceptance. */
class CauldronEmiContractTest {
    private String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/compat/emi", name + ".java"));
    }
    @Test void optionalDependencyIsGuardedAndNativeEvaluatorOwnsResults() throws Exception {
        assertTrue(source("CauldronEmiPlugin").contains("isLoaded(\"amendments\")"));
        String adapter = source("AmendmentsCauldronDisplays");
        assertTrue(adapter.contains("CauldronRecipeUtils.craft(level, boiling, capacity, reserve.copy(), working)"));
        assertTrue(adapter.contains("compact.stream().map(ItemStack::copy)"));
        assertTrue(adapter.contains("CommonConfigs.CAULDRON_HAND_CRAFTING.get()"));
        assertTrue(adapter.contains("fluid.is(ModTags.CANT_BOIL)"));
        assertTrue(adapter.contains("filled.is(Items.BOWL)"), "Native boiling-water/bowl substitution must be discoverable");
        assertTrue(adapter.contains("if (compact.size() != inputs.size()) evaluateForm"),
                "Repeated inputs must also be evaluated as separate item entities");
        assertTrue(adapter.contains("registry.getRecipeManager().getRecipes()"));
        assertFalse(adapter.contains("new CauldronRecipe("));
        assertFalse(adapter.contains("setBlock("));
        assertFalse(adapter.contains("addRecipeHandler("));
        assertTrue(adapter.contains("if (!complete) warn(recipe)"));
    }
    @Test void fluidUnitsAndNbtAreNotMisrepresentedAsBottlesOrMillibuckets() throws Exception {
        String stack = source("CauldronFluidStack");
        assertTrue(stack.contains("Comparison.compareNbt()"));
        assertTrue(stack.contains("new FluidKey(getId())"));
        assertTrue(stack.contains("SoftFluidStack.load(TagParser.parseTag"));
        assertTrue(stack.contains("getType() { return \"better_compat_fixes:cauldron_fluid\"; }"));
        assertFalse(stack.contains("return icon.getKey()"));
        assertTrue(source("AmendmentsCauldronDisplays").contains("supportsRecipeTree() { return false; }"));
    }
}
