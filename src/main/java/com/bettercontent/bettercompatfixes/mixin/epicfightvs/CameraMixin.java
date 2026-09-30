package com.bettercontent.bettercompatfixes.mixin.epicfightvs;

import com.bettercontent.bettercompatfixes.compat.epicfightvs.EpicFightVsTransformMath;
import com.bettercontent.bettercompatfixes.compat.epicfightvs.client.EpicFightVsCameraAccess;
import com.bettercontent.bettercompatfixes.compat.epicfightvs.client.EpicFightVsCameraState;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;

@Mixin(value = Camera.class, priority = 400)
public abstract class CameraMixin implements EpicFightVsCameraAccess {
    // Explicit SRG names are required because this bridge targets a method merged by VS after remapping.
    @Shadow(remap = false) @Final private Vector3f f_90554_;
    @Shadow(remap = false) @Final private Vector3f f_90555_;
    @Shadow(remap = false) @Final private Vector3f f_90556_;
    @Shadow(remap = false) @Final private Quaternionf f_90559_;
    @Shadow(remap = false) public float f_90562_;
    @Shadow(remap = false) public float f_90563_;
    @Shadow(remap = false) private Vec3 f_90552_;

    @Shadow(remap = false)
    public abstract void m_90584_(double x, double y, double z);

    @Unique private Vec3 betterContentFixes$epicPosition;
    @Unique private Vec3 betterContentFixes$epicEye;
    @Unique private Quaternionf betterContentFixes$epicRotation;

    @Override
    public Vec3 betterContentFixes$epicEye(final Entity entity, final float partialTick) {
        return new Vec3(
                Mth.lerp((double) partialTick, entity.xo, entity.getX()),
                Mth.lerp((double) partialTick, entity.yo, entity.getY())
                        + Mth.lerp((double) partialTick, this.f_90563_, this.f_90562_),
                Mth.lerp((double) partialTick, entity.zo, entity.getZ())
        );
    }

    @Inject(method = "setupWithShipMounted", at = @At("HEAD"), remap = false, require = 1)
    private void betterContentFixes$captureEpicFightCamera(
            final BlockGetter level,
            final Entity renderViewEntity,
            final boolean thirdPerson,
            final boolean thirdPersonReverse,
            final float partialTick,
            final ClientShip ship,
            final Vector3dc mountPosition,
            final CallbackInfo callback
    ) {
        if (!EpicFightVsCameraState.consume((Camera) (Object) this)) return;
        this.betterContentFixes$epicPosition = this.f_90552_;
        this.betterContentFixes$epicEye = this.betterContentFixes$epicEye(renderViewEntity, partialTick);
        this.betterContentFixes$epicRotation = new Quaternionf(this.f_90559_);
    }

    @Inject(method = "setupWithShipMounted", at = @At("TAIL"), remap = false, require = 1)
    private void betterContentFixes$restoreEpicFightCameraOnShip(
            final BlockGetter level,
            final Entity renderViewEntity,
            final boolean thirdPerson,
            final boolean thirdPersonReverse,
            final float partialTick,
            final ClientShip ship,
            final Vector3dc mountPosition,
            final CallbackInfo callback
    ) {
        if (this.betterContentFixes$epicPosition == null) return;

        final ShipTransform renderTransform = ship.getRenderTransform();
        final Vector3d shipWorldEye = renderTransform.getShipToWorldMatrix()
                .transformPosition(mountPosition, new Vector3d());
        renderTransform.getShipCoordinatesToWorldCoordinatesRotation()
                .transform(new Vector3d(0.0D, Mth.lerp(partialTick, this.f_90563_, this.f_90562_), 0.0D))
                .add(shipWorldEye, shipWorldEye);

        final Vector3d finalPosition = EpicFightVsTransformMath.cameraPointToWorld(
                new Vector3d(this.betterContentFixes$epicPosition.x, this.betterContentFixes$epicPosition.y,
                        this.betterContentFixes$epicPosition.z),
                new Vector3d(this.betterContentFixes$epicEye.x, this.betterContentFixes$epicEye.y,
                        this.betterContentFixes$epicEye.z),
                shipWorldEye,
                renderTransform.getShipCoordinatesToWorldCoordinatesRotation()
        );
        final Quaterniond finalRotation = EpicFightVsTransformMath.cameraRotationToWorld(
                new Quaterniond(this.betterContentFixes$epicRotation),
                renderTransform.getShipCoordinatesToWorldCoordinatesRotation()
        );

        this.m_90584_(finalPosition.x, finalPosition.y, finalPosition.z);
        this.f_90559_.set(finalRotation);
        this.f_90554_.set(0.0F, 0.0F, 1.0F);
        this.f_90559_.transform(this.f_90554_);
        this.f_90555_.set(0.0F, 1.0F, 0.0F);
        this.f_90559_.transform(this.f_90555_);
        this.f_90556_.set(1.0F, 0.0F, 0.0F);
        this.f_90559_.transform(this.f_90556_);

        this.betterContentFixes$epicPosition = null;
        this.betterContentFixes$epicEye = null;
        this.betterContentFixes$epicRotation = null;
    }
}
