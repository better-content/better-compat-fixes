package com.bettercontent.bettercompatfixes.compat;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

/** Detach a pending read while its NBT is still owned by the I/O mailbox. */
public final class PendingChunkReadSnapshots {
    private PendingChunkReadSnapshots() {}

    @Nullable
    public static CompoundTag detach(@Nullable CompoundTag pending) {
        // Null represents a queued deletion, not an empty chunk. CompoundTag.copy also
        // detaches nested compounds, lists and mutable primitive-array payloads.
        return pending == null ? null : pending.copy();
    }
}
