package com.bettercontent.bettercompatfixes.client.aether;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.transformer.HumanoidModelBaker;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Keeps Aether's material passes intact; only its humanoid geometry draw is substituted. */
public final class AetherCurioMeshes {
    public enum Parts { GLOVES, PENDANT, SHIELD }

    // Render-thread only, cleared by the same AddLayers callback as Epic Fight's Curios cache.
    private static final Map<HumanoidModel<?>, SkinnedMesh> MESHES = new IdentityHashMap<>();
    private static final ThreadLocal<Pose> CURRENT = new ThreadLocal<>();
    private record Pose(Armature armature, OpenMatrix4f[] joints) {}

    private AetherCurioMeshes() {}

    public static void withPose(LivingEntityPatch<?> patch, OpenMatrix4f[] joints, Runnable render) {
        Pose previous = CURRENT.get();
        CURRENT.set(new Pose(patch.getArmature(), joints));
        try {
            render.run();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static MultiBufferSource triangulated(MultiBufferSource original) {
        return type -> original.getBuffer(EpicFightRenderTypes.getTriangulated(type));
    }

    public static void draw(HumanoidModel<?> model, Parts parts, PoseStack poses, VertexConsumer vertices,
                            int light, int overlay, float red, float green, float blue, float alpha) {
        Pose pose = CURRENT.get();
        if (pose == null) {
            // Vanilla/other entities, inventory previews, and Aether's own hand path stay native.
            model.renderToBuffer(poses, vertices, light, overlay, red, green, blue, alpha);
            return;
        }
        SkinnedMesh mesh = MESHES.computeIfAbsent(model, key -> bake(key, parts));
        mesh.initialize();
        mesh.drawPosed(poses, vertices, Mesh.DrawingFunction.NEW_ENTITY, light,
                red, green, blue, alpha, overlay, pose.armature(), pose.joints());
    }

    private static SkinnedMesh bake(HumanoidModel<?> model, Parts parts) {
        return withParts(model, parts,
                () -> HumanoidModelBaker.VANILLA_TRANSFORMER.transformArmorModel(model));
    }

    private record PartState(PartPose pose, boolean visible, boolean skip, float xScale,
                             float yScale, float zScale) {
        PartState(ModelPart part) {
            this(part.storePose(), part.visible, part.skipDraw, part.xScale, part.yScale, part.zScale);
        }
        void restore(ModelPart part) {
            part.loadPose(pose);
            part.visible = visible;
            part.skipDraw = skip;
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
        }
    }

    static <T> T withParts(HumanoidModel<?> model, Parts parts, Supplier<T> bake) {
        ModelPart[] roots = {model.head, model.hat, model.body, model.rightArm,
                model.leftArm, model.rightLeg, model.leftLeg};
        Map<ModelPart, PartState> saved = new IdentityHashMap<>();
        for (ModelPart root : roots) root.getAllParts().forEach(part -> saved.putIfAbsent(part, new PartState(part)));
        if (model instanceof PlayerModel<?> player) {
            // The shield's slim model also exposes these through PlayerModel.bodyParts().
            for (ModelPart root : new ModelPart[]{player.jacket, player.rightSleeve, player.leftSleeve,
                    player.rightPants, player.leftPants}) {
                root.getAllParts().forEach(part -> saved.putIfAbsent(part, new PartState(part)));
            }
        }
        try {
            // Aether's gloves/pendant override bodyParts/headParts but the native baker also
            // inspects all seven humanoid fields directly. Exclude those unused default cubes.
            for (int i = 0; i < roots.length; i++) {
                boolean included = includes(parts, i);
                roots[i].visible = included;
                roots[i].skipDraw = false;
            }
            // The native transformer restores bind poses and assigns Epic Fight limb weights.
            return bake.get();
        } finally {
            saved.forEach((part, state) -> state.restore(part));
        }
    }

    static boolean includes(Parts parts, int root) {
        return switch (parts) {
            case GLOVES -> root == 3 || root == 4;
            case PENDANT -> root == 2;
            case SHIELD -> true;
        };
    }

    public static void clear() {
        MESHES.values().forEach(SkinnedMesh::destroy);
        MESHES.clear();
    }
}
