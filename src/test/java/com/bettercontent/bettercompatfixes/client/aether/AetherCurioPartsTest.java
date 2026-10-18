package com.bettercontent.bettercompatfixes.client.aether;

import org.junit.jupiter.api.Test;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

final class AetherCurioPartsTest {
    private static int[] roots(AetherCurioMeshes.Parts parts) {
        return IntStream.range(0, 7).filter(i -> AetherCurioMeshes.includes(parts, i)).toArray();
    }

    @Test void glovesExcludeUnusedDefaultHeadBodyAndLegCubes() {
        assertArrayEquals(new int[]{3, 4}, roots(AetherCurioMeshes.Parts.GLOVES));
    }

    @Test void pendantIncludesOnlyChest() {
        assertArrayEquals(new int[]{2}, roots(AetherCurioMeshes.Parts.PENDANT));
    }

    @Test void shieldKeepsNativeHumanoidPartSelection() {
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6}, roots(AetherCurioMeshes.Parts.SHIELD));
    }

    @Test void bakeMasksOnlyIntendedPartsAndRestoresStateWhenItThrows() {
        HumanoidModel<LivingEntity> model = new HumanoidModel<>(LayerDefinition.create(
                HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32).bakeRoot());
        model.rightArm.xRot = 0.75f;
        model.rightArm.xScale = 1.25f;
        model.body.skipDraw = true;
        PartPose original = model.rightArm.storePose();
        RuntimeException failure = new RuntimeException("bake failure");
        assertSame(failure, assertThrows(RuntimeException.class, () -> AetherCurioMeshes.withParts(
                model, AetherCurioMeshes.Parts.GLOVES, () -> {
                    assertFalse(model.head.visible);
                    assertFalse(model.body.visible);
                    assertFalse(model.body.skipDraw);
                    assertTrue(model.rightArm.visible);
                    assertTrue(model.leftArm.visible);
                    assertFalse(model.rightLeg.visible);
                    model.rightArm.resetPose();
                    throw failure;
                })));
        assertEquals(original.xRot, model.rightArm.xRot);
        assertEquals(original.x, model.rightArm.x);
        assertEquals(1.25f, model.rightArm.xScale);
        assertTrue(model.body.visible);
        assertTrue(model.body.skipDraw);
    }

    @Test void slimShieldOuterLayerStateAlsoSurvivesNativeBakerMutation() {
        PlayerModel<LivingEntity> model = new PlayerModel<>(LayerDefinition.create(
                PlayerModel.createMesh(CubeDeformation.NONE, true), 64, 64).bakeRoot(), true);
        model.leftSleeve.yRot = 0.6f;
        model.leftSleeve.yScale = 1.4f;
        model.leftSleeve.visible = false;
        PartPose original = model.leftSleeve.storePose();
        assertEquals("mesh", AetherCurioMeshes.withParts(model, AetherCurioMeshes.Parts.SHIELD, () -> {
            model.leftSleeve.resetPose();
            model.leftSleeve.visible = true;
            return "mesh";
        }));
        assertEquals(original.yRot, model.leftSleeve.yRot);
        assertEquals(original.y, model.leftSleeve.y);
        assertEquals(1.4f, model.leftSleeve.yScale);
        assertFalse(model.leftSleeve.visible);
    }
}
