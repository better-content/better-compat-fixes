package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;

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

/** Verifies the narrow local/vector bridge against reproducibly fetched ParCool 3.4.3.3. */
final class ParCoolDodgeNativeContractTest {
    @Test
    void arrowRequestEntersBeforeTheUnmodifiedNativeRestrictions() throws Exception {
        final Path jar = Path.of(System.getProperty("bc.parcool.native"));
        assertEquals("55f8b8a9fd9e23cd0dccff56461a8ae88464caed", HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar))));
        final ClassNode type = new ClassNode();
        try (ZipFile zip = new ZipFile(jar.toFile());
                var input = zip.getInputStream(zip.getEntry("com/alrex/parcool/common/action/impl/Dodge.class"))) {
            new ClassReader(input).accept(type, 0);
        }
        final MethodNode method = type.methods.stream().filter(m -> m.name.equals("canStart")).findFirst().orElseThrow();
        final var instructions = Arrays.stream(method.instructions.toArray()).filter(i -> i.getOpcode() >= 0).toList();
        final VarInsnNode firstStore = instructions.stream()
                .filter(i -> i.getOpcode() == Opcodes.ASTORE).map(i -> (VarInsnNode) i).findFirst().orElseThrow();
        assertEquals(Opcodes.ACONST_NULL, instructions.get(instructions.indexOf(firstStore) - 1).getOpcode());
        assertTrue(method.localVariables.stream().anyMatch(v -> v.index == firstStore.var
                && v.desc.equals("Lcom/alrex/parcool/common/action/impl/Dodge$DodgeDirection;")));
        assertEquals(1, instructions.stream().filter(i -> i instanceof MethodInsnNode call
                && call.owner.equals("com/alrex/parcool/client/input/KeyRecorder")
                && call.name.equals("getLastMoveVector")
                && call.desc.equals("()Lnet/minecraft/world/phys/Vec3;")).count());
        // A non-null initial direction skips only input selection, not native cooldown/stamina checks.
        assertTrue(instructions.stream().anyMatch(i -> i instanceof JumpInsnNode jump
                && jump.getOpcode() == Opcodes.IFNONNULL));
        assertTrue(instructions.stream().anyMatch(i -> i instanceof FieldInsnNode field && field.name.equals("coolTime")));
        assertTrue(instructions.stream().anyMatch(i -> i instanceof MethodInsnNode call
                && call.owner.equals("com/alrex/parcool/common/capability/IStamina") && call.name.equals("isExhausted")));
        assertTrue(instructions.stream().anyMatch(i -> i instanceof MethodInsnNode call
                && call.name.equals("isInSuccessiveCoolDown")));
    }
}
