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
        assertTrue(client.contains("parcool.ClingToCliffAscendingGrabMixin"));
        assertTrue(client.contains("epicfight.EpicParCoolLedgeCameraMixin"));
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
    void ledgeChangesKeepNativeReachAndUnrelatedPovSettings() throws IOException {
        final String grab = Files.readString(SOURCE_ROOT.resolve("mixin/parcool/ClingToCliffAscendingGrabMixin.java"));
        assertTrue(grab.contains("method = \"canStart("));
        assertTrue(grab.contains("@Constant(doubleValue = 0.2D)"));
        assertTrue(grab.contains("return Double.POSITIVE_INFINITY"));
        assertTrue(grab.contains("require = 1") && grab.contains("allow = 1"));
        assertFalse(grab.contains("callbackInfo.cancel()"));
        final String camera = Files.readString(SOURCE_ROOT.resolve("mixin/epicfight/EpicParCoolLedgeCameraMixin.java"));
        assertTrue(camera.contains("\"epicparcool\".equals(name.getNamespace())"));
        assertTrue(camera.contains("if (!ledgeHang) return"));
        assertTrue(camera.contains("original.cameraTransform()"));
        assertTrue(camera.contains("original.visibilities()"));
        assertTrue(camera.contains("original.rootTransformation()"));
        assertTrue(camera.contains("original.visibilityOthers()"));
        assertTrue(camera.contains("original.hasUniqueAnimation()"));
        assertTrue(camera.contains("original.syncFrame()"));
        final String plugin = Files.readString(SOURCE_ROOT.resolve("mixin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("hasVersion(mods, \"epicparcool\", \"20.12.0.1\")"));
    }

    @Test
    void firstPersonPolicyHidesTheFullPlayerModelAndDoesNotTouchItemLayers() throws IOException {
        final String visibility = Files.readString(SOURCE_ROOT.resolve("client/FirstPersonLimbVisibility.java"));
        final String renderer = Files.readString(SOURCE_ROOT.resolve("mixin/epicfight/FirstPersonRendererMixin.java"));
        final String config = Files.readString(SOURCE_ROOT.resolve("config/BcFixesClientConfig.java"));
        final String armorRenderer = Files.readString(
                SOURCE_ROOT.resolve("mixin/epicfight/WearableItemLayerMixin.java"));

        assertTrue(visibility.contains("mesh.getAllParts().forEach(part -> part.setHidden(true))"));
        assertTrue(config.contains("Arms, sleeves and armor limbs stay hidden while swimming or submerged; held items remain visible."));
        assertTrue(visibility.contains("hideArmorLimbs"));
        assertFalse(visibility.contains("showSwimArms"));
        assertFalse(visibility.contains("isInWaterOrBubble"));
        assertFalse(visibility.contains("isSwimming"));
        assertFalse(visibility.contains("setHidden(false)"));
        assertTrue(renderer.contains("withHiddenPlayerModel(mesh, draw)"));
        assertTrue(armorRenderer.contains("withHiddenArmorLimbs(mesh, slot, draw)"));
        assertTrue(visibility.contains("MeshPart::isHidden, MeshPart::setHidden"));
        assertTrue(visibility.contains("() -> hidePlayerModel(mesh), originalDraw"));
        assertTrue(visibility.contains("() -> hideArmorLimbs(mesh, slot), originalDraw"));
        final String scope = Files.readString(SOURCE_ROOT.resolve("client/MeshVisibilityScope.java"));
        assertTrue(scope.contains("finally"));
        assertTrue(scope.contains("hidden[i] = isHidden.test(snapshot.get(i))"));
        assertTrue(scope.contains("setHidden.accept(snapshot.get(i), hidden[i])"));
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
