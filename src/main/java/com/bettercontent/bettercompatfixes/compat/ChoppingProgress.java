package com.bettercontent.bettercompatfixes.compat;

/** Linear completion-to-rate policy, integrated over native mining ticks, never render frames. */
public final class ChoppingProgress {
    public static final double MIN_CHIPS_PER_SECOND = 2;
    public static final double MAX_CHIPS_PER_SECOND = 18;
    public static final double NATIVE_TICK_SECONDS = 1.0 / 20;
    private double remainder;
    private double previous;

    public static double rate(double progress) {
        return MIN_CHIPS_PER_SECOND + (MAX_CHIPS_PER_SECOND - MIN_CHIPS_PER_SECOND)
                * Math.max(0, Math.min(1, progress));
    }

    public int advance(double progress, double elapsedSeconds) {
        if (!Double.isFinite(progress) || progress < 0 || !Double.isFinite(elapsedSeconds)
                || elapsedSeconds <= 0) {
            reset();
            return 0;
        }
        double clamped = Math.min(1, progress);
        if (clamped < previous) {
            reset();
            previous = clamped;
            return 0;
        }
        previous = clamped;
        remainder += rate(clamped) * elapsedSeconds;
        int chips = (int) Math.floor(remainder + 1e-6);
        remainder = Math.max(0, remainder - chips);
        return chips;
    }

    public void reset() {
        remainder = 0;
        previous = 0;
    }
}
