package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Guards the fast-run accessor's actual lambda against the pinned Epic ParCool JAR. */
final class EpicParCoolFastRunStowNativeTest {
    private static final String OWNER = "com/yesman/epicparcool/animations/ParCoolAnimations";
    private static final String NATIVE_SHA1 = "d38194b73c91e5af84ef28a7a07e0eb4340ef76d";

    @Test
    void stowSuppressionTargetsFastRunAndNotAnUnrelatedParkourMotion() throws Exception {
        Path jar = Path.of(System.getProperty("bc.epicparcool.native"));
        assertEquals(NATIVE_SHA1, HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar))));
        ClassNode nativeAnimations = new ClassNode();
        try (ZipFile zip = new ZipFile(jar.toFile());
             var in = zip.getInputStream(zip.getEntry(OWNER + ".class"))) {
            new ClassReader(in).accept(nativeAnimations, ClassReader.SKIP_FRAMES);
        }
        MethodNode build = method(nativeAnimations, "build");
        String fastRun = accessorBuilder(build, "biped/fast_run");
        assertEquals("lambda$build$32", fastRun, "Pinned fast-run lambda changed: re-inspect callbacks");
        assertEquals("lambda$build$17", accessorBuilder(build, "biped/cling_to_cliff_left"),
                "Former hook target was an unrelated cliff animation");

        MethodNode fastRunBuilder = method(nativeAnimations, fastRun);
        assertEquals(1, fieldCount(fastRunBuilder, "SET_TOOLS_BACK"));
        assertEquals(1, fieldCount(fastRunBuilder, "SET_TOOLS_BACK_WHEN_ITEM_CHANGED"));
        assertEquals(1, fieldCount(fastRunBuilder, "REVERT_TO_HANDS"));
        assertEquals(1, fieldCount(fastRunBuilder, "ON_BEGIN_EVENTS"));
        assertEquals(1, fieldCount(fastRunBuilder, "ON_ITEM_CHANGE_EVENT"));
        assertEquals(1, fieldCount(fastRunBuilder, "ON_END_EVENTS"));
        List<String> eventProperties = new ArrayList<>();
        int addEvents = 0;
        int addProperties = 0;
        for (AbstractInsnNode instruction : fastRunBuilder.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.name.startsWith("ON_")) {
                eventProperties.add(field.name);
            }
            if (instruction instanceof MethodInsnNode call && call.owner.endsWith("/StaticAnimation")) {
                if (call.name.equals("addEvents") && call.desc.contains("StaticAnimationProperty")) addEvents++;
                if (call.name.equals("addProperty")) addProperties++;
            }
        }
        assertEquals(List.of("ON_BEGIN_EVENTS", "ON_ITEM_CHANGE_EVENT", "ON_END_EVENTS"), eventProperties);
        assertEquals(2, addEvents, "Ordinal zero is begin, not the preserved end callback");
        assertEquals(1, addProperties, "Ordinal zero is the item-change callback");

        String source = Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/mixin/epicfight/EpicParCoolFastRunStowMixin.java"));
        var targets = java.util.regex.Pattern.compile("method = \"([^\"]+)\"")
                .matcher(source).results().map(match -> match.group(1)).toList();
        assertEquals(List.of(fastRun, fastRun), targets, "Both stow callbacks must target the fast-run builder");
        assertEquals(2, java.util.regex.Pattern.compile("require = 1, expect = 1, allow = 1")
                .matcher(source).results().count(), "Fail if either pinned injection site disappears");
        assertTrue(source.contains("property == StaticAnimationProperty.ON_BEGIN_EVENTS"));
        assertTrue(source.contains("property == StaticAnimationProperty.ON_ITEM_CHANGE_EVENT"));
        assertFalse(source.contains("property == StaticAnimationProperty.ON_END_EVENTS"));
        assertTrue(Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"))
                .contains("epicfight.EpicParCoolFastRunStowMixin"));
        assertTrue(Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/compat/plugin/BetterContentMixinPlugin.java"))
                .contains("EpicParCoolFastRunStowMixin"));
    }

    private static MethodNode method(ClassNode type, String name) {
        return type.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    private static String accessorBuilder(MethodNode build, String asset) {
        boolean found = false;
        for (AbstractInsnNode instruction : build.instructions.toArray()) {
            if (instruction instanceof LdcInsnNode literal && asset.equals(literal.cst)) {
                found = true;
            } else if (found && instruction instanceof InvokeDynamicInsnNode factory) {
                List<String> targets = new ArrayList<>();
                for (Object argument : factory.bsmArgs) {
                    if (argument instanceof Handle handle && OWNER.equals(handle.getOwner())) {
                        targets.add(handle.getName());
                    }
                }
                assertEquals(1, targets.size(), "Expected one builder for " + asset);
                return targets.get(0);
            }
        }
        throw new AssertionError("Missing accessor builder for " + asset);
    }

    private static long fieldCount(MethodNode method, String fieldName) {
        long count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.name.equals(fieldName)) count++;
        }
        return count;
    }
}
