package com.bettercontent.bettercompatfixes.compat.vibration;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

class VibrationInterfaceTransformTest implements Opcodes {
    private ClassNode resource(String name) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, "Actual compiled ABI required, not a synthetic target");
            ClassNode node = new ClassNode(); new ClassReader(input).accept(node, 0); return node;
        }
    }
    private ClassNode vanilla() throws IOException { return resource(VibrationInterfaceTransform.TARGET); }
    private List<MethodInsnNode> calls(MethodNode method) {
        var result = new ArrayList<MethodInsnNode>();
        for (var insn : method.instructions) if (insn instanceof MethodInsnNode call) result.add(call);
        return result;
    }
    private MethodNode named(ClassNode node, String name) {
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }
    private byte[] bytes(ClassNode node) { ClassWriter writer = new ClassWriter(0); node.accept(writer); return writer.toByteArray(); }
    @Test void actualNamedVanillaReceivesOnlyThreeAfterCallTranslationsNoReplacementBodies() throws Exception {
        ClassNode node = vanilla(); var originals = new LinkedHashMap<MethodNode, List<AbstractInsnNode>>();
        for (MethodNode method : node.methods) originals.put(method, List.of(method.instructions.toArray()));
        int methods = node.methods.size(); VibrationInterfaceTransform.apply(node);
        assertEquals(methods, node.methods.size()); int bridgeCount=0;
        for (var entry : originals.entrySet()) {
            // Every actual original instruction object remains, in original order.
            assertEquals(entry.getValue(), Arrays.stream(entry.getKey().instructions.toArray()).filter(entry.getValue()::contains).toList());
            int extra = entry.getKey().instructions.size() - entry.getValue().size();
            assertEquals(switch(entry.getKey().name) { case "receiveVibration" -> 4; case "tryReloadVibrationParticle" -> 2; default -> 0; }, extra);
            for (var bridge:calls(entry.getKey())) if (bridge.owner.equals(VibrationInterfaceTransform.BRIDGE)) {
                bridgeCount++; assertEquals(INVOKESTATIC,bridge.getOpcode()); assertFalse(bridge.itf);
                assertEquals(0,((VarInsnNode)bridge.getPrevious()).var);
                assertTrue(entry.getValue().contains(bridge.getPrevious().getPrevious()));
            }
            new Analyzer<>(new BasicVerifier()).analyze(node.name,entry.getKey());
        }
        assertEquals(3,bridgeCount);
        assertEquals(1,calls(named(node,"receiveVibration")).stream().filter(c->c.name.equals("vectorToWorld")).count());
        assertEquals(1,calls(named(node,"receiveVibration")).stream().filter(c->c.name.equals("sourceToWorld")).count());
        assertEquals(1,calls(named(node,"tryReloadVibrationParticle")).stream().filter(c->c.name.equals("sourceToWorld")).count());
        assertThrows(IllegalStateException.class,()->VibrationInterfaceTransform.apply(node));
    }
    @Test void compiledBridgeKeepsUpstreamPublicConversionOptionalAndFlooringAbi() throws Exception {
        ClassNode bridge=resource(VibrationInterfaceTransform.BRIDGE);assertTrue(bridge.fields.isEmpty(),"No cached ship transform");
        int conversions=0;
        for(MethodNode method:bridge.methods) {
            for(var call:calls(method)) if(call.owner.equals("org/valkyrienskies/mod/common/VSGameUtilsKt")) {
                conversions++;assertEquals("toWorldCoordinates",call.name);assertEquals(INVOKESTATIC,call.getOpcode());
                assertEquals("(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",call.desc);
            }
            new Analyzer<>(new BasicVerifier()).analyze(bridge.name,method);
        }
        assertEquals(2,conversions);var source=named(bridge,"sourceToWorld");
        for(String name:List.of("getPosition","isPresent","get","containing","<init>"))
            assertEquals(1,calls(source).stream().filter(c->c.name.equals(name)).count(),name);
        // Empty Optional returns the original source by identity, not null or a synthetic source.
        var returns=Arrays.stream(source.instructions.toArray()).filter(i->i.getOpcode()==ARETURN).toList();
        assertEquals(2,returns.size());assertEquals(0,((VarInsnNode)returns.get(1).getPrevious()).var);
    }
    @Test void exactSrgProfileAlsoTranslatesAndMixedNamespaceFailsClosed() throws Exception {
        ClassNode node=vanilla(); Map<String,String> names=Map.of("receiveVibration","m_280174_","tryReloadVibrationParticle","m_280404_",
            "pos","f_243906_","getPositionSource","m_280010_","getPosition","m_142502_","containing","m_274446_");
        for(MethodNode method:node.methods) { method.name=names.getOrDefault(method.name,method.name);
            for(var call:calls(method)) call.name=names.getOrDefault(call.name,call.name); }
        VibrationInterfaceTransform.apply(node);
        assertEquals(1,calls(named(node,"m_280404_")).stream().filter(c->c.name.equals("sourceToWorld")).count());
        ClassNode mixed=vanilla(); named(mixed,"receiveVibration").name="m_280174_";
        byte[] before=bytes(mixed); assertThrows(IllegalStateException.class,()->VibrationInterfaceTransform.apply(mixed));
        assertArrayEquals(before,bytes(mixed));
    }
    @Test void missingDuplicateDescriptorOpcodeInterfaceBitAndVersionDriftAreRejectedBeforeAnyMutation() throws Exception {
        List<Consumer<ClassNode>> mutants=List.of(
            n->n.name="other/Target", n->n.version=V16, n->n.access &= ~ACC_INTERFACE,
            n->named(n,"receiveVibration").access=ACC_PUBLIC|ACC_STATIC,
            n->named(n,"receiveVibration").desc="()Z",
            n->{var m=named(n,"receiveVibration");var c=calls(m).stream().filter(x->x.name.equals("pos")).findFirst().orElseThrow();m.instructions.remove(c);},
            n->{var m=named(n,"receiveVibration");var c=calls(m).stream().filter(x->x.name.equals("pos")).findFirst().orElseThrow();m.instructions.add(new MethodInsnNode(c.getOpcode(),c.owner,c.name,c.desc,c.itf));},
            n->calls(named(n,"receiveVibration")).stream().filter(c->c.name.equals("pos")).findFirst().orElseThrow().setOpcode(INVOKESTATIC),
            n->calls(named(n,"receiveVibration")).stream().filter(c->c.name.equals("getPositionSource")).findFirst().orElseThrow().itf=false
        );
        for(var mutate:mutants) { ClassNode node=vanilla();mutate.accept(node);byte[] before=bytes(node);
            assertThrows(IllegalStateException.class,()->VibrationInterfaceTransform.apply(node));assertArrayEquals(before,bytes(node)); }
    }
}
