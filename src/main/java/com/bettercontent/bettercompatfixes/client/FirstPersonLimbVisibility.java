package com.bettercontent.bettercompatfixes.client;

import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.MeshPart;
import yesman.epicfight.client.mesh.HumanoidMesh;

public final class FirstPersonLimbVisibility {
    private static final List<String> ARM_PARTS = List.of("leftArm", "rightArm", "leftSleeve", "rightSleeve");

    private FirstPersonLimbVisibility() {
    }

    public static void withHiddenPlayerModel(final HumanoidMesh mesh, final Runnable originalDraw) {
        MeshVisibilityScope.draw(mesh.getAllParts(), MeshPart::isHidden, MeshPart::setHidden,
                () -> hidePlayerModel(mesh), originalDraw);
    }

    public static void withHiddenArmorLimbs(final SkinnedMesh mesh, final EquipmentSlot slot,
            final Runnable originalDraw) {
        MeshVisibilityScope.draw(mesh.getAllParts(), MeshPart::isHidden, MeshPart::setHidden,
                () -> hideArmorLimbs(mesh, slot), originalDraw);
    }

    public static void hidePlayerModel(final HumanoidMesh mesh) {
        // This is only called at first-person body draws, never held-item layers.
        mesh.getAllParts().forEach(part -> part.setHidden(true));
    }

    public static void hideArmorLimbs(final SkinnedMesh mesh, final EquipmentSlot slot) {
        if (slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
            mesh.getAllParts().forEach(part -> part.setHidden(true));
            return;
        }
        if (slot != EquipmentSlot.CHEST) {
            return;
        }
        ARM_PARTS.stream()
                .filter(mesh::hasPart)
                .map(mesh::getPart)
                .forEach(part -> part.setHidden(true));
    }

}
