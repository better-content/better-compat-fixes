package com.bettercontent.bettercompatfixes.compat;

/** Fractional chips per completed block, not per frame or swing. */
public final class ChoppingProgress {
    public static final int CHIPS_PER_BLOCK = 48;
    private double remainder;
    private double previous;

    public int advance(double progress) {
        if (!Double.isFinite(progress) || progress < 0) {
            reset();
            return 0;
        }
        double clamped = Math.min(1, progress);
        if (clamped < previous) {
            reset();
            previous = clamped;
            return 0;
        }
        remainder += (clamped - previous) * CHIPS_PER_BLOCK;
        previous = clamped;
        int chips = (int) Math.floor(remainder + 1e-6);
        remainder = Math.max(0, remainder - chips);
        return chips;
    }

    public void reset() {
        remainder = 0;
        previous = 0;
    }
}
