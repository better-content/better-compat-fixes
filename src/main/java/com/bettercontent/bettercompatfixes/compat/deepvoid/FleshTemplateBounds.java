package com.bettercontent.bettercompatfixes.compat.deepvoid;

/** Checked whole-template X/Z translation; no world/native objects or retained state. */
public final class FleshTemplateBounds {
    private FleshTemplateBounds() {}

    public static Shift fit(int minX, int maxX, int minZ, int maxZ, int centerX, int centerZ, int radius) {
        require(radius == 1, "write-radius");
        require(minX <= maxX && minZ <= maxZ, "bounds-order");
        long lowX = ((long) centerX - radius) * 16 + 1;
        long highX = ((long) centerX + radius) * 16 + 14;
        long lowZ = ((long) centerZ - radius) * 16 + 1;
        long highZ = ((long) centerZ + radius) * 16 + 14;
        require(lowX >= Integer.MIN_VALUE && highX <= Integer.MAX_VALUE
                && lowZ >= Integer.MIN_VALUE && highZ <= Integer.MAX_VALUE, "envelope-overflow");
        return new Shift(delta(minX, maxX, lowX, highX), delta(minZ, maxZ, lowZ, highZ));
    }

    private static int delta(int min, int max, long low, long high) {
        long lower = low - min;
        long upper = high - max;
        require(lower <= upper, "oversize-template");
        long shift = Math.max(lower, Math.min(0L, upper));
        require(shift >= Integer.MIN_VALUE && shift <= Integer.MAX_VALUE, "shift-overflow");
        return (int) shift;
    }

    public static int translated(int coordinate, int delta) {
        long result = (long) coordinate + delta;
        require(result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE, "position-overflow");
        return (int) result;
    }

    public static void require(boolean supported, String reason) {
        if (!supported) throw new IllegalStateException("Unsupported DeepVoid flesh template: " + reason);
    }

    public record Shift(int x, int z) {
        public boolean unchanged() { return x == 0 && z == 0; }
    }
}
