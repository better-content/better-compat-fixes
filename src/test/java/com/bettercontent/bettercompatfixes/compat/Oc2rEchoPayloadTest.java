package com.bettercontent.bettercompatfixes.compat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import static org.junit.jupiter.api.Assertions.*;

final class Oc2rEchoPayloadTest {
    @Test void exclusivelyOwnedCopySurvivesCallerReuseWithoutRewinding() {
        for (int kind = 0; kind < 4; kind++) {
            ByteBuffer storage = kind == 1 ? ByteBuffer.allocateDirect(12) : ByteBuffer.allocate(12);
            storage.position(2).put(new byte[]{3, 1, 4, 1}).limit(6).position(2);
            ByteBuffer caller = kind == 2 ? storage.slice() : kind == 3 ? storage.asReadOnlyBuffer() : storage;
            int limit = caller.limit();
            byte[] nativeCopy = new byte[caller.remaining()];
            caller.get(nativeCopy); // Exact pinned native operation BEFORE the injection.
            ByteBuffer capture = Oc2rEchoPayload.captured(nativeCopy);
            assertEquals(limit, caller.position());
            assertEquals(limit, caller.limit());
            storage.clear(); while (storage.hasRemaining()) storage.put((byte) 99);
            byte[] result = new byte[capture.remaining()]; capture.get(result);
            assertArrayEquals(new byte[]{3, 1, 4, 1}, result);
            assertThrows(ReadOnlyBufferException.class, () -> capture.put(0, (byte) 0));
        }
        assertEquals(0, Oc2rEchoPayload.captured(new byte[0]).remaining());
    }

    @Test void compiledHookRetainsExactAfterConsumptionSiteAndSingleMatchConstraints() throws Exception {
        ClassNode node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/com/bettercontent/bettercompatfixes/mixin/oc2r/EchoPayloadMixin.class")) {
            assertNotNull(input); new ClassReader(input).accept(node, 0);
        }
        var handlers = node.methods.stream().filter(m -> m.name.equals("betterContent$ownEchoPayload")).toList();
        assertEquals(1, handlers.size());
        var handler = handlers.get(0);
        var annotation = handler.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/ModifyVariable;")).findFirst().orElseThrow();
        for (String key : new String[]{"require", "expect", "allow"})
            assertEquals(1, annotation.values.get(annotation.values.indexOf(key) + 1));
        assertEquals(true, annotation.values.get(annotation.values.indexOf("argsOnly") + 1));
        assertEquals(0, annotation.values.get(annotation.values.indexOf("ordinal") + 1));
        assertEquals(false, annotation.values.get(annotation.values.indexOf("remap") + 1));
        var at = (org.objectweb.asm.tree.AnnotationNode) annotation.values.get(annotation.values.indexOf("at") + 1);
        assertEquals("FIELD", at.values.get(at.values.indexOf("value") + 1));
        assertEquals("Lli/cil/oc2/common/Main;LoadedLibrary:Z", at.values.get(at.values.indexOf("target") + 1));
        assertEquals(org.objectweb.asm.Opcodes.GETSTATIC, at.values.get(at.values.indexOf("opcode") + 1));
    }
}
