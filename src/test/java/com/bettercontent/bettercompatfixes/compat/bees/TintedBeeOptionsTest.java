package com.bettercontent.bettercompatfixes.compat.bees;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TintedBeeOptionsTest {
    @Test void rgb24AndBlackHaveExactlyFourVersionedBytesAndNoTrailingInput() {
        for (int rgb : new int[]{0, 0xffffff, 0x03fce8, 0xfc030f}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                var value = new TintedBeeOptions(rgb); value.writeToNetwork(buffer);
                assertEquals(4, buffer.readableBytes()); assertEquals(1, buffer.getUnsignedByte(0));
                assertEquals(value, TintedBeeOptions.DESERIALIZER.fromNetwork(null, buffer)); assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
    @Test void legacyEmptyEveryTruncationAndExtraBytesAreRejected() {
        for (int length : new int[]{0, 1, 2, 3, 5, 8}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try { buffer.writeZero(length); assertThrows(IllegalArgumentException.class, () -> TintedBeeOptions.DESERIALIZER.fromNetwork(null, buffer)); }
            finally { buffer.release(); }
        }
    }
    @Test void unsupportedVersionRejectedAndValuesCannotBeOutOfRange() {
        for (int version : new int[]{0, 2, 255}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try { buffer.writeByte(version); buffer.writeMedium(0); assertThrows(IllegalArgumentException.class, () -> TintedBeeOptions.DESERIALIZER.fromNetwork(null, buffer)); }
            finally { buffer.release(); }
        }
        assertThrows(IllegalArgumentException.class, () -> new TintedBeeOptions(-1));
        assertThrows(IllegalArgumentException.class, () -> new TintedBeeOptions(0x1000000));
    }
    @Test void codecRoundtripsAndRejectsNegativeLargeAndLegacyScalar() {
        for (int rgb : new int[]{0, 0xffffff, 0x03fce8, 0xfc030f}) {
            var value = new TintedBeeOptions(rgb);
            var data = TintedBeeOptions.CODEC.encodeStart(NbtOps.INSTANCE, value).result().orElseThrow();
            assertEquals(value, TintedBeeOptions.CODEC.parse(NbtOps.INSTANCE, data).result().orElseThrow());
        }
        assertTrue(TintedBeeOptions.CODEC.parse(NbtOps.INSTANCE, IntTag.valueOf(1)).error().isPresent());
        var tag = new net.minecraft.nbt.CompoundTag(); tag.putInt("rgb", -1); assertTrue(TintedBeeOptions.CODEC.parse(NbtOps.INSTANCE, tag).error().isPresent());
        tag.putInt("rgb", 0x1000000); assertTrue(TintedBeeOptions.CODEC.parse(NbtOps.INSTANCE, tag).error().isPresent());
    }
}
