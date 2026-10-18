package com.bettercontent.bettercompatfixes.client;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Native hook shapes and shipping selectors, not a real-client transform/visual verdict. */
class PerspectiveNativeContractTest {
    private static final String ROOT = "com/bettercontent/bettercompatfixes/mixin/minecraft/PerspectiveControlsMixin";

    private static ClassNode type(String name) throws Exception {
        var node = new ClassNode();
        try (var input = PerspectiveNativeContractTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(input, name);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private static Object value(AnnotationNode annotation, String key) {
        assertNotNull(annotation.values);
        int index = annotation.values.indexOf(key);
        assertTrue(index >= 0, key);
        return annotation.values.get(index + 1);
    }

    @Test
    void nativeFirstConsumeClickIsPerspectiveAndCameraUpdatesRemainInTheSameBranch() throws Exception {
        var method = type("net/minecraft/client/Minecraft").methods.stream()
                .filter(m -> m.name.equals("handleKeybinds")).findFirst().orElseThrow();
        var calls = new ArrayList<String>();
        String lastField = null;
        boolean firstClick = true;
        for (var instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field) lastField = field.name;
            if (instruction instanceof MethodInsnNode call) {
                calls.add(call.owner + "." + call.name);
                if (call.owner.equals("net/minecraft/client/KeyMapping") && call.name.equals("consumeClick") && firstClick) {
                    assertEquals("keyTogglePerspective", lastField);
                    firstClick = false;
                }
            }
        }
        assertFalse(firstClick);
        var cycle = "net/minecraft/client/CameraType.cycle";
        assertEquals(1, calls.stream().filter(cycle::equals).count());
        int cycleIndex = calls.indexOf(cycle);
        int setIndex = calls.indexOf("net/minecraft/client/Options.setCameraType");
        int postIndex = calls.indexOf("net/minecraft/client/renderer/GameRenderer.checkEntityPostEffect");
        int updateIndex = calls.indexOf("net/minecraft/client/renderer/LevelRenderer.needsUpdate");
        assertTrue(cycleIndex < setIndex && setIndex < postIndex && postIndex < updateIndex);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shippingClientMixinHasStrictDevelopmentAndProductionSelectors() throws Exception {
        try (var jar = new ZipFile("build/libs/better-compat-fixes-0.1.9.jar")) {
            var config = JsonParser.parseReader(new InputStreamReader(jar.getInputStream(
                    jar.getEntry("better_compat_fixes.mixins.json")))).getAsJsonObject();
            assertTrue(config.getAsJsonArray("client").asList().stream()
                    .anyMatch(e -> e.getAsString().equals("minecraft.PerspectiveControlsMixin")));
            assertFalse(config.getAsJsonArray("mixins").toString().contains("PerspectiveControlsMixin"));
            var node = new ClassNode();
            new ClassReader(jar.getInputStream(jar.getEntry(ROOT + ".class"))).accept(node, 0);
            int hooks = 0;
            for (var method : node.methods) {
                if (method.visibleAnnotations == null) continue;
                for (var annotation : method.visibleAnnotations) {
                    if (!annotation.desc.endsWith("/WrapOperation;")) continue;
                    hooks++;
                    assertEquals(List.of("handleKeybinds", "m_91279_"), value(annotation, "method"));
                    assertEquals(false, value(annotation, "remap"));
                    assertEquals(1, value(annotation, "require"));
                    assertEquals(1, value(annotation, "allow"));
                    var sites = (List<AnnotationNode>) value(annotation, "at");
                    assertEquals(2, sites.size());
                    var targets = sites.stream().map(a -> (String) value(a, "target")).toList();
                    if (method.name.endsWith("consumePerspectiveInput")) {
                        assertEquals(List.of("Lnet/minecraft/client/KeyMapping;consumeClick()Z",
                                "Lnet/minecraft/client/KeyMapping;m_90859_()Z"), targets);
                        sites.forEach(a -> assertEquals(0, value(a, "ordinal")));
                    } else {
                        assertEquals(List.of("Lnet/minecraft/client/CameraType;cycle()Lnet/minecraft/client/CameraType;",
                                "Lnet/minecraft/client/CameraType;m_90614_()Lnet/minecraft/client/CameraType;"), targets);
                    }
                }
            }
            assertEquals(2, hooks);
        }
    }
}
