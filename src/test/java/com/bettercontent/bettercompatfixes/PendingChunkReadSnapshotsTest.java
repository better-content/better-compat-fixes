package com.bettercontent.bettercompatfixes;

import com.bettercontent.bettercompatfixes.compat.PendingChunkReadSnapshots;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PendingChunkReadSnapshotsTest {
    // UNKNOWN safety bound for a controlled latch handshake, not a performance baseline.
    private static final long HANDSHAKE_SAFETY_SECONDS = 30;

    @Test void preservesDeletionSentinel() {
        assertNull(PendingChunkReadSnapshots.detach(null));
    }

    @Test void detachesRootNestedListsAndPrimitiveArraysWithoutChangingPayload() {
        CompoundTag stored = root();
        CompoundTag nested = new CompoundTag();
        nested.putString("nested", "original");
        ListTag list = new ListTag();
        list.add(nested);
        stored.put("list", list);
        stored.putIntArray("array", new int[] {1, 2, 3});
        CompoundTag expected = stored.copy();
        CompoundTag read = PendingChunkReadSnapshots.detach(stored);
        assertNotSame(stored, read);
        assertEquals(expected, read);
        read.put("__context", new CompoundTag());
        read.getList("list", 10).getCompound(0).putString("nested", "reader");
        read.getIntArray("array")[0] = 99;
        assertEquals(expected, stored);
        assertFalse(stored.contains("__context"));
    }

    @Test void oldPendingAliasDeterministicallyBreaksRootSerialization() throws Exception {
        CompoundTag stored = root();
        Throwable failure = interleave(stored, stored);
        assertInstanceOf(ConcurrentModificationException.class, failure);
        assertEquals(1, java.util.Arrays.stream(failure.getStackTrace())
                .filter(f -> f.getClassName().equals(CompoundTag.class.getName())
                        && f.getMethodName().equals("write")).count());
    }

    @Test void detachedPendingReadCannotMutateRootDuringSerialization() throws Exception {
        CompoundTag stored = root();
        CompoundTag expected = stored.copy();
        assertNull(interleave(stored, PendingChunkReadSnapshots.detach(stored)));
        assertEquals(expected, stored);
    }

    private static CompoundTag root() {
        CompoundTag root = new CompoundTag();
        for (int i = 0; i < 16; i++) root.putInt("key" + i, i);
        return root;
    }

    private static Throwable interleave(CompoundTag stored, CompoundTag read) throws Exception {
        GateOutput output = new GateOutput();
        CompletableFuture<Throwable> writer = CompletableFuture.supplyAsync(() -> {
            try { NbtIo.write(stored, output); return null; }
            catch (Throwable failure) { return failure; }
        });
        try {
            output.entered.get(HANDSHAKE_SAFETY_SECONDS, TimeUnit.SECONDS);
            // Exactly the root-key mutation performed before chunk data fixing.
            read.put("__context", new CompoundTag());
        } finally {
            output.release.complete(null);
        }
        return writer.get(HANDSHAKE_SAFETY_SECONDS, TimeUnit.SECONDS);
    }

    /** Pause after the root iterator has selected its first named entry. No sleeps/races. */
    private static final class GateOutput implements DataOutput {
        final CompletableFuture<Void> entered = new CompletableFuture<>();
        final CompletableFuture<Void> release = new CompletableFuture<>();
        final DataOutputStream out = new DataOutputStream(new ByteArrayOutputStream());
        @Override public void writeUTF(String value) throws IOException {
            if (!value.isEmpty() && entered.complete(null)) release.join();
            out.writeUTF(value);
        }
        @Override public void write(int value) throws IOException { out.write(value); }
        @Override public void write(byte[] value) throws IOException { out.write(value); }
        @Override public void write(byte[] value, int start, int length) throws IOException { out.write(value, start, length); }
        @Override public void writeBoolean(boolean value) throws IOException { out.writeBoolean(value); }
        @Override public void writeByte(int value) throws IOException { out.writeByte(value); }
        @Override public void writeShort(int value) throws IOException { out.writeShort(value); }
        @Override public void writeChar(int value) throws IOException { out.writeChar(value); }
        @Override public void writeInt(int value) throws IOException { out.writeInt(value); }
        @Override public void writeLong(long value) throws IOException { out.writeLong(value); }
        @Override public void writeFloat(float value) throws IOException { out.writeFloat(value); }
        @Override public void writeDouble(double value) throws IOException { out.writeDouble(value); }
        @Override public void writeBytes(String value) throws IOException { out.writeBytes(value); }
        @Override public void writeChars(String value) throws IOException { out.writeChars(value); }
    }
}
