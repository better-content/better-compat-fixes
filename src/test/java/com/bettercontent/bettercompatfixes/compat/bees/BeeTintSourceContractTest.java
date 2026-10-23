package com.bettercontent.bettercompatfixes.compat.bees;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Static/source/pixel controls only, not engine creation, transformed callbacks, or actual frames. */
class BeeTintSourceContractTest {
    private ClassNode code(String name) throws Exception {
        try (var in = getClass().getResourceAsStream("/com/bettercontent/bettercompatfixes/" + name + ".class")) {
            assertNotNull(in); var node = new ClassNode(); new ClassReader(in).accept(node, 0); return node;
        }
    }
    private List<MethodInsnNode> calls(MethodNode method) {
        var out = new ArrayList<MethodInsnNode>();
        for (var instruction : method.instructions) if (instruction instanceof MethodInsnNode call) out.add(call);
        return out;
    }
    @Test void exactEightByEightMaskPartitionPreservesAllAuthoredDetails() throws Exception {
        String base = "/assets/better_compat_fixes/textures/particle/colony_bee";
        var original = ImageIO.read(getClass().getResource(base + ".png"));
        var body = ImageIO.read(getClass().getResource(base + "_body.png"));
        var detail = ImageIO.read(getClass().getResource(base + "_detail.png"));
        assertEquals(8, original.getWidth()); assertEquals(8, original.getHeight());
        assertEquals(8, body.getWidth()); assertEquals(8, body.getHeight()); assertEquals(8, detail.getWidth()); assertEquals(8, detail.getHeight());
        int bodies = 0, marks = 0, wings = 0, transparent = 0;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            int pixel = original.getRGB(x, y);
            if (pixel == 0xfff8b423) { bodies++; assertEquals(0xffffffff, body.getRGB(x, y)); assertEquals(0, detail.getRGB(x, y)); }
            else {
                assertEquals(0, body.getRGB(x, y)); assertEquals(pixel, detail.getRGB(x, y));
                if (pixel == 0xff2d1f10) marks++; else if (pixel == 0xbedcf0ff) wings++; else { assertEquals(0, pixel); transparent++; }
            }
        }
        assertEquals(4, bodies); assertEquals(11, marks); assertEquals(9, wings); assertEquals(40, transparent);
    }
    @Test void decodedRouteCannotDecodeOrQueryNewInventoryWorldOrNestTint() throws Exception {
        var node = code("compat/bees/NativeBeeColors");
        int colors = 0, genes = 0;
        for (var method : node.methods) for (var call : calls(method)) {
            assertFalse(List.of("getChromosome", "getSpecies", "getGene", "getNestColor", "getOrCreateTag", "getStackInSlot", "getBlockState", "getBiome", "nextInt", "nextFloat", "nextDouble").contains(call.name), call.name);
            if (call.name.equals("getColor")) colors++;
            if (call.name.equals("getGenes")) genes++;
        }
        assertEquals(1, colors); assertEquals(1, genes);
    }
    @Test void originalOperationHasOneForwardAndFinallyAndNoExtraNativeQueries() throws Exception {
        var node = code("mixin/complicatedbees/ColonyActivityMixin");
        var method = node.methods.stream().filter(m -> m.name.equals("betterCompat$observeExpressedEffect")).findFirst().orElseThrow();
        assertEquals(1, calls(method).stream().filter(c -> c.owner.equals("com/llamalad7/mixinextras/injector/wrapoperation/Operation") && c.name.equals("call")).count());
        assertTrue(method.tryCatchBlocks.stream().anyMatch(c -> c.type == null));
        assertTrue(calls(method).stream().anyMatch(c -> c.name.equals("open")));
        assertTrue(calls(method).stream().anyMatch(c -> c.name.equals("close")));
        assertFalse(calls(method).stream().anyMatch(c -> List.of("getStackInSlot", "getGeneValue", "getChromosome", "getOrCreateTag", "getSpecies", "sendBlockUpdated").contains(c.name)));
        var invalidation = code("mixin/complicatedbees/QueenColorInvalidationMixin");
        assertFalse(invalidation.methods.stream().flatMap(m -> calls(m).stream()).anyMatch(c -> List.of("getStackInSlot", "getBeeItems", "getLogic", "getOrCreateTag").contains(c.name)));
    }
    @Test void renderingKeepsLegacyAndDrawsExactlyTwoQuadsInsideOneTypedParticle() throws Exception {
        var node = code("client/bees/ColonyBeeParticle");
        var render = node.methods.stream().filter(m -> m.name.equals("render")).findFirst().orElseThrow();
        // One legacy branch + two typed calls, all on the SAME particle instance.
        assertEquals(3, calls(render).stream().filter(c -> c.name.equals("render")).count());
        assertFalse(calls(render).stream().anyMatch(c -> c.name.equals("addParticle") || c.owner.startsWith("org/slf4j")));
        var types = code("compat/bees/SwarmParticles");
        var constants = new ArrayList<Object>();
        for (var method : types.methods) for (var instruction : method.instructions) if (instruction instanceof LdcInsnNode value) constants.add(value.cst);
        assertTrue(constants.contains("colony_bee")); assertTrue(constants.contains("colony_bee_tinted_v1"));
        assertEquals(1, types.methods.stream().flatMap(m -> calls(m).stream()).filter(c -> c.owner.equals("net/minecraft/core/particles/SimpleParticleType") && c.name.equals("<init>")).count());
    }
}
