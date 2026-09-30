package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.bettercontent.bettercompatfixes.client.FirstPersonLimbVisibility;
import com.bettercontent.bettercompatfixes.config.BcFixesClientConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;

@Mixin(value = WearableItemLayer.class, remap = false)
public abstract class WearableItemLayerMixin {
    @Shadow
    @Final
    private boolean firstPersonModel;

    @WrapOperation(
            method = "renderLayer(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I[Lyesman/epicfight/api/utils/math/OpenMatrix4f;FFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lyesman/epicfight/client/renderer/patched/layer/WearableItemLayer;renderArmor(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILyesman/epicfight/api/client/model/SkinnedMesh;Lyesman/epicfight/api/model/Armature;FFFLnet/minecraft/resources/ResourceLocation;[Lyesman/epicfight/api/utils/math/OpenMatrix4f;)V"),
            require = 3)
    private void betterContentFixes$hideFirstPersonArmorLimbsBeforeDraw(
            final WearableItemLayer<?, ?, ?, ?> layer,
            final PoseStack poseStack,
            final MultiBufferSource buffers,
            final int packedLight,
            final SkinnedMesh mesh,
            final Armature armature,
            final float red,
            final float green,
            final float blue,
            final ResourceLocation texture,
            final OpenMatrix4f[] poses,
            final Operation<Void> original,
            @Local final EquipmentSlot slot
    ) {
        if (firstPersonModel && BcFixesClientConfig.hideFirstPersonLimbs()) {
            FirstPersonLimbVisibility.hideArmorLimbs(mesh, slot);
        }
        original.call(layer, poseStack, buffers, packedLight, mesh, armature,
                red, green, blue, texture, poses);
    }
}
