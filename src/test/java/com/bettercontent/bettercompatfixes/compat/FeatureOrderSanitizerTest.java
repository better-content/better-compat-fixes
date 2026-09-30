package com.bettercontent.bettercompatfixes.compat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class FeatureOrderSanitizerTest {
    @Test
    void removesLaterOccurrencesAcrossGenerationSteps() {
        final Object first = new Object();
        final Object second = new Object();
        final List<List<Object>> steps = List.of(List.of(first, second), List.of(first));

        final List<List<Object>> sanitized = FeatureOrderSanitizer.deduplicateByIdentity(steps, value -> value);

        assertEquals(2, sanitized.get(0).size());
        assertEquals(0, sanitized.get(1).size());
    }

    @Test
    void preservesTheOriginalListWhenNoFeatureRepeats() {
        final List<List<Object>> steps = List.of(List.of(new Object()), List.of(new Object()));

        assertSame(steps, FeatureOrderSanitizer.deduplicateByIdentity(steps, value -> value));
    }

    @Test
    void removesDistinctObjectsThatShareVanillasFeatureEquality() {
        final String first = new String("equal feature");
        final String equalButDistinct = new String("equal feature");
        final List<List<String>> steps = List.of(List.of(first), List.of(equalButDistinct));

        final List<List<String>> sanitized = FeatureOrderSanitizer.deduplicateByIdentity(steps, value -> value);

        assertSame(first, sanitized.get(0).get(0));
        assertEquals(0, sanitized.get(1).size());
    }
}
