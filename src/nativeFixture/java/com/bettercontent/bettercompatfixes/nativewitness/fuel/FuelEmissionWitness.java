package com.bettercontent.bettercompatfixes.nativewitness.fuel;

import java.util.HashMap;
import java.util.Map;

/** Nonshipping, same-thread observation only. Never replaces/cancels an upstream operation. */
public final class FuelEmissionWitness {
    private static final ThreadLocal<Sample> ACTIVE = new ThreadLocal<>();

    private FuelEmissionWitness() {}

    private static final class Sample {
        int completed;
        final Map<Object, Double> amounts = new HashMap<>();
    }

    public record Snapshot(int completed, Map<Object, Double> amounts) {
        public Snapshot {
            amounts = Map.copyOf(amounts);
        }

        public boolean positive() {
            return amounts.values().stream().anyMatch(value -> Double.isFinite(value) && value > 0);
        }

        public boolean equivalent(Snapshot other) {
            if (completed != other.completed || !amounts.keySet().equals(other.amounts.keySet())) return false;
            return amounts.entrySet().stream().allMatch(entry -> {
                double value = entry.getValue(), expected = other.amounts.get(entry.getKey());
                return Double.isFinite(value) && Double.isFinite(expected)
                        && Math.abs(value - expected) <= 1e-10 * Math.max(1.0, Math.abs(expected));
            });
        }
    }

    public static Snapshot observe(Runnable operation) {
        if (ACTIVE.get() != null) throw new IllegalStateException("Nested fuel observation scope");
        var sample = new Sample();
        ACTIVE.set(sample);
        try {
            operation.run();
            return new Snapshot(sample.completed, sample.amounts);
        } finally {
            ACTIVE.remove();
        }
    }

    public static void completed() {
        var sample = ACTIVE.get();
        if (sample != null) sample.completed++;
    }

    /** int output plus real native fractional-state delta reconstructs effective emission. */
    public static void amount(Object pollutant, int integerOutput, double before, double after) {
        var sample = ACTIVE.get();
        if (sample != null) sample.amounts.merge(pollutant, integerOutput + after - before, Double::sum);
    }
}
