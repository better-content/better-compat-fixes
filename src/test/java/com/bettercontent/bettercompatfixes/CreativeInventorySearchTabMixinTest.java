package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class CreativeInventorySearchTabMixinTest {
    private static final Path MIXIN = Path.of(
            "src/main/java/com/bettercontent/bettercompatfixes/mixin/minecraft/CreativeModeInventoryScreenMixin.java");

    @Test
    void filtersTheForgePageSourceInsteadOfAnObsoleteVanillaTabIteration() throws IOException {
        // Forge 47.4.13 constructs CreativeTabsScreenPage instances from this exact call during init.
        assertTrue(usesForgePageSourceInInit());

        final String source = Files.readString(MIXIN);
        assertTrue(source.contains("method = {\"init\", \"m_7856_\"}"));
        assertTrue(source.contains("remap = false"));
        assertTrue(source.contains("CreativeModeTabRegistry;getSortedCreativeModeTabs()Ljava/util/List;"));
        assertTrue(source.contains("tab != CreativeModeTabs.searchTab()"));
        assertFalse(source.contains("CreativeModeTabs;tabs()Ljava/util/List;"));
    }

    private static boolean usesForgePageSourceInInit() throws IOException {
        final boolean[] pageSourceFound = {false};
        try (InputStream resource = CreativeModeInventoryScreen.class
                .getResourceAsStream("CreativeModeInventoryScreen.class")) {
            assertTrue(resource != null, "mapped Creative screen bytecode must be available to the test runtime");
            new ClassReader(resource).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(
                        int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (!name.equals("init") || !descriptor.equals("()V")) {
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(
                                int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if (opcode == Opcodes.INVOKESTATIC
                                    && owner.equals("net/minecraftforge/common/CreativeModeTabRegistry")
                                    && name.equals("getSortedCreativeModeTabs")
                                    && descriptor.equals("()Ljava/util/List;")) {
                                pageSourceFound[0] = true;
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return pageSourceFound[0];
    }
}
