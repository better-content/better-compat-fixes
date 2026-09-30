package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.bettercontent.bettercompatfixes.client.FirstPersonLimbVisibility;
import com.bettercontent.bettercompatfixes.config.BcFixesClientConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.FirstPersonRenderer;

@Mixin(value = FirstPersonRenderer.class, remap = false)
public abstract class FirstPersonRendererMixin {
    @WrapOperation(
            method = "render(Lnet/minecraft/client/player/LocalPlayer;Lyesman/epicfight/client/world/capabilites/entitypatch/player/LocalPlayerPatch;Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;IF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lyesman/epicfight/client/mesh/HumanoidMesh;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;IFFFFILyesman/epicfight/api/model/Armature;[Lyesman/epicfight/api/utils/math/OpenMatrix4f;)V"),
            require = 2)
    private void betterContentFixes$hideFirstPersonPlayerLimbsBeforeDraw(
            final HumanoidMesh mesh,
            final PoseStack poseStack,
            final MultiBufferSource buffers,
            final RenderType renderType,
            final int packedLight,
            final float red,
            final float green,
            final float blue,
            final float alpha,
            final int overlay,
            final Armature armature,
            final OpenMatrix4f[] poses,
            final Operation<Void> original
    ) {
        if (BcFixesClientConfig.hideFirstPersonLimbs()) {
            FirstPersonLimbVisibility.hidePlayerModel(mesh);
        }
        original.call(mesh, poseStack, buffers, renderType, packedLight,
                red, green, blue, alpha, overlay, armature, poses);
    }
}
