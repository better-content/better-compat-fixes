package com.bettercontent.bettercontentfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bettercontent.bettercontentfixes.compat.FleshSpreadPositionKey;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class FleshSpreadPositionKeyTest {
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
