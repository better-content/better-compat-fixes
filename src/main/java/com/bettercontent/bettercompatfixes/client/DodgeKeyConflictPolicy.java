package com.bettercontent.bettercompatfixes.client;

/** Arrow requests do not use (or compete for) either mod's direct dodge binding. */
public final class DodgeKeyConflictPolicy {
    private DodgeKeyConflictPolicy() {
    }

    public static <T> T skillForDirectKeyConflict(final T equippedSkill, final boolean arrowRequest) {
        return arrowRequest ? null : equippedSkill;
    }
}
