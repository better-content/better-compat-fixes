package com.bettercontent.bettercompatfixes.compat;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Removes equal placed features that vanilla assigns the same graph index across generation steps. */
public final class FeatureOrderSanitizer {
    private FeatureOrderSanitizer() {
    }

    public static List<HolderSet<PlacedFeature>> deduplicate(
            final List<HolderSet<PlacedFeature>> steps
    ) {
        final Object2IntOpenHashMap<PlacedFeature> indexes = new Object2IntOpenHashMap<>();
        indexes.defaultReturnValue(-1);
        final List<HolderSet<PlacedFeature>> sanitized = new ArrayList<>(steps.size());
        boolean changed = false;
        for (final HolderSet<PlacedFeature> step : steps) {
            final List<Holder<PlacedFeature>> retained = new ArrayList<>();
            for (final Holder<PlacedFeature> holder : step) {
                final int previous = indexes.putIfAbsent(holder.value(), indexes.size());
                if (previous == -1) retained.add(holder);
                else changed = true;
            }
            sanitized.add(retained.size() == step.size() ? step : HolderSet.direct(retained));
        }
        return changed ? List.copyOf(sanitized) : steps;
    }

    static <T> List<List<T>> deduplicateByIdentity(
            final List<List<T>> steps,
            final Function<T, ?> identity
    ) {
        final Set<Object> seen = new HashSet<>();
        final List<List<T>> sanitized = new ArrayList<>(steps.size());
        boolean changed = false;

        for (final List<T> step : steps) {
            final List<T> retained = new ArrayList<>(step.size());
            for (final T value : step) {
                if (seen.add(identity.apply(value))) {
                    retained.add(value);
                } else {
                    changed = true;
                }
            }
            sanitized.add(retained.size() == step.size() ? step : List.copyOf(retained));
        }
        return changed ? List.copyOf(sanitized) : steps;
    }
}
