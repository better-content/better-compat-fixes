package com.bettercontent.bettercompatfixes;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Current declared mapped dependencies, inspected without starting Minecraft or claiming mixin acceptance. */
class DynamicTreesFellingNativeContractTest {
    private static ClassNode nativeClass(String name) throws Exception {
        try (InputStream stream = DynamicTreesFellingNativeContractTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream, "Missing declared native dependency: " + name);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    private static List<MethodInsnNode> calls(MethodNode method, String name) {
        List<MethodInsnNode> calls = new ArrayList<>();
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals(name)) calls.add(call);
        }
        return calls;
    }

    @Test void forgeMiningChargePrecedesDeferredRemovalAndMitigationRemainsInsideNativeHurt() throws Exception {
        var gameMode = nativeClass("net/minecraft/server/level/ServerPlayerGameMode");
        var destroy = method(gameMode, "destroyBlock");
        var mine = calls(destroy, "mineBlock");
        assertEquals(1, mine.size());
        assertTrue(destroy.instructions.indexOf(mine.get(0))
                < destroy.instructions.indexOf(calls(destroy, "removeBlock").get(1)));
        var stack = nativeClass("net/minecraft/world/item/ItemStack");
        assertEquals(1, calls(method(stack, "mineBlock"), "mineBlock").size());
        var hurt = method(stack, "hurtAndBreak");
        assertEquals(1, calls(hurt, "damageItem").size());
        assertEquals(1, calls(hurt, "hurt").size());
        assertTrue(hurt.instructions.indexOf(calls(hurt, "damageItem").get(0))
                < hurt.instructions.indexOf(calls(hurt, "hurt").get(0)));
        assertEquals(1, calls(hurt, "shrink").size(), "Native break path remains sole stack consumer");
    }

    @Test void pinnedDynamicTreesQueuesRemovalAndSeparatesFellingFromStripping() throws Exception {
        var branch = nativeClass("com/ferreusveritas/dynamictrees/block/branch/BranchBlock");
        var removed = method(branch, "removedByEntity");
        assertEquals(1, calls(removed, "add").size());
        assertEquals(Opcodes.ICONST_0, removed.instructions.getLast().getPrevious().getOpcode());
        var future = method(branch, "futureBreak");
        var damage = calls(future, "damageAxe");
        assertEquals(1, damage.size());
        assertEquals(Opcodes.ICONST_1, damage.get(0).getPrevious().getOpcode());
        assertTrue(future.instructions.indexOf(calls(future, "dropTree").get(0))
                < future.instructions.indexOf(damage.get(0)));
        var strip = branch.methods.stream().filter(m -> m.name.equals("stripBranch")
                && !calls(m, "damageAxe").isEmpty()).findFirst().orElseThrow();
        assertEquals(Opcodes.ICONST_0, calls(strip, "damageAxe").get(0).getPrevious().getOpcode());
        var helper = method(nativeClass("com/ferreusveritas/dynamictrees/util/ItemUtils"), "damageAxe");
        assertEquals(1, calls(helper, "hurtAndBreak").size());
        assertTrue(java.util.stream.StreamSupport.stream(helper.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof IincInsnNode increment && increment.incr == -1));
        assertTrue(java.util.stream.StreamSupport.stream(helper.instructions.spliterator(), false)
                .anyMatch(i -> i.getOpcode() == Opcodes.IDIV));
    }

    @Test void tinkersInterceptsBeforeVanillaMiningAndSkipsOrdinaryWearForDeferredRemoval() throws Exception {
        var item = method(nativeClass("slimeknights/tconstruct/library/tools/item/ModifiableItem"), "onBlockStartBreak");
        assertEquals(1, calls(item, "handleBlockBreak").size());
        var logic = nativeClass("slimeknights/tconstruct/library/tools/helper/ToolHarvestLogic");
        var block = logic.methods.stream().filter(m -> m.name.equals("breakBlock")
                && m.desc.startsWith("(Lslimeknights/tconstruct/library/tools/nbt/IToolStackView;")).findFirst().orElseThrow();
        var removal = calls(block, "removeBlock").get(1);
        assertInstanceOf(VarInsnNode.class, removal.getNext());
        int removedLocal = ((VarInsnNode) removal.getNext()).var;
        var charge = calls(block, "damageAnimated");
        assertEquals(1, charge.size());
        boolean skipWhenRemovalFalse = false;
        for (var instruction : block.instructions) {
            if (instruction instanceof VarInsnNode load && load.getOpcode() == Opcodes.ILOAD
                    && load.var == removedLocal && load.getNext() instanceof JumpInsnNode jump
                    && jump.getOpcode() == Opcodes.IFEQ
                    && block.instructions.indexOf(jump) < block.instructions.indexOf(charge.get(0))
                    && block.instructions.indexOf(jump.label) > block.instructions.indexOf(charge.get(0))) {
                skipWhenRemovalFalse = true;
            }
        }
        assertTrue(skipWhenRemovalFalse, "TCon must not acquire an invented ordinary mining charge");
    }

    @Test void registrationsConfigAndProductionSelectorsStayNarrow() throws Exception {
        Path root = Path.of("src/main/java/com/bettercontent/bettercompatfixes");
        String stack = Files.readString(root.resolve("mixin/dynamictrees/FellingItemStackMixin.java"));
        String branch = Files.readString(root.resolve("mixin/dynamictrees/FellingBranchMixin.java"));
        String plugin = Files.readString(root.resolve("compat/plugin/BetterContentMixinPlugin.java"));
        String mixins = Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"));
        for (String name : List.of("FellingItemStackMixin", "FellingBranchMixin")) {
            assertTrue(plugin.contains("dynamictrees." + name));
            assertTrue(mixins.contains("dynamictrees." + name));
        }
        assertTrue(plugin.contains("hasVersion(mods, \"dynamictrees\", \"1.20.1-1.4.10\")"));
        assertTrue(stack.contains("\"mineBlock\", \"m_41686_\""));
        assertTrue(stack.contains("\"hurtAndBreak\", \"m_41622_\""));
        assertTrue(stack.contains("ToolActions.AXE_DIG"));
        assertTrue(stack.contains("instanceof BranchBlock"));
        assertTrue(stack.contains("player.getMainHandItem() != stack"));
        assertTrue(stack.contains("instanceof FakePlayer"));
        assertTrue(branch.contains("instanceof FakePlayer"));
        assertTrue(branch.contains("method = \"futureBreak\""));
        assertTrue(branch.contains("require = 1, expect = 1, allow = 1"));
        assertFalse(branch.contains("method = \"stripBranch\""));
        assertFalse(branch.contains("setDamageValue"));
        String config = Files.readString(root.resolve("config/BcFixesConfig.java"));
        assertTrue(config.contains("defineInRange(\"fellingDurabilityMultiplier\", 10, 1, 100)"));
        assertTrue(Files.readString(Path.of("../../better-content-modpack/config/better_compat_fixes-common.toml"))
                .contains("fellingDurabilityMultiplier = 10"));
        assertTrue(Files.readString(Path.of("../../better-content-modpack/defaultconfigs/dynamictrees-server.toml"))
                .contains("axeDamageMode = \"THICKNESS\""));
    }
}
