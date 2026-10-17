package com.bettercontent.bettercompatfixes.compat.epicfight;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class StyleComboPolicyTest {
    private static final List<String> MOTIONS = List.of("one", "two", "three", "dash", "air");

    @Test void ordinaryAttacksAdvanceThroughEntireComboAndWrap() {
        for (int cycle = 0; cycle < 3; cycle++) {
            for (int index = 0; index < 3; index++) {
                assertEquals((index + 1) % 3,
                        StyleComboPolicy.counter(MOTIONS, MOTIONS.get(index), (index + 1) % 3));
            }
        }
    }

    @Test void specialAttacksResetIncludingReusedClips() {
        assertEquals(0, StyleComboPolicy.counter(MOTIONS, "dash", 2));
        assertEquals(0, StyleComboPolicy.counter(MOTIONS, "air", 2));
        assertEquals(0, StyleComboPolicy.counter(List.of("shared", "two", "shared", "air"), "shared", 1));
    }

    @Test void unrelatedMissingAndMalformedInputsDelegate() {
        assertNull(StyleComboPolicy.counter(MOTIONS, "other weapon", 2));
        assertNull(StyleComboPolicy.counter(MOTIONS, null, 2));
        assertNull(StyleComboPolicy.counter(List.of("one", "two"), "one", 1));
        assertNull(StyleComboPolicy.counter(List.of(), "one", 1));
    }
}
