package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class BcFixesResourceTest {
    @Test
    void mixinConfigTargetsExistingMixinClasses() throws IOException {
        Path configPath = Path.of("src/main/resources/better_compat_fixes.mixins.json");
        JsonObject config = JsonParser.parseReader(Files.newBufferedReader(configPath)).getAsJsonObject();
        String packageName = config.get("package").getAsString();
        JsonArray mixins = config.getAsJsonArray("mixins");
        JsonArray clientMixins = config.getAsJsonArray("client");

        assertTrue(mixins != null && mixins.size() > 0, "expected packaged mixin targets");
        assertTrue(clientMixins != null && clientMixins.size() > 0, "expected packaged client mixin targets");
        assertMixinClassesExist(packageName, mixins);
        assertMixinClassesExist(packageName, clientMixins);
        assertTrue(clientMixins.contains(new JsonPrimitive("thefleshthathates.BiomeMusicMixin")),
                "TFTH proximity music suppression must remain client-only and packaged");
        assertTrue(clientMixins.contains(new JsonPrimitive("weather2.FogAdjusterMixin")),
                "Weather2 shader-fog compatibility must remain client-only and packaged");
        assertTrue(clientMixins.contains(new JsonPrimitive("pneumaticcraft.PneumaticCraftRecipeTypeMixin")),
                "PneumaticCraft recipe-level recovery must remain client-only and packaged");
        assertTrue(mixins.contains(new JsonPrimitive("forge.VanillaInventoryCodeHooksMixin")),
                "Forge hopper extraction bridge must remain a common mixin");
        assertTrue(mixins.contains(new JsonPrimitive("sophisticatedstorage.StorageBlockEntityMixin")),
                "Sophisticated Storage barrel bridge must remain a common mixin");
        assertTrue(mixins.contains(new JsonPrimitive("minecraft.FeatureSorterMixin")),
                "repeated worldgen features must be removed before feature-order sorting");
        assertTrue(mixins.contains(new JsonPrimitive("minecraft.DispenserBlockMixin")),
                "parallel mod setup must serialize global dispenser behavior registration");
        assertTrue(mixins.contains(new JsonPrimitive("minecraft.AttributeModifierMixin")),
                "missing serialized attribute modifiers must be discarded before vanilla logs a warning");
        assertTrue(mixins.contains(new JsonPrimitive("thirst.AddLootTableModifierMixin")),
                "Thirst nested chest loot must bypass recursive global modifiers");
        assertTrue(mixins.contains(new JsonPrimitive("epicfightvs.ColliderMixin")),
                "Epic Fight single colliders must be transformed on mounted ships");
        assertTrue(mixins.contains(new JsonPrimitive("epicfightvs.MultiColliderMixin")),
                "Epic Fight multi-colliders must be transformed on mounted ships");
        assertTrue(clientMixins.contains(new JsonPrimitive("epicfightvs.CameraMixin")),
                "the ship-aware Epic Fight camera bridge must remain client-only");
        assertTrue(clientMixins.contains(new JsonPrimitive("epicfightvs.EpicFightCameraApiMixin")),
                "the ship-aware Epic Fight camera ray bridge must remain client-only");
        assertTrue(mixins.contains(new JsonPrimitive("adpother.coldsweat.HearthBlockEntityMixin")),
                "Cold Sweat fuel consumption must retain its AdPother callback");
        assertTrue(mixins.contains(new JsonPrimitive("adpother.littlelogistics.SteamLocomotiveEntityMixin")),
                "steam locomotive fuel consumption must retain its AdPother callback");
        assertTrue(mixins.contains(new JsonPrimitive("adpother.littlelogistics.SteamTugEntityMixin")),
                "steam tug fuel consumption must retain its AdPother callback");
        assertTrue(mixins.contains(new JsonPrimitive("jsonthings.VanillaPackResourcesBuilderMixin")),
                "vanilla pack root discovery must stay limited to built-in pack types");
        assertTrue(mixins.contains(new JsonPrimitive("kubejs.ConsoleJSMixin")),
                "KubeJS initial-world log-location notices must not be emitted as warnings");
        assertTrue(mixins.contains(new JsonPrimitive("dtaether.TagEntryMixin")),
                "DTAether's obsolete imbued branch tag entry must remain a common mixin");
        assertTrue(clientMixins.contains(new JsonPrimitive("adpother.LevelRendererMixin")),
                "acid-rain texture selection must follow vanilla precipitation bindings");
        assertTrue(clientMixins.contains(new JsonPrimitive("sodiumdynamiclights.SodiumDynamicLightsMixin")),
                "dynamic-light resource registration must be scheduled on the client thread");
    }

    @Test
    void malformedAttributeModifiersAreDiscardedBeforeVanillaReadsTheirUuid() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/minecraft/AttributeModifierMixin.java"));

        assertTrue(source.contains("tag == null || !tag.hasUUID(\"UUID\")"),
                "the guard must reject both absent modifier compounds and compounds without a UUID");
    }

    @Test
    void dispenserRegistryItselfIsThreadSafeForDirectModWriters() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/minecraft/DispenserBlockMixin.java"));

        assertTrue(source.contains("@Shadow(remap = false)")
                        && source.contains("public static Map<Item, DispenseItemBehavior> f_52661_;")
                        && source.contains("Collections.synchronizedMap(f_52661_)"),
                "the public dispenser registry must use its production SRG name without an invalid alias");
        assertTrue(source.contains("@WrapMethod(method = {\"registerBehavior\", \"m_52672_\"}, remap = false)"),
                "dispenser registration must resolve its named and production SRG methods without a refmap");
    }

    @Test
    void voidWormRemovalShipsAsAnAlexsMobsConditionalBiomeModifier() throws IOException {
        JsonObject modifier = JsonParser.parseReader(Files.newBufferedReader(Path.of(
                "src/main/resources/data/better_compat_fixes/forge/biome_modifier/remove_void_worm_spawns.json"
        ))).getAsJsonObject();

        assertEquals("better_compat_fixes:void_worm_spawn_removal", modifier.get("type").getAsString());
    }

    @Test
    void mixinConfigKeepsOptionalTargetsNonRequired() throws IOException {
        JsonObject config = JsonParser.parseReader(Files.newBufferedReader(
                Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();

        assertTrue(!config.get("required").getAsBoolean(), "optional compatibility mixins must stay non-required");
        assertTrue(config.getAsJsonObject("injectors").get("defaultRequire").getAsInt() == 0,
                "optional compatibility injectors should not require target matches");
    }

    @Test
    void dtaetherMixinIsPinnedToTheKnownBrokenRelease() throws IOException {
        String plugin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));
        String manifest = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));

        assertTrue(plugin.contains("hasVersion(mods, \"dtaether\", \"1.20.1-1.3.3\")"));
        assertTrue(manifest.contains("versionRange=\"[1.20.1-1.3.3,1.20.1-1.3.4)\""));
    }

    @Test
    void dtaetherGoldenOakDoesNotUseTheRemovedImbuedBranch() throws IOException {
        JsonObject species = JsonParser.parseReader(Files.newBufferedReader(Path.of(
                "src/main/resources/trees/dtaether/species/golden_oak.json"))).getAsJsonObject();
        JsonArray features = species.getAsJsonArray("features");
        boolean keepsGoldenAppleFruit = false;

        for (var element : features) {
            String name = element.getAsJsonObject().get("name").getAsString();
            assertTrue(!"dtaether:alt_branch".equals(name),
                    "golden oak must not request the removed imbued skyroot branch block");
            keepsGoldenAppleFruit |= "fruit".equals(name);
        }

        assertTrue(keepsGoldenAppleFruit, "golden oak must retain its golden apple feature");
    }

    @Test
    void featureSorterMixinTargetsTheProductionSrgMethodWithoutARefmap() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/minecraft/FeatureSorterMixin.java"));

        assertTrue(source.contains("@WrapMethod(method = \"m_220603_\", remap = false)"),
                "the production JAR has no refmap, so the feature sorter hook must use its 1.20.1 SRG name");
    }

    @Test
    void fluidGeneratedBlocksAllowlistDefaultsToObsidianOnly() throws IOException {
        Path tagPath = Path.of("src/main/resources/data/better_compat_fixes/tags/blocks/allowed_fluid_generated_blocks.json");
        JsonObject tag = JsonParser.parseReader(Files.newBufferedReader(tagPath)).getAsJsonObject();
        JsonArray values = tag.getAsJsonArray("values");

        assertEquals(1, values.size(), "fluid-generated allowlist should stay narrow by default");
        assertEquals("minecraft:obsidian", values.get(0).getAsString(),
                "obsidian should remain the only default allowed fluid-generated block");
    }

    private static void assertMixinClassesExist(String packageName, JsonArray mixins) {
        mixins.forEach(element -> {
            String relativeClass = element.getAsString().replace('.', '/') + ".java";
            Path classPath = Path.of("src/main/java", packageName.replace('.', '/'), relativeClass);
            assertTrue(Files.exists(classPath), "missing mixin class " + classPath);
        });
    }
}
