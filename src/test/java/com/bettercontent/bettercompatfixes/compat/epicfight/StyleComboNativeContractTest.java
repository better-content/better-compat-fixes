package com.bettercontent.bettercompatfixes.compat.epicfight;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pins the native call boundary that used to escape our selected-list redirects. */
final class StyleComboNativeContractTest {
    private static final String E = "yesman/epicfight/";
    private static final String CAPABILITY = E + "world/capabilities/item/CapabilityItem";

    private static ClassNode type(ZipFile zip, String name) throws Exception {
        var entry = zip.getEntry(name + ".class");
        assertNotNull(entry, name);
        var node = new ClassNode();
        new ClassReader(zip.getInputStream(entry)).accept(node, 0);
        return node;
    }

    @Test void nativeCounterInvokesCapabilityValidationBeforeSelectedListClamp() throws Exception {
        Path path = Path.of(System.getProperty("bc.markerRender.8049910"));
        assertEquals("faa8eddad9d80adb9c4ba81fd0a8a21c81989d3d", HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path))));
        try (var zip = new ZipFile(path.toFile())) {
            var basic = type(zip, E + "skill/BasicAttack");
            var counter = basic.methods.stream().filter(m -> m.name.equals("setComboCounterWithEvent"))
                    .findFirst().orElseThrow();
            var calls = Arrays.stream(counter.instructions.toArray())
                    .filter(i -> i instanceof MethodInsnNode c && c.owner.equals(CAPABILITY))
                    .map(i -> (MethodInsnNode) i).toList();
            assertEquals(2, calls.size());
            assertEquals("handleComboCounter", calls.get(0).name);
            assertEquals("getAutoAttackMotion", calls.get(1).name);
            assertTrue(Arrays.stream(counter.instructions.toArray()).anyMatch(i ->
                    i instanceof FieldInsnNode f && f.name.equals("RESET_PLAYER_COMBO_COUNTER")),
                    "Native reset-property guard remains outside our redirect");
            assertTrue(Arrays.stream(counter.instructions.toArray()).anyMatch(i ->
                    i instanceof MethodInsnNode c && c.name.equals("triggerEvents")),
                    "Native combo events remain outside our redirect");
            var handler = type(zip, E + "world/entity/eventlistener/ComboCounterHandleEvent$ComboCounterHandler");
            assertEquals(2, handler.methods.stream().filter(m -> m.name.startsWith("lambda$")
                    && Arrays.stream(m.instructions.toArray()).anyMatch(i ->
                    i instanceof MethodInsnNode c && c.owner.equals(CAPABILITY)
                            && c.name.equals("getAutoAttackMotion"))).count(),
                    "Both native handler variants validate the unmodified held list");
        }
    }

    @Test void compiledRedirectTargetsExactlyTheNativeValidationCall() throws Exception {
        var node = new ClassNode();
        try (var in = getClass().getClassLoader().getResourceAsStream(
                "com/bettercontent/bettercompatfixes/mixin/epicfight/BasicAttackMixin.class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        var method = node.methods.stream().filter(m -> m.name.equals("betterContentFixes$selectedCounterValidation"))
                .findFirst().orElseThrow();
        assertTrue((method.access & Opcodes.ACC_STATIC) != 0);
        var redirect = method.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Redirect;"))
                .findFirst().orElseThrow();
        String values = redirect.values.toString();
        assertTrue(values.contains("setComboCounterWithEvent"));
        assertTrue(values.contains("require, 1") && values.contains("allow, 1"));
        var at = (AnnotationNode) redirect.values.get(redirect.values.indexOf("at") + 1);
        assertTrue(at.values.toString().contains("CapabilityItem;handleComboCounter("));
        assertEquals(1, Arrays.stream(method.instructions.toArray()).filter(i ->
                i instanceof MethodInsnNode c && c.owner.endsWith("/StyleCombat") && c.name.equals("counter")).count());
    }
}
