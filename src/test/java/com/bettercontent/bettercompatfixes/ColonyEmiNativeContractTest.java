package com.bettercontent.bettercompatfixes;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

/** Exact upstream binary contracts; native client behavior/captures are a separate acceptance gate. */
class ColonyEmiNativeContractTest {
    private static ZipFile pin(String key, String hash) throws Exception {
        Path path = Path.of(System.getProperty("bc.colonyEmi." + key));
        assertEquals(hash, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path))));
        return new ZipFile(path.toFile());
    }
    private static ClassNode type(ZipFile zip, String name) throws Exception {
        ClassNode node = new ClassNode();
        try (var in = zip.getInputStream(zip.getEntry(name + ".class"))) { new ClassReader(in).accept(node, 0); }
        return node;
    }
    private static MethodNode method(ClassNode type, String name) {
        return type.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }
    private static boolean calls(MethodNode method, String owner, String name) {
        for (var instruction : method.instructions) if (instruction instanceof MethodInsnNode m
                && m.owner.equals(owner) && m.name.equals(name)) return true;
        return false;
    }
    @Test void emiNativeInventoryAndGreedyOneExecutionMatcherAndPrePaginationSitesArePinned() throws Exception {
        try (var zip = pin("emi", "ea320200878e4a49196760234a22da763671520a")) {
            String root = "dev/emi/emi/";
            var inventory = type(zip, root + "api/recipe/EmiPlayerInventory");
            assertTrue(calls(method(inventory, "of"), root + "registry/EmiRecipeFiller", "getAllHandlers"));
            assertTrue(calls(method(inventory, "of"), root + "api/recipe/handler/EmiRecipeHandler", "getInventory"));
            var matcher = inventory.methods.stream().filter(m -> m.name.equals("canCraft") && m.desc.endsWith(";J)Z")).findFirst().orElseThrow();
            assertTrue(calls(matcher, root + "api/recipe/EmiRecipe", "getInputs"));
            assertTrue(calls(matcher, root + "api/stack/EmiIngredient", "getEmiStacks"));
            assertTrue(calls(matcher, "it/unimi/dsi/fastutil/objects/Object2LongMap", "put"), "Native greedy matcher accounts for already-used identity quantities");
            var screen = type(zip, root + "screen/RecipeScreen");
            assertTrue(screen.fields.stream().anyMatch(f -> f.name.equals("recipes") && f.desc.equals("Ljava/util/Map;")));
            var init = method(screen, "m_7856_");
            assertTrue(calls(init, "java/util/stream/Stream", "sorted"));
            assertTrue(calls(init, root + "screen/RecipeTab", "bakePages"));
            method(screen, "focusRecipe");
            var api = type(zip, root + "api/EmiApi");
            assertTrue(calls(method(api, "displayRecipes"), api.name, "focusRecipe"));
            assertTrue(calls(method(api, "setPages"), screen.name, "focusCategory"));
        }
    }
    @Test void coloniesUseNativeSpeciesAndActualProductionEligibilityWithNoUpstreamActivitySync() throws Exception {
        try (var zip = pin("bees", "066657ef103bf365cecff789b181275989bacd25")) {
            String root = "com/accbdd/complicated_bees/";
            var nest = type(zip, root + "block/entity/BeeNestBlockEntity");
            method(nest, "getSpecies");
            method(nest, "m_5995_"); // Native nest species already uses chunk update tags.
            var housing = type(zip, root + "block/entity/BaseBeeHousing");
            var tick = method(housing, "tickServer");
            assertTrue(calls(tick, root + "bees/BeeLogic", "isQueenSatisfied"));
            assertTrue(calls(tick, housing.name, "getOutputBuffer"));
            assertTrue(calls(tick, "java/util/Stack", "empty"));
            assertTrue(calls(tick, housing.name, "doBeeEffect"));
            assertFalse(housing.methods.stream().anyMatch(m -> m.name.equals("m_5995_") || m.name.equals("m_58483_")),
                    "If upstream adds activity sync, revisit minimal cosmetic packet instead of silently replacing it");
        }
    }
}
