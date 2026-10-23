package com.bettercontent.bettercompatfixes.compat.bees;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColonyAppearanceTest {
    private static CompoundTag valid() { var tag = new CompoundTag(); new ColonyAppearance(true, true, 0x03fce8).write(tag); return tag; }
    @Test void canonicalStatesRoundtripAndKeepOldBit() {
        for (var state : new ColonyAppearance[]{ColonyAppearance.INACTIVE, new ColonyAppearance(true, false, 0),
                new ColonyAppearance(true, true, 0), new ColonyAppearance(true, true, 0xffffff), new ColonyAppearance(true, true, 0xfc030f)}) {
            var tag = new CompoundTag(); state.write(tag);
            assertEquals(state, ColonyAppearance.read(tag)); assertEquals(state.active(), tag.getBoolean(ColonyAppearance.LEGACY_ACTIVE));
        }
    }
    @Test void rejectNoncanonicalConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new ColonyAppearance(false, true, 0));
        assertThrows(IllegalArgumentException.class, () -> new ColonyAppearance(true, false, 1));
        assertThrows(IllegalArgumentException.class, () -> new ColonyAppearance(true, true, -1));
        assertThrows(IllegalArgumentException.class, () -> new ColonyAppearance(true, true, 0x1000000));
    }
    @Test void legacyOrMissingNeverCreatesTint() {
        var tag = new CompoundTag(); assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
        tag.putBoolean(ColonyAppearance.LEGACY_ACTIVE, true);
        assertEquals(new ColonyAppearance(true, false, 0), ColonyAppearance.read(tag));
        tag.putInt(ColonyAppearance.LEGACY_ACTIVE, 1); assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
    }
    @Test void malformedVersionTypeMissingExtraAndLegacyMismatchClear() {
        for (String field : new String[]{"version", "active", "known", "rgb"}) {
            var tag = valid(); tag.getCompound(ColonyAppearance.KEY).remove(field);
            assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
            tag = valid(); tag.getCompound(ColonyAppearance.KEY).putString(field, "wrong_type");
            assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
        }
        var tag = valid(); tag.getCompound(ColonyAppearance.KEY).putInt("version", 2); assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
        tag = valid(); tag.getCompound(ColonyAppearance.KEY).putInt("extra", 1); assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
        tag = valid(); tag.putBoolean(ColonyAppearance.LEGACY_ACTIVE, false); assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
        tag = valid(); tag.putInt(ColonyAppearance.KEY, 1); assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
    }
    @Test void malformedFlagsAndRgbClearInsteadOfRetainingPreviousTint() {
        for (int rgb : new int[]{-1, 0x1000000}) { var tag = valid(); tag.getCompound(ColonyAppearance.KEY).putInt("rgb", rgb); assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag)); }
        var tag = valid(); tag.getCompound(ColonyAppearance.KEY).putByte("known", (byte) 2); assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
        tag = valid(); tag.getCompound(ColonyAppearance.KEY).putBoolean("active", false); tag.putBoolean(ColonyAppearance.LEGACY_ACTIVE, false); assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
        tag = valid(); tag.getCompound(ColonyAppearance.KEY).putBoolean("known", false); assertEquals(ColonyAppearance.INACTIVE, ColonyAppearance.read(tag));
    }
}
