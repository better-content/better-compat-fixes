package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Vanilla operation order with UUID deduplication, without an AttributeInstance/player. */
public final class IntrinsicAttributeMath {
    public record Contribution(UUID id, double amount, int operation) {}
    private IntrinsicAttributeMath() {}
    public static double apply(double baseline, Collection<Contribution> contributions) {
        var unique = new LinkedHashMap<UUID, Contribution>();
        contributions.forEach(c -> unique.put(c.id(), c));
        double added = baseline;
        for (var c : unique.values()) if (c.operation() == 0) added += c.amount();
        double result = added;
        for (var c : unique.values()) if (c.operation() == 1) result += added * c.amount();
        for (var c : unique.values()) if (c.operation() == 2) result *= 1 + c.amount();
        return result;
    }
}
