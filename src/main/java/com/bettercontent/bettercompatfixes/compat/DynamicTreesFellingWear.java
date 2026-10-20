package com.bettercontent.bettercompatfixes.compat;

/** Nominal wear only: native ItemStack/Item hooks still own mitigation and break handling. */
public final class DynamicTreesFellingWear {
    private static final ThreadLocal<Charge> CURRENT = new ThreadLocal<>();

    private DynamicTreesFellingWear() {}

    public static int multiply(int nominal, int multiplier) {
        if (multiplier < 1 || multiplier > 100) {
            throw new IllegalArgumentException("Felling multiplier must be in [1, 100]");
        }
        return nominal <= 0 ? nominal : Math.multiplyExact(nominal, multiplier);
    }

    public static Scope enter(Object player, Object stack, int multiplier) {
        multiply(0, multiplier);
        Charge previous = CURRENT.get();
        CURRENT.set(new Charge(player, stack, multiplier));
        return () -> {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        };
    }

    public static int scale(Object player, Object stack, int nominal) {
        Charge charge = CURRENT.get();
        return charge != null && charge.player == player && charge.stack == stack
                ? multiply(nominal, charge.multiplier) : nominal;
    }

    private record Charge(Object player, Object stack, int multiplier) {}

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override void close();
    }
}
