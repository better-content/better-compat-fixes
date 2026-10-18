package com.bettercontent.bettercompatfixes;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.junit.jupiter.api.Assertions.*;

class SaulomonkNativeContractTest {
    @Test void pinnedNativePredicateHasExactlyTheTwoExpressionsWePatch() throws Exception {
        var loader = getClass().getClassLoader();
        try (var input = loader.getResourceAsStream("reascer/wom/world/entity/mob/Saulomonk.class")) {
            assertNotNull(input, "Pinned WoM must be on the source-test classpath");
            var node = new ClassNode(Opcodes.ASM9);
            new ClassReader(input).accept(node, 0);
            var method = node.methods.stream().filter(m -> m.name.equals("checkSaulomonkSpawnRules"))
                    .findFirst().orElseThrow();
            int thresholds = 0;
            var roots = new ArrayList<MethodInsnNode>();
            boolean monsterGuard = false;
            boolean blockGuard = false;
            for (var instruction : method.instructions) {
                if (instruction instanceof LdcInsnNode ldc && Double.valueOf(5000.0).equals(ldc.cst)) thresholds++;
                if (instruction instanceof MethodInsnNode call) {
                    if (call.owner.equals("java/lang/Math") && call.name.equals("sqrt") && call.desc.equals("(D)D")) roots.add(call);
                    if (call.name.equals("checkMobSpawnRules") || call.name.equals("m_217057_")) monsterGuard = true;
                    if (call.name.equals("is") || call.name.equals("m_60713_")) blockGuard = true;
                }
            }
            assertEquals(1, thresholds);
            assertEquals(1, roots.size());
            assertTrue(monsterGuard, "Native mob-spawn predicate must remain present");
            assertTrue(blockGuard, "Native sand guard must remain present");
        }
    }
}
