package com.bettercontent.bettercompatfixes.client.aether;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;
import top.theillusivec4.curios.client.render.CuriosLayer;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.compat.CuriosCompat;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Opts the three pinned Aether renderers into Epic Fight's existing per-item dispatch. */
public interface AetherRiggedCurioRenderer extends ICurioRenderer, CuriosCompat.EpicFightCurioRenderer {
    @Override
    @SuppressWarnings("unchecked")
    default void draw(ItemStack stack, SlotContext slot, LivingEntityPatch<LivingEntity> patch,
                      LivingEntity entity, CuriosLayer<LivingEntity, EntityModel<LivingEntity>> layer,
                      PoseStack poses, MultiBufferSource buffers, int light, OpenMatrix4f[] joints,
                      float age, float yaw, float pitch, float partialTick) {
        RenderLayerParent<LivingEntity, EntityModel<LivingEntity>> parent =
                (RenderLayerParent<LivingEntity, EntityModel<LivingEntity>>) Minecraft.getInstance()
                        .getEntityRenderDispatcher().getRenderer(entity);
        AetherCurioMeshes.withPose(patch, joints, () -> render(stack, slot, poses, parent,
                AetherCurioMeshes.triangulated(buffers), light,
                entity.walkAnimation.position(partialTick), entity.walkAnimation.speed(partialTick),
                partialTick, age, yaw, pitch));
    }
}
