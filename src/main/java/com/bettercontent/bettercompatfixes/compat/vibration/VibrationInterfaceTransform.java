package com.bettercontent.bettercompatfixes.compat.vibration;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Exact post-apply translation of the three pinned VS 2.4.11 wrappers, without interface injectors. */
public final class VibrationInterfaceTransform implements Opcodes {
    public static final String TARGET = "net/minecraft/world/level/gameevent/vibrations/VibrationSystem$Ticker";
    public static final String BRIDGE = "com/bettercontent/bettercompatfixes/compat/vibration/VibrationWorldCoordinates";
    private static final String ROOT = "net/minecraft/";
    private static final String LEVEL = ROOT + "server/level/ServerLevel";
    private static final String WORLD = ROOT + "world/level/Level";
    private static final String DATA = TARGET.replace("$Ticker", "$Data");
    private static final String USER = TARGET.replace("$Ticker", "$User");
    private static final String INFO = ROOT + "world/level/gameevent/vibrations/VibrationInfo";
    private static final String SOURCE = ROOT + "world/level/gameevent/PositionSource";
    private static final String POS = ROOT + "core/BlockPos";
    private static final String VEC = ROOT + "world/phys/Vec3";
    private static final String THREE = "(L" + LEVEL + ";L" + DATA + ";L" + USER + ";";

    private VibrationInterfaceTransform() {}
    private static void need(boolean condition, String reason) {
        if (!condition) throw new IllegalStateException("Pinned VS vibration transfer: " + reason);
    }
    private static MethodNode method(ClassNode node, String name, String desc) {
        List<MethodNode> found = node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).toList();
        need(found.size() == 1, "exact target method " + name);
        MethodNode result = found.get(0);
        need(result.access == (ACC_PRIVATE | ACC_STATIC), "target private static flags " + name);
        return result;
    }
    private static List<MethodInsnNode> calls(MethodNode method) {
        var result = new ArrayList<MethodInsnNode>();
        for (var insn : method.instructions) if (insn instanceof MethodInsnNode c) result.add(c);
        return result;
    }
    private static MethodInsnNode call(MethodNode method, String owner, String name, String desc, int opcode) {
        var found = calls(method).stream().filter(c -> c.owner.equals(owner) && c.name.equals(name) && c.desc.equals(desc)).toList();
        need(found.size() == 1 && found.get(0).getOpcode() == opcode &&
            found.get(0).itf == (opcode == INVOKEINTERFACE), "exact call " + method.name + " / " + owner + "." + name);
        return found.get(0);
    }
    public static void apply(ClassNode node) {
        need(node.name.equals(TARGET) && (node.access & ACC_INTERFACE) != 0 && node.version == V17,
            "Minecraft 1.20.1 Java-17 interface identity");
        need(node.methods.stream().flatMap(m -> calls(m).stream()).noneMatch(c -> c.owner.equals(BRIDGE)),
            "already transformed / bridge collision");
        boolean named = node.methods.stream().anyMatch(m -> m.name.equals("receiveVibration"));
        MethodNode receive = method(node, named ? "receiveVibration" : "m_280174_", THREE + "L" + INFO + ";)Z");
        MethodNode reload = method(node, named ? "tryReloadVibrationParticle" : "m_280404_", THREE + ")V");
        MethodInsnNode vector = call(receive, INFO, named ? "pos" : "f_243906_", "()L" + VEC + ";", INVOKEVIRTUAL);
        MethodInsnNode receiveSource = call(receive, USER, named ? "getPositionSource" : "m_280010_", "()L" + SOURCE + ";", INVOKEINTERFACE);
        MethodInsnNode reloadSource = call(reload, USER, named ? "getPositionSource" : "m_280010_", "()L" + SOURCE + ";", INVOKEINTERFACE);
        call(receive, SOURCE, named ? "getPosition" : "m_142502_", "(L" + WORLD + ";)Ljava/util/Optional;", INVOKEINTERFACE);
        call(reload, SOURCE, named ? "getPosition" : "m_142502_", "(L" + WORLD + ";)Ljava/util/Optional;", INVOKEINTERFACE);
        call(receive, POS, named ? "containing" : "m_274446_", "(L" + ROOT + "core/Position;)L" + POS + ";", INVOKESTATIC);
        // Validate everything before mutation. Each original operation still executes
        // once, then the same upstream conversion sees its result and ServerLevel local 0.
        append(receive, vector, "vectorToWorld", VEC);
        append(receive, receiveSource, "sourceToWorld", SOURCE);
        append(reload, reloadSource, "sourceToWorld", SOURCE);
        receive.maxStack++; reload.maxStack++;
    }
    private static void append(MethodNode method, MethodInsnNode original, String bridge, String result) {
        InsnList code = new InsnList(); code.add(new VarInsnNode(ALOAD, 0));
        code.add(new MethodInsnNode(INVOKESTATIC, BRIDGE, bridge, "(L" + result + ";L" + LEVEL + ";)L" + result + ";", false));
        method.instructions.insert(original, code);
    }
}
