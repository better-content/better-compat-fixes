package com.bettercontent.bettercontentfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bettercontent.bettercontentfixes.compat.FleshSpreadPositionKey;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class FleshSpreadPositionKeyTest {
    @Test
    void savedDataMixinRunsOnDedicatedServers() throws IOException {
        final JsonObject config = JsonParser.parseReader(Files.newBufferedReader(
                Path.of("src/main/resources/better_content_fixes.mixins.json"))).getAsJsonObject();
        assertTrue(config.getAsJsonArray("mixins").toString().contains("thefleshthathates.FleshBlockSpreadMixin"));
        assertFalse(config.getAsJsonArray("client").toString().contains("thefleshthathates.FleshBlockSpreadMixin"));
    }

    @Test
    void restoresPrintedKeysFromExistingWorldsAndWritesPackedKeys() {
        final BlockPos position = new BlockPos(99966, 66, 100141);
        assertEquals(position.asLong(), FleshSpreadPositionKey.decode(position.toString()));
        assertEquals(position.asLong(), FleshSpreadPositionKey.decode(FleshSpreadPositionKey.encode(position)));
    }

    @Test
    void rejectsUnrecognizedKeys() {
        assertThrows(NumberFormatException.class, () -> FleshSpreadPositionKey.decode("BlockPos{x=1, y=2}"));
    }
}
