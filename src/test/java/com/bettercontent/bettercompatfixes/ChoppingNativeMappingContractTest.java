package com.bettercontent.bettercompatfixes;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

/** Inspect pinned native shapes and the actual staged no-refmap archive, not client acceptance. */
class ChoppingNativeMappingContractTest {
    private static final String MC = "net/minecraft/client/Minecraft";
    private static final String MODE = "net/minecraft/client/multiplayer/MultiPlayerGameMode";
    private static final String BLOCK = "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z";
    private static final String HIT = "Lnet/minecraft/client/particle/ParticleEngine;addBlockHitEffects(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)V";

    private ClassNode nativeType(String name) throws Exception {
        var type = new ClassNode();
        try (var input = getClass().getResourceAsStream("/" + name + ".class")) {
            assertNotNull(input); new ClassReader(input).accept(type, 0);
        }
        return type;
    }
    private static MethodNode method(ClassNode type, String name, String descriptor) {
        return type.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(descriptor)).findFirst().orElseThrow();
    }
    private static Object value(AnnotationNode annotation, String key) {
        int index = annotation.values.indexOf(key); assertTrue(index >= 0, key);
        return annotation.values.get(index + 1);
    }
    private static AnnotationNode annotation(List<AnnotationNode> annotations, String simpleName) {
        assertNotNull(annotations);
        return annotations.stream().filter(a -> a.desc.endsWith("/" + simpleName + ";")).findFirst().orElseThrow();
    }
    private static ClassNode staged(String name) throws Exception {
        try (var archive = new ZipFile("build/libs/better-compat-fixes-0.1.9.jar")) {
            var type = new ClassNode();
            var entry = archive.getEntry("com/bettercontent/bettercompatfixes/mixin/minecraft/" + name + ".class");
            assertNotNull(entry); new ClassReader(archive.getInputStream(entry)).accept(type, 0);
            return type;
        }
    }
    private static void strict(AnnotationNode annotation, int count, List<String> selectors) {
        assertEquals(false, value(annotation, "remap"));
        assertEquals(count, value(annotation, "require"));
        assertEquals(count, value(annotation, "allow"));
        assertEquals(selectors, value(annotation, "method"));
    }

    @Test void pinnedNativeMembersHaveTheExactMappingsPrivacyAndInjectionSites() throws Exception {
        var minecraft = nativeType(MC);
        var attack = method(minecraft, "continueAttack", "(Z)V");
        int hitSites = 0;
        for (var instruction : attack.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("net/minecraft/client/particle/ParticleEngine")
                    && call.name.equals("addBlockHitEffects") && ("L" + call.owner + ";" + call.name + call.desc).equals(HIT)) hitSites++;
        }
        assertEquals(2, hitSites, "Forge normal and cancelled hold-hit branches must both remain covered");
        var mode = nativeType(MODE);
        assertEquals(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                mode.fields.stream().filter(f -> f.name.equals("minecraft")).findFirst().orElseThrow().access);
        assertEquals(Opcodes.ACC_PRIVATE,
                mode.fields.stream().filter(f -> f.name.equals("destroyProgress")).findFirst().orElseThrow().access);
        method(mode, "startDestroyBlock", BLOCK); method(mode, "stopDestroyBlock", "()V");
        var progress = method(mode, "continueDestroyBlock", BLOCK);
        int writes = 0;
        for (var instruction : progress.instructions) {
            if (instruction instanceof FieldInsnNode field && field.owner.equals(MODE) && field.name.equals("destroyProgress")
                    && field.getOpcode() == Opcodes.PUTFIELD) {
                if (writes++ == 0) assertEquals(Opcodes.FADD, instruction.getPrevious().getOpcode(), "First write is native incremental completion, not reset");
            }
        }
        assertEquals(2, writes);
        String mappings = Files.readString(Path.of("build/createMcpToSrg/output.tsrg"));
        String mcSection = mappings.substring(mappings.indexOf(MC + " " + MC + "\n"));
        mcSection = mcSection.substring(0, mcSection.indexOf("\nnet/", 1));
        assertTrue(mcSection.contains("\tcontinueAttack (Z)V m_91386_"));
        String modeSection = mappings.substring(mappings.indexOf(MODE + " " + MODE + "\n"));
        modeSection = modeSection.substring(0, modeSection.indexOf("\nnet/", 1));
        for (String row : List.of("minecraft f_105189_", "destroyProgress f_105193_",
                "startDestroyBlock " + BLOCK + " m_105269_", "stopDestroyBlock ()V m_105276_",
                "continueDestroyBlock " + BLOCK + " m_105283_")) assertTrue(modeSection.contains("\t" + row), row);
    }

    @Test void stagedHoldHitRedirectHasBothExactEnvironmentSelectorsAndTwoRequiredForgeSites() throws Exception {
        var type = staged("ChoppingHitEffectsMixin");
        var redirect = annotation(type.methods.stream().filter(m -> m.name.equals("bc$replaceWoodHits")).findFirst().orElseThrow().visibleAnnotations, "Redirect");
        strict(redirect, 2, List.of("continueAttack(Z)V", "m_91386_(Z)V"));
        var at = (AnnotationNode) value(redirect, "at");
        assertEquals("INVOKE", value(at, "value")); assertEquals(HIT, value(at, "target"));
        assertEquals(false, value(at, "remap"));
    }

    @Test void stagedProgressHooksKeepPrivateAliasesAndExactFirstWriteAfterTimingWithoutARefmap() throws Exception {
        var type = staged("ChoppingProgressMixin");
        for (String[] pair : new String[][]{{"minecraft", "f_105189_"}, {"destroyProgress", "f_105193_"}}) {
            var field = type.fields.stream().filter(f -> f.name.equals(pair[0])).findFirst().orElseThrow();
            assertTrue((field.access & Opcodes.ACC_PRIVATE) != 0);
            var shadow = annotation(field.visibleAnnotations, "Shadow");
            assertEquals(false, value(shadow, "remap")); assertEquals(List.of(pair[1]), value(shadow, "aliases"));
        }
        for (String[] hook : new String[][]{{"bc$newTarget", "startDestroyBlock", "m_105269_", BLOCK},
                {"bc$stop", "stopDestroyBlock", "m_105276_", "()V"}, {"bc$progress", "continueDestroyBlock", "m_105283_", BLOCK}}) {
            var inject = annotation(type.methods.stream().filter(m -> m.name.equals(hook[0])).findFirst().orElseThrow().visibleAnnotations, "Inject");
            strict(inject, 1, List.of(hook[1] + hook[3], hook[2] + hook[3]));
            @SuppressWarnings("unchecked") var sites = (List<AnnotationNode>) value(inject, "at");
            if (!hook[0].equals("bc$progress")) {
                assertEquals(1, sites.size()); assertEquals("HEAD", value(sites.get(0), "value")); continue;
            }
            assertEquals(2, sites.size());
            for (int i = 0; i < sites.size(); i++) {
                var site = sites.get(i);
                assertEquals("FIELD", value(site, "value")); assertEquals(false, value(site, "remap"));
                assertEquals("L" + MODE + ";" + (i == 0 ? "destroyProgress" : "f_105193_") + ":F", value(site, "target"));
                assertEquals(Opcodes.PUTFIELD, value(site, "opcode")); assertEquals(0, value(site, "ordinal"));
                assertArrayEquals(new String[]{"Lorg/spongepowered/asm/mixin/injection/At$Shift;", "AFTER"}, (String[]) value(site, "shift"));
            }
        }
    }
}
