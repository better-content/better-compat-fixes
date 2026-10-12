package com.bettercontent.bettercompatfixes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Current reproducibly fetched, hash-pinned native implementations, never retained javap dumps. */
final class MarkerAndLimbNativeContractTest {
    private static final String RBP = "xbigellx/rbp/internal/";
    private static final String EPIC = "yesman/epicfight/";
    private static final String CONTEXT = "Lxbigellx/realisticphysics/internal/level/block/RPBlockContext;";
    private static final String SCHEDULE2 = "(L" + RBP + "physics/BlockOperation;" + CONTEXT + ")V";
    private static final String SCHEDULE3 = "(L" + RBP + "physics/BlockOperation;" + CONTEXT
            + "L" + RBP + "physics/engine/PhysicsEngineBehaviour;)V";
    private static final Path SOURCE = Path.of("src/main/java/com/bettercontent/bettercompatfixes");

    private static Path pinned(String id, String sha1) throws Exception {
        Path path = Path.of(System.getProperty("bc.markerRender." + id));
        assertEquals(sha1, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path))));
        return path;
    }
    private static ClassNode type(Path jar, String name) throws Exception {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entry = zip.getEntry(name + ".class");
            assertNotNull(entry, name);
            ClassNode node = new ClassNode();
            new ClassReader(zip.getInputStream(entry)).accept(node, 0);
            return node;
        }
    }
    private static MethodNode method(ClassNode node, String name, String desc) {
        return node.methods.stream().filter(m -> m.name.equals(name) && (desc == null || m.desc.equals(desc)))
                .findFirst().orElseThrow();
    }
    private static long calls(MethodNode method, String owner, String name, String desc) {
        long count = 0;
        for (var insn : method.instructions) if (insn instanceof MethodInsnNode call
                && call.owner.equals(owner) && call.name.equals(name) && (desc == null || call.desc.equals(desc))) count++;
        return count;
    }

    @Test
    void rbpPlacementNeighborsAndAnalysisConvergeOnGuardedOperationAdmission() throws Exception {
        Path rbp = pinned("6393411", "1c6a18411d5e4c53ebfd57e0b1bbaa56455ec44c");
        pinned("6026115", "adbfd229421190f4cbe3a45d3aea5b42e8dde7c6");
        ClassNode scheduler = type(rbp, RBP + "physics/BlockOperationScheduler");
        assertEquals(1, calls(method(scheduler, "schedule", SCHEDULE2), RBP + "physics/BlockOperationScheduler", "schedule", SCHEDULE3));
        MethodNode admission = method(scheduler, "schedule", SCHEDULE3);
        assertEquals(1, calls(admission, RBP + "physics/BlockOperationScheduler$BlockOperationQueue", "add", null));
        ClassNode behaviour = type(rbp, RBP + "physics/engine/DefaultPhysicsEngineBehaviour");
        assertTrue(calls(method(behaviour, "onBlockPlaced", null), RBP + "physics/task/BlockIntegrityCheckTask", "<init>", null) > 0);
        assertTrue(calls(method(behaviour, "onBlockNeighbourNotify", null), RBP + "physics/PhysicsHelper", "scheduleBlockNeighbourFallChecks", null) > 0);
        assertTrue(calls(method(behaviour, "onBlockAnalysis", null), RBP + "physics/BlockOperationScheduler", "schedule", SCHEDULE2) > 0);
        assertTrue(calls(method(type(rbp, RBP + "physics/task/BlockIntegrityCheckTask"), "run", null), RBP + "physics/BlockOperationScheduler", "schedule", SCHEDULE2) > 0);
        assertTrue(calls(method(type(rbp, RBP + "physics/task/AnalyseChunkTask"), "run", null), RBP + "physics/engine/PhysicsEngineBehaviour", "onBlockAnalysis", null) > 0);
        // Neighbor fall checks create integrity tasks, which use the same operation admission.
        ClassNode helper = type(rbp, RBP + "physics/PhysicsHelper");
        assertTrue(helper.methods.stream().anyMatch(m -> calls(m, RBP + "physics/task/BlockIntegrityCheckTask", "<init>", null) > 0));
        String mixin = Files.readString(SOURCE.resolve("mixin/rbp/BlockOperationSchedulerMixin.java"));
        assertTrue(mixin.contains("schedule" + SCHEDULE3));
        assertTrue(mixin.contains("@At(\"HEAD\")") && mixin.contains("require = 1, allow = 1"));
        assertTrue(mixin.contains("RespawnMarkerPhysics.isExcluded(block.blockState())"));
        assertTrue(mixin.contains("callback.cancel()"));
        String policy = Files.readString(SOURCE.resolve("compat/RespawnMarkerPhysics.java"));
        assertTrue(policy.contains("new ResourceLocation(\"better_spawns\", \"respawn_marker\")"));
        assertTrue(policy.contains("isExcludedId(ForgeRegistries.BLOCKS.getKey(state.getBlock()))"));
        assertTrue(policy.contains("return MARKER.equals(id)"));
        assertFalse(policy.contains("crying_obsidian"));
        String plugin = Files.readString(SOURCE.resolve("mixin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("hasVersion(mods, \"rbp\", \"1.0.0\")"));
        assertTrue(plugin.contains("hasVersion(mods, \"realisticphysics\", \"1.0.1\")"));
        assertTrue(Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json")).contains("rbp.BlockOperationSchedulerMixin"));
    }

    @Test
    void nativeSharedMeshesReinitializeBeforeThirdPersonBodyAndArmorDraws() throws Exception {
        Path epic = pinned("8049910", "faa8eddad9d80adb9c4ba81fd0a8a21c81989d3d");
        ClassNode meshPart = type(epic, EPIC + "api/client/model/MeshPart");
        assertNotNull(method(meshPart, "isHidden", "()Z"));
        assertNotNull(method(meshPart, "setHidden", "(Z)V"));
        ClassNode staticMesh = type(epic, EPIC + "api/client/model/StaticMesh");
        assertEquals(1, calls(method(staticMesh, "initialize", "()V"), "java/util/Collection", "forEach", null));
        MethodNode reset = method(staticMesh, "lambda$initialize$0", null);
        assertEquals(1, calls(reset, EPIC + "api/client/model/MeshPart", "setHidden", "(Z)V"));
        assertTrue(java.util.Arrays.stream(reset.instructions.toArray()).anyMatch(i -> i.getOpcode() == org.objectweb.asm.Opcodes.ICONST_0));
        ClassNode thirdPerson = type(epic, EPIC + "client/renderer/patched/entity/PatchedLivingEntityRenderer");
        assertEquals(1, calls(method(thirdPerson, "prepareModel", null), EPIC + "api/client/model/SkinnedMesh", "initialize", "()V"));
        ClassNode armor = type(epic, EPIC + "client/renderer/patched/layer/WearableItemLayer");
        assertEquals(1, calls(method(armor, "renderLayer", "(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I[Lyesman/epicfight/api/utils/math/OpenMatrix4f;FFFF)V"), EPIC + "api/client/model/SkinnedMesh", "initialize", "()V"));
        ClassNode firstPerson = type(epic, EPIC + "client/renderer/FirstPersonRenderer");
        assertEquals(2, calls(method(firstPerson, "render", null), EPIC + "client/mesh/HumanoidMesh", "draw", null));
        assertEquals(1, calls(method(firstPerson, "<init>", null), EPIC + "client/renderer/patched/layer/PatchedItemInHandLayer", "<init>", null));
        assertTrue(Files.readString(SOURCE.resolve("mixin/epicfight/WearableItemLayerMixin.java")).contains("firstPersonModel && BcFixesClientConfig.hideFirstPersonLimbs()"));
        // No hook is added to the native shared initialize/reset or held-item layer.
        String mixins = Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"));
        assertFalse(mixins.contains("StaticMeshMixin"));
        assertFalse(mixins.contains("PatchedItemInHandLayerMixin"));
    }
}
