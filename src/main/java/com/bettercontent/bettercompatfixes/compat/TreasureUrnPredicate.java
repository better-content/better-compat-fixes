package com.bettercontent.bettercompatfixes.compat;

/** Lookup admission only. Global hasBlockEntity, native factories and real decoders stay unchanged. */
public final class TreasureUrnPredicate {
    private TreasureUrnPredicate() { }
    public static boolean skipLookup(boolean exactStatelessTreasureUrn, boolean existingLiveEntry, boolean absentOrMinimalMarker) {
        return exactStatelessTreasureUrn && !existingLiveEntry && absentOrMinimalMarker;
    }
}
