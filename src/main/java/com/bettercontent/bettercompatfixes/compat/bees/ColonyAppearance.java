package com.bettercontent.bettercompatfixes.compat.bees;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.Set;

/** Strict, bounded cosmetic metadata; unrelated native/genetic tags are never inspected. */
public record ColonyAppearance(boolean active, boolean known, int rgb) {
    public static final String LEGACY_ACTIVE = "better_compat_fixes:colony_active";
    public static final String KEY = "better_compat_fixes:colony_appearance_v1";
    public static final int VERSION = 1;
    public static final ColonyAppearance INACTIVE = new ColonyAppearance(false, false, 0);
    private static final Set<String> FIELDS = Set.of("version", "active", "known", "rgb");
    public ColonyAppearance {
        if (rgb < 0 || rgb > 0xffffff || !known && rgb != 0 || known && !active)
            throw new IllegalArgumentException("Noncanonical colony appearance");
    }
    public void write(CompoundTag tag) {
        tag.putBoolean(LEGACY_ACTIVE, active); // Keep the old bit and meaning for old readers.
        CompoundTag data = new CompoundTag();
        data.putInt("version", VERSION); data.putBoolean("active", active);
        data.putBoolean("known", known); data.putInt("rgb", rgb);
        tag.put(KEY, data);
    }
    public static ColonyAppearance read(CompoundTag tag) {
        if (!tag.contains(KEY)) return new ColonyAppearance(tag.contains(LEGACY_ACTIVE, Tag.TAG_BYTE)
                && tag.getByte(LEGACY_ACTIVE) == 1, false, 0); // Legacy can never invent a tint.
        if (!tag.contains(KEY, Tag.TAG_COMPOUND)) return INACTIVE;
        CompoundTag data = tag.getCompound(KEY);
        if (!data.getAllKeys().equals(FIELDS) || !data.contains("version", Tag.TAG_INT)
                || data.getInt("version") != VERSION || !data.contains("active", Tag.TAG_BYTE)
                || !data.contains("known", Tag.TAG_BYTE) || !data.contains("rgb", Tag.TAG_INT)) return INACTIVE;
        int a = data.getByte("active"), k = data.getByte("known"), rgb = data.getInt("rgb");
        if (a < 0 || a > 1 || k < 0 || k > 1 || rgb < 0 || rgb > 0xffffff
                || k == 0 && rgb != 0 || k == 1 && a != 1) return INACTIVE;
        if (!tag.contains(LEGACY_ACTIVE, Tag.TAG_BYTE) || tag.getByte(LEGACY_ACTIVE) != a) return INACTIVE;
        return new ColonyAppearance(a == 1, k == 1, rgb);
    }
}
