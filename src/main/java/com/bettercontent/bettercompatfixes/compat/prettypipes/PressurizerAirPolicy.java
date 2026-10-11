package com.bettercontent.bettercompatfixes.compat.prettypipes;

/** Fixed pneumatic balance, independent of client state and electrical units. */
public final class PressurizerAirPolicy {
    public static final int VOLUME = 1_000;
    public static final float MIN_WORKING_PRESSURE = 2.0F;
    public static final int AIR_PER_ITEM = 10;
    private PressurizerAirPolicy() {}

    public static int cost(int count) {
        long cost = (long) count * AIR_PER_ITEM;
        return count > 0 && cost <= Integer.MAX_VALUE ? (int) cost : -1;
    }

    public static boolean canPay(int air, int volume, int count) {
        int cost = cost(count);
        return cost > 0 && volume > 0
                && (long) air - cost >= (long) Math.ceil(volume * (double) MIN_WORKING_PRESSURE);
    }

    public static int low(int air) { return air & 0xffff; }
    public static int high(int air) { return air >>> 16; }
    public static int combine(int low, int high) { return (low & 0xffff) | ((high & 0xffff) << 16); }
}
