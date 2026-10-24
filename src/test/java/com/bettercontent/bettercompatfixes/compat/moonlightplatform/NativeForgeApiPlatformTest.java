package com.bettercontent.bettercompatfixes.compat.moonlightplatform;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Pure exact raw/official SDK controls, not native acceptance or an all-module compatibility claim. */
public final class NativeForgeApiPlatformTest {
    private static Path sdk() { return Path.of(System.getProperty("bc.nativeforge.sdk.attempt")); }
    private static Map<String, NativeForgeApiPlatform.Provider> providers(boolean mapped) throws Exception {
        byte[] root = Files.readAllBytes(sdk().resolve(mapped ? "ffapi-native.jar" : "ffapi-production-resource-corrected.jar"));
        byte[] moon = Files.readAllBytes(sdk().resolve(mapped ? "moonlight-native.jar" : "moonlight-original.jar"));
        var entries = NativeForgeApiIdentity.entries(root);
        var result = new HashMap<String, NativeForgeApiPlatform.Provider>();
        for (String id : NativeForgeApiPlatform.INSTANCES.keySet()) {
            byte[] archive = id.equals("fabric_api") ? root : id.equals("moonlight") ? moon : entries.get(NativeForgeApiIdentity.MODULE_PATHS.get(id));
            result.put(id, new NativeForgeApiPlatform.Provider(id, NativeForgeApiPlatform.VERSIONS.get(id),
                    NativeForgeApiPlatform.INSTANCES.get(id), true, true, archive));
        }
        return result;
    }
    @Test public void exactRawAndOfficialSdkRolesBindCompleteRootAndModules() throws Exception {
        assertTrue(NativeForgeApiPlatform.knownNativePair(true, "47.4.22", "1.20.1", providers(false)));
        assertTrue(NativeForgeApiPlatform.knownNativePair(false, "47.4.13", "1.20.1", providers(true)));
        assertFalse(NativeForgeApiPlatform.knownNativePair(true, "47.4.13", "1.20.1", providers(true)));
        assertFalse(NativeForgeApiPlatform.knownNativePair(false, "47.4.22", "1.20.1", providers(true)));
        assertFalse(NativeForgeApiPlatform.knownNativePair(true, "47.4.22", "1.20.2", providers(false)));
    }
    @Test public void absentDuplicateExtraAndUnknownProviderKeepDiagnostic() throws Exception {
        var map = providers(false);
        map.remove("fabric_api"); assertFalse(NativeForgeApiPlatform.knownNativePair(true, "47.4.22", "1.20.1", map));
        map = providers(false); map.put("extra", map.get("fabric_api"));
        assertFalse(NativeForgeApiPlatform.knownNativePair(true, "47.4.22", "1.20.1", map));
        map = providers(false); var original = map.get("fabric_api");
        byte[] changed = original.archive().clone(); changed[changed.length - 1] ^= 1;
        map.put("fabric_api", new NativeForgeApiPlatform.Provider(original.id(), original.version(), original.instanceClass(), true, true, changed));
        assertFalse(NativeForgeApiPlatform.knownNativePair(true, "47.4.22", "1.20.1", map));
    }
    @Test public void metadataInstanceOwnerAndEveryNestedModuleMustMatch() throws Exception {
        for (String id : NativeForgeApiPlatform.INSTANCES.keySet()) {
            var original = providers(false).get(id);
            var wrong = java.util.List.of(
                    new NativeForgeApiPlatform.Provider("other", original.version(), original.instanceClass(), true, true, original.archive()),
                    new NativeForgeApiPlatform.Provider(id, "unknown", original.instanceClass(), true, true, original.archive()),
                    new NativeForgeApiPlatform.Provider(id, original.version(), "fabric.loader.ForeignProvider", true, true, original.archive()),
                    new NativeForgeApiPlatform.Provider(id, original.version(), original.instanceClass(), false, true, original.archive()),
                    new NativeForgeApiPlatform.Provider(id, original.version(), original.instanceClass(), true, false, original.archive()),
                    new NativeForgeApiPlatform.Provider(id, original.version(), original.instanceClass(), true, true, null),
                    new NativeForgeApiPlatform.Provider(id, original.version(), original.instanceClass(), true, true, new byte[] {1}));
            for (var drift : wrong) {
                var map = providers(false); map.put(id, drift);
                assertFalse(NativeForgeApiPlatform.knownNativePair(true, "47.4.22", "1.20.1", map), id);
            }
        }
    }
    private static ClassNode node(byte[] bytes) { var node = new ClassNode(); new ClassReader(bytes).accept(node, 0); return node; }
    private static byte[] bytes(ClassNode node) { var out = new ClassWriter(0); node.accept(out); return out.toByteArray(); }
    @Test public void exactMoonlightCallsiteAndEntireOriginalBodyPreservedByPreApplyCheck() throws Exception {
        for (boolean mapped : new boolean[] {false, true}) {
            byte[] archive = providers(mapped).get("moonlight").archive();
            byte[] input = NativeForgeApiIdentity.entries(archive).get(NativeForgeApiIdentity.MOONLIGHT_CLASS);
            ClassNode node = node(input); byte[] original = bytes(node);
            MoonlightPlatformPlugin.verifySite(input, node);
            assertArrayEquals(original, bytes(node));
            ClassNode expanded = new ClassNode(); new ClassReader(input).accept(expanded, ClassReader.EXPAND_FRAMES);
            MoonlightPlatformPlugin.verifySite(input, expanded);
            var setup = node.methods.stream().filter(m -> m.name.equals("lambda$new$1")).findFirst().orElseThrow();
            setup.instructions.insert(new InsnNode(Opcodes.NOP));
            assertThrows(IllegalStateException.class, () -> MoonlightPlatformPlugin.verifySite(input, node));
        }
    }
    @Test public void originalPresenceQueryFirstAndExactlyOneNarrowRedirect() throws Exception {
        byte[] platform;
        try (var in = getClass().getClassLoader().getResourceAsStream("com/bettercontent/bettercompatfixes/compat/moonlightplatform/NativeForgeApiPlatform.class")) { platform = in.readAllBytes(); }
        var method = node(platform).methods.stream().filter(m -> m.name.equals("hasUnsupportedFabricApiProvider")).findFirst().orElseThrow();
        var calls = Arrays.stream(method.instructions.toArray()).filter(i -> i instanceof MethodInsnNode).map(i -> (MethodInsnNode) i).toList();
        assertEquals("net/minecraftforge/fml/ModList", calls.get(0).owner);
        assertEquals("isLoaded", calls.get(0).name);
        assertEquals(1, calls.stream().filter(c -> c.owner.equals("net/minecraftforge/fml/ModList") && c.name.equals("isLoaded")).count());
        String source = Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/mixin/moonlightplatform/MoonlightForgePlatformMixin.java"));
        assertTrue(source.contains("method = \"lambda$new$1()V\""));
        assertTrue(source.contains("require = 1, allow = 1, remap = false"));
        assertTrue(source.contains("Lnet/minecraftforge/fml/ModList;isLoaded(Ljava/lang/String;)Z"));
        String adapter = Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/compat/moonlightplatform/NativeForgeApiPlatform.java"));
        assertFalse(adapter.contains("Class.forName")); assertFalse(adapter.contains("getDeclared"));
        assertFalse(adapter.contains("getDisplayName")); assertFalse(adapter.contains("catch (Throwable"));
        String config = Files.readString(Path.of("src/main/resources/better_compat_fixes.moonlight_platform.mixins.json"));
        assertTrue(config.contains("\"required\": true"));
        assertTrue(config.contains("\"defaultRequire\": 1"));
        assertTrue(adapter.contains("instanceModule.getName().equals(owner.moduleName())"));
        assertTrue(config.contains("compat.moonlightplatform.MoonlightPlatformPlugin"));
    }
    @Test public void unreadableArchivesCannotBecomeKnownNative() throws Exception {
        assertThrows(java.io.IOException.class, () -> NativeForgeApiIdentity.read(sdk().resolve("does-not-exist.jar")));
        assertNull(NativeForgeApiIdentity.pair(true, new byte[] {1}, new byte[] {2}));
    }
    public static void main(String[] args) throws Exception {
        var t = new NativeForgeApiPlatformTest();
        t.exactRawAndOfficialSdkRolesBindCompleteRootAndModules();
        t.absentDuplicateExtraAndUnknownProviderKeepDiagnostic();
        t.metadataInstanceOwnerAndEveryNestedModuleMustMatch();
        t.exactMoonlightCallsiteAndEntireOriginalBodyPreservedByPreApplyCheck();
        t.originalPresenceQueryFirstAndExactlyOneNarrowRedirect();
        t.unreadableArchivesCannotBecomeKnownNative();
        System.out.println("PASS: 6 exact raw/SDK platform controls; native provider/bridge acceptance pending");
    }
}
