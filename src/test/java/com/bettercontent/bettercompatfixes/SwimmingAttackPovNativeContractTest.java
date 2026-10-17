package com.bettercontent.bettercompatfixes;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Inspects the reproducibly fetched production Epic Fight archive, not historical dumps. */
final class SwimmingAttackPovNativeContractTest {
    private static final String E = "yesman/epicfight/";
    private static final String PLAYER = E + "client/world/capabilites/entitypatch/player/LocalPlayerPatch";
    private static final String ANIMATOR = E + "api/client/animation/ClientAnimator";
    private static final String LAYER = E + "api/client/animation/Layer";
    private static final Path SOURCE = Path.of("src/main/java/com/bettercontent/bettercompatfixes");

    private static Path pinned() throws Exception {
        Path path = Path.of(System.getProperty("bc.markerRender.8049910"));
        assertEquals("faa8eddad9d80adb9c4ba81fd0a8a21c81989d3d", HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path))));
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

    private static MethodNode method(ClassNode type, String name, String desc) {
        return type.methods.stream().filter(m -> m.name.equals(name) && (desc == null || m.desc.equals(desc)))
                .findFirst().orElseThrow();
    }

    private static long calls(MethodNode method, String owner, String name, String desc) {
        return Arrays.stream(method.instructions.toArray()).filter(i -> i instanceof MethodInsnNode call
                && call.owner.equals(owner) && call.name.equals(name)
                && (desc == null || call.desc.equals(desc))).count();
    }

    private static boolean field(MethodNode method, String owner, String name, int opcode) {
        return Arrays.stream(method.instructions.toArray()).anyMatch(i -> i instanceof FieldInsnNode f
                && f.owner.equals(owner) && f.name.equals(name) && f.getOpcode() == opcode);
    }

    @Test void nonStiffCombosPlayOnAClientCompositeOverlayWhileServerStillAnimates() throws Exception {
        MethodNode attack = method(type(pinned(), E + "skill/BasicAttack"), "executeOnServer", null);
        assertTrue(field(attack, E + "world/gamerule/EpicFightGameRules", "STIFF_COMBO_ATTACKS", Opcodes.GETSTATIC));
        assertTrue(field(attack, E + "network/common/AnimatorControlPacket$Action", "PLAY_CLIENT", Opcodes.GETSTATIC));
        assertTrue(field(attack, E + "network/common/AnimatorControlPacket$Layer", "COMPOSITE_LAYER", Opcodes.GETSTATIC));
        assertTrue(field(attack, E + "network/common/AnimatorControlPacket$Priority", "HIGHEST", Opcodes.GETSTATIC));
        assertEquals(1, calls(attack, E + "api/animation/Animator", "playAnimation", null));
        ClassNode packet = type(pinned(), E + "network/server/SPAnimatorControl");
        assertEquals(1, packet.methods.stream().mapToLong(m -> calls(m, ANIMATOR, "playAnimationAt", null)).sum());
    }

    @Test void nativePovSelectorCanFallThroughAnAttackToSwimmingAndRenderOnlyThatPov() throws Exception {
        Path epic = pinned();
        try (ZipFile zip = new ZipFile(epic.toFile())) {
            assertNotNull(zip.getEntry("assets/epicfight/animmodels/animations/biped/living/pov/swim.json"));
            assertNotNull(zip.getEntry("assets/epicfight/animmodels/animations/biped/living/pov/float.json"));
            assertNull(zip.getEntry("assets/epicfight/animmodels/animations/biped/combat/pov/sword_auto1.json"));
        }
        MethodNode selector = method(type(epic, PLAYER), "lambda$clientTick$2", null);
        assertTrue(field(selector, E + "api/client/animation/property/ClientAnimationProperties",
                "POV_ANIMATION", Opcodes.GETSTATIC));
        assertEquals(1, calls(selector, "java/util/Optional", "isPresent", "()Z"));
        assertTrue(Arrays.stream(selector.instructions.toArray()).noneMatch(i -> i instanceof TypeInsnNode t
                && t.desc.equals(E + "api/animation/types/AttackAnimation")));
        MethodNode traversal = method(type(epic, ANIMATOR), "iterVisibleLayersUntilFalse",
                "(Ljava/util/function/Function;)Z");
        assertEquals(1, calls(traversal, LAYER + "$Priority", "highers", null));
        assertTrue(Arrays.stream(traversal.instructions.toArray()).anyMatch(i -> i instanceof IincInsnNode inc
                && inc.incr == -1), "Native traversal visits high priorities first");
        MethodNode renderer = method(type(epic, E + "client/renderer/FirstPersonRenderer"), "render",
                "(Lnet/minecraft/client/player/LocalPlayer;L" + PLAYER
                        + ";Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;"
                        + "Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;IF)V");
        assertTrue(calls(renderer, PLAYER, "getPovSettings", null) > 0);
        assertEquals(1, calls(renderer, PLAYER + "$FirstPersonLayer", "getEnabledPose", null));
        assertEquals(1, calls(renderer, E + "api/animation/Animator", "getPose", "(F)L" + E + "api/animation/Pose;"));
    }

    @Test void nativeOffEmptiesFirstPersonLayerAndClientTickClearsPovSettings() throws Exception {
        Path epic = pinned();
        MethodNode constructor = method(type(epic, PLAYER + "$FirstPersonLayer"), "<init>", null);
        assertTrue(Arrays.stream(constructor.instructions.toArray()).anyMatch(i -> i instanceof MethodInsnNode c
                && c.owner.equals(LAYER) && c.name.equals("<init>")
                && c.getPrevious().getOpcode() == Opcodes.ACONST_NULL));
        MethodNode off = method(type(epic, LAYER), "off", "(L" + E + "world/capabilities/entitypatch/LivingEntityPatch;)V");
        assertEquals(1, calls(off, LAYER, "disableLayer", "()V"));
        var priority = Arrays.stream(off.instructions.toArray()).filter(i -> i instanceof FieldInsnNode f
                && f.owner.equals(LAYER) && f.name.equals("priority")).findFirst().orElseThrow();
        var skipDisable = (JumpInsnNode) priority.getNext();
        assertEquals(Opcodes.IFNONNULL, skipDisable.getOpcode());
        var disableCall = Arrays.stream(off.instructions.toArray()).filter(i -> i instanceof MethodInsnNode c
                && c.owner.equals(LAYER) && c.name.equals("disableLayer")).findFirst().orElseThrow();
        assertTrue(off.instructions.indexOf(priority) < off.instructions.indexOf(disableCall));
        assertTrue(off.instructions.indexOf(disableCall) < off.instructions.indexOf(skipDisable.label),
                "Only non-null-priority layers skip immediate disable");
        MethodNode firstPersonOff = method(type(epic, PLAYER + "$FirstPersonLayer"), "off", "()V");
        assertEquals(1, calls(firstPersonOff, LAYER, "off", null));
        MethodNode disable = method(type(epic, LAYER), "disableLayer", "()V");
        assertTrue(field(disable, E + "gameasset/Animations", "EMPTY_ANIMATION", Opcodes.GETSTATIC));
        assertEquals(1, calls(disable, E + "api/animation/AnimationPlayer", "setPlayAnimation", null));
        MethodNode tick = method(type(epic, PLAYER), "clientTick",
                "(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingTickEvent;)V");
        assertEquals(1, calls(tick, ANIMATOR, "iterVisibleLayersUntilFalse", "(Ljava/util/function/Function;)Z"));
        var clearSettings = Arrays.stream(tick.instructions.toArray()).filter(i -> i instanceof FieldInsnNode f
                && f.owner.equals(PLAYER) && f.name.equals("povSettings") && f.getOpcode() == Opcodes.PUTFIELD
                && f.getPrevious().getOpcode() == Opcodes.ACONST_NULL).findFirst().orElseThrow();
        var firstPersonUpdate = Arrays.stream(tick.instructions.toArray()).filter(i -> i instanceof MethodInsnNode c
                && c.owner.equals(PLAYER + "$FirstPersonLayer") && c.name.equals("update")).findFirst().orElseThrow();
        var emptyCheck = Arrays.stream(tick.instructions.toArray()).filter(i -> i instanceof FieldInsnNode f
                && f.owner.equals(E + "gameasset/Animations") && f.name.equals("EMPTY_ANIMATION"))
                .reduce((first, last) -> last).orElseThrow();
        assertTrue(tick.instructions.indexOf(firstPersonUpdate) < tick.instructions.indexOf(emptyCheck));
        assertTrue(tick.instructions.indexOf(emptyCheck) < tick.instructions.indexOf(clearSettings));
        assertEquals(Opcodes.IFEQ, emptyCheck.getNext().getNext().getOpcode(),
                "The post-update equality check must skip clearing only for a non-empty POV animation");
    }

    @Test void wrapperIsClientOnlyPinnedAndDoesNotChangeCombatOrLimbHiding() throws Exception {
        var config = JsonParser.parseString(Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json")))
                .getAsJsonObject();
        assertTrue(config.getAsJsonArray("client").toString().contains("epicfight.LocalPlayerAttackPovMixin"));
        assertFalse(config.getAsJsonArray("mixins").toString().contains("LocalPlayerAttackPovMixin"));
        String plugin = Files.readString(SOURCE.resolve("mixin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("EPIC_FIGHT_ATTACK_POV_MIXIN.equals(mixinClassName)"));
        assertTrue(plugin.contains("hasVersion(mods, \"epicfight\", \"20.14.17\")"));
        String hook = Files.readString(SOURCE.resolve("mixin/epicfight/LocalPlayerAttackPovMixin.java"));
        assertTrue(hook.contains("motion == LivingMotions.SWIM || motion == LivingMotions.FLOAT"));
        assertTrue(hook.contains("!layer.isOff()") && hook.contains("getRealAnimation().get() instanceof AttackAnimation"));
        assertTrue(hook.contains("getProperty(ClientAnimationProperties.POV_ANIMATION).isEmpty()"));
        assertTrue(hook.contains("() -> player.getFirstPersonLayer().off()"));
        assertTrue(hook.contains("iterVisibleLayersUntilFalse(Ljava/util/function/Function;)Z"));
        assertTrue(hook.contains("require = 1") && hook.contains("allow = 1"));
        assertFalse(hook.contains("playAnimation(") || hook.contains("setHidden") || hook.contains("STIFF_COMBO"));
    }
}
