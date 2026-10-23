package com.bettercontent.bettercompatfixes;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Inspects fetched hash-pinned implementations and the current reobfuscated artifact, not dumps. */
final class AetherCuriosNativeContractTest {
    private static final String A = "com/aetherteam/aether/client/renderer/accessory/";
    private static final String E = "yesman/epicfight/";
    private static final String B = "com/bettercontent/bettercompatfixes/";
    private static final String DRAW = "(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V";

    private static Path pinned(String property, String sha1) throws Exception {
        Path path = Path.of(System.getProperty(property));
        assertEquals(sha1, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path))));
        return path;
    }

    private static ClassNode type(Path jar, String name) throws Exception {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entry = zip.getEntry(name + ".class");
            assertNotNull(entry, name);
            ClassNode node = new ClassNode();
            new ClassReader(zip.getInputStream(entry)).accept(node, 0);
            return node;
        }
    }

    private static long calls(ClassNode node, String methodPrefix, String owner, String name, String desc) {
        return node.methods.stream().filter(m -> methodPrefix.equals("render")
                        ? m.name.equals("render") : m.name.startsWith(methodPrefix))
                .flatMap(m -> java.util.Arrays.stream(m.instructions.toArray()))
                .filter(i -> i instanceof MethodInsnNode c && c.owner.equals(owner) && c.name.equals(name)
                        && (desc == null || c.desc.equals(desc))).count();
    }

    @Test void nativeAetherPassesHaveExactRedirectSitesAndVanillaPoseDependencies() throws Exception {
        Path aether = pinned("bc.aetherRender.native", "fb6af601028e399016271bc166fd1de888680de7");
        try (ZipFile zip = new ZipFile(aether.toFile())) {
            var manifest = new java.util.jar.Manifest(zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF")));
            assertEquals("1.20.1-1.5.2-neoforge", manifest.getMainAttributes().getValue("Implementation-Version"));
        }
        for (String renderer : List.of("GlovesRenderer", "PendantRenderer", "ShieldOfRepulsionRenderer")) {
            ClassNode node = type(aether, A + renderer);
            assertEquals(List.of("top/theillusivec4/curios/api/client/ICurioRenderer"), node.interfaces);
            assertTrue(calls(node, "render", "top/theillusivec4/curios/api/client/ICurioRenderer", "followBodyRotations", null) > 0);
        }
        ClassNode gloves = type(aether, A + "GlovesRenderer");
        assertEquals(2, calls(gloves, "render", A + "model/GlovesModel", "m_7695_", DRAW));
        assertEquals(1, calls(gloves, "lambda$render$0", A + "model/GlovesModel", "m_7695_", DRAW));
        assertEquals(0, calls(gloves, "renderFirstPerson", A + "model/GlovesModel", "m_7695_", DRAW));
        assertEquals(1, calls(type(aether, A + "PendantRenderer"), "render", A + "model/PendantModel", "m_7695_", DRAW));
        assertEquals(1, calls(type(aether, A + "ShieldOfRepulsionRenderer"), "render", "net/minecraft/client/model/HumanoidModel", "m_7695_", DRAW));
        // Do not duplicate Aether's changing material logic in our adapter.
        assertEquals(1, calls(gloves, "render", "com/aetherteam/aether/item/accessories/gloves/GlovesItem", "getGlovesTexture", null));
        assertEquals(3, calls(gloves, "render", "com/aetherteam/aether/item/accessories/gloves/GlovesItem", "getColors", null));
        assertEquals(1, calls(gloves, "render", "org/violetmoon/quark/content/tools/module/ColorRunesModule$Client", "getArmorEntityGlint", null));
    }

    @Test void nativeEpicDispatchRecognizesCustomInterfaceAndOwnsReloadBoundary() throws Exception {
        Path epic = pinned("bc.markerRender.8049910", "faa8eddad9d80adb9c4ba81fd0a8a21c81989d3d");
        ClassNode layer = type(epic, E + "compat/CuriosCompat$PatchedCuriosLayerRenderer");
        assertEquals(1, calls(layer, "lambda$renderLayer$0", E + "compat/CuriosCompat$EpicFightCurioRenderer", "draw", null));
        assertEquals(1, calls(type(epic, E + "compat/CuriosCompat"), "lambda$onModEventBusClient$1", "java/util/Map", "clear", null));
        // The native transformer considers all humanoid fields, not only Aether's overridden bodyParts.
        ClassNode baker = type(epic, E + "api/client/model/transformer/VanillaModelTransformer");
        assertEquals(8, calls(baker, "transformArmorModel", E + "api/client/model/transformer/VanillaModelTransformer$VanillaModelPartition", "<init>", null));
    }

    @Test void productionHooksRetainBothNamespacesAndOptionalClientOnlyGate() throws Exception {
        Path jar = Path.of("build/libs/better-compat-fixes-0.1.9.jar");
        for (String renderer : List.of("GlovesRenderer", "PendantRenderer", "ShieldOfRepulsionRenderer")) {
            ClassNode hook = type(jar, B + "mixin/aetherepicfight/" + renderer + "Mixin");
            assertTrue(hook.interfaces.contains(B + "client/aether/AetherRiggedCurioRenderer"));
            assertEquals(2, calls(hook, "bc$draw", B + "client/aether/AetherCurioMeshes", "draw", null));
            String bytes;
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                bytes = new String(zip.getInputStream(zip.getEntry(hook.name + ".class")).readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
            }
            assertTrue(bytes.contains(";renderToBuffer"));
            assertTrue(bytes.contains(";m_7695_"));
            assertTrue(bytes.contains("org/spongepowered/asm/mixin/injection/Group"));
        }
        ClassNode mesh = type(jar, B + "client/aether/AetherCurioMeshes");
        assertEquals(1, calls(mesh, "draw", E + "api/client/model/SkinnedMesh", "drawPosed", null));
        assertEquals(1, calls(mesh, "draw", "net/minecraft/client/model/HumanoidModel", "m_7695_", DRAW));
        assertEquals(1, calls(mesh, "lambda$bake$", E + "api/client/model/transformer/HumanoidModelTransformer", "transformArmorModel", null));
        String config = Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"));
        var json = com.google.gson.JsonParser.parseString(config).getAsJsonObject();
        assertEquals(4, json.getAsJsonArray("client").asList().stream().filter(e -> e.getAsString().startsWith("aetherepicfight.")).count());
        assertFalse(json.getAsJsonArray("mixins").toString().contains("aetherepicfight"));
        String plugin = Files.readString(Path.of("src/main/java/" + B + "compat/plugin/BetterContentMixinPlugin.java"));
        int start = plugin.indexOf("mixin.aetherepicfight.");
        String gate = plugin.substring(start, plugin.indexOf("\n        }", start));
        assertTrue(gate.contains("hasVersion(mods, \"aether\", \"1.20.1-1.5.2-neoforge\")"));
        assertTrue(gate.contains("hasVersion(mods, \"curios\", \"5.14.1+1.20.1\")"));
        assertTrue(gate.contains("hasVersion(mods, \"epicfight\", \"20.14.17\")"));
    }
}
