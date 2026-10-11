package com.bettercontent.bettercompatfixes.compat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Inspect the pinned Gradle-fetched native artifact, not a historical decompiler/log output. */
final class DeepVoidNativeProcedureTest {
    @Test
    void pinnedNativeFallProcedureHasExactlyThreeAbsoluteThresholdConversions() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/net/mcreator/thedeepvoid/procedures/IfPlayerFallsInVoidProcedure.class")) {
            assertNotNull(input, "Pinned Deep Void fixture must be on the test classpath");
            ClassNode type = new ClassNode();
            new ClassReader(input).accept(type, 0);
            var execute = type.methods.stream().filter(method -> method.name.equals("execute")
                    && method.desc.equals("(Lnet/minecraftforge/eventbus/api/Event;DDLnet/minecraft/world/entity/Entity;)V"))
                    .findFirst().orElseThrow();
            int heights = 0;
            int conversions = 0;
            int guards = 0;
            for (var instruction : execute.instructions) {
                if (instruction instanceof FieldInsnNode field
                        && field.owner.equals("net/mcreator/thedeepvoid/configuration/DeepVoidConfigConfiguration")) {
                    if (field.name.equals("TELEPORTHEIGHT")) heights++;
                    if (field.name.equals("PLAYERSTELEPORTINVOID") || field.name.equals("ALLOWOTHERWORLDTP")
                            || field.name.equals("PLAYERSTELEPORTINEND")) guards++;
                }
                if (instruction instanceof MethodInsnNode call && call.owner.equals("java/lang/Double")
                        && call.name.equals("doubleValue") && call.desc.equals("()D")) conversions++;
            }
            assertEquals(3, heights, "normal, any-dimension and End fall routes must all be covered");
            assertEquals(3, conversions, "Exact expression selector must cover only the three thresholds");
            assertEquals(3, guards, "Native enable/eligibility guards must remain present");
        }
    }
}
