package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class MovementPresentationResourceTest {
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/better_compat_fixes.mixins.json");
    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/bettercontent/bettercompatfixes");

    @Test
    void clientMixinsArePackagedOnTheClientSide() throws IOException {
        final JsonObject config = JsonParser.parseReader(Files.newBufferedReader(MIXIN_CONFIG)).getAsJsonObject();
        final String client = config.getAsJsonArray("client").toString();

        assertTrue(client.contains("parcool.DodgeMixin"));
        assertTrue(client.contains("parcool.ClingToCliffPlayerRotationMixin"));
        assertFalse(client.contains("minecraft.LocalPlayerSprintMixin"));
        assertTrue(client.contains("epicfight.FirstPersonRendererMixin"));
        assertTrue(client.contains("epicfight.WearableItemLayerMixin"));
        assertFalse(client.contains("epicfightfirstperson"));
    }

    @Test
    void optionalCompatibilityIsPinnedToInspectedVersions() throws IOException {
        final String plugin = Files.readString(SOURCE_ROOT.resolve("mixin/BetterContentMixinPlugin.java"));
        final String metadata = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));

        assertTrue(plugin.contains("hasVersion(mods, \"parcool\", \"3.4.3.3\")"));
        assertTrue(plugin.contains("hasVersion(mods, \"epicfight\", \"20.14.17\")"));
        assertTrue(metadata.contains("modId=\"parcool\""));
        assertTrue(metadata.contains("modId=\"pingwheel\""));
        assertFalse(plugin.contains("epicfight_first_person_model"));
        assertFalse(metadata.contains("epicfight_first_person_model"));
    }

    @Test
    void directionalDoubleTapDodgeIsDisabledWithoutReplacingVanillaSprint() throws IOException {
        final String mixin = Files.readString(SOURCE_ROOT.resolve("mixin/parcool/DodgeMixin.java"));
        final String config = Files.readString(SOURCE_ROOT.resolve("config/BcFixesClientConfig.java"));

        assertTrue(mixin.contains("return Boolean.FALSE"));
        assertFalse(config.contains("directionalDoubleTapDodge"));
        assertFalse(config.contains("doubleTapWindowTicks"));
        assertFalse(config.contains("replaceForwardDoubleTapSprint"));
        assertFalse(Files.exists(SOURCE_ROOT.resolve("client/ParCoolDirectionalDodgeClient.java")));
        assertFalse(Files.exists(SOURCE_ROOT.resolve("client/DirectionalDoubleTapTracker.java")));
        assertFalse(Files.exists(SOURCE_ROOT.resolve("client/VanillaDoubleTapSprintSuppressor.java")));
    }

    @Test
    void parCoolClimbOnlySuppressesItsForcedBodyRotation() throws IOException {
        final String mixin = Files.readString(
                SOURCE_ROOT.resolve("mixin/parcool/ClingToCliffPlayerRotationMixin.java"));

        assertTrue(mixin.contains("com.alrex.parcool.common.action.impl.ClingToCliff"));
        assertTrue(mixin.contains("method = \"onRenderTick\""));
        assertTrue(mixin.contains("cancellable = true"));
        assertTrue(mixin.contains("callbackInfo.cancel()"));
        assertTrue(mixin.contains("climb motion and camera input"));
    }

    @Test
    void firstPersonPolicyHidesTheFullPlayerModelAndDoesNotTouchItemLayers() throws IOException {
        final String visibility = Files.readString(SOURCE_ROOT.resolve("client/FirstPersonLimbVisibility.java"));
        final String renderer = Files.readString(SOURCE_ROOT.resolve("mixin/epicfight/FirstPersonRendererMixin.java"));
        final String config = Files.readString(SOURCE_ROOT.resolve("config/BcFixesClientConfig.java"));
        final String armorRenderer = Files.readString(
                SOURCE_ROOT.resolve("mixin/epicfight/WearableItemLayerMixin.java"));

        assertTrue(visibility.contains("mesh.getAllParts().forEach(part -> part.setHidden(true))"));
        assertTrue(config.contains("Animated arms remain visible while swimming or submerged."));
        assertTrue(visibility.contains("hideArmorLimbs"));
        assertTrue(visibility.contains("player.isInWaterOrBubble() || player.isSwimming()"));
        assertTrue(renderer.contains("hidePlayerModel"));
        assertTrue(renderer.contains("HumanoidMesh;draw"));
        assertTrue(renderer.contains("require = 2"));
        assertTrue(renderer.contains("hideFirstPersonPlayerLimbsBeforeDraw"));
        assertFalse(renderer.contains("prepareModel"));
        assertTrue(armorRenderer.contains("WearableItemLayer.class"));
        assertTrue(armorRenderer.contains("firstPersonModel"));
        assertTrue(armorRenderer.contains("WearableItemLayer;renderArmor"));
        assertTrue(armorRenderer.contains("hideFirstPersonArmorLimbsBeforeDraw"));
        assertTrue(!visibility.contains("PatchedItemInHandLayer"));
        assertTrue(!renderer.contains("PatchedItemInHandLayer"));
        assertTrue(!armorRenderer.contains("PatchedItemInHandLayer"));
        assertFalse(Files.exists(SOURCE_ROOT.resolve(
                "mixin/epicfightfirstperson/FirstPersonBodyRendererMixin.java")));
        assertFalse(Files.exists(SOURCE_ROOT.resolve(
                "mixin/epicfightfirstperson/FirstPersonWearableItemLayerMixin.java")));
    }
}
