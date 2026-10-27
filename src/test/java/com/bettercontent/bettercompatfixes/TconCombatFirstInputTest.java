package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;

/** Executes the installed EF decision with input doubles, not a rendered-client verdict. */
public final class TconCombatFirstInputTest {
    private static final String PATCH = "yesman/epicfight/client/world/capabilites/entitypatch/player/LocalPlayerPatch";
    private static final String CAMERA = "yesman/epicfight/api/client/camera/EpicFightCameraAPI";
    private static final String RENDER = "yesman/epicfight/client/events/engine/RenderEngine";
    private static final String CONFIG = "yesman/epicfight/config/ClientConfig";
    private static final Map<String, Class<?>> TYPES = types();

    @Test
    public void blockClicksBecomeCombatWithoutChangingOtherDecisions() throws Exception {
        var nativeDecision = decision(false);
        var changedDecision = decision(true);
        for (boolean eligible : new boolean[]{false, true}) {
            for (boolean adaptive : new boolean[]{false, true}) {
                for (boolean preferred : new boolean[]{false, true}) {
                    for (boolean suitable : new boolean[]{false, true}) {
                        for (boolean canAttack : new boolean[]{false, true}) {
                            for (Hit hit : new Hit[]{null, new Hit(), new BlockHit(),
                                    new EntityHit(new Living()), new EntityHit(new Part()), new EntityHit(new Entity())}) {
                                for (boolean vanilla : new boolean[]{false, true}) {
                                    var a = patch(nativeDecision, eligible, vanilla, hit, suitable, canAttack);
                                    var b = patch(changedDecision, eligible, vanilla, hit, suitable, canAttack);
                                    Config.preferenceWork.check = adaptive;
                                    Config.combatPreferredItems = preferred ? Set.of(((Player) a.original).stack.item, ((Player) b.original).stack.item) : Set.of();
                                    Camera.instance.tps = false;
                                    Camera.instance.locked = false;
                                    boolean before = evaluate(a);
                                    boolean after = evaluate(b);
                                    boolean override = eligible && !vanilla && hit instanceof BlockHit;
                                    assertEquals(override || before, after, "Only eligible block rejection may change");
                                    if (!override) assertEquals(before, after);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void thirdPersonSelectionAndLockOnStayNative() throws Exception {
        var nativeDecision = decision(false);
        var changedDecision = decision(true);
        Config.preferenceWork.check = true;
        Config.combatPreferredItems = Set.of();
        for (boolean eligible : new boolean[]{false, true}) {
            for (boolean near : new boolean[]{false, true}) {
                for (boolean locked : new boolean[]{false, true}) {
                    var a = patch(nativeDecision, eligible, false, new BlockHit(), false, true);
                    var b = patch(changedDecision, eligible, false, new BlockHit(), false, true);
                    Camera.instance.tps = true;
                    Camera.instance.locked = locked;
                    Camera.instance.hit = new EntityHit(new Living());
                    Camera.instance.hit.position.distance = near ? 1 : 100;
                    // Far TPS ray falls back to Minecraft's block hit; a near entity ray remains unchanged.
                    boolean before = evaluate(a);
                    boolean after = evaluate(b);
                    assertEquals(before || (eligible && !near), after);
                }
            }
        }
        Camera.instance.tps = false;
        Camera.instance.locked = false;
    }

    @Test
    public void registrationAndGuardsAreNarrowAndOptionalSafe() throws Exception {
        var root = Path.of("src/main/java/com/bettercontent/bettercompatfixes");
        var routing = Files.readString(root.resolve("mixin/tconcombat/CombatFirstLocalPlayerMixin.java"));
        assertTrue(routing.contains("@Local HitResult selectedHit"));
        assertTrue(routing.contains("RenderEngine.asBlockHitResult(selectedHit)"));
        assertTrue(routing.contains("PreferenceWork;checkHitResult()Z"));
        assertTrue(routing.contains("require = 1"));
        var guard = Files.readString(root.resolve("mixin/tconcombat/CombatFirstMinecraftMixin.java"));
        for (String token : new String[]{"startAttack()Z", "m_202354_()Z", "continueAttack(Z)V", "m_91386_(Z)V",
                "priority = 1100", "stopDestroyBlock()", "cir.setReturnValue(false)", "ci.cancel()"})
            assertTrue(guard.contains(token), token);
        for (String forbidden : new String[]{"keyAttack", "consumeClick", "setDown(", "swing(", "startUseItem", "setBlock("})
            assertFalse(guard.contains(forbidden), forbidden);
        var bridge = Files.readString(root.resolve("compat/tconcombat/TconCombatFirstIntegration.java"));
        for (String token : new String[]{"instanceof IModifiable", "isDataLoaded()", "hasHook(ToolHooks.IS_EFFECTIVE)",
                "getItemStackCapability(stack)", "getResult(stack)", "!capability.isEmpty()"}) assertTrue(bridge.contains(token));
        for (String forbidden : new String[]{"WeaponCapability", "isBroken(", "getOffhandItem", "getStats(", "isCorrectToolForDrops"})
            assertFalse(bridge.contains(forbidden));
        var facade = Files.readString(root.resolve("compat/TconCombatFirstPolicy.java"));
        assertTrue(facade.contains("isLoaded(\"tconstruct\") && mods.isLoaded(\"epicfight\")"));
        assertFalse(facade.contains("import slimeknights"));
        assertFalse(facade.contains("import yesman"));
        var events = Files.readString(root.resolve("compat/TconCombatFirstEvents.java"));
        assertTrue(events.contains("Action.ABORT"));
        assertTrue(events.contains("event.setUseBlock(Event.Result.DENY)"));
        assertTrue(events.contains("event.setUseItem(Event.Result.DENY)"));
        assertFalse(events.contains("isCreative()"));
        assertFalse(events.contains("BreakSpeed"));
        var config = Files.readString(Path.of("src/main/resources/better_compat_fixes.tcon_combat_first.mixins.json"));
        assertTrue(config.substring(config.indexOf("\"client\"")).contains("CombatFirstMinecraftMixin"));
        assertTrue(config.contains("CombatFirstLocalPlayerMixin"));
        assertTrue(config.contains("TconCombatFirstMixinPlugin"));
        var plugin = Files.readString(root.resolve("compat/tconcombat/TconCombatFirstMixinPlugin.java"));
        assertTrue(plugin.contains("getModFileById(\"tconstruct\") == null"));
        assertTrue(plugin.contains("getModFileById(\"epicfight\") == null"));
        assertTrue(plugin.contains("\"20.14.17\".equals(mod.getVersion().toString())"));
        var build = Files.readString(Path.of("build.gradle.kts"));
        assertTrue(build.contains("better_compat_fixes.tcon_combat_first.mixins.json"));
        assertTrue(build.contains("config(\"better_compat_fixes.tcon_combat_first.mixins.json\")"));
        var legacy = Files.readString(root.resolve("compat/plugin/BetterContentMixinPlugin.java"));
        assertFalse(legacy.contains("tconcombat"), "Do not change the bytecode-pinned legacy plugin");
    }

    private static Class<? extends Patch> decision(boolean changed) throws Exception {
        var jar = Path.of(System.getProperty("tconCombat.epicJar", System.getProperty("user.home")
                + "/.cache/bc/packwiz-downloads/mods/epic-fight-20.14.17-mc1.20.1-forge.jar"));
        var type = new ClassNode();
        try (var zip = new ZipFile(jar.toFile())) {
            new ClassReader(zip.getInputStream(zip.getEntry(PATCH + ".class"))).accept(type, 0);
        }
        var method = type.methods.stream().filter(m -> m.name.equals("canPlayAttackAnimation") && m.desc.equals("()Z"))
                .findFirst().orElseThrow();
        int targets = 0;
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(CONFIG + "$PreferenceWork")
                    && call.name.equals("checkHitResult") && call.desc.equals("()Z")) {
                targets++;
                if (changed) {
                    var injected = new InsnList();
                    injected.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    injected.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(TconCombatFirstInputTest.class),
                            "adaptive", "(Z" + Type.getDescriptor(Hit.class) + Type.getDescriptor(Patch.class) + ")Z", false));
                    method.instructions.insert(call, injected);
                }
            }
        }
        assertEquals(1, targets, "Pinned EF injection must match exactly once");
        assertTrue(method.localVariables.stream().anyMatch(v -> v.index == 2
                && v.desc.equals("Lnet/minecraft/world/phys/HitResult;")), "@Local selected HitResult ABI");
        var output = new ClassNode();
        output.version = Opcodes.V17;
        output.access = Opcodes.ACC_PUBLIC;
        output.name = "com/bettercontent/bettercompatfixes/CombatDecision" + (changed ? "Patched" : "Native");
        output.superName = Type.getInternalName(Patch.class);
        method.name = "evaluate";
        output.methods.add(method);
        var ctor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ctor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, output.superName, "<init>", "()V", false));
        ctor.instructions.add(new InsnNode(Opcodes.RETURN));
        output.methods.add(ctor);
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        output.accept(new ClassRemapper(writer, new Remapper() {
            @Override public String map(String name) { return TYPES.containsKey(name) ? Type.getInternalName(TYPES.get(name)) : name; }
        }));
        byte[] bytes = writer.toByteArray();
        return new ClassLoader(TconCombatFirstInputTest.class.getClassLoader()) {
            Class<?> emit() { return defineClass(null, bytes, 0, bytes.length); }
        }.emit().asSubclass(Patch.class);
    }

    public static boolean adaptive(boolean original, Hit selected, Patch patch) {
        return original && !(selected instanceof BlockHit && patch.eligible);
    }
    private static Patch patch(Class<? extends Patch> type, boolean eligible, boolean vanilla, Hit hit,
            boolean suitable, boolean canAttack) throws Exception {
        var patch = type.getConstructor().newInstance();
        patch.eligible = eligible; patch.vanilla = vanilla; patch.minecraft.f_91077_ = hit;
        ((Player) patch.original).stack.suitable = suitable;
        ((Player) patch.original).stack.item.canAttack = canAttack;
        return patch;
    }
    private static boolean evaluate(Patch patch) throws Exception { return (boolean) patch.getClass().getMethod("evaluate").invoke(patch); }
    private static Map<String, Class<?>> types() {
        var map = new HashMap<String, Class<?>>();
        map.put(PATCH, Patch.class); map.put(CAMERA, Camera.class); map.put(RENDER, Render.class);
        map.put(CONFIG, Config.class); map.put(CONFIG + "$PreferenceWork", Preference.class);
        map.put("yesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch$PlayerMode", Mode.class);
        String[][] names = {{"client/Minecraft", "Game"}, {"client/player/LocalPlayer", "Player"},
                {"world/entity/player/Player", "Player"}, {"world/entity/Entity", "Entity"}, {"world/entity/LivingEntity", "Living"},
                {"world/phys/HitResult", "Hit"}, {"world/phys/HitResult$Type", "HitType"}, {"world/phys/BlockHitResult", "BlockHit"},
                {"world/phys/EntityHitResult", "EntityHit"}, {"world/phys/Vec3", "Vec"}, {"core/BlockPos", "Pos"},
                {"world/item/ItemStack", "Stack"}, {"world/item/Item", "Item"}, {"client/multiplayer/ClientLevel", "Level"},
                {"world/level/Level", "Level"}, {"world/level/block/state/BlockState", "State"}};
        for (var pair : names) try { map.put("net/minecraft/" + pair[0], Class.forName(TconCombatFirstInputTest.class.getName() + "$" + pair[1])); }
            catch (ClassNotFoundException e) { throw new AssertionError(e); }
        map.put("net/minecraftforge/entity/PartEntity", Part.class);
        return map;
    }
    public static class Patch {
        public Entity original = new Player(); public Game minecraft = new Game(); public boolean eligible, vanilla;
        public boolean isVanillaMode() { return vanilla; } public Mode getPlayerMode() { return vanilla ? Mode.VANILLA : Mode.EPICFIGHT; }
    }
    public enum Mode { VANILLA, EPICFIGHT }
    public enum HitType { BLOCK }
    public static class Entity {}
    public static class Living extends Entity {}
    public static class Part extends Entity {}
    public static class Player extends Living {
        public Stack stack = new Stack(); public Vec m_146892_() { return new Vec(); }
        public double getBlockReach() { return 4; } public Stack m_21205_() { return stack; }
        public Level m_9236_() { return new Level(); }
    }
    public static class Vec { public double distance; public double m_82557_(Vec other) { return distance; } }
    public static class Hit { public Vec position = new Vec(); public Vec m_82450_() { return position; } }
    public static class BlockHit extends Hit { public Pos m_82425_() { return new Pos(); } }
    public static class EntityHit extends Hit { private final Entity entity; public EntityHit(Entity e) { entity = e; } public Entity m_82443_() { return entity; } }
    public static class Pos {}
    public static class State {}
    public static class Level { public State m_8055_(Pos pos) { return new State(); } }
    public static class Stack { public Item item = new Item(); public boolean suitable; public Item m_41720_() { return item; } public boolean m_41735_(State state) { return suitable; } }
    public static class Item { public boolean canAttack; public boolean m_6777_(State state, Level level, Pos pos, Player player) { return canAttack; } }
    public static class Game { public Hit f_91077_; public Level f_91073_ = new Level(); }
    public static class Camera {
        public static final Camera instance = new Camera(); public boolean tps, locked; public Hit hit;
        public static Camera getInstance() { return instance; } public boolean isTPSMode() { return tps; }
        public Hit getCrosshairHitResult() { return hit; } public boolean isLockingOnTarget() { return locked; }
    }
    public static class Preference { public boolean check; public boolean checkHitResult() { return check; } }
    public static class Config { public static Preference preferenceWork = new Preference(); public static Set<Item> combatPreferredItems = Set.of(); }
    public static class Render {
        public static EntityHit asEntityHitResult(Hit hit) { return hit instanceof EntityHit e ? e : null; }
        public static BlockHit asBlockHitResult(Hit hit) { return hit instanceof BlockHit b ? b : null; }
        public static boolean hitResultNotEquals(Hit hit, HitType type) { return !(hit instanceof BlockHit); }
    }
}
