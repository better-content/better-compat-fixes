package com.bettercontent.bettercompatfixes.compat;

import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.junit.jupiter.api.Assertions.*;

final class DeepVoidEntranceTest {
    private ClassNode type(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/" + name + ".class")) {
            assertNotNull(input, name);
            var type = new ClassNode();
            new ClassReader(input).accept(type, 0);
            return type;
        }
    }
    @Test void rareSpacingAndStructureIdentityAreAuthoredResources() throws Exception {
        try (var input = getClass().getResourceAsStream("/data/better_compat_fixes/worldgen/structure_set/deep_void_entrances.json")) {
            assertNotNull(input);
            var root = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(1, root.getAsJsonArray("structures").size());
            assertEquals("better_compat_fixes:deep_void_entrance", root.getAsJsonArray("structures").get(0).getAsJsonObject().get("structure").getAsString());
            var placement = root.getAsJsonObject("placement");
            assertEquals(128, placement.get("spacing").getAsInt());
            assertEquals(64, placement.get("separation").getAsInt());
            assertEquals("minecraft:random_spread", placement.get("type").getAsString());
        }
    }
    @Test void compassSelectorMatchesTheActualNativeClientCall() throws Exception {
        var nativeType = type("net/minecraft/client/renderer/item/CompassItemPropertyFunction");
        var rotation = nativeType.methods.stream().filter(m -> m.name.equals("getCompassRotation")).findFirst().orElseThrow();
        int matches = 0;
        for (var instruction : rotation.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.endsWith("CompassItemPropertyFunction$CompassTarget")
                    && call.name.equals("getPos")) matches++;
        }
        assertEquals(1, matches);
        var manager = type("net/minecraft/world/level/StructureManager");
        assertTrue(manager.fields.stream().anyMatch(field -> field.name.equals("level")
                && field.desc.equals("Lnet/minecraft/world/level/LevelAccessor;")));
    }
    @Test void searchRequestsOnlyStructureMetadataAndDoesNotBlockOnChunkFutures() throws Exception {
        var service = type("com/bettercontent/bettercompatfixes/deepvoid/DeepVoidCompassSearch");
        int metadata = 0, requests = 0;
        for (var method : service.methods) for (var instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field && field.owner.equals("net/minecraft/world/level/chunk/ChunkStatus")) {
                assertEquals("STRUCTURE_STARTS", field.name, "Compass lookup must never request terrain/full chunks");
                metadata++;
            }
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("getChunkFuture")) requests++;
                assertFalse(call.owner.equals("java/util/concurrent/CompletableFuture")
                        && (call.name.equals("join") || call.name.equals("get")), "Do not block a server tick on chunk lookup");
            }
        }
        assertEquals(1, metadata);
        assertEquals(1, requests);
    }
    @Test void variantSelectionIsOneUniformNativeRandomDraw() throws Exception {
        var structure = type("com/bettercontent/bettercompatfixes/deepvoid/DeepVoidEntranceStructure");
        var generation = structure.methods.stream().filter(m -> m.name.equals("findGenerationPoint")).findFirst().orElseThrow();
        int draws = 0;
        for (var instruction : generation.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("nextInt") && call.desc.equals("(I)I")) draws++;
        }
        assertEquals(1, draws, "Equal template weights must use one bounded uniform draw");
    }
}
