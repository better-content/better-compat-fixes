package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;
import com.bettercontent.bettercompatfixes.client.CombatMiningPolicy;
import com.bettercontent.bettercompatfixes.client.CombatMiningPolicy.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Executes the compiled routing boundary handlers and checks the pinned upstream ABI. */
public class CombatMiningBoundaryTest {
    @Test void compiledAnimationAndInteractionHandlersUseIdenticalRoutes() throws Exception {
        var animation = handler("CombatFirstLocalPlayerMixin", "$routeAttack");
        var interaction = handler("CombatMiningInteractionMixin", "$allowKnifeMining");
        for (Route route : Route.values()) {
            Routing.route = route;
            var callback = new CallbackInfoReturnable<Boolean>("canPlayAttackAnimation", true, false);
            animation.getMethod("run", CallbackInfoReturnable.class).invoke(animation.getConstructor().newInstance(), callback);
            assertEquals(route != Route.NATIVE, callback.isCancelled());
            if (route != Route.NATIVE) assertEquals(route == Route.COMBAT, callback.getReturnValue());
            for (boolean preferred : new boolean[]{false, true})
                assertEquals(preferred && route != Route.MINE, interaction.getMethod("run", boolean.class).invoke(null, preferred));
        }
    }

    @Test void precedenceAndLocalAdmissionAreExplicit() {
        for (ItemKind kind : ItemKind.values()) for (Target target : Target.values())
        for (boolean locked : new boolean[]{false, true}) {
            assertEquals(Route.NATIVE, CombatMiningPolicy.decide(false, true, kind, target, locked, true, true));
            assertEquals(Route.NATIVE, CombatMiningPolicy.decide(true, false, kind, target, locked, true, true));
            if (kind == ItemKind.OTHER || target == Target.NONLIVING)
                assertEquals(Route.NATIVE, CombatMiningPolicy.decide(true, true, kind, target, locked, true, true));
            else if (locked)
                assertEquals(Route.COMBAT, CombatMiningPolicy.decide(true, true, kind, target, true, true, true));
        }
        assertEquals(Route.COMBAT, CombatMiningPolicy.decide(true, true, ItemKind.BUTCHER_KNIFE, Target.BLOCK, false, false, true));
        assertEquals(Route.COMBAT, CombatMiningPolicy.decide(true, true, ItemKind.BUTCHER_KNIFE, Target.BLOCK, false, true, false));
    }

    @Test void upstreamAndProductionTargetsArePinnedAndClientOnly() throws Exception {
        var nativeJar = Path.of(System.getProperty("user.home"), ".cache/bc/packwiz-downloads/mods/epic-fight-20.14.17-mc1.20.1-forge.jar");
        try (var z = new ZipFile(nativeJar.toFile())) {
            var local = read(z, "yesman/epicfight/client/world/capabilites/entitypatch/player/LocalPlayerPatch.class");
            assertEquals(1, local.methods.stream().filter(m -> m.name.equals("canPlayAttackAnimation") && m.desc.equals("()Z")).count());
            var events = read(z, "yesman/epicfight/client/events/engine/ControlEngine$Events.class");
            var method = events.methods.stream().filter(m -> m.name.equals("interactionEvent")).findFirst().orElseThrow();
            boolean foundPreference = false, foundContains = false;
            for (var n : method.instructions) {
                if (n instanceof FieldInsnNode f && f.name.equals("combatPreferredItems")) foundPreference = true;
                if (n instanceof MethodInsnNode call && call.owner.equals("java/util/Set") && call.name.equals("contains")) {
                    assertTrue(foundPreference, "Ordinal zero must still belong to combatPreferredItems"); foundContains = true; break;
                }
            }
            assertTrue(foundContains);
        }
        try (var z = new ZipFile(Path.of("build/libs/better-compat-fixes-0.1.9.jar").toFile())) {
            var adapter = read(z, "com/bettercontent/bettercompatfixes/client/CombatMiningRouting.class");
            var current = adapter.methods.stream().filter(m -> m.name.equals("current")).findFirst().orElseThrow();
            boolean mainHand = false, nativePermission = false;
            for (var n : current.instructions) if (n instanceof MethodInsnNode call) {
                assertNotEquals("getMainHandItem", call.name, "Production MC calls must be reobfuscated");
                assertNotEquals("canAttackBlock", call.name);
                if (call.name.equals("m_21205_")) mainHand = true;
                if (call.name.equals("m_6777_")) nativePermission = true;
            }
            assertTrue(mainHand); assertTrue(nativePermission);
            String config = new String(z.getInputStream(z.getEntry("better_compat_fixes.tcon_combat_first.mixins.json")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(config.contains("\"client\"")); assertFalse(config.contains("\"mixins\""));
            for (String name : new String[]{"CombatFirstLocalPlayerMixin", "CombatFirstMinecraftMixin", "CombatMiningInteractionMixin"})
                assertNotNull(z.getEntry("com/bettercontent/bettercompatfixes/mixin/tconcombat/" + name + ".class"));
        }
    }
    private static ClassNode read(ZipFile z, String path) throws Exception {
        var node = new ClassNode(); new ClassReader(z.getInputStream(z.getEntry(path))).accept(node, 0); return node;
    }
    private static Class<?> handler(String name, String suffix) throws Exception {
        var source = new ClassNode();
        try (var input = CombatMiningBoundaryTest.class.getClassLoader().getResourceAsStream(
                "com/bettercontent/bettercompatfixes/mixin/tconcombat/" + name + ".class")) {
            assertNotNull(input); new ClassReader(input).accept(source, 0);
        }
        var method = source.methods.stream().filter(m -> m.name.endsWith(suffix)).findFirst().orElseThrow();
        method.name = "run"; method.access = Opcodes.ACC_PUBLIC | (method.access & Opcodes.ACC_STATIC);
        var output = new ClassNode(); output.version = Opcodes.V17; output.access = Opcodes.ACC_PUBLIC;
        output.name = "com/bettercontent/bettercompatfixes/Isolated" + name; output.superName = "java/lang/Object";
        output.methods.add(method);
        var ctor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ctor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        ctor.instructions.add(new InsnNode(Opcodes.RETURN)); output.methods.add(ctor);
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        output.accept(new ClassRemapper(writer, new Remapper() {
            @Override public String map(String type) {
                return type.equals("com/bettercontent/bettercompatfixes/client/CombatMiningRouting") ? Type.getInternalName(Routing.class) : type;
            }
        }));
        var bytes = writer.toByteArray();
        return new ClassLoader(CombatMiningBoundaryTest.class.getClassLoader()) { Class<?> emit() { return defineClass(null, bytes, 0, bytes.length); } }.emit();
    }
    public static class Routing { public static Route route; public static Route current() { return route; } }
}
