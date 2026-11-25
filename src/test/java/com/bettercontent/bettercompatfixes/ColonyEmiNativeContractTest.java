package com.bettercontent.bettercompatfixes;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import java.io.InputStreamReader;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
            var inventoryHandler = type(zip, root + "handler/InventoryRecipeHandler");
            assertTrue(calls(method(inventoryHandler, "supportsRecipe"), root + "api/recipe/EmiCraftingRecipe", "canFit"),
                    "Inventory crafting must reject recipes too large for its 2x2 grid");
            assertTrue(calls(method(inventoryHandler, "canCraft"), "net/minecraft/world/inventory/RecipeBookMenu", "m_6635_"));
            assertTrue(calls(method(inventoryHandler, "canCraft"), "net/minecraft/world/inventory/RecipeBookMenu", "m_6656_"));
            var craftingHandler = type(zip, root + "handler/CraftingRecipeHandler");
            assertTrue(calls(method(craftingHandler, "supportsRecipe"), root + "api/recipe/EmiRecipe", "getCategory"));
            var screen = type(zip, root + "screen/RecipeScreen");
            assertTrue(screen.fields.stream().anyMatch(f -> f.name.equals("recipes") && f.desc.equals("Ljava/util/Map;")));
            var init = method(screen, "m_7856_");
            // This production class has no render-named method: the client mixin must
            // target its SRG alias as well as the mapped development name.
            assertFalse(screen.methods.stream().anyMatch(m -> m.name.equals("render")));
            assertEquals("(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", method(screen, "m_88315_").desc);
            ClassNode mixin = new ClassNode();
            try (var in = getClass().getClassLoader().getResourceAsStream(
                    "com/bettercontent/bettercompatfixes/mixin/emi/RecipeScreenMixin.class")) {
                assertNotNull(in, "compiled EMI recipe screen mixin missing");
                new ClassReader(in).accept(mixin, 0);
            }
            var surfaceCheck = method(mixin, "betterCompat$craftableOnScreen");
            assertTrue(calls(surfaceCheck, root + "registry/EmiRecipeFiller", "getFirstValidHandler"));
            assertTrue(calls(surfaceCheck, root + "api/recipe/handler/EmiRecipeHandler", "supportsRecipe"));
            assertTrue(calls(surfaceCheck, root + "api/recipe/handler/EmiRecipeHandler", "canCraft"));
            var renderInjector = method(mixin, "betterCompat$refreshAfterTransfer");
            assertTrue(renderInjector.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Inject;"))
                    .flatMap(a -> a.values.stream()).anyMatch(v -> v instanceof List<?> targets
                        && targets.contains("render") && targets.contains("m_88315_")),
                    "the compiled mixin must target both mapped and pinned production EMI render names");
            assertTrue(calls(init, "java/util/stream/Stream", "sorted"));
            assertTrue(calls(init, root + "screen/RecipeTab", "bakePages"));
            method(screen, "focusRecipe");
            var api = type(zip, root + "api/EmiApi");
            assertTrue(calls(method(api, "displayRecipes"), api.name, "focusRecipe"));
            assertTrue(calls(method(api, "setPages"), screen.name, "focusCategory"));
        }
    }
    @Test void jemiShimsCoverThePinnedJeiSlotViewAbiOnBothClientAdapters() throws Exception {
        var resources = getClass().getClassLoader();
        try (var emi = pin("emi", "ea320200878e4a49196760234a22da763671520a");
             var jei = pin("jei", "1ae408badfe8b57417f465a06fc4d03215dba993");
             var config = new InputStreamReader(resources.getResourceAsStream("better_compat_fixes.mixins.json"),
                     java.nio.charset.StandardCharsets.UTF_8)) {
            var slotView = type(jei, "mezz/jei/api/gui/ingredient/IRecipeSlotView");
            var clientMixins = JsonParser.parseReader(config).getAsJsonObject().getAsJsonArray("client");
            for (String targetName : List.of("JemiRecipeSlot", "JemiRecipeSlotDrawable")) {
                var target = type(emi, "dev/emi/emi/jemi/impl/" + targetName);
                var missing = slotView.methods.stream()
                        .filter(required -> (required.access & Opcodes.ACC_ABSTRACT) != 0)
                        .filter(required -> !provides(target, required)).toList();
                assertEquals(Set.of("getAllIngredientsList", "getDisplayedIngredients", "getTagKey"),
                        missing.stream().map(m -> m.name).collect(Collectors.toSet()), targetName);
                var mixinName = "emi." + targetName + "ViewMixin";
                assertTrue(clientMixins.asList().stream().anyMatch(m -> m.getAsString().equals(mixinName)));
                var mixin = new ClassNode();
                try (var in = resources.getResourceAsStream("com/bettercontent/bettercompatfixes/mixin/"
                        + mixinName.replace('.', '/') + ".class")) {
                    assertNotNull(in, mixinName);
                    new ClassReader(in).accept(mixin, 0);
                }
                for (var required : missing) {
                    assertTrue(provides(mixin, required), mixinName + " must implement " + required.name + required.desc);
                }
            }
        }
    }

    private static boolean provides(ClassNode type, MethodNode required) {
        return type.methods.stream().anyMatch(m -> m.name.equals(required.name) && m.desc.equals(required.desc)
                && (m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC)) == Opcodes.ACC_PUBLIC);
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
