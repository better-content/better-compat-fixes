package com.bettercontent.bettercompatfixes.client;

/** Input routing only: MINE authorizes an attempt, never drops or server permission. */
public final class CombatMiningPolicy {
    public enum Route { NATIVE, COMBAT, MINE }
    public enum ItemKind { OTHER, COMBAT_FIRST, BUTCHER_KNIFE }
    public enum Target { NONE, BLOCK, LIVING, NONLIVING }

    private CombatMiningPolicy() {}

    public static Route decide(boolean ready, boolean epicFightMode, ItemKind item,
            Target selected, boolean lockedOn, boolean miningTargetMatches, boolean locallyMineable) {
        if (!ready || !epicFightMode || item == ItemKind.OTHER) return Route.NATIVE;
        // EF handles nonliving interactions before lock-on. Keep that ordering.
        if (selected == Target.NONLIVING) return Route.NATIVE;
        if (lockedOn) return Route.COMBAT;
        if (item == ItemKind.COMBAT_FIRST || selected != Target.BLOCK) return Route.COMBAT;
        return miningTargetMatches && locallyMineable ? Route.MINE : Route.COMBAT;
    }
}
