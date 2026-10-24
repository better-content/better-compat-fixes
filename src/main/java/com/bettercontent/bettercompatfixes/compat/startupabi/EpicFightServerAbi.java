package com.bettercontent.bettercompatfixes.compat.startupabi;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/** Exact physical-server side correction. Inspects resources/ASM only; never loads Epic classes. */
public final class EpicFightServerAbi {
    public static final String TARGET = "yesman.epicfight.world.capabilities.EpicFightCapabilities";
    public static final String RESOURCE = TARGET.replace('.', '/') + ".class";
    public static final String MIXIN = "com.bettercontent.bettercompatfixes.mixin.startupabi.EpicFightCapabilitiesServerAbiMixin";
    public static final String METHOD = "getLocalPlayerPatch";
    public static final String DESCRIPTOR = "(Lnet/minecraft/client/player/LocalPlayer;)Lyesman/epicfight/client/world/capabilites/entitypatch/player/LocalPlayerPatch;";
    // The exact raw pack and independently inspected official-mapped SDK class, Epic Fight 20.14.17.
    private static final Set<String> CLASS_HASHES = Set.of(
            "1bf1464d8d139f6998d5987e2934bae7737199b73231efc9dff622c2539d32d4",
            "b9e59862e62ee64675834fcfd0abf14d82a4b357d7ffd1931e2b523510925c80");

    private EpicFightServerAbi() {}

    public static boolean shouldApply(boolean dedicatedServer, byte[] original) {
        if (!dedicatedServer || original == null) return false;
        expected(original);
        return true;
    }

    /** Returns exactly one on the server, zero/no mutation on clients. All checks precede mutation. */
    public static int apply(boolean dedicatedServer, String target, String mixin, byte[] original, ClassNode node) {
        if (!dedicatedServer) return 0;
        require(TARGET.equals(target) && MIXIN.equals(mixin), "unexpected anchor/target");
        ClassNode expected = expected(original);
        require(node != null && expected.name.equals(node.name), "unexpected target class node");
        // Match every original member including Code, annotations, generics, flags, frames and locals.
        // This also rejects partial/repeated application or another transformer changing this ABI.
        require(memberFingerprints(expected).equals(memberFingerprints(node)), "target member ABI drift");
        MethodNode removed = selected(node);
        require(node.methods.remove(removed), "missing selected member");
        return 1;
    }

    private static ClassNode expected(byte[] original) {
        require(original != null && CLASS_HASHES.contains(hash(original)), "unsupported Epic Fight 20.14.17 class resource");
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        require(node.name.equals(TARGET.replace('.', '/')) && node.methods.size() == 15 && node.fields.size() == 4,
                "unexpected pinned class shape");
        selected(node);
        for (MethodNode method : node.methods) {
            require(method.name.equals(METHOD) && method.desc.equals(DESCRIPTOR) || !clientDescriptor(method.desc),
                    "additional client method descriptor");
        }
        for (FieldNode field : node.fields) require(!clientDescriptor(field.desc), "client field descriptor");
        return node;
    }

    private static MethodNode selected(ClassNode node) {
        List<MethodNode> matches = node.methods.stream()
                .filter(method -> method.name.equals(METHOD) && method.desc.equals(DESCRIPTOR)).toList();
        require(matches.size() == 1, "selected member count differs from one");
        MethodNode method = matches.get(0);
        require(method.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC), "selected member flags changed");
        return method;
    }

    private static boolean clientDescriptor(String descriptor) {
        return descriptor.contains("Lnet/minecraft/client/") || descriptor.contains("Lyesman/epicfight/client/");
    }

    /** Canonical member serialization does not resolve any type, compute frames or load a class. */
    private static List<String> memberFingerprints(ClassNode node) {
        var result = new java.util.ArrayList<String>();
        for (FieldNode field : node.fields) {
            ClassWriter writer = container();
            field.accept(writer);
            writer.visitEnd();
            result.add("field:" + hash(writer.toByteArray()));
        }
        for (MethodNode method : node.methods) {
            ClassWriter writer = container();
            method.accept(writer);
            writer.visitEnd();
            result.add("method:" + hash(writer.toByteArray()));
        }
        return result;
    }

    private static ClassWriter container() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "bc/StartupAbiMemberWitness", null, "java/lang/Object", null);
        return writer;
    }

    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static void require(boolean valid, String reason) {
        if (!valid) throw new IllegalStateException("Epic Fight dedicated-server ABI correction: " + reason);
    }
}
