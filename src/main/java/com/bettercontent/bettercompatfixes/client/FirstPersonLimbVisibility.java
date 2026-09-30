package com.bettercontent.bettercompatfixes.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.client.mesh.HumanoidMesh;

public final class FirstPersonLimbVisibility {
    private static final List<String> ARM_PARTS = List.of("leftArm", "rightArm", "leftSleeve", "rightSleeve");

    private FirstPersonLimbVisibility() {
    }

    public static void hidePlayerModel(final HumanoidMesh mesh) {
        // Keep the camera clear, but retain Epic Fight's animated arms in water.
        mesh.getAllParts().forEach(part -> part.setHidden(true));
        if (showSwimArms()) {
            ARM_PARTS.stream().filter(mesh::hasPart).map(mesh::getPart)
                    .forEach(part -> part.setHidden(false));
        }
    }

    public static void hideArmorLimbs(final SkinnedMesh mesh, final EquipmentSlot slot) {
        if (slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
            mesh.getAllParts().forEach(part -> part.setHidden(true));
            return;
        }
        if (slot != EquipmentSlot.CHEST) {
            return;
        }
        if (showSwimArms()) return;
        ARM_PARTS.stream()
                .filter(mesh::hasPart)
                .map(mesh::getPart)
                .forEach(part -> part.setHidden(true));
    }

    private static boolean showSwimArms() {
        var player = Minecraft.getInstance().player;
        return player != null && (player.isInWaterOrBubble() || player.isSwimming());
    }
}
