package com.bettercontent.bettercompatfixes.compat.bees;

/** Cosmetic activity, not a client inventory, production simulator, or bee entity. */
public interface ColonyActivity {
    boolean betterCompat$colonyActive();
    default boolean betterCompat$colonyColorKnown() { return false; }
    default int betterCompat$colonyColor() { return 0; }
    /** Existing native queen-change notification: primitives only, no new inventory read. */
    default void betterCompat$clearColonyColor() { }
}
