package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;

import com.bettercontent.bettercompatfixes.client.DodgeKeyConflictPolicy;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Executes the hash-pinned native guard with tiny input doubles; not a Forge/client verdict. */
final class EpicParCoolDodgeKeyConflictTest {
    private static final String GUARD = "lambda$onSetup$17";
    private static final String SKILL_GETTER = "Lyesman/epicfight/skill/SkillContainer;getSkill()Lyesman/epicfight/skill/Skill;";

    @Test
    void nativeGuardPreservesStaminaAndRealKeyConflictsButAllowsIndependentArrows() throws Exception {
        Method nativeGuard = guard(false, false);
        Method arrowGuard = guard(true, true);
        Method directGuard = guard(true, false);
        Object unknown = new Object(); // Both unbound bindings share this canonical key.
        PlayerPatch player = new PlayerPatch();
        Keys.DODGE = new Key(unknown);
        Bindings.dodge = new Key(unknown);
        assertTrue(cancelled(nativeGuard, player), "Reproduce native both-unbound collision");
        assertFalse(cancelled(arrowGuard, player), "Independent arrow request must not collide");
        assertTrue(cancelled(directGuard, player), "Direct-key behavior must stay native");

        player.stamina = false;
        assertTrue(cancelled(arrowGuard, player), "Arrow requests still require Epic Fight stamina");
        player.creative = true;
        assertFalse(cancelled(arrowGuard, player), "Retain native creative stamina exemption");
        player.creative = false;
        player.stamina = true;
        Object bound = new Object();
        Keys.DODGE = new Key(bound);
        Bindings.dodge = new Key(bound);
        assertTrue(cancelled(directGuard, player), "Real shared-key collision remains protected");
        assertFalse(cancelled(arrowGuard, player));
        Bindings.dodge = new Key(new Object());
        assertFalse(cancelled(directGuard, player), "Distinct keys must not collide");
        player.container.skill = null;
        Bindings.dodge = Keys.DODGE;
        assertFalse(cancelled(directGuard, player), "No equipped dodge skill means no collision");
        player.stamina = false;
        assertTrue(cancelled(directGuard, player));
        assertTrue(cancelled(arrowGuard, player));
    }

