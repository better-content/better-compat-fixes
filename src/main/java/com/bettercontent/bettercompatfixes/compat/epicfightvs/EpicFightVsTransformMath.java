package com.bettercontent.bettercompatfixes.compat.epicfightvs;

import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class EpicFightVsTransformMath {
    private EpicFightVsTransformMath() {
    }

    /** Converts a Minecraft-space transform to Epic Fight's mirrored X/Z matrix basis. */
    public static Matrix4d toEpicFightBasis(final Matrix4dc shipToWorld) {
        final Matrix4d mirror = new Matrix4d().scaling(-1.0D, 1.0D, -1.0D);
        return new Matrix4d(mirror).mul(shipToWorld).mul(mirror);
    }

    /** Moves an Epic Fight camera-space point around its eye pivot into rendered ship-world space. */
    public static Vector3d cameraPointToWorld(
            final Vector3dc point,
            final Vector3dc epicEye,
            final Vector3dc shipWorldEye,
            final Quaterniondc shipRotation
    ) {
        return shipRotation.transform(new Vector3d(point).sub(epicEye)).add(shipWorldEye);
    }

    public static Vector3d cameraPointFromWorld(
            final Vector3dc point,
            final Vector3dc epicEye,
            final Vector3dc shipWorldEye,
            final Quaterniondc shipRotation
    ) {
        return new Quaterniond(shipRotation).conjugate()
                .transform(new Vector3d(point).sub(shipWorldEye))
                .add(epicEye);
    }

    public static Quaterniond cameraRotationToWorld(
            final Quaterniondc epicRotation,
            final Quaterniondc shipRotation
    ) {
        return shipRotation.mul(epicRotation, new Quaterniond()).normalize();
    }
}
