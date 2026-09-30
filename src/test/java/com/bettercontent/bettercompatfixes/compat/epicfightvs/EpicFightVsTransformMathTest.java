package com.bettercontent.bettercompatfixes.compat.epicfightvs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

final class EpicFightVsTransformMathTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void convertsTranslationAndDeckRotationIntoEpicFightsMirroredBasis() {
        Matrix4d shipToWorld = new Matrix4d()
                .translation(13.0D, -4.0D, 29.0D)
                .rotate(new Quaterniond().rotateXYZ(0.31D, -0.72D, 0.18D));
        Vector3d minecraftLocal = new Vector3d(2.0D, 3.0D, -5.0D);
        Vector3d epicLocal = mirror(minecraftLocal);

        Vector3d expected = mirror(shipToWorld.transformPosition(new Vector3d(minecraftLocal)));
        Vector3d actual = EpicFightVsTransformMath.toEpicFightBasis(shipToWorld)
                .transformPosition(epicLocal, new Vector3d());

        assertVector(expected, actual);
    }

    @Test
    void cameraPointConversionRoundTripsAcrossYawPitchAndRoll() {
        Quaterniond shipRotation = new Quaterniond().rotateXYZ(0.42D, 1.13D, -0.27D);
        Vector3d epicEye = new Vector3d(140.0D, 67.5D, -81.0D);
        Vector3d shipWorldEye = new Vector3d(-23.0D, 104.0D, 311.0D);
        Vector3d epicCamera = new Vector3d(137.5D, 69.0D, -77.0D);

        Vector3d worldCamera = EpicFightVsTransformMath.cameraPointToWorld(
                epicCamera, epicEye, shipWorldEye, shipRotation);
        Vector3d roundTrip = EpicFightVsTransformMath.cameraPointFromWorld(
                worldCamera, epicEye, shipWorldEye, shipRotation);

        assertVector(epicCamera, roundTrip);
    }

    @Test
    void cameraCompositionAppliesTheShipRotationExactlyOnce() {
        Quaterniond shipRotation = new Quaterniond().rotateXYZ(0.35D, -0.64D, 0.51D);
        Quaterniond epicRotation = new Quaterniond().rotateYXZ(-0.9D, 0.2D, 0.0D);
        Quaterniond once = EpicFightVsTransformMath.cameraRotationToWorld(epicRotation, shipRotation);
        Quaterniond expected = shipRotation.mul(epicRotation, new Quaterniond()).normalize();
        Quaterniond twice = shipRotation.mul(once, new Quaterniond()).normalize();

        assertQuaternion(expected, once);
        assertNotEquals(1.0D, Math.abs(once.dot(twice)), 1.0E-6D,
                "a second ship transform must produce a detectably different orientation");
    }

    private static Vector3d mirror(final Vector3d value) {
        return new Vector3d(-value.x, value.y, -value.z);
    }

    private static void assertVector(final Vector3d expected, final Vector3d actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
    }

    private static void assertQuaternion(final Quaterniond expected, final Quaterniond actual) {
        assertEquals(1.0D, Math.abs(expected.dot(actual)), EPSILON);
    }
}
