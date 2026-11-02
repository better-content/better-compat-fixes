package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;
import static com.bettercontent.bettercompatfixes.client.CombatMiningPolicy.*;
import com.bettercontent.bettercompatfixes.client.CombatMiningPolicy;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/** Executes pinned EF bytecode with input doubles. Not rendered-client gameplay evidence. */
public final class TconCombatFirstInputTest {
    private static final String PATCH = "yesman/epicfight/client/world/capabilites/entitypatch/player/LocalPlayerPatch";
    private static final String CAMERA = "yesman/epicfight/api/client/camera/EpicFightCameraAPI";
    private static final String RENDER = "yesman/epicfight/client/events/engine/RenderEngine";
    private static final String CONFIG = "yesman/epicfight/config/ClientConfig";
    private static final String ENGINE = "yesman/epicfight/client/events/engine/ControlEngine";
    private static final Map<String, Class<?>> TYPES = types();

    @Test void reproducesLegacyTpsDeadClickAndNativeFirstPersonPredicateDisagreement() throws Exception {
        var legacy = decision(1);
        var p = patch(legacy, ItemKind.COMBAT_FIRST, new BlockHit());
        Config.preferenceWork.check = true; Config.combatPreferredItems = Set.of();
        Camera.instance.tps = true; Camera.instance.locked = false;
        Camera.instance.hit = new EntityHit(new Living()); Camera.instance.hit.position.distance = 1;
        assertFalse(evaluate(p), "Legacy combat choice loses TPS entity/block disagreement");
        assertTrue(p.kind == ItemKind.COMBAT_FIRST, "Existing server policy forbids mining");

        // Controlled native input-double counterexample, not a claim about a particular real block.
        p = patch(decision(0), ItemKind.BUTCHER_KNIFE, new BlockHit());
        Camera.instance.tps = false;
        ((Player)p.original).stack.suitable = true;
        ((Player)p.original).stack.speed = 1;
        Config.combatPreferredItems = Set.of(((Player)p.original).stack.item);
        assertFalse(evaluate(p), "Native correct-tool check chooses mining");
        var event = interaction(p, false);
        assertTrue(event.canceled, "Native separate speed check rejects that mining");
        assertFalse(event.swing);
    }

    @Test void combinedRouteMatrixCoversBothPerspectivesAndPreferences() throws Exception {
        var nativeType = decision(0); var fixed = decision(2);
        for (var kind : ItemKind.values()) for (boolean tps : bools())
        for (boolean near : bools()) for (boolean locked : bools())
        for (boolean adaptive : bools()) for (boolean preferred : bools())
        for (boolean suitable : bools()) for (float speed : new float[]{1, 2})
        for (Hit raw : hits()) for (Hit camera : hits()) {
            var a = patch(nativeType, kind, raw); var b = patch(fixed, kind, raw);
            Camera.instance.tps = tps; Camera.instance.locked = locked;
            Camera.instance.hit = camera;
            if (camera != null) camera.position.distance = near ? 1 : 100;
            Config.preferenceWork.check = adaptive;
            ((Player)a.original).stack.suitable = suitable; ((Player)b.original).stack.suitable = suitable;
            ((Player)a.original).stack.speed = speed; ((Player)b.original).stack.speed = speed;
            Config.combatPreferredItems = preferred ? Set.of(((Player)a.original).stack.item, ((Player)b.original).stack.item) : Set.of();
            Route route = route(b);
            boolean combat = evaluate(b);
            var event = raw instanceof BlockHit ? interaction(b, true) : new Event();
            boolean miningGuard = TconCombatFirstGuardTest.deniesContinuation(route);
            if (route == Route.COMBAT) {
                assertTrue(combat); assertTrue(miningGuard);
            } else if (route == Route.MINE) {
                assertFalse(combat); assertFalse(miningGuard); assertFalse(event.canceled);
            } else {
                assertEquals(evaluate(a), combat);
                if (raw instanceof BlockHit) assertEquals(interaction(a, false).canceled, event.canceled);
            }
        }
    }

