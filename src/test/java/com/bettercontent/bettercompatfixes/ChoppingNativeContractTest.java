package com.bettercontent.bettercompatfixes;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;

/** Inspects current mapped Forge bytecode; does not claim a rendered-client run. */
class ChoppingNativeContractTest {
    @Test void nativeIncrementPrecedesCompletionResetAndBothHoldHitSitesAreCovered() throws Exception {
        var writes = new ArrayList<Boolean>();
        try (InputStream in = getClass().getResourceAsStream("/net/minecraft/client/multiplayer/MultiPlayerGameMode.class")) {
            assertNotNull(in);
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int a, String name, String d, String s, String[] e) {
                    if (!name.equals("continueDestroyBlock")) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        boolean increment;
                        @Override public void visitInsn(int op) { increment = op == Opcodes.FADD; }
                        @Override public void visitFieldInsn(int op, String owner, String field, String descriptor) {
                            if (op == Opcodes.PUTFIELD && field.equals("destroyProgress")) {
                                writes.add(increment);
                                increment = false;
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals(java.util.List.of(true, false), writes, "First write adds progress; second resets after completion");
        int[] hits = {0};
        try (InputStream in = getClass().getResourceAsStream("/net/minecraft/client/Minecraft.class")) {
            assertNotNull(in);
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int a, String name, String d, String s, String[] e) {
                    if (!name.equals("continueAttack")) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int op, String owner, String name, String descriptor, boolean itf) {
                            if (owner.equals("net/minecraft/client/particle/ParticleEngine") && name.equals("addBlockHitEffects")) hits[0]++;
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals(2, hits[0]);
        String mixins = Files.readString(Path.of("src/main/resources/better_compat_fixes.chopping.mixins.json"));
        assertTrue(mixins.contains("minecraft.ChoppingProgressMixin"));
        assertTrue(mixins.contains("minecraft.ChoppingHitEffectsMixin"));
        String renderer = Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/compat/ChoppingParticles.java"));
        assertTrue(renderer.contains("state.is(BlockTags.LOGS)"));
        assertTrue(renderer.contains("instanceof BranchBlock"));
        assertTrue(renderer.contains("getPrimitiveLog()"));
        assertTrue(renderer.contains("hit.getLocation()"));
        assertFalse(renderer.contains("sendToServer"));
    }
}
