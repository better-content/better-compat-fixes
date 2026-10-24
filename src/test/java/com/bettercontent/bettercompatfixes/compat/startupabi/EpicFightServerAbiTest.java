package com.bettercontent.bettercompatfixes.compat.startupabi;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Actual pinned resources and pure ASM controls. Does not load Epic/Minecraft or launch Forge. */
public final class EpicFightServerAbiTest {
    private static byte[] resource(String jar) throws Exception {
        try (ZipFile zip = new ZipFile(jar)) {
            return zip.getInputStream(zip.getEntry(EpicFightServerAbi.RESOURCE)).readAllBytes();
        }
    }
    private static List<byte[]> originals() throws Exception {
        String raw = System.getProperty("bc.markerRender.8049910");
        assertEquals("faa8eddad9d80adb9c4ba81fd0a8a21c81989d3d", hash(Files.readAllBytes(Path.of(raw)), "SHA-1"));
        return List.of(resource(raw), resource(System.getProperty("bc.startupabi.epic.mapped")));
    }
    private static ClassNode node(byte[] bytes) {
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0); return node;
    }
    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(0); node.accept(writer); return writer.toByteArray();
    }
    private static MethodNode selected(ClassNode node) {
        return node.methods.stream().filter(m -> m.name.equals(EpicFightServerAbi.METHOD)
                && m.desc.equals(EpicFightServerAbi.DESCRIPTOR)).findFirst().orElseThrow();
    }
    private static String hash(byte[] bytes, String algorithm) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
    }
    @Test public void exactRawAndOfficialMappedResourcesAreIndependentlyPinned() throws Exception {
        List<byte[]> inputs = originals();
        assertEquals("1bf1464d8d139f6998d5987e2934bae7737199b73231efc9dff622c2539d32d4", hash(inputs.get(0), "SHA-256"));
        assertEquals("b9e59862e62ee64675834fcfd0abf14d82a4b357d7ffd1931e2b523510925c80", hash(inputs.get(1), "SHA-256"));
        for (byte[] input : inputs) assertTrue(EpicFightServerAbi.shouldApply(true, input));
        assertFalse(EpicFightServerAbi.shouldApply(true, null));
        assertFalse(EpicFightServerAbi.shouldApply(false, new byte[] {1}));
    }
    @Test public void serverRemovesExactlyOneMemberAndPreservesEveryOtherBytecodeAttribute() throws Exception {
        for (byte[] input : originals()) {
            byte[] untouched = input.clone();
            ClassNode actual = node(input), expected = node(input);
            expected.methods.remove(selected(expected));
            assertEquals(1, EpicFightServerAbi.apply(true, EpicFightServerAbi.TARGET, EpicFightServerAbi.MIXIN, input, actual));
            assertEquals(14, actual.methods.size()); assertEquals(4, actual.fields.size());
            assertArrayEquals(bytes(expected), bytes(actual)); // all remaining Code/fields/annotations/Signatures/frames/locals
            assertArrayEquals(untouched, input); // source/resource bytes never mutated
            assertTrue(actual.methods.stream().anyMatch(m -> m.name.equals("getServerPlayerPatch")));
            assertTrue(actual.methods.stream().anyMatch(m -> m.name.equals("getLocalPlayerPatchAsOptional")
                    && m.desc.equals("(Lnet/minecraft/world/entity/Entity;)Ljava/util/Optional;") && m.signature.contains("LocalPlayerPatch")));
        }
    }
    @Test public void clientIsExactNoopEvenForUnrecognizedResource() throws Exception {
        for (byte[] input : originals()) {
            ClassNode actual = node(input); byte[] before = bytes(actual);
            assertEquals(0, EpicFightServerAbi.apply(false, "ignored", "ignored", new byte[] {1}, actual));
            assertArrayEquals(before, bytes(actual));
            assertNotNull(selected(actual));
        }
    }
    @Test public void unknownResourcesAndWrongTargetFailBeforeMutation() throws Exception {
        byte[] input = originals().get(0); ClassNode actual = node(input); byte[] before = bytes(actual);
        byte[] wrong = input.clone(); wrong[wrong.length - 1] ^= 1;
        assertThrows(IllegalStateException.class, () -> EpicFightServerAbi.shouldApply(true, wrong));
        assertThrows(IllegalStateException.class, () -> EpicFightServerAbi.apply(true, EpicFightServerAbi.TARGET, EpicFightServerAbi.MIXIN, null, actual));
        assertThrows(IllegalStateException.class, () -> EpicFightServerAbi.apply(true, "other", EpicFightServerAbi.MIXIN, input, actual));
        assertThrows(IllegalStateException.class, () -> EpicFightServerAbi.apply(true, EpicFightServerAbi.TARGET, "other", input, actual));
        assertThrows(IllegalStateException.class, () -> EpicFightServerAbi.apply(true, EpicFightServerAbi.TARGET, EpicFightServerAbi.MIXIN, wrong, actual));
        assertArrayEquals(before, bytes(actual));
    }
    @Test public void memberDriftDuplicateMissingFlagsBodyAndServerApiChangesFailClosed() throws Exception {
        List<Consumer<ClassNode>> mutations = List.of(
                n -> n.methods.remove(selected(n)),
                n -> n.methods.add(selected(n)),
                n -> selected(n).access = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                n -> selected(n).desc = "(Lnet/minecraft/world/entity/Entity;)Lyesman/epicfight/client/world/capabilites/entitypatch/player/LocalPlayerPatch;",
                n -> selected(n).instructions.insert(new InsnNode(Opcodes.NOP)),
                n -> selected(n).invisibleAnnotations = null,
                n -> n.methods.removeIf(m -> m.name.equals("getServerPlayerPatch")),
                n -> n.methods.stream().filter(m -> m.name.equals("getItemCapability")).findFirst().orElseThrow().signature = null,
                n -> n.fields.remove(0),
                n -> n.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "unexpected", "Lnet/minecraft/client/player/LocalPlayer;", null, null)),
                n -> n.name = "other/Class");
        for (byte[] input : originals()) for (Consumer<ClassNode> mutation : mutations) {
            ClassNode actual = node(input); mutation.accept(actual); byte[] before = bytes(actual);
            assertThrows(IllegalStateException.class, () -> EpicFightServerAbi.apply(true, EpicFightServerAbi.TARGET, EpicFightServerAbi.MIXIN, input, actual));
            assertArrayEquals(before, bytes(actual), "failed validation must not mutate even generated nodes");
        }
    }
    @Test public void repeatedApplicationFailsAndCannotRemoveAnyAdditionalMethod() throws Exception {
        byte[] input = originals().get(0); ClassNode actual = node(input);
        EpicFightServerAbi.apply(true, EpicFightServerAbi.TARGET, EpicFightServerAbi.MIXIN, input, actual);
        byte[] once = bytes(actual);
        assertThrows(IllegalStateException.class, () -> EpicFightServerAbi.apply(true, EpicFightServerAbi.TARGET, EpicFightServerAbi.MIXIN, input, actual));
        assertArrayEquals(once, bytes(actual));
    }
    @Test public void requiredServerOnlyConfigAndPluginAreOutsideReservedNamespace() throws Exception {
        var config = JsonParser.parseString(Files.readString(Path.of("src/main/resources/better_compat_fixes.startup_abi.mixins.json"))).getAsJsonObject();
        assertTrue(config.get("required").getAsBoolean());
        String prefix = config.get("package").getAsString();
        assertEquals("com.bettercontent.bettercompatfixes.mixin.startupabi", prefix);
        assertEquals(EpicFightServerAbi.MIXIN, prefix + "." + config.getAsJsonArray("server").get(0).getAsString());
        assertEquals(1, config.getAsJsonArray("server").size());
        assertEquals(0, config.getAsJsonArray("mixins").size()); assertEquals(0, config.getAsJsonArray("client").size());
        assertEquals("com.bettercontent.bettercompatfixes.compat.startupabi.EpicFightServerAbiPlugin", config.get("plugin").getAsString());
        assertFalse(config.get("plugin").getAsString().startsWith(prefix + "."));
        String build = Files.readString(Path.of("build.gradle.kts"));
        assertTrue(build.contains("chunk_io.mixins.json,better_compat_fixes.startup_abi.mixins.json"));
        assertTrue(build.contains("config(\"better_compat_fixes.startup_abi.mixins.json\")"));
        assertTrue(build.contains("dependsOn(verifyRuntimeStartupAbi)"));
        String plugin = Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/compat/startupabi/EpicFightServerAbiPlugin.java"));
        assertTrue(plugin.contains("FMLEnvironment.dist == Dist.DEDICATED_SERVER"));
        assertTrue(plugin.contains("getResourceAsStream(EpicFightServerAbi.RESOURCE)"));
        assertFalse(plugin.contains("Class.forName")); assertFalse(plugin.contains("getDeclared"));
        assertFalse(plugin.contains("catch (Throwable"));
        String preparation = plugin.substring(plugin.indexOf("public boolean shouldApplyMixin"), plugin.indexOf("public void preApply"));
        assertFalse(preparation.contains("resource()"), "preparation must not swallow resource-validation failures");
        assertFalse(preparation.contains("exactAnchor("));
    }
    @Test public void actualMixinLoaderMakesRequiredConfigDriftAnErrorNotAnOptionalWarning() throws Exception {
        // Inspect actual pinned Mixin control flow without initializing a launcher or loading targets.
        byte[] processor;
        try (var input = getClass().getClassLoader().getResourceAsStream("org/spongepowered/asm/mixin/transformer/MixinProcessor.class")) {
            processor = input.readAllBytes();
        }
        ClassNode processorNode = node(processor);
        MethodNode handler = processorNode.methods.stream().filter(m -> m.name.equals("handleMixinError")).findFirst().orElseThrow();
        var ops = Arrays.stream(handler.instructions.toArray()).filter(i -> i.getOpcode() >= 0).toList();
        int required = -1;
        for (int i = 0; i < ops.size(); i++) if (ops.get(i) instanceof MethodInsnNode call && call.name.equals("isRequired")) required = i;
        assertTrue(required >= 0);
        assertEquals(Opcodes.IFEQ, ops.get(required + 1).getOpcode());
        assertEquals("ERROR", ((FieldInsnNode) ops.get(required + 2)).name);
        assertTrue(ops.stream().anyMatch(i -> i instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW
                && type.desc.equals("org/spongepowered/asm/mixin/throwables/MixinApplyError")));
        // preApply is inside the applicator's Exception handler, which wraps plugin failures in
        // MixinApplicatorException (an InvalidMixinException), routed to the required ERROR above.
        try (var input = getClass().getClassLoader().getResourceAsStream("org/spongepowered/asm/mixin/transformer/MixinApplicatorStandard.class")) {
            ClassNode applicator = node(input.readAllBytes());
            MethodNode apply = applicator.methods.stream().filter(m -> m.name.equals("apply")).findFirst().orElseThrow();
            AbstractInsnNode preApply = Arrays.stream(apply.instructions.toArray()).filter(i -> i instanceof MethodInsnNode call
                    && call.owner.equals("org/spongepowered/asm/mixin/transformer/MixinTargetContext") && call.name.equals("preApply")).findFirst().orElseThrow();
            assertTrue(apply.tryCatchBlocks.stream().anyMatch(block -> "java/lang/Exception".equals(block.type)
                    && apply.instructions.indexOf(block.start) < apply.instructions.indexOf(preApply)
                    && apply.instructions.indexOf(preApply) < apply.instructions.indexOf(block.end)));
            assertTrue(Arrays.stream(apply.instructions.toArray()).anyMatch(i -> i instanceof TypeInsnNode type
                    && type.getOpcode() == Opcodes.NEW && type.desc.endsWith("/MixinApplicatorException")));
        }
        try (var input = getClass().getClassLoader().getResourceAsStream("org/spongepowered/asm/mixin/transformer/throwables/MixinApplicatorException.class")) {
            assertEquals("org/spongepowered/asm/mixin/transformer/throwables/InvalidMixinException", node(input.readAllBytes()).superName);
        }
        // Native application remains a separate gate.
    }
    public static void main(String[] args) throws Exception {
        var test = new EpicFightServerAbiTest();
        test.exactRawAndOfficialMappedResourcesAreIndependentlyPinned();
        test.serverRemovesExactlyOneMemberAndPreservesEveryOtherBytecodeAttribute();
        test.clientIsExactNoopEvenForUnrecognizedResource();
        test.unknownResourcesAndWrongTargetFailBeforeMutation();
        test.memberDriftDuplicateMissingFlagsBodyAndServerApiChangesFailClosed();
        test.repeatedApplicationFailsAndCannotRemoveAnyAdditionalMethod();
        test.requiredServerOnlyConfigAndPluginAreOutsideReservedNamespace();
        test.actualMixinLoaderMakesRequiredConfigDriftAnErrorNotAnOptionalWarning();
        System.out.println("PASS: 8 pure startup ABI controls, exact raw/mapped resources; actual native application pending");
    }
}
