package com.bettercontent.bettercompatfixes.compat;

/** Pure selection rule for matching dynamic branch hardness to its native log. */
public final class DynamicTreesBranchHardnessPolicy {
    public static final float DYNAMIC_TREES_DEFAULT_HARDNESS = 2.0F;

    private DynamicTreesBranchHardnessPolicy() {
    }

    public static float resolve(final Float primitiveLogHardness) {
        if (primitiveLogHardness == null || !Float.isFinite(primitiveLogHardness)) {
            return DYNAMIC_TREES_DEFAULT_HARDNESS;
        }
        return primitiveLogHardness;
    }
}
