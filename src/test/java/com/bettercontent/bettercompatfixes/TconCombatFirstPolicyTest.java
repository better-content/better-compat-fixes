package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;

/** Executes the actual compiled policy with isolated integration doubles; not Forge acceptance. */
public final class TconCombatFirstPolicyTest {
    private static final String ROOT = "com/bettercontent/bettercompatfixes/compat/";

    @Test
    public void compiledPolicyCoversEligibilityMatrixAndEveryCapabilityKind() throws Exception {
        Method policy = integration();
        for (boolean modifiable : new boolean[]{false, true})
            for (boolean loaded : new boolean[]{false, true})
                for (boolean mining : new boolean[]{false, true})
                    for (Cap cap : new Cap[]{null, new Cap(true), new Cap(false), new RangedCap(), new ShieldCap(), new TinkerCap()}) {
                        var definition = new Definition(); definition.loaded = loaded; definition.data.hooks.mining = mining;
                        var stack = new Stack(); stack.item = modifiable ? new Tool(definition) : new Item(); stack.capability = cap;
                        boolean expected = modifiable && loaded && !mining && cap != null && !cap.isEmpty();
                        assertEquals(expected, deny(policy, stack));
                        stack.broken = true; stack.material = "different";
                        assertEquals(expected, deny(policy, stack), "Broken/material state must not classify mining");
                    }
        var stack = new Stack(); stack.empty = true;
        assertFalse(deny(policy, stack));
        stack.empty = false; stack.item = new Tool(null);
        assertFalse(deny(policy, stack), "Unknown definition must preserve behavior");
    }

    @Test
    public void sameStackResolvesConditionalVariantsAndReloadsWithoutCache() throws Exception {
        Method policy = integration();
        var definition = new Definition(); definition.loaded = true;
        var stack = new Stack(); stack.item = new Tool(definition);
        stack.capability = new Cap(false) {
            @Override public Cap getResult(Stack value) { return value.variant ? this : new Cap(true); }
        };
        assertFalse(deny(policy, stack));
        stack.variant = true;
        assertTrue(deny(policy, stack));
        definition.data.hooks.mining = true;
        assertFalse(deny(policy, stack));
        definition.data.hooks.mining = false;
        assertTrue(deny(policy, stack));
        definition.loaded = false;
        assertFalse(deny(policy, stack));
        definition.loaded = true;
        assertTrue(deny(policy, stack));
        stack.capability = new Cap(false) { @Override public Cap getResult(Stack value) { return null; } };
        assertFalse(deny(policy, stack));
        stack.capability = null;
        assertFalse(deny(policy, stack));
    }

    @Test
    public void absentOptionalModsNeverEnterBridgeAndOnlyMainHandControlsPolicy() throws Exception {
        Bridge.policy = integration();
        var facade = emit(ROOT + "TconCombatFirstPolicy", Map.of(
                "net/minecraft/world/entity/player/Player", Player.class,
                "net/minecraft/world/item/ItemStack", Stack.class,
                "net/minecraftforge/fml/ModList", Mods.class,
                ROOT + "tconcombat/TconCombatFirstIntegration", Bridge.class), Player.class);
        var player = new Player();
        var definition = new Definition(); definition.loaded = true;
        player.main.item = new Tool(definition); player.main.capability = new TinkerCap();
        for (boolean tcon : new boolean[]{false, true}) for (boolean epic : new boolean[]{false, true}) {
            Mods.current = new Mods(); Mods.current.tcon = tcon; Mods.current.epic = epic;
            Bridge.calls = 0;
            assertEquals(tcon && epic, facade.invoke(null, player));
            assertEquals(tcon && epic ? 1 : 0, Bridge.calls, "Absent mods must not resolve integration classes");
        }
        Mods.current = null;
        assertFalse((boolean) facade.invoke(null, player));
        assertFalse((boolean) facade.invoke(null, new Object[]{null}));
        Mods.current = new Mods(); Mods.current.tcon = true; Mods.current.epic = true;
        player.offhand = player.main;
        player.main = new Stack(); player.main.item = new Tool(definition); player.main.capability = new TinkerCap();
        definition.data.hooks.mining = true;
        assertFalse((boolean) facade.invoke(null, player));
        var nonMining = new Definition(); nonMining.loaded = true;
        player.offhand.item = new Tool(nonMining);
        assertFalse((boolean) facade.invoke(null, player), "Non-mining offhand must not suppress main-hand mining");
        player.main = player.offhand;
        assertTrue((boolean) facade.invoke(null, player));
        player.main.empty = true;
        assertFalse((boolean) facade.invoke(null, player));
    }

