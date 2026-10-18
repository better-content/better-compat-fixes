package com.bettercontent.bettercompatfixes.mixin.aetherepicfight;

import com.bettercontent.bettercompatfixes.client.aether.AetherCurioMeshes;
import com.bettercontent.bettercompatfixes.client.aether.AetherRiggedCurioRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.aetherteam.aether.client.renderer.accessory.ShieldOfRepulsionRenderer", remap = false)
public abstract class ShieldOfRepulsionRendererMixin implements AetherRiggedCurioRenderer {
    @Group(name = "aetherShieldDraw", min = 1, max = 1)
    @Redirect(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"), require = 0)
    private void bc$drawMapped(HumanoidModel<?> model, PoseStack poses, VertexConsumer vertices,
                               int light, int overlay, float r, float g, float b, float a) {
        AetherCurioMeshes.draw(model, AetherCurioMeshes.Parts.SHIELD, poses, vertices, light, overlay, r, g, b, a);
    }

    @Group(name = "aetherShieldDraw", min = 1, max = 1)
    @Redirect(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/model/HumanoidModel;m_7695_(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"), require = 0)
    private void bc$drawRuntime(HumanoidModel<?> model, PoseStack poses, VertexConsumer vertices,
                                int light, int overlay, float r, float g, float b, float a) {
        AetherCurioMeshes.draw(model, AetherCurioMeshes.Parts.SHIELD, poses, vertices, light, overlay, r, g, b, a);
    }
}
