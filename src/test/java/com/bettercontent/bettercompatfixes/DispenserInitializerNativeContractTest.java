package com.bettercontent.bettercompatfixes;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class DispenserInitializerNativeContractTest {
    @Test void pinnedNativeInitializerMakesTheMapThenAssignsItsFinalFieldInClinit() throws Exception {
        var nativeType = new ClassNode();
        try (var input = getClass().getResourceAsStream("/net/minecraft/world/level/block/DispenserBlock.class")) {
            assertNotNull(input); new ClassReader(input).accept(nativeType, 0);
        }
        var clinit = nativeType.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElseThrow();
        int matches = 0;
        for (var instruction : clinit.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("net/minecraft/Util")
                    && call.name.equals("make") && call.desc.equals("(Ljava/lang/Object;Ljava/util/function/Consumer;)Ljava/lang/Object;")) {
                matches++;
                var next = meaningful(instruction.getNext());
                assertInstanceOf(TypeInsnNode.class, next);
                assertEquals(Opcodes.CHECKCAST, next.getOpcode());
                assertEquals("java/util/Map", ((TypeInsnNode) next).desc);
                var assignment = (FieldInsnNode) meaningful(next.getNext());
                assertEquals(Opcodes.PUTSTATIC, assignment.getOpcode());
                assertEquals(nativeType.name, assignment.owner);
                assertEquals("DISPENSER_REGISTRY", assignment.name);
                var field = nativeType.fields.stream().filter(f -> f.name.equals(assignment.name)).findFirst().orElseThrow();
                assertEquals(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, field.access,
                        "Pinned raw field is private static final (0x001a), not a public diagnostic API");
            }
        }
        assertEquals(1, matches, "Exactly one pinned native initialization expression must match");
    }

    @Test void productionArchiveCannotRelocateTheFinalFieldWriteIntoAnyMixinBridge() throws Exception {
        Path runtime = Path.of("build/libs/better-compat-fixes-0.1.9.jar");
        var mixin = new ClassNode();
        try (var archive = new ZipFile(runtime.toFile())) {
            var entry = archive.getEntry("com/bettercontent/bettercompatfixes/mixin/minecraft/DispenserBlockMixin.class");
            assertNotNull(entry); new ClassReader(archive.getInputStream(entry)).accept(mixin, 0);
            assertNull(archive.getEntry("com/bettercontent/bettercompatfixes/nativefixture/DispenserRegistryAccessor.class"));
            assertNull(archive.getEntry("com/bettercontent/bettercompatfixes/gametest/DispenserRegistryGameTests.class"));
            assertNull(archive.getEntry("better_compat_fixes.native_fixture.mixins.json"));
        }
        assertTrue(mixin.fields.isEmpty(), "No shadow/Mutable/access-transformer workaround for the final field");
        for (var method : mixin.methods) {
            for (var instruction : method.instructions) {
                assertFalse(instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTSTATIC
                                && field.owner.equals("net/minecraft/world/level/block/DispenserBlock"),
                        "Production mixin must leave the original final-field write in native clinit");
            }
            if (method.name.contains("makeBehaviorRegistryThreadSafe")) {
                assertEquals("(Ljava/lang/Object;)Ljava/lang/Object;", method.desc);
                var annotations = method.visibleAnnotations;
                assertNotNull(annotations);
                var annotation = annotations.stream().filter(a -> a.desc.endsWith("/ModifyExpressionValue;")).findFirst().orElseThrow();
                assertEquals(1, annotation.values.get(annotation.values.indexOf("require") + 1));
                assertEquals(1, annotation.values.get(annotation.values.indexOf("allow") + 1));
            }
        }
    }

    @Test void actualMixinLauncherAcceptsTheExactFixtureConfigOption() {
        String config = "better_compat_fixes.native_fixture.mixins.json";
        assertEquals(java.util.List.of(config), org.spongepowered.asm.launch.platform.CommandLineOptions.ofArgs(
                java.util.List.of("--mixin.config", config)).getConfigs());
    }

    @Test void sourceFixtureAccessorIsOnlyAnExactStaticGetter() throws Exception {
        var accessor = new ClassNode();
        new ClassReader(java.nio.file.Files.readAllBytes(Path.of(
                "build/classes/java/nativeFixture/com/bettercontent/bettercompatfixes/nativefixture/DispenserRegistryAccessor.class")))
                .accept(accessor, 0);
        assertTrue(accessor.fields.isEmpty());
        assertEquals(1, accessor.methods.size());
        var getter = accessor.methods.get(0);
        assertEquals("()Ljava/util/Map;", getter.desc);
        assertTrue((getter.access & Opcodes.ACC_STATIC) != 0);
        var annotation = getter.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Accessor;")).findFirst().orElseThrow();
        assertEquals("DISPENSER_REGISTRY", annotation.values.get(annotation.values.indexOf("value") + 1));
        assertEquals(false, annotation.values.get(annotation.values.indexOf("remap") + 1));
    }

    private static AbstractInsnNode meaningful(AbstractInsnNode instruction) {
        while (instruction != null && instruction.getOpcode() < 0) instruction = instruction.getNext();
        return instruction;
    }
}
