package com.bettercontent.bettercompatfixes.compat.emi;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class IngredientOrderTest {
    @Test void stableCompletePartitionNeverMutatesSourcesOrReevaluatesDuringBrowsing() {
        for (int mask = 0; mask < 256; mask++) {
            final int available = mask;
            List<Integer> source = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7));
            AtomicInteger calls = new AtomicInteger();
            List<Integer> ordered = IngredientOrder.availableFirst(source, i -> {
                calls.incrementAndGet(); return (available & (1 << i)) != 0;
            });
            List<Integer> expected = new ArrayList<>();
            source.stream().filter(i -> (available & (1 << i)) != 0).forEach(expected::add);
            source.stream().filter(i -> (available & (1 << i)) == 0).forEach(expected::add);
            assertEquals(expected, ordered);
            assertEquals(8, calls.get());
            assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), source);
            source.clear();
            assertEquals(expected, ordered);
            assertThrows(UnsupportedOperationException.class, () -> ordered.add(9));
        }
        assertEquals(List.of(), IngredientOrder.availableFirst(List.of(), x -> { throw new AssertionError(); }));
    }
}
