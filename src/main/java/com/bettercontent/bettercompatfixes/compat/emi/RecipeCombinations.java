package com.bettercontent.bettercompatfixes.compat.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Deterministic bounded expansion. The caller must report a truncated result, never hide it. */
public final class RecipeCombinations {
    private RecipeCombinations() {}
    public static <T> boolean visit(List<List<T>> choices, int limit, Consumer<List<T>> consumer) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        if (choices.stream().anyMatch(List::isEmpty)) return true;
        long count = 1;
        for (var choice : choices) count = Math.min((long) limit + 1, count * choice.size());
        int[] emitted = {0};
        expand(choices, 0, new ArrayList<>(), limit, emitted, consumer);
        return count <= limit;
    }
    private static <T> void expand(List<List<T>> choices, int index, List<T> row, int limit,
                                   int[] emitted, Consumer<List<T>> consumer) {
        if (emitted[0] >= limit) return;
        if (index == choices.size()) { consumer.accept(List.copyOf(row)); emitted[0]++; return; }
        for (T value : choices.get(index)) {
            row.add(value);
            expand(choices, index + 1, row, limit, emitted, consumer);
            row.remove(row.size() - 1);
            if (emitted[0] >= limit) return;
        }
    }
}
