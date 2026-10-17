package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/** Checks the exact native animation identity, restrictions and deserializer selector. */
final class ClimbCameraNativeContractTest {
    @Test
    void pinnedNativeClimbHasTheLimitsAndDeserializerTargetWeOverride() throws Exception {
        Path jar = Path.of(System.getProperty("bc.markerRender.8049910"));
        assertEquals("faa8eddad9d80adb9c4ba81fd0a8a21c81989d3d", HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar))));
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entry = zip.getEntry("assets/epicfight/animmodels/animations/biped/living/pov/climb.json");
            assertNotNull(entry);
            try (var reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                var pov = JsonParser.parseReader(reader).getAsJsonObject();
                assertEquals("world", pov.get("root").getAsString());
                var limits = pov.getAsJsonObject("limited_view_degrees");
                assertEquals("[-90.0,0.0]", limits.getAsJsonArray("xRot").toString());
                assertEquals("[-30.0,30.0]", limits.getAsJsonArray("yRot").toString());
            }
            String owner = "yesman/epicfight/api/client/animation/AnimationSubFileReader";
            ClassNode type = new ClassNode();
            try (var input = zip.getInputStream(zip.getEntry(owner + "$PovAnimationDeserializer.class"))) {
                new ClassReader(input).accept(type, 0);
            }
            assertEquals(1, type.methods.stream().filter(m -> m.name.equals("deserialize")
                    && m.desc.equals("(Lyesman/epicfight/api/animation/types/StaticAnimation;Lcom/google/gson/JsonElement;)L"
                            + owner + "$PovSettings;")).count());
        }
    }
}