    @Test
    void registeredMixinChangesOnlyTheSkillExpressionAndIsVersionGated() throws Exception {
        Path root = Path.of("src/main");
        String mixin = Files.readString(root.resolve("java/com/bettercontent/bettercompatfixes/mixin/epicfight/EpicParCoolDodgeKeyConflictMixin.java"));
        assertTrue(mixin.contains("method = \"" + GUARD + "\""));
        assertTrue(mixin.contains(SKILL_GETTER));
        assertTrue(mixin.contains("require = 1, expect = 1, allow = 1"));
        assertTrue(mixin.contains("original, ArrowDoubleTapDodgeClient.pendingMove() != null"));
        assertFalse(mixin.contains("@Inject")); // Never return early around native stamina.
        assertTrue(Files.readString(root.resolve("resources/better_compat_fixes.mixins.json"))
                .contains("epicfight.EpicParCoolDodgeKeyConflictMixin"));
        String plugin = Files.readString(root.resolve("java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("EpicParCoolDodgeKeyConflictMixin"));
        assertTrue(plugin.contains("hasVersion(mods, \"epicparcool\", \"20.12.0.1\")"));
        Object equipped = new Object();
        assertSame(equipped, DodgeKeyConflictPolicy.skillForDirectKeyConflict(equipped, false));
        assertNull(DodgeKeyConflictPolicy.skillForDirectKeyConflict(equipped, true));
        assertNull(DodgeKeyConflictPolicy.skillForDirectKeyConflict(null, false));
    }

    private static boolean cancelled(Method guard, PlayerPatch player) throws Exception {
        return (Boolean) guard.invoke(null, player, new Object());
    }

    private static Method guard(boolean applyExpressionHook, boolean arrows) throws Exception {
        Path jar = Path.of(System.getProperty("bc.epicparcool.native"));
        assertEquals("d38194b73c91e5af84ef28a7a07e0eb4340ef76d", HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar))));
        ClassNode type = new ClassNode();
        try (ZipFile zip = new ZipFile(jar.toFile());
             var in = zip.getInputStream(zip.getEntry("com/yesman/epicparcool/event/ParCoolEvents.class"))) {
            new ClassReader(in).accept(type, ClassReader.SKIP_FRAMES);
        }
        MethodNode setup = type.methods.stream().filter(m -> m.name.equals("onSetup")).findFirst().orElseThrow();
        boolean dodge = false;
        boolean cancellationMapping = false;
        boolean mapped = false;
        for (AbstractInsnNode insn : setup.instructions) {
            if (insn instanceof FieldInsnNode f && f.name.equals("PARCOOL_ACTION_CANCEL_EVENTS")) cancellationMapping = true;
            if (cancellationMapping && insn instanceof LdcInsnNode l && l.cst instanceof Type t)
                dodge = t.getInternalName().equals("com/alrex/parcool/common/action/impl/Dodge");
            if (cancellationMapping && dodge && insn instanceof InvokeDynamicInsnNode dynamic) {
                for (Object arg : dynamic.bsmArgs)
                    if (arg instanceof Handle h && h.getName().equals(GUARD)) mapped = true;
            }
        }
        assertTrue(mapped, "Guard must still be registered for Dodge cancellation");
        MethodNode method = type.methods.stream().filter(m -> m.name.equals(GUARD)).findFirst().orElseThrow();
        int hooks = 0;
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof MethodInsnNode call && ("L" + call.owner + ";" + call.name + call.desc).equals(SKILL_GETTER)) {
                hooks++;
                if (applyExpressionHook) {
                    InsnList hook = new InsnList();
                    hook.add(new InsnNode(arrows ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(DodgeKeyConflictPolicy.class),
                            "skillForDirectKeyConflict", "(Ljava/lang/Object;Z)Ljava/lang/Object;", false));
                    hook.add(new TypeInsnNode(Opcodes.CHECKCAST, "yesman/epicfight/skill/Skill"));
                    method.instructions.insert(call, hook);
                }
            }
        }
        assertEquals(1, hooks, "Exact required production expression anchor");
        // Remap only external input types to deterministic doubles. Preserve native branches,
        // key identity comparison, skill check, creative exemption and stamina arithmetic.
        Map<String, String> names = new LinkedHashMap<>();
        names.put("yesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch", Type.getInternalName(PlayerPatch.class));
        names.put("yesman/epicfight/client/input/EpicFightKeyMappings", Type.getInternalName(Keys.class));
        names.put("com/alrex/parcool/client/input/KeyBindings", Type.getInternalName(Bindings.class));
        names.put("net/minecraft/client/KeyMapping", Type.getInternalName(Key.class));
        names.put("com/mojang/blaze3d/platform/InputConstants$Key", "java/lang/Object");
        names.put("yesman/epicfight/skill/SkillContainer", Type.getInternalName(Container.class));
        names.put("yesman/epicfight/skill/SkillSlots", Type.getInternalName(Slots.class));
        names.put("yesman/epicfight/skill/SkillSlot", Type.getInternalName(Slot.class));
        names.put("yesman/epicfight/skill/Skill", Type.getInternalName(Skill.class));
        names.put("yesman/epicfight/gameasset/EpicFightSkills", Type.getInternalName(Skills.class));
        names.put("com/alrex/parcool/common/action/Action", "java/lang/Object");
        names.put("net/minecraft/world/entity/player/Player", Type.getInternalName(PlayerPatch.class));
        names.put("net/minecraft/world/entity/Entity", Type.getInternalName(PlayerPatch.class));
        method.desc = remap(method.desc, names);
        method.signature = null;
        method.localVariables = null;
        method.name = "evaluate";
        method.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode c) { c.owner = remap(c.owner, names); c.desc = remap(c.desc, names); }
            if (insn instanceof FieldInsnNode f) { f.owner = remap(f.owner, names); f.desc = remap(f.desc, names); }
            if (insn instanceof TypeInsnNode t) t.desc = remap(t.desc, names);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "NativeDodgeGuard", null, "java/lang/Object", null);
        method.accept(writer);
        writer.visitEnd();
        class Loader extends ClassLoader {
            Loader() { super(EpicParCoolDodgeKeyConflictTest.class.getClassLoader()); }
            Class<?> loadGuard() { byte[] b = writer.toByteArray(); return defineClass(null, b, 0, b.length); }
        }
        return new Loader().loadGuard().getMethod("evaluate", PlayerPatch.class, Object.class);
    }

    private static String remap(String text, Map<String, String> names) {
        for (var entry : names.entrySet()) text = text.replace(entry.getKey(), entry.getValue());
        return text;
    }

    public static final class Key {
        private final Object key;
        Key(Object key) { this.key = key; }
        public Object getKey() { return key; }
    }
    public static final class Keys { public static Key DODGE; }
    public static final class Bindings {
        static Key dodge;
        public static Key getKeyDodge() { return dodge; }
    }
    public static class Slot { }
    public static final class Slots extends Slot { public static final Slots DODGE = new Slots(); }
    public static final class Skill { public float getConsumption() { return 1.0f; } }
    public static final class Skills { public static final Skill ROLL = new Skill(); }
    public static final class Container {
        Skill skill = new Skill();
        public Skill getSkill() { return skill; }
    }
    public static final class PlayerPatch {
        boolean stamina = true;
        boolean creative;
        final Container container = new Container();
        public Container getSkill(Slot slot) { return container; }
        public PlayerPatch getOriginal() { return this; }
        public boolean m_7500_() { return creative; }
        public boolean hasStamina(float cost) { return stamina; }
    }
}
