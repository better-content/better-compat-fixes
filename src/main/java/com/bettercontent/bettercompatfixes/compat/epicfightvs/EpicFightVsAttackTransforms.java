package com.bettercontent.bettercompatfixes.compat.epicfightvs;

import org.joml.Matrix4d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.entity.ShipMountedToData;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class EpicFightVsAttackTransforms {
    private EpicFightVsAttackTransforms() {
    }

    public static OpenMatrix4f shipToWorld(
            final OpenMatrix4f epicFightTransform,
            final LivingEntityPatch<?> entityPatch
    ) {
        final ShipMountedToData mounted = VSGameUtilsKt.getShipMountedToData(entityPatch.getOriginal(), null);
        if (mounted == null) return epicFightTransform;

        final Matrix4d converted = EpicFightVsTransformMath.toEpicFightBasis(
                mounted.getShipMountedTo().getTransform().getShipToWorld()
        );
        return epicFightTransform.mulFront(new OpenMatrix4f(
                (float) converted.m00(), (float) converted.m01(), (float) converted.m02(), (float) converted.m03(),
                (float) converted.m10(), (float) converted.m11(), (float) converted.m12(), (float) converted.m13(),
                (float) converted.m20(), (float) converted.m21(), (float) converted.m22(), (float) converted.m23(),
                (float) converted.m30(), (float) converted.m31(), (float) converted.m32(), (float) converted.m33()
        ));
    }
}
