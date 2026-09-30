package com.bettercontent.bettercompatfixes.mixin.epicfightvs;

import com.bettercontent.bettercompatfixes.compat.epicfightvs.EpicFightVsTransformMath;
import com.bettercontent.bettercompatfixes.compat.epicfightvs.client.EpicFightVsCameraAccess;
import com.bettercontent.bettercompatfixes.compat.epicfightvs.client.EpicFightVsCameraState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.entity.ShipMountedToData;
import org.valkyrienskies.mod.common.world.RaycastUtilsKt;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.event.types.BuildCameraTransform;

@Mixin(value = EpicFightCameraAPI.class, remap = false)
public abstract class EpicFightCameraApiMixin {
    @Inject(method = "setupCamera", at = @At("RETURN"), require = 1)
    private void betterContentFixes$markCompletedCamera(
            final Camera camera,
            final float partialTick,
            final CallbackInfoReturnable<BuildCameraTransform.Pre> callback
    ) {
        final BuildCameraTransform.Pre result = callback.getReturnValue();
        if (result != null
                && result.isVanillaCameraSetupCanceled()
                && VSGameUtilsKt.getShipMountedToData(camera.getEntity(), partialTick) != null) {
            EpicFightVsCameraState.mark(camera);
        }
    }

    @WrapOperation(
            method = "setupCamera",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;m_45547_(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;",
                    remap = false
            ),
            require = 1
    )
    private BlockHitResult betterContentFixes$clipCameraInShipWorld(
            final ClientLevel level,
            final ClipContext context,
            final Operation<BlockHitResult> original,
            final Camera camera,
            final float partialTick
    ) {
        final ShipMountedToData mounted = VSGameUtilsKt.getShipMountedToData(camera.getEntity(), partialTick);
        if (mounted == null || !(mounted.getShipMountedTo() instanceof ClientShip ship)) {
            return original.call(level, context);
        }

        final ShipTransform renderTransform = ship.getRenderTransform();
        final Vec3 epicEyeMc = ((EpicFightVsCameraAccess) camera)
                .betterContentFixes$epicEye(camera.getEntity(), partialTick);
        final Vector3d epicEye = new Vector3d(epicEyeMc.x, epicEyeMc.y, epicEyeMc.z);
        final Vector3d shipWorldEye = renderTransform.getShipToWorldMatrix()
                .transformPosition(mounted.getMountPosInShip(), new Vector3d());
        renderTransform.getShipCoordinatesToWorldCoordinatesRotation()
                .transform(new Vector3d(0.0D, epicEyeMc.y - Mth.lerp(
                        (double) partialTick, camera.getEntity().yo, camera.getEntity().getY()), 0.0D))
                .add(shipWorldEye, shipWorldEye);

        final Vector3dc from = EpicFightVsTransformMath.cameraPointToWorld(
                toJoml(context.getFrom()), epicEye, shipWorldEye,
                renderTransform.getShipCoordinatesToWorldCoordinatesRotation());
        final Vector3dc to = EpicFightVsTransformMath.cameraPointToWorld(
                toJoml(context.getTo()), epicEye, shipWorldEye,
                renderTransform.getShipCoordinatesToWorldCoordinatesRotation());
        final BlockHitResult hit = RaycastUtilsKt.clipIncludeShips(
                level,
                new ClipContext(toMinecraft(from), toMinecraft(to), ClipContext.Block.VISUAL,
                        ClipContext.Fluid.NONE, camera.getEntity()),
                true,
                ship.getId()
        );
        final Vec3 mapped = toMinecraft(EpicFightVsTransformMath.cameraPointFromWorld(
                toJoml(hit.getLocation()), epicEye, shipWorldEye,
                renderTransform.getShipCoordinatesToWorldCoordinatesRotation()));
        if (hit.getType() == HitResult.Type.MISS) {
            return BlockHitResult.miss(mapped, hit.getDirection(), BlockPos.containing(mapped));
        }
        return new BlockHitResult(mapped, hit.getDirection(), hit.getBlockPos(), hit.isInside());
    }

    private static Vector3d toJoml(final Vec3 vector) {
        return new Vector3d(vector.x, vector.y, vector.z);
    }

    private static Vec3 toMinecraft(final Vector3dc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
