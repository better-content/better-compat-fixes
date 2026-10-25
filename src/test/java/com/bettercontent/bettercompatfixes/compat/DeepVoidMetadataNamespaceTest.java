package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.compat.deepvoid.DeepVoidMetadataNamespace;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.junit.jupiter.api.Assertions.*;

final class DeepVoidMetadataNamespaceTest {
    private static final String PREFIX = "com.bettercontent.bettercompatfixes.mixin.deepvoid.";
    private static final String[] BRIDGES = {"TemplateMetadata", "RegionWriteRadius", "IgnoreMetadata"};

    @Test void exactlyOneBridgePerTargetInEachRealNamespace() {
        for (boolean production : new boolean[]{false, true}) {
            for (String bridge : BRIDGES) {
                assertEquals(!production, DeepVoidMetadataNamespace.applies(PREFIX + bridge + "Accessor", production));
                assertEquals(production, DeepVoidMetadataNamespace.applies(PREFIX + bridge + "SrgAccessor", production));
            }
            for (String other : new String[]{"FleshStructureFeatureMixin", "CultTemplePlacementProcedureMixin", "IfPlayerFallsInVoidProcedureMixin"}) {
                assertTrue(DeepVoidMetadataNamespace.applies(PREFIX + other, production));
            }
        }
    }

    @Test void bothNamespaceImplementationsAreRegisteredExactlyOnce() throws Exception {
        try (var input = getClass().getResourceAsStream("/better_compat_fixes.mixins.json")) {
            assertNotNull(input);
            var mixins = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("mixins");
            for (String bridge : BRIDGES) for (String suffix : new String[]{"Accessor", "SrgAccessor"}) {
                long count = java.util.stream.StreamSupport.stream(mixins.spliterator(), false)
                        .filter(value -> value.getAsString().equals("deepvoid." + bridge + suffix)).count();
                assertEquals(1, count);
            }
        }
    }

    @Test void actualPluginUsesForgeProductionStateAndNamespacePolicy() throws Exception {
        try (var input = getClass().getResourceAsStream("/com/bettercontent/bettercompatfixes/compat/plugin/BetterContentMixinPlugin.class")) {
            assertNotNull(input);
            var type = new ClassNode();
            new ClassReader(input).accept(type, 0);
            var method = type.methods.stream().filter(value -> value.name.equals("shouldApplyMixin")).findFirst().orElseThrow();
            int policyCalls = 0, nativeStateReads = 0;
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.endsWith("/DeepVoidMetadataNamespace")
                        && call.name.equals("applies") && call.desc.equals("(Ljava/lang/String;Z)Z")) policyCalls++;
                if (instruction instanceof FieldInsnNode field && field.owner.equals("net/minecraftforge/fml/loading/FMLEnvironment")
                        && field.name.equals("production") && field.desc.equals("Z")) nativeStateReads++;
            }
            assertEquals(1, policyCalls);
            assertEquals(1, nativeStateReads);
        }
    }
}
