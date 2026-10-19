package com.bettercontent.bettercompatfixes.client;

/** Select inputs only; native Epic ParCool still selects and plays its own animation. */
public final class ArrowDodgeAnimationInputs {
    private ArrowDodgeAnimationInputs() {
    }

    public static float impulse(final float vanilla, final double arrow, final boolean requested) {
        return requested ? (float) arrow : vanilla;
    }
}
