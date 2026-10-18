package com.bettercontent.bettercompatfixes;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

/** Native shapes and standard generated mappings; not an actual client-transform verdict. */
class ChoppingNativeMappingContractTest {
    private static final String MC = "net/minecraft/client/Minecraft";
    private static final String MODE = "net/minecraft/client/multiplayer/MultiPlayerGameMode";
    private static final String ROOT = "com/bettercontent/bettercompatfixes/mixin/minecraft/";
    private static final String BLOCK = "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z";
    private static final String REF = "better_compat_fixes.chopping.refmap.json";
    private static final String CONFIG = "better_compat_fixes.chopping.mixins.json";
    private static final String HIT = "Lnet/minecraft/client/particle/ParticleEngine;addBlockHitEffects(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)V";
    private ClassNode nativeType(String name) throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/" + name + ".class")) {
            assertNotNull(input); new ClassReader(input).accept(node, 0);
        }
        return node;
    }
    private static Object optional(AnnotationNode annotation, String key) {
        if (annotation.values == null || !annotation.values.contains(key)) return null;
        return annotation.values.get(annotation.values.indexOf(key) + 1);
    }
    private static Object value(AnnotationNode annotation, String key) {
        Object result = optional(annotation, key); assertNotNull(result, key); return result;
    }
    private static AnnotationNode annotation(List<AnnotationNode> annotations, String name) {
        return annotations.stream().filter(a -> a.desc.endsWith("/" + name + ";")).findFirst().orElseThrow();
    }
    private static ClassNode staged(ZipFile jar, String name) throws Exception {
        var node = new ClassNode(); new ClassReader(jar.getInputStream(jar.getEntry(ROOT + name + ".class"))).accept(node, 0); return node;
    }
    private static void strict(AnnotationNode annotation, int count, String selector) {
        assertNotEquals(false, optional(annotation, "remap"));
        assertEquals(count, value(annotation, "require")); assertEquals(count, value(annotation, "allow"));
        assertEquals(List.of(selector), value(annotation, "method"));
    }
    @Test void pinnedNativeHasTwoForgeHitSitesAndFirstIncrementBeforeReset() throws Exception {
        var minecraft = nativeType(MC);
        var attack = minecraft.methods.stream().filter(m -> m.name.equals("continueAttack") && m.desc.equals("(Z)V")).findFirst().orElseThrow();
        int hits = 0;
        for (var insn : attack.instructions) if (insn instanceof MethodInsnNode call && ("L" + call.owner + ";" + call.name + call.desc).equals(HIT)) hits++;
        assertEquals(2, hits);
        var mode = nativeType(MODE);
        assertEquals(0x12, mode.fields.stream().filter(f -> f.name.equals("minecraft")).findFirst().orElseThrow().access);
        assertEquals(0x02, mode.fields.stream().filter(f -> f.name.equals("destroyProgress")).findFirst().orElseThrow().access);
        var progress = mode.methods.stream().filter(m -> m.name.equals("continueDestroyBlock") && m.desc.equals(BLOCK)).findFirst().orElseThrow();
        int writes = 0;
        for (var insn : progress.instructions) if (insn instanceof FieldInsnNode field && field.owner.equals(MODE) && field.name.equals("destroyProgress") && field.getOpcode() == Opcodes.PUTFIELD) {
            if (writes++ == 0) {
                var previous = insn.getPrevious(); while (previous.getOpcode() < 0) previous = previous.getPrevious();
                assertEquals(Opcodes.FADD, previous.getOpcode());
            }
        }
        assertEquals(2, writes);
    }
    @Test void archiveRegistersOnlyChoppingWithItsActualGeneratedRefmapAndSrgShadowBindings() throws Exception {
        try (var jar = new ZipFile("build/libs/better-compat-fixes-0.1.9.jar")) {
            var manifest = new Manifest(jar.getInputStream(jar.getEntry("META-INF/MANIFEST.MF")));
            assertTrue(List.of(manifest.getMainAttributes().getValue("MixinConfigs").split(",")).contains(CONFIG));
            var config = JsonParser.parseReader(new java.io.InputStreamReader(jar.getInputStream(jar.getEntry(CONFIG)))).getAsJsonObject();
            assertEquals(REF, config.get("refmap").getAsString()); assertEquals(2, config.getAsJsonArray("client").size());
            var old = JsonParser.parseReader(new java.io.InputStreamReader(jar.getInputStream(jar.getEntry("better_compat_fixes.mixins.json")))) .getAsJsonObject();
            assertFalse(old.has("refmap")); assertFalse(old.getAsJsonArray("client").toString().contains("Chopping"));
            assertNotNull(jar.getEntry(REF));
            var mapper = ReferenceMapper.read(new java.io.InputStreamReader(jar.getInputStream(jar.getEntry(REF))), REF);
            assertFalse(mapper.isDefault());
            for (String context : new String[]{null, "searge"}) {
                mapper.setContext(context);
                for (String[] pair : new String[][]{{"ChoppingHitEffectsMixin", "continueAttack(Z)V", "m_91386_"},
                        {"ChoppingProgressMixin", "startDestroyBlock" + BLOCK, "m_105269_"},
                        {"ChoppingProgressMixin", "stopDestroyBlock()V", "m_105276_"},
                        {"ChoppingProgressMixin", "continueDestroyBlock" + BLOCK, "m_105283_"},
                        {"ChoppingProgressMixin", "L" + MODE + ";destroyProgress:F", "f_105193_"}}) {
                    assertTrue(mapper.remap(ROOT + pair[0], pair[1]).contains(pair[2]), pair[1] + " context=" + context);
                }
            }
            var progress = staged(jar, "ChoppingProgressMixin");
            for (String name : new String[]{"f_105189_", "f_105193_"}) {
                var field = progress.fields.stream().filter(f -> f.name.equals(name)).findFirst().orElseThrow();
                var shadow = annotation(field.visibleAnnotations, "Shadow");
                assertNull(optional(shadow, "aliases")); assertNotEquals(false, optional(shadow, "remap"));
                // Standard AP reobfuscation binds the SAME primary name, irrespective of target AT privacy.
                var target = nativeType(MODE).fields.stream().filter(f -> f.name.equals(name.equals("f_105189_") ? "minecraft" : "destroyProgress")).findFirst().orElseThrow();
                target.name = name;
                for (int visibility : new int[]{Opcodes.ACC_PRIVATE, Opcodes.ACC_PUBLIC}) {
                    target.access = (target.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) | visibility;
                    assertEquals(field.name, target.name, "No alias lookup or visibility widening is needed for private or AT-public targets");
                }
            }
        }
    }
    @Test void stagedCallbacksKeepExactCountsForgeApiAndFirstWriteAfterTiming() throws Exception {
        try (var jar = new ZipFile("build/libs/better-compat-fixes-0.1.9.jar")) {
            var hit = staged(jar, "ChoppingHitEffectsMixin");
            var redirect = annotation(hit.methods.stream().filter(m -> m.name.equals("bc$replaceWoodHits")).findFirst().orElseThrow().visibleAnnotations, "Redirect");
            strict(redirect, 2, "continueAttack(Z)V");
            var at = (AnnotationNode) value(redirect, "at"); assertEquals(HIT, value(at, "target")); assertEquals(false, value(at, "remap"));
            var progress = staged(jar, "ChoppingProgressMixin");
            for (String[] hook : new String[][]{{"bc$newTarget", "startDestroyBlock" + BLOCK}, {"bc$stop", "stopDestroyBlock()V"}, {"bc$progress", "continueDestroyBlock" + BLOCK}}) {
                var inject = annotation(progress.methods.stream().filter(m -> m.name.equals(hook[0])).findFirst().orElseThrow().visibleAnnotations, "Inject");
                strict(inject, 1, hook[1]);
                @SuppressWarnings("unchecked") var sites = (List<AnnotationNode>) value(inject, "at");
                assertEquals(1, sites.size()); var site = sites.get(0);
                if (!hook[0].equals("bc$progress")) { assertEquals("HEAD", value(site, "value")); continue; }
                assertNotEquals(false, optional(site, "remap")); assertEquals("FIELD", value(site, "value"));
                assertEquals("L" + MODE + ";destroyProgress:F", value(site, "target"));
                assertEquals(Opcodes.PUTFIELD, value(site, "opcode")); assertEquals(0, value(site, "ordinal"));
                assertArrayEquals(new String[]{"Lorg/spongepowered/asm/mixin/injection/At$Shift;", "AFTER"}, (String[]) value(site, "shift"));
            }
        }
    }
}
