package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.Set;

/** Recognizes only vanilla's non-data synthetic marker, never parses/coerces authored payload. */
public final class TreasureUrnPending {
    private TreasureUrnPending() { }
    public static boolean absentOrMinimal(CompoundTag tag, BlockPos pos) { return tag == null || minimalDummy(tag, pos); }
    public static boolean minimalDummy(CompoundTag tag, BlockPos pos) {
        if (tag == null || !tag.contains("id", Tag.TAG_STRING) || !tag.getString("id").equals("DUMMY")) return false;
        Set<String> keys = tag.getAllKeys();
        if (!keys.equals(Set.of("id", "x", "y", "z")) && !keys.equals(Set.of("id", "x", "y", "z", "keepPacked"))) return false;
        if (!tag.contains("x", Tag.TAG_INT) || !tag.contains("y", Tag.TAG_INT) || !tag.contains("z", Tag.TAG_INT)) return false;
        if (tag.getInt("x") != pos.getX() || tag.getInt("y") != pos.getY() || tag.getInt("z") != pos.getZ()) return false;
        return !keys.contains("keepPacked") || (tag.contains("keepPacked", Tag.TAG_BYTE) && tag.getByte("keepPacked") == 1);
    }
}
