package com.bettercontent.bettercompatfixes.compat;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

final class ClientReloadNativeContractTest {
    private static final String DH = "loaderCommon/forge/com/seibel/distanthorizons/common/wrappers/block/";
    private static final String UPDATE = "(L" + DH + "BiomeWrapper;L" + DH + "BlockStateWrapper;Lcom/seibel/distanthorizons/core/dataObjects/fullData/sources/FullDataSourceV2;Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/IClientLevelWrapper;)V";

    @Test void eyesResolveAllThreeNativeModelsAndRetainHeadGeometry() throws Exception {
        Path jar = pin("bc.clientReload.iceandfire", "2b80245fc9b7d6fdc61d71f9892f4c6114eb7f303f65634845aaab25f84d1e82");
        try (ZipFile zip = new ZipFile(jar.toFile()); URLClassLoader loader = new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, null)) {
            var eyes = method(read(zip, "com/github/alexthe666/iceandfire/client/render/entity/layer/LayerDragonEyes"), "<init>", "(Lnet/minecraft/client/renderer/entity/MobRenderer;)V");
            List<String> paths = new ArrayList<>();
            for (var op : eyes.instructions) if (op instanceof LdcInsnNode ldc && ldc.cst instanceof String path && path.contains("_Ground")) paths.add(path);
            assertEquals(3, paths.size());
            for (String path : paths) {
                assertNull(loader.getResourceAsStream(path + ".tbl"), "Original ClassLoader lookup must reproduce failure");
                String repaired = DragonEyesModelPaths.classLoaderPath(path + ".tbl");
                try (var input = loader.getResourceAsStream(repaired)) {
                    assertNotNull(input, repaired);
                    try (ZipInputStream model = new ZipInputStream(input)) {
                        boolean head = false;
                        for (var entry = model.getNextEntry(); entry != null; entry = model.getNextEntry())
                            if (entry.getName().equals("model.json"))
                                head = new String(model.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("HeadFront");
                        assertTrue(head, "Actual eye model must contain the cube consumed by onlyKeepCubes");
                    }
                }
            }
            var helper = method(read(zip, "com/github/alexthe666/iceandfire/client/model/util/TabulaModelHandlerHelper"), "loadTabulaModel",
                    "(Ljava/lang/String;)Lcom/github/alexthe666/citadel/client/model/container/TabulaModelContainer;");
            assertEquals(1, calls(helper, "java/lang/ClassLoader", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;").size());
        }
    }

    @Test void dhInheritedUpdateIsTheMissingParentSiteAndExplicitOverloadStillWins() throws Exception {
        Path jar = pin("bc.dhDepth.dh", "e29c5d6a47ce40ec174f96104ac3639605de30e3811b39907de7070feb230cc2");
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var base = read(zip, DH + "AbstractDhTintGetter");
            var getter = read(zip, DH + "TintGetterOverride");
            assertEquals(DH + "AbstractDhTintGetter", getter.superName);
            assertTrue(getter.fields.stream().anyMatch(f -> f.name.equals("parent") && f.desc.equals("Lnet/minecraft/world/level/LevelReader;") && f.access == Opcodes.ACC_PRIVATE));
            assertFalse(getter.methods.stream().anyMatch(m -> m.name.equals("update") && m.desc.equals(UPDATE)));
            var update = method(base, "update", UPDATE);
            assertEquals(1, Arrays.stream(update.instructions.toArray()).filter(op -> op.getOpcode() == Opcodes.RETURN).count());
            assertTrue(Arrays.stream(update.instructions.toArray()).anyMatch(op -> op instanceof FieldInsnNode f && f.getOpcode() == Opcodes.PUTFIELD && f.name.equals("clientLevelWrapper")));
            var color = read(zip, DH + "ClientBlockStateColorCache");
            long sites = color.methods.stream().mapToLong(m -> calls(m, DH + "TintGetterOverride", "update", UPDATE).size()).sum();
            assertEquals(1, sites, "Native getColor calls inherited update, not the explicit-parent overload");
            var explicit = method(getter, "update", "(Lnet/minecraft/world/level/LevelReader;" + UPDATE.substring(1));
            int superUpdate = explicit.instructions.indexOf(calls(explicit, base.name, "update", UPDATE).get(0));
            List<FieldInsnNode> writes = Arrays.stream(explicit.instructions.toArray()).filter(op -> op instanceof FieldInsnNode f && f.name.equals("parent") && f.getOpcode() == Opcodes.PUTFIELD).map(op -> (FieldInsnNode) op).toList();
            assertEquals(1, writes.size());
            assertTrue(superUpdate < explicit.instructions.indexOf(writes.get(0)), "Explicit parent must override inherited contextual default");
            var wrapper = read(zip, "loaderCommon/forge/com/seibel/distanthorizons/common/wrappers/world/ClientLevelWrapper");
            assertTrue(wrapper.methods.stream().anyMatch(m -> m.name.equals("getWrappedMcObject") && m.desc.equals("()Ljava/lang/Object;")));
            method(read(zip, "com/seibel/distanthorizons/api/interfaces/IDhApiUnsafeWrapper"), "getWrappedMcObject", "()Ljava/lang/Object;");
            var compiled = compiled("mixin/distanthorizons/AbstractDhTintGetterMixin");
            var hook = compiled.methods.stream().filter(m -> m.name.equals("bettercompatfixes$refreshTintContext")).findFirst().orElseThrow();
            assertEquals(1, calls(hook, "com/seibel/distanthorizons/core/wrapperInterfaces/world/IClientLevelWrapper", "getWrappedMcObject", "()Ljava/lang/Object;").size());
            var annotation = hook.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Inject;")).findFirst().orElseThrow();
            assertEquals(List.of("update" + UPDATE), annotation.values.get(annotation.values.indexOf("method") + 1));
            assertEquals(1, annotation.values.get(annotation.values.indexOf("require") + 1));
            assertEquals(1, annotation.values.get(annotation.values.indexOf("allow") + 1));
        }
    }

    @Test void optionalSkinHelperIsExactlyPinnedNonUniqueAndDelegatesBothOptionalDefaults() throws Exception {
        Path jar = pin("bc.clientReload.tinkercompat", "e8929d00d44efbff0811810f15faa31be07e53f7073a5b068638e07690670f49");
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var mixin = read(zip, "com/minhhjjj/epicfighttinkercompat/mixin/tool/MixinItemSkinsReloadListener");
            var helper = method(mixin, "addTinkerWeapon", "(Ljava/util/Map;Ljava/lang/String;DDDDDDI)V");
            assertEquals(Opcodes.ACC_PRIVATE, helper.access);
            assertTrue(helper.visibleAnnotations == null || helper.visibleAnnotations.stream().noneMatch(a -> a.desc.endsWith("/Unique;")));
            assertTrue(helper.invisibleAnnotations == null || helper.invisibleAnnotations.stream().noneMatch(a -> a.desc.endsWith("/Unique;")));
            var shortHelper = method(mixin, "addTinkerWeapon", "(Ljava/util/Map;Ljava/lang/String;DDDDDD)V");
            assertEquals(1, calls(shortHelper, mixin.name, "addTinkerWeapon", helper.desc).size());
            var inject = mixin.methods.stream().filter(m -> m.name.equals("injectTinkerSkins")).findFirst().orElseThrow();
            List<String> ids = Arrays.stream(inject.instructions.toArray()).filter(op -> op instanceof LdcInsnNode ldc && ldc.cst instanceof String).map(op -> (String) ((LdcInsnNode) op).cst).toList();
            assertEquals(11, ids.size());
            assertTrue(ids.containsAll(List.of("constructs_casting:battlestaff", "constructs_casting:flamberge")));
            assertEquals(1, calls(helper, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;").size());
        }
        var skinHook = compiled("mixin/epicfight/OptionalTinkerItemSkinsMixin");
        var mixinAnnotation = skinHook.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
        assertEquals(1100, mixinAnnotation.values.get(mixinAnnotation.values.indexOf("priority") + 1));
        var injector = skinHook.methods.stream().filter(m -> m.name.equals("bettercompatfixes$registeredOptionalSkin")).findFirst().orElseThrow();
        var injection = injector.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Inject;")).findFirst().orElseThrow();
        assertEquals(List.of("addTinkerWeapon(Ljava/util/Map;Ljava/lang/String;DDDDDDI)V"), injection.values.get(injection.values.indexOf("method") + 1));
        assertEquals(true, injection.values.get(injection.values.indexOf("cancellable") + 1));
        assertEquals(1, injection.values.get(injection.values.indexOf("require") + 1));
        assertEquals(1, injection.values.get(injection.values.indexOf("allow") + 1));
        // The real Mixin priority predicate allows 1100 to inject into a method merged at 1000.
        var point = new org.spongepowered.asm.mixin.injection.InjectionPoint() {
            public boolean find(String descriptor, InsnList instructions, Collection<AbstractInsnNode> nodes) { return false; }
        };
        assertTrue(point.checkPriority(1000, 1100));
        assertFalse(point.checkPriority(1000, 1000));
    }

    @Test void allNewHooksAreClientOnlyAndVersionGated() throws Exception {
        var config = JsonParser.parseString(Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();
        for (String name : List.of("iceandfire.TabulaModelHandlerHelperMixin", "distanthorizons.AbstractDhTintGetterMixin", "distanthorizons.TintGetterOverrideMixin", "epicfight.OptionalTinkerItemSkinsMixin")) {
            assertTrue(config.getAsJsonArray("client").asList().stream().anyMatch(v -> v.getAsString().equals(name)));
            assertFalse(config.getAsJsonArray("mixins").asList().stream().anyMatch(v -> v.getAsString().equals(name)));
        }
        String plugin = Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/compat/plugin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("hasVersion(mods, \"iceandfire\", \"2.1.13-1.20.1\")"));
        assertTrue(plugin.contains("hasVersion(mods, \"distanthorizons\", \"2.4.5-b\")"));
        assertTrue(plugin.contains("hasVersion(mods, \"epicfighttinkercompat\", \"20.2.8\")"));
    }

    private static Path pin(String property, String hash) throws Exception {
        String value = System.getProperty(property); assertNotNull(value, property);
        Path jar = Path.of(value);
        assertEquals(hash, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
        return jar;
    }
    private static ClassNode read(ZipFile zip, String name) throws Exception {
        var entry = zip.getEntry(name + ".class"); assertNotNull(entry, name);
        ClassNode type = new ClassNode();
        try (var input = zip.getInputStream(entry)) { new ClassReader(input).accept(type, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES); }
        return type;
    }
    private static ClassNode compiled(String suffix) throws Exception {
        try (var input = ClientReloadNativeContractTest.class.getClassLoader().getResourceAsStream(
                "com/bettercontent/bettercompatfixes/" + suffix + ".class")) {
            assertNotNull(input); ClassNode type = new ClassNode();
            new ClassReader(input).accept(type, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES); return type;
        }
    }
    private static MethodNode method(ClassNode node, String name, String desc) {
        return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst().orElseThrow();
    }
    private static List<MethodInsnNode> calls(MethodNode method, String owner, String name, String desc) {
        return Arrays.stream(method.instructions.toArray()).filter(op -> op instanceof MethodInsnNode m && m.owner.equals(owner) && m.name.equals(name) && m.desc.equals(desc)).map(op -> (MethodInsnNode) op).toList();
    }
    public static void main(String[] args) throws Exception {
        var test = new ClientReloadNativeContractTest();
        test.eyesResolveAllThreeNativeModelsAndRetainHeadGeometry();
        test.dhInheritedUpdateIsTheMissingParentSiteAndExplicitOverloadStillWins();
        test.optionalSkinHelperIsExactlyPinnedNonUniqueAndDelegatesBothOptionalDefaults();
        test.allNewHooksAreClientOnlyAndVersionGated();
    }
}