    private static Method integration() throws Exception {
        return emit(ROOT + "tconcombat/TconCombatFirstIntegration", Map.ofEntries(
                Map.entry("net/minecraft/world/item/ItemStack", Stack.class),
                Map.entry("net/minecraft/world/item/Item", Item.class),
                Map.entry("slimeknights/tconstruct/library/tools/item/IModifiable", Modifiable.class),
                Map.entry("slimeknights/tconstruct/library/tools/definition/ToolDefinition", Definition.class),
                Map.entry("slimeknights/tconstruct/library/tools/definition/ToolDefinitionData", Data.class),
                Map.entry("slimeknights/tconstruct/library/module/ModuleHookMap", Hooks.class),
                Map.entry("slimeknights/tconstruct/library/module/ModuleHook", Hook.class),
                Map.entry("slimeknights/tconstruct/library/tools/definition/module/ToolHooks", ToolHooks.class),
                Map.entry("yesman/epicfight/world/capabilities/EpicFightCapabilities", Caps.class),
                Map.entry("yesman/epicfight/world/capabilities/item/CapabilityItem", Cap.class)), Stack.class);
    }
    private static boolean deny(Method method, Stack stack) throws Exception { return (boolean) method.invoke(null, stack); }
    private static Method emit(String name, Map<String, Class<?>> types, Class<?> parameter) throws Exception {
        var source = new ClassNode();
        try (var input = TconCombatFirstPolicyTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, "Actual compiled production class is required");
            new ClassReader(input).accept(source, 0);
        }
        var method = source.methods.stream().filter(m -> m.name.equals("deniesBlockAttack")).findFirst().orElseThrow();
        var output = new ClassNode(); output.version = Opcodes.V17; output.access = Opcodes.ACC_PUBLIC;
        output.name = "com/bettercontent/bettercompatfixes/IsolatedPolicy"; output.superName = "java/lang/Object";
        output.methods.add(method);
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        output.accept(new ClassRemapper(writer, new Remapper() {
            @Override public String map(String type) { return types.containsKey(type) ? Type.getInternalName(types.get(type)) : type; }
        }));
        var bytes = writer.toByteArray();
        return new ClassLoader(TconCombatFirstPolicyTest.class.getClassLoader()) {
            Class<?> emit() { return defineClass(null, bytes, 0, bytes.length); }
        }.emit().getMethod("deniesBlockAttack", parameter);
    }
    public static class Item {}
    public interface Modifiable { Definition getToolDefinition(); }
    public static class Tool extends Item implements Modifiable {
        private final Definition definition; public Tool(Definition definition) { this.definition = definition; }
        public Definition getToolDefinition() { return definition; }
    }
    public static class Definition { public boolean loaded; public Data data = new Data();
        public boolean isDataLoaded() { return loaded; } public Data getData() { return data; } }
    public static class Data { public Hooks hooks = new Hooks(); public Hooks getHooks() { return hooks; } }
    public static class Hook {}
    public static class ToolHooks { public static Hook IS_EFFECTIVE = new Hook(); }
    public static class Hooks { public boolean mining; public boolean hasHook(Hook hook) { return mining; } }
    public static class Stack {
        public boolean empty, broken, variant; public String material = "initial"; public Item item = new Item(); public Cap capability;
        public boolean isEmpty() { return empty; } public Item getItem() { return item; }
    }
    public static class Cap {
        private final boolean empty; public Cap(boolean empty) { this.empty = empty; }
        public boolean isEmpty() { return empty; } public Cap getResult(Stack stack) { return this; }
    }
    public static class RangedCap extends Cap { public RangedCap() { super(false); } }
    public static class ShieldCap extends Cap { public ShieldCap() { super(false); } }
    public static class TinkerCap extends Cap { public TinkerCap() { super(false); } }
    public static class Caps { public static Cap getItemStackCapability(Stack stack) { return stack.capability; } }
    public static class Player { public Stack main = new Stack(), offhand = new Stack(); public Stack getMainHandItem() { return main; } }
    public static class Mods {
        public static Mods current; public boolean tcon, epic;
        public static Mods get() { return current; } public boolean isLoaded(String id) { return id.equals("tconstruct") ? tcon : epic; }
    }
    public static class Bridge {
        public static Method policy; public static int calls;
        public static boolean deniesBlockAttack(Stack stack) {
            calls++; try { return deny(policy, stack); } catch (Exception e) { throw new AssertionError(e); }
        }
    }
}