    @Test void knifeLocalAdmissionAndNativeRestrictions() throws Exception {
        var fixed = decision(2);
        Camera.instance.tps = false; Camera.instance.locked = false;
        Config.preferenceWork.check = false;
        for (boolean canAttack : bools()) for (boolean mineable : bools()) {
            var p = patch(fixed, ItemKind.BUTCHER_KNIFE, new BlockHit());
            p.mineable = mineable; ((Player)p.original).stack.item.canAttack = canAttack;
            assertEquals(canAttack && mineable ? Route.MINE : Route.COMBAT, route(p));
            assertEquals(!(canAttack && mineable), evaluate(p));
        }
        var p = patch(fixed, ItemKind.BUTCHER_KNIFE, new BlockHit());
        Camera.instance.tps = true; Camera.instance.hit = new BlockHit(7);
        Camera.instance.hit.position.distance = 1;
        assertEquals(Route.COMBAT, route(p), "Do not mine a different block from the selected one");
        p.vanilla = true;
        assertEquals(Route.NATIVE, route(p)); assertFalse(evaluate(p));
        // A protection cancellation must never be cleared by our preference bypass.
        p.vanilla = false; Camera.instance.tps = false;
        var protectedEvent = new Event(); protectedEvent.canceled = true;
        interaction(p, true, protectedEvent);
        assertTrue(protectedEvent.canceled);
        p.kind = ItemKind.OTHER; assertEquals(Route.NATIVE, route(p));
        p.kind = ItemKind.COMBAT_FIRST; assertEquals(Route.COMBAT, route(p));
        p.kind = ItemKind.BUTCHER_KNIFE; assertEquals(Route.MINE, route(p));
    }

    @Test void registrationAndNarrowBoundaryContracts() throws Exception {
        var root = Path.of("src/main/java/com/bettercontent/bettercompatfixes");
        var routing = Files.readString(root.resolve("mixin/tconcombat/CombatFirstLocalPlayerMixin.java"));
        assertTrue(routing.contains("CombatMiningRouting.current()"));
        assertTrue(routing.contains("@At(\"HEAD\")"));
        assertTrue(routing.contains("require = 1"));
        var event = Files.readString(root.resolve("mixin/tconcombat/CombatMiningInteractionMixin.java"));
        assertTrue(event.contains("ordinal = 0")); assertTrue(event.contains("Route.MINE"));
        assertFalse(event.contains("setCanceled"));
        var adapter = Files.readString(root.resolve("client/CombatMiningRouting.java"));
        for (String s : new String[]{"\"additionalweaponry\", \"butcher_knife\"", "player.getMainHandItem()",
                "< reach * reach", "raw.getBlockPos().equals(blockHit.getBlockPos())", "!state.isAir()",
                "state.getDestroySpeed(minecraft.level, pos) >= 0", "canAttackBlock(state, minecraft.level, pos, player)"})
            assertTrue(adapter.contains(s), s);
        assertFalse(adapter.contains("isCorrectToolForDrops"));
        var config = Files.readString(Path.of("src/main/resources/better_compat_fixes.tcon_combat_first.mixins.json"));
        for (String s : new String[]{"CombatFirstMinecraftMixin", "CombatFirstLocalPlayerMixin", "CombatMiningInteractionMixin"})
            assertTrue(config.substring(config.indexOf("\"client\"")).contains(s));
        var plugin = Files.readString(root.resolve("compat/tconcombat/TconCombatFirstMixinPlugin.java"));
        assertFalse(plugin.contains("getModFileById(\"tconstruct\")"));
        assertTrue(plugin.contains("\"20.14.17\".equals(mod.getVersion().toString())"));
        var facade = Files.readString(root.resolve("compat/TconCombatFirstPolicy.java"));
        assertTrue(facade.contains("isLoaded(\"tconstruct\") && mods.isLoaded(\"epicfight\")"));
        var serverEvents = Files.readString(root.resolve("compat/TconCombatFirstEvents.java"));
        assertTrue(serverEvents.contains("Action.ABORT"));
        assertFalse(serverEvents.contains("CombatMiningRouting"), "No client linkage on dedicated servers");
    }

