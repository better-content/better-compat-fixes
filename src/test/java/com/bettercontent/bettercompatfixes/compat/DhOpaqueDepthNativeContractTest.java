package com.bettercontent.bettercompatfixes.compat;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Native signature/selector checks on freshly resolved, hash-pinned archives; not decompilation. */
final class DhOpaqueDepthNativeContractTest {
    private static final String ROOT = "src/main/java/com/bettercontent/bettercompatfixes/";

    @Test
    void pinnedOculusGetterAndLiveFieldMatchTheInjection() throws Exception {
        Path jar = pinnedJar("bc.dhDepth.oculus", "984f774e71790deaec674c7587bd24e0711871b2");
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ClassNode type = readClass(zip, "net/irisshaders/iris/compat/dh/DHCompatInternal.class");
            assertTrue(type.fields.stream().anyMatch(field -> field.name.equals("storedDepthTex")
                    && field.desc.equals("I") && (field.access & Opcodes.ACC_STATIC) == 0));
            var getter = type.methods.stream().filter(method -> method.name.equals("getDepthTexNoTranslucent")
                    && method.desc.equals("()I")).findFirst().orElseThrow();
            int returns = 0;
            boolean readsCopy = false;
            for (var instruction : getter.instructions) {
                if (instruction.getOpcode() == Opcodes.IRETURN) returns++;
                if (instruction instanceof FieldInsnNode field && field.name.equals("depthTexNoTranslucent"))
                    readsCopy = true;
            }
            assertEquals(2, returns, "Both native fallback and texture returns must be patched");
            assertTrue(readsCopy);
            int copies = 0;
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().startsWith("net/irisshaders/iris/compat/dh/LodRendererEvents$")
                        || !entry.getName().endsWith(".class")) continue;
                for (var method : readClass(zip, entry.getName()).methods) {
                    for (var instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode call && call.name.equals("copyTranslucents")) copies++;
                    }
                }
            }
            assertEquals(1, copies, "Pinned Oculus has only the transparent-event copy site");
        }
    }

    @Test
    void pinnedDhExposesTheDirectTransparencyApi() throws Exception {
        Path jar = pinnedJar("bc.dhDepth.dh", "ce3814dd5971edda4d04c3a42ef0df5c6cf8e10d");
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ClassNode graphics = readClass(zip,
                    "com/seibel/distanthorizons/api/interfaces/config/client/IDhApiGraphicsConfig.class");
            assertTrue(graphics.methods.stream().anyMatch(method -> method.name.equals("transparency")
                    && method.desc.equals("()Lcom/seibel/distanthorizons/api/interfaces/config/IDhApiConfigValue;")));
            ClassNode modes = readClass(zip,
                    "com/seibel/distanthorizons/api/enums/rendering/EDhApiTransparency.class");
            for (String mode : new String[]{"DISABLED", "FAKE", "COMPLETE"})
                assertTrue(modes.fields.stream().anyMatch(field -> field.name.equals(mode)));
        }
    }

    @Test
    void hookIsClientOnlyVersionGatedAndDoesNotModifyCameraOrHistory() throws Exception {
        var config = JsonParser.parseString(Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json")))
                .getAsJsonObject();
        String name = "oculus.DHCompatInternalMixin";
        assertTrue(config.getAsJsonArray("client").asList().stream().anyMatch(value -> value.getAsString().equals(name)));
        assertFalse(config.getAsJsonArray("mixins").asList().stream().anyMatch(value -> value.getAsString().equals(name)));
        String plugin = Files.readString(Path.of(ROOT + "compat/plugin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("hasVersion(mods, \"oculus\", \"1.8.0\")"));
        assertTrue(plugin.contains("hasVersion(mods, \"distanthorizons\", \"2.4.5-b\")"));
        String mixin = Files.readString(Path.of(ROOT + "mixin/oculus/DHCompatInternalMixin.java"));
        assertTrue(mixin.contains("getDepthTexNoTranslucent()I"));
        assertTrue(mixin.contains("require = 2, allow = 2"));
        assertTrue(mixin.contains("graphics().transparency().getValue() == EDhApiTransparency.DISABLED"));
        assertTrue(mixin.contains("DhOpaqueDepthPolicy.selectDepth(disabled, storedDepthTex, copiedDepth)"));
        assertFalse(mixin.contains("cameraPosition"));
        assertFalse(mixin.contains("blendFactor"));
    }

    private static Path pinnedJar(String property, String sha1) throws Exception {
        String location = System.getProperty(property);
        assertNotNull(location, "Gradle must prepare the declared native fixture: " + property);
        Path path = Path.of(location);
        assertEquals(sha1, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path))));
        return path;
    }

    private static ClassNode readClass(ZipFile zip, String path) throws Exception {
        var entry = zip.getEntry(path);
        assertNotNull(entry, "Required pinned native class: " + path);
        ClassNode type = new ClassNode();
        try (var input = zip.getInputStream(entry)) {
            new ClassReader(input).accept(type, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return type;
    }
}
