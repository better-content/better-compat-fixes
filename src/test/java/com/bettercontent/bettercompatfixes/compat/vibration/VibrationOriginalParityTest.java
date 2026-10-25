package com.bettercontent.bettercompatfixes.compat.vibration;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Required actual-upstream bytecode comparison; not a simulated provider API or native receipt. */
class VibrationOriginalParityTest implements Opcodes {
    private ClassNode resource(String path) throws Exception {
        try(var input=getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input,"Mandatory original/compiled ABI resource");byte[] raw=input.readAllBytes();
            if(path.startsWith("vibration/")) assertEquals("399ca37359fbbf851eb54f5e3524c59a25751998cae0c9f534bb68a45f56659f",
                java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(raw)));
            ClassNode node=new ClassNode();new ClassReader(raw).accept(node,0);return node;
        }
    }
    private MethodNode method(ClassNode node,String name) {return node.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    private List<String> trace(List<AbstractInsnNode> nodes,boolean upstreamLocals) {
        var out=new ArrayList<String>();Map<String,String> mapped=Map.of("m_142502_","getPosition","m_274446_","containing");
        for(var insn:nodes) {
            if(insn.getOpcode()<0)continue;
            String item=""+insn.getOpcode();
            if(insn instanceof VarInsnNode v) item+=":"+(upstreamLocals?switch(v.var){case 3->0;case 2->1;case 4->2;default->throw new AssertionError("Unknown upstream local");}:v.var);
            else if(insn instanceof MethodInsnNode c) item+=":"+c.owner+":"+mapped.getOrDefault(c.name,c.name)+":"+c.desc+":"+c.itf;
            else if(insn instanceof TypeInsnNode t)item+=":"+t.desc;
            else if(insn instanceof JumpInsnNode j) {
                AbstractInsnNode target=j.label;while(target.getOpcode()<0)target=target.getNext();
                assertInstanceOf(VarInsnNode.class,target);assertEquals(ALOAD,target.getOpcode());
                assertEquals(upstreamLocals?3:0,((VarInsnNode)target).var,"Absent Optional must branch to original source identity return");
            } else assertInstanceOf(InsnNode.class,insn,"Unexpected ABI instruction");
            out.add(item);
        }
        return out;
    }
    @Test void sourceConversionBodyIsTheOriginalWrapperAfterItsOriginalOperationReturns() throws Exception {
        ClassNode raw=resource("vibration/upstream-original.class");
        ClassNode bridge=resource(VibrationInterfaceTransform.BRIDGE+".class");
        MethodNode original=method(raw,"destSourcePos");assertEquals(ACC_PRIVATE|ACC_STATIC,original.access);
        var all=List.of(original.instructions.toArray());int store=-1;
        for(int i=0;i<all.size();i++)if(all.get(i) instanceof VarInsnNode v && v.getOpcode()==ASTORE && v.var==3){assertEquals(-1,store);store=i;}
        assertTrue(store>0,"Original operation result must have one source local");
        assertEquals(trace(all.subList(store+1,all.size()),true),trace(List.of(method(bridge,"sourceToWorld").instructions.toArray()),false));
    }
    @Test void originalVectorAndReloadWrappersRetainExactlyTheirOperationThenSameConversion() throws Exception {
        ClassNode raw=resource("vibration/upstream-original.class");ClassNode bridge=resource(VibrationInterfaceTransform.BRIDGE+".class");
        List<AbstractInsnNode> vector=Arrays.stream(method(bridge,"vectorToWorld").instructions.toArray()).filter(i->i.getOpcode()>=0).toList();
        assertEquals(4,vector.size());assertEquals(1,((VarInsnNode)vector.get(0)).var);assertEquals(0,((VarInsnNode)vector.get(1)).var);
        var conversion=(MethodInsnNode)vector.get(2);assertEquals("toWorldCoordinates",conversion.name);assertEquals(ARETURN,vector.get(3).getOpcode());
        var originalConversion=Arrays.stream(method(raw,"destWorldPos").instructions.toArray()).filter(i->i instanceof MethodInsnNode c && c.name.equals("toWorldCoordinates")).map(i->(MethodInsnNode)i).toList();
        assertEquals(1,originalConversion.size());assertEquals(originalConversion.get(0).owner,conversion.owner);
        assertEquals(originalConversion.get(0).desc,conversion.desc);assertEquals(originalConversion.get(0).getOpcode(),conversion.getOpcode());
        var reload=Arrays.stream(method(raw,"destReloadSourcePos").instructions.toArray()).filter(i->i.getOpcode()>=0).toList();
        assertEquals(List.of(ALOAD,ALOAD,ALOAD,INVOKESTATIC,ARETURN),reload.stream().map(AbstractInsnNode::getOpcode).toList());
        for(int i=0;i<3;i++)assertEquals(i,((VarInsnNode)reload.get(i)).var);
        assertEquals("destSourcePos",((MethodInsnNode)reload.get(3)).name);
        assertEquals(raw.name,((MethodInsnNode)reload.get(3)).owner);
    }
}