    private static boolean[] bools() { return new boolean[]{false, true}; }
    private static Hit[] hits() { return new Hit[]{null, new Hit(), new BlockHit(), new EntityHit(new Living()), new EntityHit(new Entity()), new EntityHit(new Part())}; }
    public static Route route(Patch p) {
        Hit selected = p.minecraft.f_91077_;
        if (Camera.instance.tps && Camera.instance.hit != null && Camera.instance.hit.position.distance < 16)
            selected = Camera.instance.hit;
        Target target = selected instanceof EntityHit e ? (e.entity instanceof Living || e.entity instanceof Part ? Target.LIVING : Target.NONLIVING)
                : selected instanceof BlockHit ? Target.BLOCK : Target.NONE;
        boolean matches = selected instanceof BlockHit s && p.minecraft.f_91077_ instanceof BlockHit b && s.pos.id == b.pos.id;
        return CombatMiningPolicy.decide(true, !p.vanilla, p.kind, target, selected != null && Camera.instance.locked,
                matches, p.mineable && ((Player)p.original).stack.item.canAttack);
    }
    public static int routing(Patch p) { return route(p).ordinal(); }
    public static boolean legacyAdaptive(boolean original, Hit selected, Patch p) {
        return original && !(selected instanceof BlockHit && p.kind == ItemKind.COMBAT_FIRST);
    }
    public static Route interactionRoute = Route.NATIVE;
    public static boolean preferred(boolean original) { return original && interactionRoute != Route.MINE; }
    private static Patch patch(Class<? extends Patch> type, ItemKind kind, Hit hit) throws Exception {
        var p = type.getConstructor().newInstance(); p.kind = kind; p.minecraft.f_91077_ = hit;
        ((Player)p.original).stack.item.canAttack = true; return p;
    }
    private static boolean evaluate(Patch p) throws Exception { return (boolean)p.getClass().getMethod("evaluate").invoke(p); }
    private static final Map<Boolean, Class<?>> INTERACTIONS = new HashMap<>();
    private static Event interaction(Patch p, boolean changed) throws Exception { var e = new Event(); interaction(p, changed, e); return e; }
    private static void interaction(Patch p, boolean changed, Event e) throws Exception {
        interactionRoute = route(p); Events.controlEngine.minecraft = p.minecraft; Events.controlEngine.player = (Player)p.original;
        var type = INTERACTIONS.get(changed);
        if (type == null) { type = interactionDecision(changed); INTERACTIONS.put(changed, type); }
        type.getMethod("evaluate", Event.class).invoke(null, e);
    }
    private static ClassNode nativeClass(String name) throws Exception {
        var jar = Path.of(System.getProperty("tconCombat.epicJar", System.getProperty("user.home")
                + "/.cache/bc/packwiz-downloads/mods/epic-fight-20.14.17-mc1.20.1-forge.jar"));
        var type = new ClassNode();
        try (var zip = new ZipFile(jar.toFile())) { new ClassReader(zip.getInputStream(zip.getEntry(name + ".class"))).accept(type, 0); }
        return type;
    }
    private static Class<? extends Patch> decision(int mode) throws Exception {
        var method = nativeClass(PATCH).methods.stream().filter(m -> m.name.equals("canPlayAttackAnimation") && m.desc.equals("()Z")).findFirst().orElseThrow();
        if (mode == 1) {
            int targets = 0;
            for (var node : method.instructions.toArray()) if (node instanceof MethodInsnNode call && call.owner.equals(CONFIG + "$PreferenceWork") && call.name.equals("checkHitResult")) {
                targets++; var injected = new InsnList();
                injected.add(new VarInsnNode(Opcodes.ALOAD, 2)); injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
                injected.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(TconCombatFirstInputTest.class), "legacyAdaptive", "(Z" + Type.getDescriptor(Hit.class) + Type.getDescriptor(Patch.class) + ")Z", false));
                method.instructions.insert(call, injected);
            }
            assertEquals(1, targets);
        } else if (mode == 2) {
            var injected = new InsnList(); var nativePath = new LabelNode(); var mine = new LabelNode();
            injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
            injected.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(TconCombatFirstInputTest.class), "routing", "(" + Type.getDescriptor(Patch.class) + ")I", false));
            injected.add(new InsnNode(Opcodes.DUP)); injected.add(new JumpInsnNode(Opcodes.IFEQ, nativePath));
            injected.add(new InsnNode(Opcodes.ICONST_1)); injected.add(new JumpInsnNode(Opcodes.IF_ICMPNE, mine));
            injected.add(new InsnNode(Opcodes.ICONST_1)); injected.add(new InsnNode(Opcodes.IRETURN));
            injected.add(mine); injected.add(new InsnNode(Opcodes.ICONST_0)); injected.add(new InsnNode(Opcodes.IRETURN));
            injected.add(nativePath); injected.add(new InsnNode(Opcodes.POP));
            method.instructions.insert(injected);
        }
        return emit("CombatDecision" + mode, method, Patch.class, true).asSubclass(Patch.class);
    }
    private static Class<?> interactionDecision(boolean changed) throws Exception {
        var method = nativeClass(ENGINE + "$Events").methods.stream().filter(m -> m.name.equals("interactionEvent")).findFirst().orElseThrow();
        var nodes = method.instructions.toArray();
        FieldInsnNode begin = null; MethodInsnNode contains = null;
        for (var n : nodes) {
            if (begin == null && n instanceof FieldInsnNode f && f.owner.equals(CONFIG) && f.name.equals("combatPreferredItems")) begin = f;
            if (begin != null && n instanceof MethodInsnNode c && c.owner.equals("java/util/Set") && c.name.equals("contains")) { contains = c; break; }
        }
        assertNotNull(begin); assertNotNull(contains);
        AbstractInsnNode after = contains.getNext(); while (!(after instanceof JumpInsnNode)) after = after.getNext();
        var end = ((JumpInsnNode)after).label;
        // Execute precisely EF's block/attack-only rejection branch; the rest of the handler is untouched.
        while (method.instructions.getFirst() != begin) method.instructions.remove(method.instructions.getFirst());
        while (end.getNext() != null) method.instructions.remove(end.getNext());
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.localVariables = null; method.tryCatchBlocks.clear();
        if (changed) {
            method.instructions.insert(contains, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    Type.getInternalName(TconCombatFirstInputTest.class), "preferred", "(Z)Z", false));
        }
        return emit("InteractionDecision" + changed, method, Object.class, false);
    }
    private static Class<?> emit(String suffix, MethodNode method, Class<?> parent, boolean ctorNeeded) {
        var output = new ClassNode(); output.version = Opcodes.V17; output.access = Opcodes.ACC_PUBLIC;
        output.name = "com/bettercontent/bettercompatfixes/" + suffix; output.superName = Type.getInternalName(parent);
        method.name = "evaluate"; output.methods.add(method);
        if (ctorNeeded) {
            var ctor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            ctor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, output.superName, "<init>", "()V", false));
            ctor.instructions.add(new InsnNode(Opcodes.RETURN)); output.methods.add(ctor);
        }
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        output.accept(new ClassRemapper(writer, new Remapper() {
            @Override public String map(String name) { return TYPES.containsKey(name) ? Type.getInternalName(TYPES.get(name)) : name; }
        }));
        byte[] bytes = writer.toByteArray();
        return new ClassLoader(TconCombatFirstInputTest.class.getClassLoader()) { Class<?> emit() { return defineClass(null, bytes, 0, bytes.length); } }.emit();
    }
    private static Map<String, Class<?>> types() {
        var map = new HashMap<String, Class<?>>();
        map.put(PATCH, Patch.class); map.put(CAMERA, Camera.class); map.put(RENDER, Render.class);
        map.put(CONFIG, Config.class); map.put(CONFIG + "$PreferenceWork", Preference.class);
        map.put(ENGINE, Engine.class); map.put(ENGINE + "$Events", Events.class);
        map.put("net/minecraftforge/client/event/InputEvent$InteractionKeyMappingTriggered", Event.class);
        map.put("yesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch$PlayerMode", Mode.class);
        String[][] names = {{"client/Minecraft", "Game"}, {"client/player/LocalPlayer", "Player"},
                {"world/entity/player/Player", "Player"}, {"world/entity/Entity", "Entity"}, {"world/entity/LivingEntity", "Living"},
                {"world/phys/HitResult", "Hit"}, {"world/phys/HitResult$Type", "HitType"}, {"world/phys/BlockHitResult", "BlockHit"},
                {"world/phys/EntityHitResult", "EntityHit"}, {"world/phys/Vec3", "Vec"}, {"core/BlockPos", "Pos"},
                {"world/item/ItemStack", "Stack"}, {"world/item/Item", "Item"}, {"client/multiplayer/ClientLevel", "Level"},
                {"world/level/Level", "Level"}, {"world/level/block/state/BlockState", "State"}};
        for (var pair : names) try { map.put("net/minecraft/" + pair[0], Class.forName(TconCombatFirstInputTest.class.getName() + "$" + pair[1])); }
        catch (ClassNotFoundException e) { throw new AssertionError(e); }
        map.put("net/minecraftforge/entity/PartEntity", Part.class); return map;
    }
    public static class Patch {
        public Entity original = new Player(); public Game minecraft = new Game(); public ItemKind kind = ItemKind.OTHER; public boolean vanilla, mineable = true;
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
    public static class BlockHit extends Hit { public Pos pos; public BlockHit() { this(0); } public BlockHit(int id) { pos = new Pos(id); } public Pos m_82425_() { return pos; } }
    public static class EntityHit extends Hit { public final Entity entity; public EntityHit(Entity e) { entity = e; } public Entity m_82443_() { return entity; } }
    public static class Pos { public int id; public Pos(int id) { this.id = id; } }
    public static class State {}
    public static class Level { public State m_8055_(Pos pos) { return new State(); } }
    public static class Stack { public Item item = new Item(); public boolean suitable; public float speed = 1; public Item m_41720_() { return item; } public boolean m_41735_(State state) { return suitable; } public float m_41691_(State state) { return speed; } }
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
    public static class Event { public boolean canceled, swing = true; public void setCanceled(boolean v) { canceled = v; } public void setSwingHand(boolean v) { swing = v; } }
    public static class Engine { public Player player; public Game minecraft; }
    public static class Events { public static Engine controlEngine = new Engine(); }
}
